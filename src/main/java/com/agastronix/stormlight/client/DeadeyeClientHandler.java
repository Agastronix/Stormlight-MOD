package com.agastronix.stormlight.client;

import com.agastronix.stormlight.shardblade.OpenDeadeyeBondPayload;
import net.minecraft.client.Minecraft;

public class DeadeyeClientHandler {
    public static void open(OpenDeadeyeBondPayload payload) {
        Minecraft.getInstance().setScreen(new DeadeyeBondScreen(payload.hand()));
    }
}
