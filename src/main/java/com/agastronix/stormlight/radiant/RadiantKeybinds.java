package com.agastronix.stormlight.radiant;

import com.agastronix.stormlight.StormlightArchiveMOD;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = StormlightArchiveMOD.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RadiantKeybinds {
    public static final KeyMapping TOGGLE_RADIANT = new KeyMapping(
            "key.stormlightarchivemod.toggle_radiant",
            GLFW.GLFW_KEY_Z,
            "key.categories.stormlightarchivemod"
    );

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_RADIANT);
    }
}