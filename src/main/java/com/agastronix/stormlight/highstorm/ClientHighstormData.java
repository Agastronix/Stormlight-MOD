package com.agastronix.stormlight.highstorm;

public class ClientHighstormData {
    public static volatile int phase = 0;   // ordinal into HighstormPhase.values()
    public static volatile int ticks = 0;
    public static volatile int length = 0;

    public static void set(int phase, int ticks, int length) {
        ClientHighstormData.phase = phase;
        ClientHighstormData.ticks = ticks;
        ClientHighstormData.length = length;
    }

    public static HighstormPhase phase() {
        return HighstormPhase.byId(phase);
    }

    /** Advanced every client tick so visuals stay smooth between server syncs. */
    public static void tick() {
        if (phase != 0 && ticks < length) ticks++;
    }

    public static float progress(float partialTick) {
        return length <= 0 ? 0.0F : Math.min(1.0F, (ticks + partialTick) / length);
    }
}
