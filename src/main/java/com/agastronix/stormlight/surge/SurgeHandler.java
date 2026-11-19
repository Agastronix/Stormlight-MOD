package com.agastronix.stormlight.surge;

import com.agastronix.stormlight.radiant.RadiantAttachments;
import com.agastronix.stormlight.radiant.RadiantOrder;
import com.agastronix.stormlight.resource.StormlightAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class SurgeHandler {
    public static final float GRAVITATION_COST = 1.0F;
    public static final double GRAVITATION_POWER = 2.5D;
    public static final float ADHESION_COST = 15.0F;
    public static final double ADHESION_RANGE = 8.0D;
    public static final int ADHESION_DURATION = 100;
    public static final float PROGRESSION_COST = 20.0F;
    public static final double PROGRESSION_RANGE = 8.0D;
    private static final int MAX_GROWTH_STEPS = 16;

    public static void activate(ServerPlayer player, int slot) {
        if (RadiantAttachments.getOath(player) < 2) {
            player.displayClientMessage(Component.literal("You have not yet spoken the Second Ideal."), true);
            return;
        }
        if (!RadiantAttachments.isRadiantActive(player)) {
            player.displayClientMessage(Component.literal("You must draw in Stormlight first (Radiant mode)."), true);
            return;
        }
        Surge surge = surgeFor(RadiantAttachments.getOrder(player), slot);
        if (surge == null) {
            player.displayClientMessage(Component.literal("Your order has no Surge there."), true);
            return;
        }
        switch (surge) {
            case GRAVITATION -> gravitation(player);
            case ADHESION -> adhesion(player);
            case PROGRESSION -> progression(player);
            case ABRASION -> {} // handled client-side
        }
    }

    private static Surge surgeFor(RadiantOrder order, int slot) {
        return switch (order) {
            case WINDRUNNER -> switch (slot) { case 0 -> Surge.GRAVITATION; case 1 -> Surge.ADHESION; default -> null; };
            case EDGEDANCER -> switch (slot) { case 1 -> Surge.PROGRESSION; default -> null; };
            default -> null;
        };
    }

    private static void gravitation(ServerPlayer player) {
        if (!StormlightAttachments.consume(player, GRAVITATION_COST)) {
            player.displayClientMessage(Component.literal("Not enough Stormlight."), true);
            return;
        }
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(look.scale(GRAVITATION_POWER));
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        if (player.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(),
                    10, 0.2, 0.1, 0.2, 0.02);
        }
    }

    private static void adhesion(ServerPlayer player) {
        LivingEntity target = raycastTarget(player, ADHESION_RANGE);
        if (target == null) { player.displayClientMessage(Component.literal("Nothing there to lash."), true); return; }
        if (!StormlightAttachments.consume(player, ADHESION_COST)) {
            player.displayClientMessage(Component.literal("Not enough Stormlight."), true); return;
        }
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ADHESION_DURATION, 9, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ADHESION_DURATION, 2, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, ADHESION_DURATION, 0, false, false));
        if (player.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.5,
                    target.getZ(), 20, 0.3, 0.4, 0.3, 0.1);
        }
    }

    private static void progression(ServerPlayer player) {
        LivingEntity target = raycastTarget(player, PROGRESSION_RANGE);
        HitResult blockHit = player.pick(PROGRESSION_RANGE, 1.0F, false);
        // A plant in front of whatever creature is behind it takes priority.
        if (blockHit instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK
                && (target == null || player.getEyePosition().distanceToSqr(bhr.getLocation())
                        < player.getEyePosition().distanceToSqr(target.position()))
                && growPlant(player, bhr.getBlockPos())) {
            return;
        }
        if (target == null) { player.displayClientMessage(Component.literal("Nothing there to mend."), true); return; }
        if (target.getHealth() >= target.getMaxHealth()) {
            player.displayClientMessage(Component.literal("It is already whole."), true); return;
        }
        if (!StormlightAttachments.consume(player, PROGRESSION_COST)) {
            player.displayClientMessage(Component.literal("Not enough Stormlight."), true); return;
        }
        target.setHealth(target.getMaxHealth()); // Regrowth — mended to full
        if (player.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + target.getBbHeight() * 0.5,
                    target.getZ(), 12, 0.4, 0.5, 0.4, 0.0);
        }
    }

    /**
     * Regrowth on a plant: bonemeals it repeatedly until it's fully grown (crops to max age,
     * saplings into trees). Returns false if the block isn't something that can grow.
     */
    private static boolean growPlant(ServerPlayer player, BlockPos pos) {
        if (!(player.level() instanceof ServerLevel server)) return false;
        BlockState state = server.getBlockState(pos);
        if (!(state.getBlock() instanceof BonemealableBlock plant) || !plant.isValidBonemealTarget(server, pos, state)) {
            return false;
        }
        if (!StormlightAttachments.consume(player, PROGRESSION_COST)) {
            player.displayClientMessage(Component.literal("Not enough Stormlight."), true);
            return true;
        }
        // Stop once it's fully grown, turned into something else (sapling -> tree), or stopped
        // changing (grass blocks stay grass, so they only get one burst of growth).
        for (int i = 0; i < MAX_GROWTH_STEPS; i++) {
            state = server.getBlockState(pos);
            if (!(state.getBlock() instanceof BonemealableBlock b) || !b.isValidBonemealTarget(server, pos, state)) break;
            b.performBonemeal(server, server.random, pos, state);
            if (server.getBlockState(pos) == state) break;
        }
        server.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                15, 0.4, 0.4, 0.4, 0.0);
        return true;
    }

    private static LivingEntity raycastTarget(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        AABB box = player.getBoundingBox().expandTowards(player.getLookAngle().scale(range)).inflate(1.0);
        LivingEntity best = null;
        double bestDist = range * range;
        for (Entity e : player.level().getEntities(player, box, x -> x instanceof LivingEntity && x.isAlive())) {
            Optional<Vec3> clip = e.getBoundingBox().inflate(0.3).clip(eye, end);
            if (clip.isPresent()) {
                double d = eye.distanceToSqr(clip.get());
                if (d < bestDist) { bestDist = d; best = (LivingEntity) e; }
            }
        }
        return best;
    }
}