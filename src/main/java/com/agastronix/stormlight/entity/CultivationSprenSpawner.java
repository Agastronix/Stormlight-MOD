package com.agastronix.stormlight.entity;

import com.agastronix.stormlight.StormlightArchiveMOD;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Code-driven spawner for cultivationspren, mirroring {@link SprenSpawner}. Where the
 * honorspren gathers at village bells, the cultivationspren appears out in the wild —
 * forest and jungle only, and rarely.
 */
@EventBusSubscriber(modid = StormlightArchiveMOD.MODID)
public class CultivationSprenSpawner {
    private static final int MAX_SPREN_NEAR_PLAYER = 2;   // unbonded ones only
    private static final int CHECK_INTERVAL = 40;        // ~30s between passes
    private static final int SEARCH_RADIUS = 64;          // how far out we may place one
    private static final int MIN_SPAWN_DISTANCE = 16;     // never right on top of the player
    private static final int COUNT_RADIUS = 48;           // crowding check
    private static final double SPAWN_CHANCE = 0.9;      // per player, per pass
    private static final int PLACEMENT_ATTEMPTS = 8;
    private static int timer = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++timer < CHECK_INTERVAL) return;
        timer = 0;

        MinecraftServer server = event.getServer();
        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                trySpawnNearPlayer(level, player);
            }
        }
    }

    private static void trySpawnNearPlayer(ServerLevel level, ServerPlayer player) {
        if (level.random.nextDouble() > SPAWN_CHANCE) return;
        if (sprenCountNear(level, player) >= MAX_SPREN_NEAR_PLAYER) return;

        BlockPos center = player.blockPosition();
        for (int i = 0; i < PLACEMENT_ATTEMPTS; i++) {
            int x = center.getX() + level.random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            int z = center.getZ() + level.random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            if (!level.isLoaded(new BlockPos(x, center.getY(), z))) continue;

            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (pos.distSqr(center) < (double) MIN_SPAWN_DISTANCE * MIN_SPAWN_DISTANCE) continue;
            if (!isForestOrJungle(level, pos)) continue;
            // Heightmap tops out above water, so reject wet ground rather than spawning on a lake.
            if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.below()).isEmpty()) continue;

            spawn(level, pos);
            return;
        }
    }

    private static boolean isForestOrJungle(ServerLevel level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        return biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_JUNGLE);
    }

    private static int sprenCountNear(ServerLevel level, ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(COUNT_RADIUS);
        // Only count UNBONDED spren — someone's bonded spren shouldn't hold a slot.
        return level.getEntitiesOfClass(CultivationSprenEntity.class, box, s -> !s.isBonded()).size();
    }

    private static void spawn(ServerLevel level, BlockPos pos) {
        CultivationSprenEntity spren = ModEntities.CULTIVATION_SPREN.get().create(level);
        if (spren == null) return;
        spren.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                level.random.nextFloat() * 360F, 0F);
        level.addFreshEntity(spren);
    }
}
