package com.agastronix.stormlight.entity;

import com.agastronix.stormlight.radiant.RadiantAttachments;
import com.agastronix.stormlight.radiant.RadiantNetwork;
import com.agastronix.stormlight.radiant.RadiantOrder;
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
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Cultivationspren bond Edgedancers. Unlike the honorspren it is a ground creature: it
 * walks under normal gravity, and climbs sheer walls the way a spider does.
 */
public class CultivationSprenEntity extends PathfinderMob implements BondingSpren {

    /** How far an unbonded cultivationspren notices a player who has sworn no oath. */
    public static final double SEEK_RADIUS = 24.0D;

    private UUID ownerUUID;

    public CultivationSprenEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SprenFollowOwnerGoal<>(this, 1.3D, 6.0F, 4.0F, 20.0F));
        this.goalSelector.addGoal(2, new SprenSeekUnbondedGoal<>(this, 1.0D, SEEK_RADIUS));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    // Climbs like a spider: anything it walks into becomes a ladder.
    @Override
    public boolean onClimbable() {
        return this.horizontalCollision;
    }

    // --- Ownership ---
    @Override
    public boolean isBonded() { return this.ownerUUID != null; }
    @Override
    public boolean isOwnedBy(Player player) { return player.getUUID().equals(this.ownerUUID); }
    @Override
    public void setOwner(Player player) { this.ownerUUID = player.getUUID(); }
    @Override
    public void clearOwner() { this.ownerUUID = null; }

    @Override
    public Player getOwnerPlayer() {
        if (this.ownerUUID == null || this.level().isClientSide()) return null;
        return this.level().getPlayerByUUID(this.ownerUUID);
    }

    /** Cultivationspren bond Edgedancers. */
    @Override
    public RadiantOrder getGrantedOrder() { return RadiantOrder.EDGEDANCER; }

    // --- Persistence ---
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerUUID != null) tag.putUUID("Owner", this.ownerUUID);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.ownerUUID = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    // --- Immortality ---
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        return false;
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
