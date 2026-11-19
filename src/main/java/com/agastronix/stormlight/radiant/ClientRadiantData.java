package com.agastronix.stormlight.radiant;

public class ClientRadiantData {
    public static volatile int order = 0;   // ordinal into RadiantOrder.values()
    public static volatile int oath = 0;
    public static volatile boolean active = false;

    public static void set(int order, int oath, boolean active) {
        ClientRadiantData.order = order;
        ClientRadiantData.oath = oath;
        ClientRadiantData.active = active;
    }
}