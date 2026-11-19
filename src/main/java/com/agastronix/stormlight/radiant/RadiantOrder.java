package com.agastronix.stormlight.radiant;

import net.minecraft.util.StringRepresentable;

public enum RadiantOrder implements StringRepresentable {
    NONE("none"),
    WINDRUNNER("windrunner"),
    EDGEDANCER("edgedancer");

    public static final StringRepresentable.EnumCodec<RadiantOrder> CODEC =
            StringRepresentable.fromEnum(RadiantOrder::values);

    private final String name;
    RadiantOrder(String name) { this.name = name; }

    @Override
    public String getSerializedName() { return name; }
}