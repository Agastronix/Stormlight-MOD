package com.agastronix.stormlight.entity;

import com.agastronix.stormlight.StormlightArchiveMOD;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

@EventBusSubscriber(modid = StormlightArchiveMOD.MODID)
public class SprenSpawner {
    private static final int MAX_SPREN_PER_VILLAGE = 3; // set to whatever you like
    private static final int CHECK_INTERVAL = 600;   // ~30s between passes
    private static final int SEARCH_RADIUS  = 48;    // how far from a player to look for bells
    private static final double SPAWN_CHANCE = 0.25; // per eligible bell, per pass
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
        PoiManager poi = level.getPoiManager();
        BlockPos center = player.blockPosition();

        List<BlockPos> bells = poi.getInRange(
                holder -> holder.is(PoiTypes.MEETING),
                center, SEARCH_RADIUS,
                PoiManager.Occupancy.ANY
        ).map(PoiRecord::getPos).toList();

        for (BlockPos bell : bells) {
            if (sprenCountNear(level, bell) >= MAX_SPREN_PER_VILLAGE) continue;
            if (level.random.nextDouble() > SPAWN_CHANCE) continue;
            spawnAtBell(level, bell);
            return;
        }
    }

    private static int sprenCountNear(ServerLevel level, BlockPos bell) {
        AABB box = new AABB(bell).inflate(HonorSprenEntity.VILLAGE_RADIUS);
        // Only count UNBONDED spren — bonded ones following a player through shouldn't hold a slot.
        return level.getEntitiesOfClass(HonorSprenEntity.class, box, s -> !s.isBonded()).size();
    }

    private static void spawnAtBell(ServerLevel level, BlockPos bell) {
        HonorSprenEntity spren = ModEntities.HONOR_SPREN.get().create(level);
        if (spren == null) return;
        spren.moveTo(bell.getX() + 0.5, bell.getY() + 1.5, bell.getZ() + 0.5,
                level.random.nextFloat() * 360F, 0F);
        spren.setAnchor(bell);
        level.addFreshEntity(spren);
    }
}