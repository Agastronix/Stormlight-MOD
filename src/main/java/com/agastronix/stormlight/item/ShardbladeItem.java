package com.agastronix.stormlight.item;

import com.agastronix.stormlight.StormlightArchiveMOD;
import com.agastronix.stormlight.shardblade.DeadeyeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import java.util.List;

public class ShardbladeItem extends SwordItem {
    public static final ResourceLocation REACH_ID =
            ResourceLocation.fromNamespaceAndPath(StormlightArchiveMOD.MODID, "shardblade_reach");
    public static final ResourceLocation SWEEP_ID =
            ResourceLocation.fromNamespaceAndPath(StormlightArchiveMOD.MODID, "shardblade_sweep");

    public ShardbladeItem(Tier tier, Item.Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Soul-sever flash (visual only).
        if (target.level() instanceof ServerLevel server) {
            server.sendParticles(
                    ParticleTypes.SOUL,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    12, 0.2, 0.4, 0.2, 0.0
            );
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    // Shardblades always shimmer (flip to false if you'd rather it not look enchanted).
    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getOrDefault(ModComponents.RADIANT.get(), false);
    }

    /** A dead blade: the plain Shardblade without the RADIANT component. Living blades never match. */
    public static boolean isDeadeye(ItemStack stack) {
        return stack.getItem() instanceof ShardbladeItem
                && !stack.getOrDefault(ModComponents.RADIANT.get(), false);
    }

    public static boolean isBondedDeadeye(ItemStack stack) {
        return isDeadeye(stack) && stack.has(ModComponents.DEADEYE_OWNER.get());
    }

    // Right-clicking an unbonded Deadeye offers the bond (server decides who may take it).
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isDeadeye(stack) || isBondedDeadeye(stack)) {
            return super.use(level, player, hand);
        }
        if (player instanceof ServerPlayer sp) {
            DeadeyeManager.offerBond(sp, hand);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (isDeadeye(stack)) {
            tooltip.add(Component.literal("Deadeye").withStyle(ChatFormatting.GRAY));
        }
    }

    // A bonded Deadeye is never meant to lie on the ground (tossing is blocked, death unbonds it),
    // so any that does is a stray copy and dissolves.
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide() && isBondedDeadeye(stack)) {
            entity.discard();
            return true;
        }
        return false;
    }

    // Blocks the enchanting table and random enchantment sources.
    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return false;
    }

    // Blocks the anvil + enchanted books.
    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return false;
    }

    public static ItemAttributeModifiers createShardbladeAttributes(double attackDamage, double attackSpeed, double extraReach) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, attackDamage, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, attackSpeed, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE,
                        new AttributeModifier(REACH_ID, extraReach, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                // Sweep damage is 1 + ratio × attack damage, so a ratio of 1 makes sweeps hit full force.
                .add(Attributes.SWEEPING_DAMAGE_RATIO,
                        new AttributeModifier(SWEEP_ID, 1.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }
}