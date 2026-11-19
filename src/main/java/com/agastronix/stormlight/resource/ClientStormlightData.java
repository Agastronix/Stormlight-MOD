package com.agastronix.stormlight.resource;

public class ClientStormlightData {
    public static volatile float stormlight = 0.0F;
    public static volatile float max = 100.0F;

    public static void set(float stormlight, float max) {
        ClientStormlightData.stormlight = stormlight;
        ClientStormlightData.max = max;
    }
}