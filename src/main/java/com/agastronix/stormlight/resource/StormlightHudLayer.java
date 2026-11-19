package com.agastronix.stormlight.resource;

import com.agastronix.stormlight.StormlightArchiveMOD;
import com.agastronix.stormlight.radiant.ClientRadiantData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = StormlightArchiveMOD.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class StormlightHudLayer implements LayeredDraw.Layer {

    private static final int BAR_WIDTH  = 81;   // same width as the vanilla XP bar
    private static final int BAR_HEIGHT = 5;
    private static final int BG_COLOR   = 0xFF101820; // dark backing (ARGB)
    private static final int FILL_COLOR = 0xFF5FD0FF; // light blue (ARGB) — tweak freely

    @SubscribeEvent
    public static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.EXPERIENCE_BAR,
                ResourceLocation.fromNamespaceAndPath(StormlightArchiveMOD.MODID, "stormlight_bar"),
                new StormlightHudLayer()
        );
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (ClientRadiantData.oath < 1) return;
        if (ClientStormlightData.max <= 0) return;

        float ratio = Math.max(0.0F, Math.min(1.0F,
                ClientStormlightData.stormlight / ClientStormlightData.max));

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        // Right edge of the hotbar is (centerX + 91); the hunger bar lives in that right half.
        // Right-align the Stormlight bar to it, and lift it above the hunger row (screenH - 39).
        int x = (screenW / 2 + 91) - BAR_WIDTH;
        int y = screenH - 50;

        graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xFF000000); // outline
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, BG_COLOR);                    // backing
        graphics.fill(x, y, x + (int) (BAR_WIDTH * ratio), y + BAR_HEIGHT, FILL_COLOR);  // fill
    }
}