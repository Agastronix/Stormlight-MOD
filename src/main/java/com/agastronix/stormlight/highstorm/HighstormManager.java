package com.agastronix.stormlight.highstorm;

import com.agastronix.stormlight.StormlightArchiveMOD;
import com.agastronix.stormlight.item.ModItems;
import com.agastronix.stormlight.resource.StormlightAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;

/**
 * Runs the single global highstorm in the overworld: scheduling, the WARNING / STORM / RAIN
 * phases, weather control, storm effects, sleep blocking, syncing and the /highstorm command.
 */
@EventBusSubscriber(modid = StormlightArchiveMOD.MODID)
public class HighstormManager {
    private static final Component NO_SLEEP = Component.literal("You can't sleep through a highstorm.");

    /** An entity standing exposed in the overworld during the STORM phase. Server side only. */
    public static boolean isExposedToStorm(Entity entity) {
        return entity.level() instanceof ServerLevel level
                && level.dimension() == Level.OVERWORLD
                && HighstormData.get(level).phase == HighstormPhase.STORM
                && Highstorm.isExposed(entity);
    }

    private static boolean enabled(ServerLevel level) {
        return level.getGameRules().getBoolean(Highstorm.RULE_DO_HIGHSTORMS);
    }

    // --- Ticking ---

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        HighstormData data = HighstormData.get(level);

        tickSchedule(level, data);
        if (data.phase == HighstormPhase.NONE) return;
        if (!enabled(level)) {
            stop(level, data);   // gamerule switched off mid-storm
            return;
        }
        tickPhase(level, data);
    }

    private static void tickSchedule(ServerLevel level, HighstormData data) {
        long day = level.getDayTime();
        if (data.nextStormDayTime < 0) {
            schedule(level, data);  // first storm of a new world: the usual 5-7 days
        } else if (data.lastDayTime >= 0 && day < data.lastDayTime) {
            // Time was set backwards: keep the same wait instead of pushing the storm days away.
            data.nextStormDayTime += day - data.lastDayTime;
            data.setDirty();
        }
        data.lastDayTime = day;

        if (data.phase == HighstormPhase.NONE && day >= data.nextStormDayTime) {
            if (enabled(level)) {
                start(level, data);
            } else {
                schedule(level, data);  // due while disabled: skip this one
            }
        }
    }

    private static void schedule(ServerLevel level, HighstormData data) {
        data.nextStormDayTime = level.getDayTime()
                + Mth.nextInt(level.getRandom(), Highstorm.MIN_INTERVAL, Highstorm.MAX_INTERVAL);
        data.setDirty();
    }

    private static void tickPhase(ServerLevel level, HighstormData data) {
        data.phaseTicks++;
        switch (data.phase) {
            case WARNING -> holdWeather(level);
            case STORM -> {
                holdStorm(level, data);
                applyStorm(level, data);
            }
            default -> {}
        }

        if (data.phaseTicks >= data.phase.length) {
            advance(level, data);
        } else if (data.phaseTicks % Highstorm.SYNC_INTERVAL == 0) {
            sync(level, data);
        }
        data.setDirty();
    }

    // --- Phases ---

    public static void start(ServerLevel level, HighstormData data) {
        setPhase(level, data, HighstormPhase.WARNING);
        holdWeather(level);
        Component warning = Component.literal("A highstorm approaches from the east.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(warning);
        }
    }

    private static void advance(ServerLevel level, HighstormData data) {
        switch (data.phase) {
            case WARNING -> {
                setPhase(level, data, HighstormPhase.STORM);
                holdStorm(level, data);
            }
            case STORM -> {
                // Hand back to vanilla: plain rain for this phase's length, exactly like /weather rain.
                level.setWeatherParameters(0, Highstorm.RAIN_TICKS, true, false);
                resetExposure(level);
                setPhase(level, data, HighstormPhase.RAIN);
            }
            default -> {
                // Vanilla's own cycle carries on from the rain, unless it's switched off.
                if (!level.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE)) {
                    clearWeather(level);
                }
                end(level, data);
            }
        }
    }

    /** Ends any storm immediately and rolls the next one. */
    public static void stop(ServerLevel level, HighstormData data) {
        if (data.phase == HighstormPhase.STORM || data.phase == HighstormPhase.RAIN) {
            clearWeather(level);
        }
        end(level, data);
    }

    private static void end(ServerLevel level, HighstormData data) {
        resetExposure(level);
        setPhase(level, data, HighstormPhase.NONE);
        schedule(level, data);
    }

    private static void setPhase(ServerLevel level, HighstormData data, HighstormPhase phase) {
        data.phase = phase;
        data.phaseTicks = 0;
        data.setDirty();
        sync(level, data);
    }

    // --- Weather ---

    /** WARNING: freeze vanilla's weather timers so it can't flip under the approaching storm. */
    private static void holdWeather(ServerLevel level) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE)) return; // nothing counts down anyway
        ServerLevelData weather = level.getServer().getWorldData().overworldData();
        // Vanilla flips the weather when a timer counts down from 1 to 0, so keep them at 2 or more.
        if (weather.getClearWeatherTime() > 0) {
            weather.setClearWeatherTime(Math.max(weather.getClearWeatherTime() + 1, 2));
        } else {
            if (weather.getRainTime() > 0) weather.setRainTime(Math.max(weather.getRainTime() + 1, 2));
            if (weather.getThunderTime() > 0) weather.setThunderTime(Math.max(weather.getThunderTime() + 1, 2));
        }
    }

    /** STORM: rain and thunder, held for the rest of the phase. */
    private static void holdStorm(ServerLevel level, HighstormData data) {
        level.setWeatherParameters(0, Math.max(2, Highstorm.STORM_TICKS - data.phaseTicks + 1), true, true);
    }

    /** Clear skies, like /weather clear: vanilla picks the next rain from its usual delay. */
    private static void clearWeather(ServerLevel level) {
        level.setWeatherParameters(ServerLevel.RAIN_DELAY.sample(level.getRandom()), 0, false, false);
    }

    // --- Storm effects ---

    private static void applyStorm(ServerLevel level, HighstormData data) {
        boolean secondTick = data.phaseTicks % Highstorm.DAMAGE_INTERVAL == 0;

        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Player || entity instanceof ArmorStand || entity.isPassenger()) continue;
            if (!(entity instanceof ItemEntity) && !(entity instanceof LivingEntity)) continue;
            if (!Highstorm.isExposed(entity)) continue;

            double push = Highstorm.WIND_PUSH * Highstorm.biomeIntensity(level.getBiome(entity.blockPosition()));
            if (entity instanceof ItemEntity item) {
                chargeSphere(level, item);
            } else {
                push *= Highstorm.MOB_WIND_FACTOR;
            }
            entity.setDeltaMovement(entity.getDeltaMovement().add(-push, 0.0, 0.0));
            entity.hasImpulse = true;
        }

        for (ServerPlayer player : level.players()) {
            tickPlayer(level, player, secondTick);
        }
    }

    // A player's wind is applied by their own client (HighstormClientEvents): pushing here and
    // syncing with hurtMarked would overwrite the client's momentum every tick and break jumping.
    private static void tickPlayer(ServerLevel level, ServerPlayer player, boolean secondTick) {
        if (!player.isAlive() || player.isSpectator() || !Highstorm.isExposed(player)) {
            StormlightAttachments.setHighstormExposure(player, 0);
            return;
        }
        if (secondTick) chargeSpheres(player);
        if (player.isCreative()) {
            StormlightAttachments.setHighstormExposure(player, 0);
            return;
        }

        int exposed = StormlightAttachments.getHighstormExposure(player) + 1;
        StormlightAttachments.setHighstormExposure(player, exposed);
        if (secondTick) hurt(level, player, exposed);
    }

    private static void hurt(ServerLevel level, ServerPlayer player, int exposedTicks) {
        float raw = Math.min(Highstorm.DAMAGE_MAX,
                Highstorm.DAMAGE_BASE + Highstorm.DAMAGE_RAMP_PER_SECOND * (exposedTicks / 20.0F))
                * Highstorm.biomeIntensity(level.getBiome(player.blockPosition()));
        if (raw <= 0.0F) return;

        DamageSource source = new DamageSource(level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(Highstorm.DAMAGE_TYPE));
        // The damage type bypasses armor and enchantments; work out the vanilla reduction here
        // and apply only part of it, so armor helps without making anyone storm-proof.
        float reduced = CombatRules.getDamageAfterAbsorb(player, raw, source, player.getArmorValue(),
                (float) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        reduced = CombatRules.getDamageAfterMagicAbsorb(reduced,
                EnchantmentHelper.getDamageProtection(level, player, source));
        player.hurt(source, raw - (raw - reduced) * Highstorm.ARMOR_EFFECTIVENESS);
    }

    private static void chargeSphere(ServerLevel level, ItemEntity item) {
        ItemStack stack = item.getItem();
        if (!stack.is(ModItems.EMPTY_SPHERE.get())) return;
        item.setItem(new ItemStack(ModItems.FULL_SPHERE.get(), stack.getCount()));
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, item.getX(), item.getY() + 0.25, item.getZ(),
                6, 0.1, 0.1, 0.1, 0.05);
    }

    private static void chargeSpheres(ServerPlayer player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(ModItems.EMPTY_SPHERE.get())) {
                inv.setItem(i, new ItemStack(ModItems.FULL_SPHERE.get(), stack.getCount()));
            }
        }
        ItemStack carried = player.containerMenu.getCarried();
        if (carried.is(ModItems.EMPTY_SPHERE.get())) {
            player.containerMenu.setCarried(new ItemStack(ModItems.FULL_SPHERE.get(), carried.getCount()));
        }
    }

    private static void resetExposure(ServerLevel level) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            StormlightAttachments.setHighstormExposure(player, 0);
        }
    }

    // --- Sync ---

    private static HighstormPayload payload(HighstormData data) {
        return new HighstormPayload(data.phase.ordinal(), data.phaseTicks, data.phase.length);
    }

    private static void sync(ServerLevel level, HighstormData data) {
        PacketDistributor.sendToPlayersInDimension(level, payload(data));
    }

    /** Players outside the overworld are told there's no storm, so their visuals switch off. */
    private static void syncTo(ServerPlayer player) {
        HighstormPayload payload = new HighstormPayload(HighstormPhase.NONE.ordinal(), 0, 0);
        if (player.level() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
            payload = payload(HighstormData.get(level));
        }
        PacketDistributor.sendToPlayer(player, payload);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncTo(player);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StormlightAttachments.setHighstormExposure(player, 0);
            syncTo(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncTo(player);
    }

    // --- Sleep ---

    private static boolean sleepBlocked(Level level) {
        if (!(level instanceof ServerLevel server) || server.dimension() != Level.OVERWORLD) return false;
        HighstormPhase phase = HighstormData.get(server).phase;
        return phase == HighstormPhase.WARNING || phase == HighstormPhase.STORM;
    }

    @SubscribeEvent
    public static void onCanSleep(CanPlayerSleepEvent event) {
        if (event.getProblem() == null && sleepBlocked(event.getLevel())) {
            // OTHER_PROBLEM carries no vanilla text; ours goes to the action bar where vanilla's would.
            event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
            event.getEntity().displayClientMessage(NO_SLEEP, true);
        }
    }

    // Anyone already asleep when the warning sounds is woken.
    @SubscribeEvent
    public static void onContinueSleeping(CanContinueSleepingEvent event) {
        if (event.mayContinueSleeping() && sleepBlocked(event.getEntity().level())) {
            event.setContinueSleeping(false);
            if (event.getEntity() instanceof ServerPlayer player) {
                player.displayClientMessage(NO_SLEEP, true);
            }
        }
    }

    // --- Commands ---

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("highstorm")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("start").executes(ctx -> {
                            ServerLevel level = ctx.getSource().getServer().overworld();
                            HighstormData data = HighstormData.get(level);
                            if (!enabled(level)) {
                                ctx.getSource().sendFailure(Component.literal("Highstorms are disabled (gamerule doHighstorms)."));
                                return 0;
                            }
                            if (data.phase != HighstormPhase.NONE) {
                                ctx.getSource().sendFailure(Component.literal("A highstorm is already underway."));
                                return 0;
                            }
                            start(level, data);
                            ctx.getSource().sendSuccess(() -> Component.literal("Highstorm warning started."), true);
                            return 1;
                        }))
                        .then(Commands.literal("stop").executes(ctx -> {
                            ServerLevel level = ctx.getSource().getServer().overworld();
                            HighstormData data = HighstormData.get(level);
                            boolean active = data.phase != HighstormPhase.NONE;
                            stop(level, data);
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    (active ? "Highstorm stopped. " : "No highstorm was active. ") + nextStormText(level, data)), true);
                            return 1;
                        }))
                        .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
        );
    }

    private static int status(CommandSourceStack source) {
        ServerLevel level = source.getServer().overworld();
        HighstormData data = HighstormData.get(level);
        String text = data.phase != HighstormPhase.NONE
                ? "Highstorm phase: " + data.phase.name() + ", " + clock(data.phase.length - data.phaseTicks) + " left in this phase."
                : nextStormText(level, data);
        if (!enabled(level)) text += " (gamerule doHighstorms is false)";
        String message = text;
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static String nextStormText(ServerLevel level, HighstormData data) {
        long wait = Math.max(0, data.nextStormDayTime - level.getDayTime());
        return String.format(Locale.ROOT, "Next highstorm in %.2f in-game days (%s at normal speed).",
                wait / 24000.0, clock(wait));
    }

    private static String clock(long ticks) {
        long seconds = ticks / 20;
        return seconds >= 3600
                ? String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
                : String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }
}
