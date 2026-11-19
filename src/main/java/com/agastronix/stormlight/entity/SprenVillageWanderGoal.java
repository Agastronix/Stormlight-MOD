package com.agastronix.stormlight.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.EnumSet;

public class SprenVillageWanderGoal extends Goal {
    private final HonorSprenEntity spren;
    private final double speed;
    private final int radius;
    private double tx, ty, tz;

    public SprenVillageWanderGoal(HonorSprenEntity spren, double speed, int radius) {
        this.spren = spren;
        this.speed = speed;
        this.radius = radius;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (spren.isBonded() || !spren.hasAnchor()) return false;
        if (!spren.getNavigation().isDone()) return false;
        if (spren.getRandom().nextInt(10) != 0) return false; // idle most ticks

        // Drawn toward an unbonded player who is inside the village.
        Player target = spren.findUnbondedPlayerInVillage(radius);
        if (target != null) {
            this.tx = target.getX();
            this.ty = target.getY();
            this.tz = target.getZ();
            return true;
        }

        // Otherwise pick a random low spot within the village.
        BlockPos anchor = spren.getAnchor();
        for (int i = 0; i < 8; i++) {
            int x = anchor.getX() + spren.getRandom().nextInt(radius * 2 + 1) - radius;
            int z = anchor.getZ() + spren.getRandom().nextInt(radius * 2 + 1) - radius;
            int y = spren.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos candidate = new BlockPos(x, y, z);
            if (candidate.distSqr(anchor) <= (double) radius * radius) {
                this.tx = x + 0.5;
                this.ty = y + 1.5;   // hover just above ground, within reach
                this.tz = z + 0.5;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return !spren.getNavigation().isDone() && !spren.isBonded();
    }

    @Override
    public void start() {
        spren.getNavigation().moveTo(tx, ty, tz, speed);
    }
}