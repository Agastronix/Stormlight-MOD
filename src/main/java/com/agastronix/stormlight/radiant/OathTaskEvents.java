package com.agastronix.stormlight.radiant;

import com.agastronix.stormlight.StormlightArchiveMOD;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/** Detects the deeds that count toward {@link OathTask}s. */
@EventBusSubscriber(modid = StormlightArchiveMOD.MODID)
public class OathTaskEvents {

    /** Set on a zombie when a player strikes it while it is hunting a villager. */
    private static final String DEFENDER_TAG = StormlightArchiveMOD.MODID + ":villager_defender";

    // Splash radius, matching vanilla ThrownPotion.applySplash.
    private static final double SPLASH_XZ = 4.0;
    private static final double SPLASH_Y = 2.0;
    private static final double SPLASH_DIST_SQR = 16.0;

    /** A heal we can't confirm until the interaction / splash has been applied. */
    private record PendingHeal(ServerPlayer player, LivingEntity mob, float healthBefore, long checkAt) {}
    private static final List<PendingHeal> PENDING_HEALS = new ArrayList<>();

    // --- Windrunner II: zombies slain while they were after a villager ---

    // Hitting a zombie makes it turn on you, so remember it was hunting a villager when you engaged it.
    @SubscribeEvent
    public static void onZombieHurt(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Zombie zombie
                && event.getSource().getEntity() instanceof ServerPlayer player
                && zombie.getTarget() instanceof AbstractVillager) {
            zombie.getPersistentData().putUUID(DEFENDER_TAG, player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onZombieDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        boolean huntingVillager = zombie.getTarget() instanceof AbstractVillager;
        boolean engagedWhileHunting = zombie.getPersistentData().hasUUID(DEFENDER_TAG)
                && player.getUUID().equals(zombie.getPersistentData().getUUID(DEFENDER_TAG));
        if (huntingVillager || engagedWhileHunting) {
            OathProgress.increment(player, OathTask.WINDRUNNER_ZOMBIES_DEFENDING_VILLAGERS);
        }
    }

    // --- Windrunner III: win a raid (Hero of the Village is granted on victory) ---

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        MobEffectInstance effect = event.getEffectInstance();
        if (event.getEntity() instanceof ServerPlayer player && effect.is(MobEffects.HERO_OF_THE_VILLAGE)) {
            OathProgress.increment(player, OathTask.WINDRUNNER_RAIDS_WON);
        }
    }

    // --- Edgedancer II: heal injured mobs (feeding, or a thrown Splash Potion of Healing) ---

    @SubscribeEvent
    public static void onFeed(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof Mob mob)) return;
        if (player.getItemInHand(event.getHand()).isEmpty()) return;
        queueIfInjured(player, mob);
    }

    @SubscribeEvent
    public static void onPotionImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof ThrownPotion potion)) return;
        if (!(potion.level() instanceof ServerLevel level)) return;
        if (!(potion.getOwner() instanceof ServerPlayer player)) return;   // the thrower, recorded by vanilla
        if (!potion.getItem().is(Items.SPLASH_POTION)) return;             // lingering clouds aren't attributed
        PotionContents contents = potion.getItem().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        boolean heals = false;
        for (MobEffectInstance effect : contents.getAllEffects()) {
            if (effect.is(MobEffects.HEAL)) { heals = true; break; }
        }
        if (!heals) return;

        for (Mob mob : level.getEntitiesOfClass(Mob.class, potion.getBoundingBox().inflate(SPLASH_XZ, SPLASH_Y, SPLASH_XZ))) {
            if (mob.isInvertedHealAndHarm() || !mob.isAffectedByPotions()) continue; // undead are harmed
            if (potion.distanceToSqr(mob) >= SPLASH_DIST_SQR) continue;
            queueIfInjured(player, mob);
        }
    }

    private static void queueIfInjured(ServerPlayer player, LivingEntity mob) {
        if (mob.getHealth() >= mob.getMaxHealth()) return;   // full health (e.g. breeding) never counts
        for (PendingHeal p : PENDING_HEALS) {
            if (p.player() == player && p.mob() == mob) return; // both hands fire; count once
        }
        PENDING_HEALS.add(new PendingHeal(player, mob, mob.getHealth(),
                mob.level().getGameTime() + 1));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING_HEALS.isEmpty()) return;
        PENDING_HEALS.removeIf(p -> {
            if (p.mob().level().getGameTime() < p.checkAt()) return false;
            if (p.mob().isAlive() && !p.player().hasDisconnected() && p.mob().getHealth() > p.healthBefore()) {
                OathProgress.increment(p.player(), OathTask.EDGEDANCER_MOBS_HEALED);
            }
            return true;
        });
    }

    // --- Edgedancer III: trade with wandering traders ---

    @SubscribeEvent
    public static void onTrade(TradeWithVillagerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getAbstractVillager() instanceof WanderingTrader) {
            OathProgress.increment(player, OathTask.EDGEDANCER_WANDERER_TRADES);
        }
    }
}
