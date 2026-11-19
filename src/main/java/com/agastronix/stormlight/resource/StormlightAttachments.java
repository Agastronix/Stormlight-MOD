package com.agastronix.stormlight.resource;

import com.agastronix.stormlight.StormlightArchiveMOD;
import com.agastronix.stormlight.highstorm.HighstormManager;
import com.mojang.serialization.Codec;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class StormlightAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, StormlightArchiveMOD.MODID);

    public static final float MAX_STORMLIGHT = 500.0F;

    // A single float stored per-player: how much Stormlight they're holding.
    // serialize -> saved to disk; copyOnDeath -> kept through respawns.
    public static final Supplier<AttachmentType<Float>> STORMLIGHT =
            ATTACHMENT_TYPES.register("stormlight",
                    () -> AttachmentType.builder(() -> 0.0F)
                            .serialize(Codec.FLOAT)
                            .copyOnDeath()
                            .build());

    // --- Convenience accessors ---
    public static float get(Player player) {
        return player.getData(STORMLIGHT.get());
    }

    public static void set(Player player, float amount) {
        float clamped = Math.max(0.0F, Math.min(MAX_STORMLIGHT, amount));
        player.setData(STORMLIGHT.get(), clamped);
    }

    public static void add(Player player, float amount) {
        set(player, get(player) + amount);
    }

    // Ticks of continuous highstorm exposure, driving the damage ramp. Transient: resets on death/logout.
    public static final Supplier<AttachmentType<Integer>> HIGHSTORM_EXPOSURE =
            ATTACHMENT_TYPES.register("highstorm_exposure",
                    () -> AttachmentType.builder(() -> 0).build());

    public static int getHighstormExposure(Player player) { return player.getData(HIGHSTORM_EXPOSURE.get()); }
    public static void setHighstormExposure(Player player, int ticks) { player.setData(HIGHSTORM_EXPOSURE.get(), ticks); }

    /** Out under open sky during the STORM phase of a highstorm. Server side; always false on the client. */
    public static boolean isInHighstorm(Player player) {
        return HighstormManager.isExposedToStorm(player);
    }

    /**
     * Spend Stormlight. Returns true (and deducts) only if there's enough. Surges will call this.
     * In a highstorm the Stormlight is replenished as fast as it's spent, so nothing is deducted.
     */
    public static boolean consume(Player player, float amount) {
        float current = get(player);
        if (current < amount) return false;
        if (!isInHighstorm(player)) set(player, current - amount);
        return true;
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}