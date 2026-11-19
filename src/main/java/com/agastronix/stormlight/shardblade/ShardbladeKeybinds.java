package com.agastronix.stormlight.shardblade;

import com.agastronix.stormlight.StormlightArchiveMOD;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = StormlightArchiveMOD.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ShardbladeKeybinds {
    public static final KeyMapping TOGGLE_SHARDBLADE = new KeyMapping(
            "key.stormlightarchivemod.toggle_shardblade",
            GLFW.GLFW_KEY_X,
            "key.categories.stormlightarchivemod"
    );

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_SHARDBLADE);
    }
}