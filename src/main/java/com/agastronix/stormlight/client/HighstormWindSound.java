package com.agastronix.stormlight.client;

import com.agastronix.stormlight.highstorm.ClientHighstormData;
import com.agastronix.stormlight.highstorm.HighstormPhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/** The highstorm's wind: builds through WARNING, howls in STORM, dies away in RAIN. */
public class HighstormWindSound extends AbstractTickableSoundInstance {
    private static final float PITCH = 0.7F;            // lower than the elytra rush, closer to a howl
    private static final float SHELTERED_VOLUME = 0.25F; // fraction still heard indoors / underground

    public HighstormWindSound() {
        super(SoundEvents.ELYTRA_FLYING, SoundSource.WEATHER, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.volume = 0.0F;
        this.pitch = PITCH;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        HighstormPhase phase = ClientHighstormData.phase();
        if (mc.level == null || mc.level.dimension() != Level.OVERWORLD || phase == HighstormPhase.NONE) {
            this.stop();
            return;
        }
        float progress = ClientHighstormData.progress(0.0F);
        float strength = switch (phase) {
            case WARNING -> 0.15F + 0.45F * progress;
            case STORM -> 1.0F;
            case RAIN -> 0.6F * (1.0F - progress);
            default -> 0.0F;
        };
        this.volume = strength * (SHELTERED_VOLUME + (1.0F - SHELTERED_VOLUME) * HighstormClientEvents.skyFactor());
    }
}
