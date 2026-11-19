package com.agastronix.stormlight.shardblade;

import com.agastronix.stormlight.StormlightArchiveMOD;
import com.agastronix.stormlight.item.ModComponents;
import com.agastronix.stormlight.item.ShardbladeItem;
import com.agastronix.stormlight.radiant.RadiantAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Deadeye Shardblades: the plain SHARDBLADE item without the RADIANT component.
 *
 * <p>Anyone who is not a Radiant may bond one. The bonded blade lives in
 * {@link RadiantAttachments#DEADEYE_BLADE}; while summoned, a copy marked with
 * {@link ModComponents#DEADEYE_OWNER} sits in the owner's inventory. Living blades never pass
 * {@link ShardbladeItem#isDeadeye}, so nothing here touches them.
 */
@EventBusSubscriber(modid = StormlightArchiveMOD.MODID)
public class DeadeyeManager {
    public static final int SUMMON_TICKS = 6 * 20;          // summon charge: 6 seconds
    public static final int HEARTBEAT_INTERVAL = 20;        // one heartbeat per second of the charge
    // Refreshed every second; the duration stays above the 60 ticks below which the wobble eases off.
    public static final int NAUSEA_TICKS = 100;
    public static final int NAUSEA_REFRESH_INTERVAL = 20;

    // --- Bonding ---

    /** Right-click on an unbonded Deadeye. */
    public static void offerBond(ServerPlayer player, InteractionHand hand) {
        if (RadiantAttachments.isRadiant(player)) {
            player.displayClientMessage(Component.literal("The dead blade will not bond with a Radiant."), true);
        } else if (RadiantAttachments.hasDeadeye(player)) {
            player.displayClientMessage(Component.literal("You are already bonded to a Shardblade."), true);
        } else {
            PacketDistributor.sendToPlayer(player, new OpenDeadeyeBondPayload(hand));
        }
    }

    /** The Bond button. Server authority: every check is repeated, the client is never trusted. */
    public static void bond(ServerPlayer player, InteractionHand hand) {
        if (RadiantAttachments.isRadiant(player) || RadiantAttachments.hasDeadeye(player)) return;
        ItemStack held = player.getItemInHand(hand);
        if (!ShardbladeItem.isDeadeye(held) || ShardbladeItem.isBondedDeadeye(held)) {
            player.displayClientMessage(Component.literal("You must hold the blade to bond it."), true);
            return;
        }
        ItemStack blade = held.copyWithCount(1);
        blade.set(ModComponents.DEADEYE_OWNER.get(), player.getUUID());
        held.shrink(1);
        RadiantAttachments.setDeadeye(player, blade);
        RadiantAttachments.setDeadeyeSummoned(player, false);
        player.displayClientMessage(Component.literal("The blade is bonded to you."), true);
        ShardbladeManager.mist(player);
    }

    /** Breaks the bond, leaving the blade with the player as an ordinary unbonded item. */
    public static void releaseBond(ServerPlayer player) {
        if (!RadiantAttachments.hasDeadeye(player)) return;

        // Prefer the summoned copy, converted where it sits, so a held blade stays in hand.
        Inventory inv = player.getInventory();
        int slot = -1;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (isOwnCopy(inv.getItem(i), player)) {
                slot = i;
                break;
            }
        }
        ItemStack blade = slot >= 0 ? inv.getItem(slot).copy() : RadiantAttachments.getDeadeye(player).copy();
        takeOwnCopies(player);
        clearBond(player);

        blade.remove(ModComponents.DEADEYE_OWNER.get());
        if (slot >= 0) {
            inv.setItem(slot, blade);
        } else if (!inv.add(blade)) {
            player.drop(blade, false);
        }
        player.displayClientMessage(Component.literal("Your bond with the dead blade breaks."), true);
    }

    private static void clearBond(ServerPlayer player) {
        RadiantAttachments.setDeadeye(player, ItemStack.EMPTY);
        RadiantAttachments.setDeadeyeSummoned(player, false);
        RadiantAttachments.setDeadeyeCharge(player, 0);
    }

    // --- Summon / dismiss (X) ---

    /** X for a non-Radiant. */
    public static void toggle(ServerPlayer player) {
        if (RadiantAttachments.getDeadeyeCharge(player) > 0) {
            RadiantAttachments.setDeadeyeCharge(player, 0);
            player.displayClientMessage(Component.literal("You let the summoning fade."), true);
        } else if (RadiantAttachments.isDeadeyeSummoned(player)) {
            dismiss(player);
        } else if (RadiantAttachments.hasDeadeye(player)) {
            RadiantAttachments.setDeadeyeCharge(player, SUMMON_TICKS);
            heartbeat(player);
            player.displayClientMessage(Component.literal("You begin to summon your Blade..."), true);
        } else {
            player.displayClientMessage(Component.literal("You have no bonded Shardblade."), true);
        }
    }

    private static void tickCharge(ServerPlayer player) {
        int ticks = RadiantAttachments.getDeadeyeCharge(player);
        if (ticks <= 0) return;
        if (!RadiantAttachments.hasDeadeye(player) || RadiantAttachments.isDeadeyeSummoned(player)) {
            RadiantAttachments.setDeadeyeCharge(player, 0);
            return;
        }
        ticks--;
        RadiantAttachments.setDeadeyeCharge(player, ticks);
        if (ticks == 0) {
            finishSummon(player);
        } else if (ticks % HEARTBEAT_INTERVAL == 0) {
            heartbeat(player);
        }
    }

    private static void finishSummon(ServerPlayer player) {
        if (!player.getMainHandItem().isEmpty()) {
            player.displayClientMessage(Component.literal("Your hand must be empty to summon your Blade."), true);
            return;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, RadiantAttachments.getDeadeye(player).copy());
        RadiantAttachments.setDeadeyeSummoned(player, true);
        ShardbladeManager.mist(player);
    }

    private static void dismiss(ServerPlayer player) {
        ItemStack blade = takeOwnCopies(player);
        if (!blade.isEmpty()) {
            RadiantAttachments.setDeadeye(player, blade); // keeps an anvil rename
        }
        RadiantAttachments.setDeadeyeSummoned(player, false);
        ShardbladeManager.mist(player);
    }

    private static void heartbeat(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    // --- Copy bookkeeping ---

    private static boolean isOwnCopy(ItemStack stack, Player player) {
        return ShardbladeItem.isBondedDeadeye(stack)
                && player.getUUID().equals(stack.get(ModComponents.DEADEYE_OWNER.get()));
    }

    /** Removes every copy of the player's bonded blade from their inventory and cursor, returning the first. */
    private static ItemStack takeOwnCopies(ServerPlayer player) {
        ItemStack first = ItemStack.EMPTY;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (isOwnCopy(stack, player)) {
                if (first.isEmpty()) first = stack.copy();
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        ItemStack carried = player.containerMenu.getCarried();
        if (isOwnCopy(carried, player)) {
            if (first.isEmpty()) first = carried.copy();
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
        return first;
    }

    /**
     * Keeps exactly one bonded copy in play. Strays (left in a chest, kept through a keepInventory
     * death, or someone else's) dissolve, and a summoned blade that has left the owner's inventory
     * returns to storage.
     */
    private static void reconcile(ServerPlayer player) {
        boolean summoned = RadiantAttachments.isDeadeyeSummoned(player) && RadiantAttachments.hasDeadeye(player);
        boolean kept = false;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!ShardbladeItem.isBondedDeadeye(stack)) continue;
            if (summoned && !kept && isOwnCopy(stack, player)) {
                kept = true;
            } else {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        ItemStack carried = player.containerMenu.getCarried();
        if (ShardbladeItem.isBondedDeadeye(carried)) {
            if (summoned && !kept && isOwnCopy(carried, player)) {
                kept = true;
            } else {
                player.containerMenu.setCarried(ItemStack.EMPTY);
            }
        }
        if (summoned && !kept) {
            RadiantAttachments.setDeadeyeSummoned(player, false);
        }
    }

    private static boolean carriesDeadeye(ServerPlayer player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (ShardbladeItem.isDeadeye(inv.getItem(i))) return true;
        }
        return ShardbladeItem.isDeadeye(player.containerMenu.getCarried());
    }

    // --- Events ---

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Belt and braces: however someone became Radiant, they can't keep a Deadeye bond.
        if (RadiantAttachments.isRadiant(player)) {
            releaseBond(player);
        }

        tickCharge(player);
        reconcile(player);

        // A dead blade sickens a Radiant who carries it.
        if (player.tickCount % NAUSEA_REFRESH_INTERVAL == 0
                && RadiantAttachments.isRadiant(player) && carriesDeadeye(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NAUSEA_TICKS, 0, false, false, true));
        }
    }

    // A summoned Deadeye can't be thrown away. If it can't go back in, reconcile returns it to storage.
    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        if (ShardbladeItem.isBondedDeadeye(stack)) {
            event.setCanceled(true);
            event.getPlayer().getInventory().add(stack);
        }
    }

    // Death breaks the bond; the blade drops where they fell as an ordinary item, summoned or stored.
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack blade = RadiantAttachments.getDeadeye(player).copy();
        for (ItemEntity drop : event.getDrops()) {
            if (isOwnCopy(drop.getItem(), player)) {
                blade = drop.getItem().copy();
                break;
            }
        }
        event.getDrops().removeIf(drop -> ShardbladeItem.isBondedDeadeye(drop.getItem()));
        if (blade.isEmpty()) return;

        clearBond(player);
        blade.remove(ModComponents.DEADEYE_OWNER.get());
        ItemEntity drop = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), blade);
        drop.setDefaultPickUpDelay();
        event.getDrops().add(drop);
    }

    // --- Radiants and dead blades ---

    private static boolean radiantHoldingDeadeye(Player player) {
        return !player.level().isClientSide()
                && RadiantAttachments.isRadiant(player)
                && ShardbladeItem.isDeadeye(player.getMainHandItem());
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (radiantHoldingDeadeye(event.getEntity())) {
            event.setCanceled(true);
            event.getEntity().displayClientMessage(Component.literal("The dead blade will not answer you."), true);
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (radiantHoldingDeadeye(event.getPlayer())) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(Component.literal("The dead blade will not answer you."), true);
        }
    }

    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Post event) {
        Player player = event.getPlayer();
        if (RadiantAttachments.isRadiant(player) && ShardbladeItem.isDeadeye(event.getOriginalStack())) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
