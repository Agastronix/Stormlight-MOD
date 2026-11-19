package com.agastronix.stormlight.entity;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Keeps a bonded spren with its Radiant. Works off generic navigation, so it serves a
 * flying spren and a walking one alike.
 */
public class SprenFollowOwnerGoal<T extends PathfinderMob & BondingSpren> extends Goal {
    private final T spren;
    private final double speedModifier;
    private final PathNavigation navigation;
    private final float startDistance;
    private final float stopDistance;
    private final float teleportDistance;
    private Player owner;
    private int timeToRecalcPath;

    public SprenFollowOwnerGoal(T spren, double speed,
                                float startDistance, float stopDistance, float teleportDistance) {
        this.spren = spren;
        this.speedModifier = speed;
        this.navigation = spren.getNavigation();
        this.startDistance = startDistance;
        this.stopDistance = stopDistance;
        this.teleportDistance = teleportDistance;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        Player o = spren.getOwnerPlayer();
        if (o == null || o.isSpectator()) return false;
        if (o.level() != spren.level()) return false;
        if (spren.distanceToSqr(o) < (double) (startDistance * startDistance)) return false;
        this.owner = o;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (owner == null || owner.level() != spren.level()) return false;
        return spren.distanceToSqr(owner) > (double) (stopDistance * stopDistance);
    }

    @Override
    public void start() { this.timeToRecalcPath = 0; }

    @Override
    public void stop() { this.owner = null; this.navigation.stop(); }

    @Override
    public void tick() {
        spren.getLookControl().setLookAt(owner, 10.0F, (float) spren.getMaxHeadXRot());
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = this.adjustedTickDelay(10);
            if (spren.distanceToSqr(owner) >= (double) (teleportDistance * teleportDistance)) {
                spren.teleportTo(owner.getX(), owner.getY() + 1.0, owner.getZ()); // never left behind
                this.navigation.stop();
            } else {
                this.navigation.moveTo(owner, this.speedModifier);
            }
        }
    }
}
