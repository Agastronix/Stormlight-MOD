package com.agastronix.stormlight.entity;

import com.agastronix.stormlight.radiant.RadiantAttachments;
import com.agastronix.stormlight.radiant.RadiantNetwork;
import com.agastronix.stormlight.radiant.RadiantOrder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class HonorSprenEntity extends PathfinderMob implements BondingSpren {

    public static final int VILLAGE_RADIUS = 40;

    private UUID ownerUUID;
    private BlockPos anchorPos; // the bell this spren belongs to (unbonded only)

    public HonorSprenEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.FLYING_SPEED, 0.9D)   // was 0.4 — noticeably zippier
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SprenFollowOwnerGoal<>(this, 1.3D, 6.0F, 4.0F, 20.0F));
        this.goalSelector.addGoal(2, new SprenVillageWanderGoal(this, 1.0D, VILLAGE_RADIUS));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    // --- Ownership ---
    @Override
    public boolean isBonded() { return this.ownerUUID != null; }
    @Override
    public boolean isOwnedBy(Player player) { return player.getUUID().equals(this.ownerUUID); }
    @Override
    public void setOwner(Player player) { this.ownerUUID = player.getUUID(); this.anchorPos = null; }
    @Override
    public void clearOwner() { this.ownerUUID = null; }

    @Override
    public Player getOwnerPlayer() {
        if (this.ownerUUID == null || this.level().isClientSide()) return null;
        return this.level().getPlayerByUUID(this.ownerUUID);
    }

    /** Honorspren bond Windrunners. */
    @Override
    public RadiantOrder getGrantedOrder() { return RadiantOrder.WINDRUNNER; }

    // --- Village anchor ---
    public boolean hasAnchor() { return this.anchorPos != null; }
    public BlockPos getAnchor() { return this.anchorPos; }
    public void setAnchor(BlockPos pos) { this.anchorPos = pos; }

    /** Nearest player with no bond (oath 0) who is inside this village. Server-side. */
    public Player findUnbondedPlayerInVillage(int radius) {
        if (this.anchorPos == null) return null;
        Player nearest = null;
        double best = Double.MAX_VALUE;
        for (Player p : this.level().players()) {
            if (RadiantAttachments.getOath(p) != 0) continue;
            if (p.blockPosition().distSqr(this.anchorPos) > (double) radius * radius) continue;
            double d = this.distanceToSqr(p);
            if (d < best) { best = d; nearest = p; }
        }
        return nearest;
    }

    // --- Persistence ---
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerUUID != null) tag.putUUID("Owner", this.ownerUUID);
        if (this.anchorPos != null) {
            tag.putInt("AnchorX", this.anchorPos.getX());
            tag.putInt("AnchorY", this.anchorPos.getY());
            tag.putInt("AnchorZ", this.anchorPos.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.ownerUUID = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        if (tag.contains("AnchorX")) {
            this.anchorPos = new BlockPos(tag.getInt("AnchorX"), tag.getInt("AnchorY"), tag.getInt("AnchorZ"));
        }
    }

    // --- Immortality + village leash ---
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() || this.isBonded() || this.anchorPos == null) return;

        double cx = this.anchorPos.getX() + 0.5;
        double cy = this.anchorPos.getY() + 0.5;
        double cz = this.anchorPos.getZ() + 0.5;
        double distSqr = this.distanceToSqr(cx, cy, cz);
        double r = VILLAGE_RADIUS;

        if (distSqr > (r * 2.0) * (r * 2.0)) {
            this.getNavigation().stop();
            this.teleportTo(cx, cy + 1.0, cz);        // knocked way out — snap home
        } else if (distSqr > r * r) {
            this.getNavigation().moveTo(cx, cy + 1.0, cz, 1.0); // past the edge — return
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide()) {
            return InteractionResult.sidedSuccess(true);
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }

        if (this.isBonded()) {
            if (this.isOwnedBy(sp)) {
                RadiantNetwork.sendOpenDialogue(sp, this.getId(), true);
            } else {
                sp.displayClientMessage(Component.literal("This spren is bound to another."), true);
            }
        } else {
            if (RadiantAttachments.getOath(sp) >= 1) {
                sp.displayClientMessage(Component.literal("You are already bound to a spren."), true);
            } else {
                RadiantNetwork.sendOpenDialogue(sp, this.getId(), false);
            }
        }
        return InteractionResult.sidedSuccess(false);
    }
}