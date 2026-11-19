package com.agastronix.stormlight.entity;

import com.agastronix.stormlight.radiant.RadiantAttachments;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * An unbonded spren is drawn toward the nearest player who has sworn no oath, so a
 * wandering spren will come to you rather than waiting to be stumbled upon.
 */
public class SprenSeekUnbondedGoal<T extends PathfinderMob & BondingSpren> extends Goal {
    private final T spren;
    private final double speed;
    private final double radius;
    private Player target;
    private int timeToRecalcPath;

    public SprenSeekUnbondedGoal(T spren, double speed, double radius) {
        this.spren = spren;
        this.speed = speed;
        this.radius = radius;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (spren.isBonded()) return false;
        if (spren.getRandom().nextInt(10) != 0) return false; // idle most ticks
        this.target = nearestUnbondedPlayer();
        return this.target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (spren.isBonded() || target == null) return false;
        if (!target.isAlive() || target.isSpectator()) return false;
        if (target.level() != spren.level()) return false;
        if (RadiantAttachments.getOath(target) != 0) return false;  // someone else got there first
        return spren.distanceToSqr(target) <= radius * radius;
    }

    @Override
    public void start() { this.timeToRecalcPath = 0; }

    @Override
    public void stop() {
        this.target = null;
        spren.getNavigation().stop();
    }

    @Override
    public void tick() {
        spren.getLookControl().setLookAt(target, 10.0F, (float) spren.getMaxHeadXRot());
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = this.adjustedTickDelay(10);
            spren.getNavigation().moveTo(target, this.speed);
        }
    }

    /** Nearest oath-0 player in range. Server-side. */
    private Player nearestUnbondedPlayer() {
        Player nearest = null;
        double best = radius * radius;
        for (Player p : spren.level().players()) {
            if (p.isSpectator() || RadiantAttachments.getOath(p) != 0) continue;
            double d = spren.distanceToSqr(p);
            if (d < best) { best = d; nearest = p; }
        }
        return nearest;
    }
}
