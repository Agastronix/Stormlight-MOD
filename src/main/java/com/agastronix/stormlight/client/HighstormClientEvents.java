package com.agastronix.stormlight.client;

import com.agastronix.stormlight.StormlightArchiveMOD;
import com.agastronix.stormlight.highstorm.ClientHighstormData;
import com.agastronix.stormlight.highstorm.Highstorm;
import com.agastronix.stormlight.highstorm.HighstormPhase;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;

/** Highstorm visuals, sound and the local player's wind. Client only. */
@EventBusSubscriber(modid = StormlightArchiveMOD.MODID, value = Dist.CLIENT)
public class HighstormClientEvents {
    private static final float WARNING_PEAK = 0.6F;       // darkness reached by the end of WARNING
    private static final int STORM_ONSET_TICKS = 40;      // the wall hits: full darkness over 2 seconds
    private static final float STORM_FOG_FAR = 28.0F;
    private static final float STORM_FOG_NEAR = 0.0F;
    private static final float FOG_R = 0.11F, FOG_G = 0.12F, FOG_B = 0.14F;
    private static final float SKY_FACTOR_SMOOTHING = 0.05F;

    private static final int WIND_PARTICLES_PER_TICK = 8;
    private static final int WALL_PARTICLES_PER_TICK = 6;
    private static final float WALL_PARTICLE_RANGE = 64.0F;
    private static final float WALL_HALF_WIDTH = 320.0F;
    private static final float WALL_BOTTOM = -64.0F, WALL_TOP = 192.0F;
    private static final float WALL_R = 0.10F, WALL_G = 0.09F, WALL_B = 0.08F;

    // How open the player's surroundings are (sky light at the eyes), smoothed: 1 outside, 0 deep underground.
    private static float skyFactor = 0.0F;
    private static float prevSkyFactor = 0.0F;
    private static HighstormWindSound windSound;

    static float skyFactor() {
        return skyFactor;
    }

    private static boolean inOverworld(Minecraft mc) {
        return mc.level != null && mc.level.dimension() == Level.OVERWORLD;
    }

    /** Storm strength 0..1 by phase: ramps up through WARNING, full in STORM, easing off in RAIN. */
    private static float phaseIntensity(float partialTick) {
        float progress = ClientHighstormData.progress(partialTick);
        return switch (ClientHighstormData.phase()) {
            case WARNING -> WARNING_PEAK * progress;
            case STORM -> Math.min(1.0F, WARNING_PEAK
                    + (1.0F - WARNING_PEAK) * (ClientHighstormData.ticks + partialTick) / STORM_ONSET_TICKS);
            case RAIN -> 1.0F - progress;
            default -> 0.0F;
        };
    }

    /** Fog and sky darkening, eased off underground so caves aren't fogged in. */
    private static float visualIntensity(float partialTick) {
        if (!inOverworld(Minecraft.getInstance())) return 0.0F;
        return phaseIntensity(partialTick) * Mth.lerp(partialTick, prevSkyFactor, skyFactor);
    }

    /** Blocks between the player and the approaching stormwall, or -1 when there's no wall. */
    private static float wallDistance(float partialTick) {
        if (ClientHighstormData.phase() != HighstormPhase.WARNING) return -1.0F;
        float remaining = ClientHighstormData.length - (ClientHighstormData.ticks + partialTick);
        if (remaining > Highstorm.STORMWALL_TICKS) return -1.0F;
        return Highstorm.STORMWALL_START_DISTANCE * Math.max(0.0F, remaining) / Highstorm.STORMWALL_TICKS;
    }

    // --- Tick: wind, particles, sound ---

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.isPaused()) return;
        ClientHighstormData.tick();

        prevSkyFactor = skyFactor;
        float open = mc.level.getBrightness(LightLayer.SKY, BlockPos.containing(player.getEyePosition())) / 15.0F;
        skyFactor += (open - skyFactor) * SKY_FACTOR_SMOOTHING;

        HighstormPhase phase = ClientHighstormData.phase();
        if (phase == HighstormPhase.NONE || !inOverworld(mc)) return;

        if (windSound == null || !mc.getSoundManager().isActive(windSound)) {
            windSound = new HighstormWindSound();
            mc.getSoundManager().play(windSound);
        }

        if (phase == HighstormPhase.STORM) {
            pushPlayer(player);
            spawnWind(mc.level, player);
        } else if (phase == HighstormPhase.WARNING) {
            float distance = wallDistance(0.0F);
            if (distance >= 0.0F && distance <= WALL_PARTICLE_RANGE) spawnWallParticles(mc.level, player, distance);
        }
    }

    // The client owns its player's movement, so the wind is applied here; the server pushes everything else.
    private static void pushPlayer(LocalPlayer player) {
        if (player.isCreative() || player.isSpectator() || !Highstorm.isExposed(player)) return;
        double push = Highstorm.WIND_PUSH * Highstorm.biomeIntensity(player.level().getBiome(player.blockPosition()));
        player.push(-push, 0.0, 0.0);
    }

    private static void spawnWind(ClientLevel level, LocalPlayer player) {
        RandomSource random = level.random;
        for (int i = 0; i < WIND_PARTICLES_PER_TICK; i++) {
            double x = player.getX() + random.nextDouble() * 40.0 - 16.0;  // biased upwind (east)
            double y = player.getY() + random.nextDouble() * 10.0 - 2.0;
            double z = player.getZ() + random.nextDouble() * 32.0 - 16.0;
            BlockPos pos = BlockPos.containing(x, y, z);
            if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) > pos.getY()) continue;
            level.addParticle(ParticleTypes.CLOUD, x, y, z,
                    -(0.8 + random.nextDouble() * 0.6),
                    (random.nextDouble() - 0.5) * 0.05,
                    (random.nextDouble() - 0.5) * 0.1);
        }
    }

    private static void spawnWallParticles(ClientLevel level, LocalPlayer player, float distance) {
        RandomSource random = level.random;
        for (int i = 0; i < WALL_PARTICLES_PER_TICK; i++) {
            level.addParticle(ParticleTypes.LARGE_SMOKE,
                    player.getX() + distance + random.nextDouble() * 2.0,
                    player.getY() - 4.0 + random.nextDouble() * 20.0,
                    player.getZ() + random.nextDouble() * 48.0 - 24.0,
                    -0.3, 0.0, 0.0);
        }
    }

    // --- Fog and sky ---

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (event.getCamera().getFluidInCamera() != FogType.NONE) return;
        float intensity = visualIntensity((float) event.getPartialTick());
        if (intensity <= 0.0F) return;
        event.setRed(Mth.lerp(intensity, event.getRed(), FOG_R));
        event.setGreen(Mth.lerp(intensity, event.getGreen(), FOG_G));
        event.setBlue(Mth.lerp(intensity, event.getBlue(), FOG_B));
    }

    // Applies to both terrain and sky fog; pulling the sky fog in is what darkens the sky.
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (event.getType() != FogType.NONE) return;
        float intensity = visualIntensity((float) event.getPartialTick());
        if (intensity <= 0.0F) return;
        // Only ever pull the fog in, so blindness and the like stay as thick as they are.
        float far = event.getFarPlaneDistance();
        float near = event.getNearPlaneDistance();
        event.setFarPlaneDistance(Math.min(far, Mth.lerp(intensity, far, STORM_FOG_FAR)));
        event.setNearPlaneDistance(Math.min(near, Mth.lerp(intensity, near, STORM_FOG_NEAR)));
        event.setCanceled(true);
    }

    // --- Stormwall: a dark wall that sweeps in from the east over the last seconds of WARNING ---

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;
        if (!inOverworld(Minecraft.getInstance())) return;
        float distance = wallDistance(event.getPartialTick().getGameTimeDeltaPartialTick(false));
        if (distance < 0.0F) return;

        // The model-view stack already holds the camera rotation; vertices are camera-relative.
        Matrix4f pose = event.getPoseStack().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        wallQuad(buffer, pose, distance, 0.85F);
        wallQuad(buffer, pose, distance + 10.0F, 0.95F);  // a second, denser layer behind for depth
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void wallQuad(BufferBuilder buffer, Matrix4f pose, float x, float alpha) {
        buffer.addVertex(pose, x, WALL_BOTTOM, -WALL_HALF_WIDTH).setColor(WALL_R, WALL_G, WALL_B, alpha);
        buffer.addVertex(pose, x, WALL_TOP, -WALL_HALF_WIDTH).setColor(WALL_R, WALL_G, WALL_B, alpha);
        buffer.addVertex(pose, x, WALL_TOP, WALL_HALF_WIDTH).setColor(WALL_R, WALL_G, WALL_B, alpha);
        buffer.addVertex(pose, x, WALL_BOTTOM, WALL_HALF_WIDTH).setColor(WALL_R, WALL_G, WALL_B, alpha);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientHighstormData.set(HighstormPhase.NONE.ordinal(), 0, 0);
        windSound = null;
        skyFactor = 0.0F;
        prevSkyFactor = 0.0F;
    }
}
