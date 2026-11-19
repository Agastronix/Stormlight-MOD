package com.agastronix.stormlight.client;

import com.agastronix.stormlight.radiant.OpenSprenDialoguePayload;
import net.minecraft.client.Minecraft;

public class HonorSprenClientHandler {
    public static void open(OpenSprenDialoguePayload payload) {
        Minecraft.getInstance().setScreen(new HonorSprenScreen(
                payload.entityId(), payload.currentOath(), payload.maxOath(), payload.manage()));
    }
}