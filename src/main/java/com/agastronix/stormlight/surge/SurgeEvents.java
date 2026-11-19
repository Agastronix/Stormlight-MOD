package com.agastronix.stormlight.surge;

import com.agastronix.stormlight.StormlightArchiveMOD;
import com.agastronix.stormlight.radiant.RadiantAttachments;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

@EventBusSubscriber(modid = StormlightArchiveMOD.MODID)
public class SurgeEvents {
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player
                && !player.level().isClientSide()
                && RadiantAttachments.isRadiantActive(player)) {
            event.setCanceled(true);
        }
    }
}