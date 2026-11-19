package com.agastronix.stormlight.client;

import com.agastronix.stormlight.shardblade.BondDeadeyePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;

public class DeadeyeBondScreen extends Screen {
    private final InteractionHand hand;

    public DeadeyeBondScreen(InteractionHand hand) {
        super(Component.literal("Deadeye"));
        this.hand = hand;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        this.addRenderableWidget(Button.builder(
                Component.literal("Bond"),
                btn -> {
                    PacketDistributor.sendToServer(new BondDeadeyePayload(hand));
                    this.onClose();
                }).bounds(cx - 100, cy - 8, 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                btn -> this.onClose()
        ).bounds(cx - 100, cy + 16, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Draws the blurred background and widgets; all text goes on top afterwards.
        super.render(graphics, mouseX, mouseY, partialTick);

        int cx = this.width / 2;
        int cy = this.height / 2;
        graphics.drawCenteredString(this.font, Component.literal("The blade is silent. Its spren is dead."), cx, cy - 44, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.literal("Bond this blade?"), cx, cy - 30, 0xAAAAAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
