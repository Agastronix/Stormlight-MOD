package com.agastronix.stormlight.highstorm;

import com.agastronix.stormlight.StormlightArchiveMOD;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Map;

/** Every highstorm tuning value and rule lives here. */
public final class Highstorm {
    private Highstorm() {}

    // --- Scheduling (overworld day time, so sleeping brings the next storm closer) ---
    public static final int MIN_INTERVAL = 5 * 24000;           // 120000 ticks
    public static final int MAX_INTERVAL = 7 * 24000;           // 168000 ticks

    // --- Phase lengths ---
    public static final int WARNING_TICKS = 60 * 20;            // 1 minute
    public static final int STORM_TICKS = 3 * 60 * 20;          // 3 minutes
    public static final int RAIN_TICKS = 60 * 20;               // 1 minute

    // --- Wind (STORM only, exposed only) ---
    public static final double WIND_PUSH = 0.03;                // velocity added per tick toward -X (players, items)
    public static final double MOB_WIND_FACTOR = 0.3;           // mobs get a fraction so villages aren't emptied

    // --- Damage (STORM only, exposed players only) ---
    public static final int DAMAGE_INTERVAL = 20;               // one hit per second
    // Unarmored: ~30s outside kills a hungry player, ~55s a well-fed one (natural regen offsets the early hits).
    public static final float DAMAGE_BASE = 0.4F;               // first hit
    public static final float DAMAGE_RAMP_PER_SECOND = 0.015F;  // added per second of continuous exposure
    public static final float DAMAGE_MAX = 4.0F;                // per hit cap
    // Fraction of the normal armor + Protection reduction that applies (1 = vanilla, 0 = none).
    public static final float ARMOR_EFFECTIVENESS = 0.5F;

    // --- Client visuals ---
    public static final int SYNC_INTERVAL = 20;                 // phase resync to clients
    public static final int STORMWALL_TICKS = 15 * 20;          // the wall's approach, at the end of WARNING
    public static final float STORMWALL_START_DISTANCE = 160.0F;

    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(StormlightArchiveMOD.MODID, "highstorm"));

    // Assigned on the main thread during common setup (see StormlightArchiveMOD).
    public static GameRules.Key<GameRules.BooleanValue> RULE_DO_HIGHSTORMS;

    public static void registerGameRule() {
        RULE_DO_HIGHSTORMS = GameRules.register("doHighstorms", GameRules.Category.UPDATES,
                GameRules.BooleanValue.create(true));
    }

    // --- Biome intensity: scales wind and damage. Unlisted biomes are 1.0. ---
    // e.g. Map.entry(Biomes.DESERT, 0.6F)
    private static final Map<ResourceKey<Biome>, Float> BIOME_INTENSITY = Map.ofEntries();

    public static float biomeIntensity(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> BIOME_INTENSITY.getOrDefault(key, 1.0F)).orElse(1.0F);
    }

    /** Open sky at head height with nothing solid overhead: the same test vanilla uses for rain. */
    public static boolean isExposed(Entity entity) {
        Level level = entity.level();
        BlockPos head = BlockPos.containing(entity.getEyePosition());
        return level.canSeeSky(head)
                && level.getHeight(Heightmap.Types.MOTION_BLOCKING, head.getX(), head.getZ()) <= head.getY();
    }
}
