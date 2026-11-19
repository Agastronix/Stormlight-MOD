package com.agastronix.stormlight.shardblade;

import com.agastronix.stormlight.StormlightArchiveMOD;
import com.agastronix.stormlight.entity.BondingSpren;
import com.agastronix.stormlight.entity.ModEntities;
import com.agastronix.stormlight.item.ModComponents;
import com.agastronix.stormlight.item.ModItems;
import com.agastronix.stormlight.radiant.RadiantAttachments;
import com.agastronix.stormlight.radiant.RadiantOrder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = StormlightArchiveMOD.MODID)
public class ShardbladeManager {

    public static void toggle(ServerPlayer player) {
        // Non-Radiants can only wield a Deadeye; everything below is the living blade, unchanged.
        if (!RadiantAttachments.isRadiant(player)) {
            DeadeyeManager.toggle(player);
            return;
        }
        if (RadiantAttachments.getOath(player) < 3) {
            player.displayClientMessage(Component.literal("You have not sworn the Third Ideal."), true);
            return;
        }
        if (RadiantAttachments.isShardbladeSummoned(player)) {
            dismiss(player);
        } else {
            summon(player);
        }
    }

    private static void summon(ServerPlayer player) {
        if (!player.getMainHandItem().isEmpty()) {
            player.displayClientMessage(Component.literal("Your hand must be empty to summon your Blade."), true);
            return;
        }
        despawnBondedSpren(player); // the spren becomes the Blade

        ItemStack blade = new ItemStack(ModItems.SHARDBLADE.get());
        blade.set(ModComponents.RADIANT.get(), true);
        player.setItemInHand(InteractionHand.MAIN_HAND, blade);
        RadiantAttachments.setShardbladeSummoned(player, true);
        mist(player);
    }

    private static void dismiss(ServerPlayer player) {
        removeShardblades(player);
        RadiantAttachments.setShardbladeSummoned(player, false);
        spawnBondedSpren(player); // the spren returns
        mist(player);
    }

    private static boolean isRadiantBlade(ItemStack stack) {
        return stack.is(ModItems.SHARDBLADE.get())
                && stack.getOrDefault(ModComponents.RADIANT.get(), false);
    }

    private static void removeShardblades(ServerPlayer player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (isRadiantBlade(inv.getItem(i))) inv.setItem(i, ItemStack.EMPTY);
        }
    }

    /** The player's bonded spren, whichever order they belong to. */
    private static Mob findBondedSpren(ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(48.0);
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, box,
                m -> m instanceof BondingSpren s && s.isBonded() && s.isOwnedBy(player))) {
            return mob;
        }
        return null;
    }

    /** The spren that belongs to an order, so a dismissed Blade returns the right one. */
    private static EntityType<? extends Mob> sprenTypeFor(RadiantOrder order) {
        return switch (order) {
            case EDGEDANCER -> ModEntities.CULTIVATION_SPREN.get();
            default -> ModEntities.HONOR_SPREN.get();
        };
    }

    private static void despawnBondedSpren(ServerPlayer player) {
        Mob spren = findBondedSpren(player);
        if (spren != null) {
            if (player.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.END_ROD, spren.getX(), spren.getY() + 0.3, spren.getZ(),
                        15, 0.2, 0.3, 0.2, 0.02);
            }
            spren.discard();
        }
    }

    private static void spawnBondedSpren(ServerPlayer player) {
        if (findBondedSpren(player) != null) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        Mob spren = sprenTypeFor(RadiantAttachments.getOrder(player)).create(level);
        if (!(spren instanceof BondingSpren bond)) return;
        Vec3 pos = player.position().add(0, 1.2, 0);
        spren.moveTo(pos.x, pos.y, pos.z, player.getYRot(), 0);
        bond.setOwner(player);
        level.addFreshEntity(spren);
    }

    static void mist(ServerPlayer player) {
        if (player.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(),
                    15, 0.3, 0.5, 0.3, 0.02);
        }
    }

    // --- Undroppable + no loot ---
    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        if (isRadiantBlade(event.getEntity().getItem())) {
            event.setCanceled(true);
            event.getPlayer().getInventory().add(event.getEntity().getItem());
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            event.getDrops().removeIf(itemEntity -> isRadiantBlade(itemEntity.getItem()));
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RadiantAttachments.setShardbladeSummoned(player, false);
            removeShardblades(player); // clears any leftover after a keepInventory death
        }
    }
}