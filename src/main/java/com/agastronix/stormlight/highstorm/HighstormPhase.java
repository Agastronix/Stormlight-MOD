package com.agastronix.stormlight.highstorm;

public enum HighstormPhase {
    NONE(0),
    WARNING(Highstorm.WARNING_TICKS),
    STORM(Highstorm.STORM_TICKS),
    RAIN(Highstorm.RAIN_TICKS);

    public final int length;

    HighstormPhase(int length) {
        this.length = length;
    }

    public HighstormPhase next() {
        return switch (this) {
            case WARNING -> STORM;
            case STORM -> RAIN;
            default -> NONE;
        };
    }

    public static HighstormPhase byId(int id) {
        HighstormPhase[] phases = values();
        return id >= 0 && id < phases.length ? phases[id] : NONE;
    }

    public static HighstormPhase byName(String name) {
        for (HighstormPhase phase : values()) {
            if (phase.name().equals(name)) return phase;
        }
        return NONE;
    }
}
