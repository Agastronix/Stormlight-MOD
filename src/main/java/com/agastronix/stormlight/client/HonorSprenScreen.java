package com.agastronix.stormlight.client;

import com.agastronix.stormlight.radiant.BreakBondPayload;
import com.agastronix.stormlight.radiant.ClientOathProgress;
import com.agastronix.stormlight.radiant.ClientRadiantData;
import com.agastronix.stormlight.radiant.OathTask;
import com.agastronix.stormlight.radiant.RadiantOrder;
import com.agastronix.stormlight.radiant.SwearWordsPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class HonorSprenScreen extends Screen {
    private static final int LINE_HEIGHT = 12;
    private static final int DENIED_MESSAGE_TICKS = 40;

    private final int entityId;
    private final int currentOath;
    private final int maxOath;
    private final boolean manage;
    private boolean confirmingBreak = false;
    private Button swearButton;
    private int deniedTicks = 0;

    public HonorSprenScreen(int entityId, int currentOath, int maxOath, boolean manage) {
        super(manage ? Component.literal("Your Bond") : sprenName(entityId));
        this.entityId = entityId;
        this.currentOath = currentOath;
        this.maxOath = maxOath;
        this.manage = manage;
    }

    /** Title the window with the actual spren species, whichever kind was clicked. */
    private static Component sprenName(int entityId) {
        ClientLevel level = Minecraft.getInstance().level;
        Entity spren = level == null ? null : level.getEntity(entityId);
        return spren == null ? Component.literal("A Spren") : spren.getDisplayName();
    }

    private static RadiantOrder clientOrder() {
        RadiantOrder[] orders = RadiantOrder.values();
        int ordinal = ClientRadiantData.order;
        return ordinal >= 0 && ordinal < orders.length ? orders[ordinal] : RadiantOrder.NONE;
    }

    /** Tasks for the next Ideal (none for the first bond — the First Ideal is free). */
    private List<OathTask> nextTasks() {
        return manage ? OathTask.tasksFor(clientOrder(), currentOath + 1) : List.of();
    }

    /** Read live every frame from synced data, so progress updates while the screen is open. */
    private boolean requirementsMet() {
        return !manage || ClientOathProgress.isMet(clientOrder(), currentOath + 1);
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        boolean canAdvance = currentOath < maxOath;
        swearButton = null;

        if (canAdvance) {
            swearButton = this.addRenderableWidget(Button.builder(
                    Component.literal("Say the Words"),
                    btn -> {
                        PacketDistributor.sendToServer(new SwearWordsPayload(entityId));
                        this.onClose();
                    }).bounds(cx - 100, cy - 8, 200, 20).build());
            swearButton.active = requirementsMet();
        }

        if (manage) {
            this.addRenderableWidget(Button.builder(
                    Component.literal(confirmingBreak ? "Really break the bond?" : "Break Bond"),
                    btn -> {
                        if (!confirmingBreak) {
                            confirmingBreak = true;
                            this.rebuildWidgets();
                        } else {
                            PacketDistributor.sendToServer(new BreakBondPayload(entityId));
                            this.onClose();
                        }
                    }).bounds(cx - 100, cy + 16, 200, 20).build());
        }

        this.addRenderableWidget(Button.builder(
                Component.literal(manage ? "Close" : "Leave"),
                btn -> this.onClose()
        ).bounds(cx - 100, cy + 40, 200, 20).build());
    }

    @Override
    public void tick() {
        super.tick();
        if (deniedTicks > 0) deniedTicks--;
    }

    // A disabled vanilla Button swallows no clicks, so catch clicks on the greyed-out one here.
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (swearButton != null && !swearButton.active && button == 0
                && mouseX >= swearButton.getX() && mouseX < swearButton.getX() + swearButton.getWidth()
                && mouseY >= swearButton.getY() && mouseY < swearButton.getY() + swearButton.getHeight()) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            deniedTicks = DENIED_MESSAGE_TICKS;
            return true;                                       // nothing is sent
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (swearButton != null) swearButton.active = requirementsMet();

        // Draws the blurred background and widgets; all text goes on top afterwards.
        super.render(graphics, mouseX, mouseY, partialTick);

        int cx = this.width / 2;
        int cy = this.height / 2;

        List<OathTask> tasks = currentOath < maxOath ? nextTasks() : List.of();
        int taskTop = cy - 20 - LINE_HEIGHT * Math.max(0, tasks.size() - 1);

        Component prompt = manage
                ? Component.literal("Your bonded spren")
                : Component.literal("The spren regards you, waiting.");
        Component sub;
        if (!manage) {
            sub = Component.literal("Will you speak the Words?");
        } else if (currentOath >= maxOath) {
            sub = Component.literal("Oath " + currentOath + " — you have spoken all the Words");
        } else if (tasks.isEmpty()) {
            sub = Component.literal("Oath " + currentOath);
        } else {
            sub = Component.literal("Oath " + currentOath + " — before the next Ideal:");
        }

        int subY = tasks.isEmpty() ? cy - 30 : taskTop - LINE_HEIGHT - 4;
        graphics.drawCenteredString(this.font, prompt, cx, subY - 14, 0xFFFFFF);
        graphics.drawCenteredString(this.font, sub, cx, subY, 0xAADDFF);

        for (int i = 0; i < tasks.size(); i++) {
            OathTask task = tasks.get(i);
            int value = Math.min(ClientOathProgress.get(task), task.target());
            boolean done = value >= task.target();
            String text = (done ? "✔ " : "") + task.label() + ": " + value + " / " + task.target();
            graphics.drawCenteredString(this.font, Component.literal(text), cx, taskTop + i * LINE_HEIGHT,
                    done ? 0x55FF55 : 0xCCCCCC);
        }

        if (deniedTicks > 0) {
            graphics.drawCenteredString(this.font, Component.literal("Requirements not met"), cx, cy + 66, 0xFF5555);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
