package com.agastronix.stormlight.surge;

import com.agastronix.stormlight.StormlightArchiveMOD;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = StormlightArchiveMOD.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SurgeKeybinds {
    public static final KeyMapping SURGE_PRIMARY = new KeyMapping(
            "key.stormlightarchivemod.surge_primary", GLFW.GLFW_KEY_G, "key.categories.stormlightarchivemod");

    public static final KeyMapping SURGE_SECONDARY = new KeyMapping(
            "key.stormlightarchivemod.surge_secondary", GLFW.GLFW_KEY_H, "key.categories.stormlightarchivemod");

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(SURGE_PRIMARY);
        event.register(SURGE_SECONDARY);
    }
}