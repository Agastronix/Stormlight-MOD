package com.agastronix.stormlight.radiant;

import com.agastronix.stormlight.resource.StormlightAttachments;
import com.agastronix.stormlight.shardblade.DeadeyeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public class RadiantProgression {

    // --- Scheduled title lines, so we can stagger the First Ideal reveal ---
    private record PendingLine(ServerPlayer player, Component text, int showAtTick) {}
    private static final List<PendingLine> PENDING = new ArrayList<>();
    private static final int LINE_GAP = 45; // ticks between lines (~2.25s)

    public static void swearOath(ServerPlayer player, int newOath) {
        int oldOath = RadiantAttachments.getOath(player);

        if (newOath >= 1 && RadiantAttachments.getOrder(player) == RadiantOrder.NONE) {
            RadiantAttachments.setOrder(player, RadiantOrder.WINDRUNNER);
        }

        RadiantAttachments.setOath(player, newOath);

        // A Radiant can't hold a Deadeye bond: the dead blade comes loose as an ordinary item.
        if (newOath >= 1) {
            DeadeyeManager.releaseBond(player);
        }

        // Speaking an ideal fills you with Stormlight and ignites radiance.
        if (newOath >= 1 && newOath > oldOath) {
            StormlightAttachments.set(player, StormlightAttachments.MAX_STORMLIGHT);
            RadiantMode.activate(player);
            showIdeal(player, RadiantAttachments.getOrder(player), newOath);
        }

        RadiantNetwork.sendToClient(player);
    }

    private static void showIdeal(ServerPlayer player, RadiantOrder order, int oath) {
        List<Component> lines = idealLines(order, oath);
        if (lines.isEmpty()) return;

        // Snap each line in (no long fade) so staggered swaps look clean.
        player.connection.send(new ClientboundSetTitlesAnimationPacket(2, LINE_GAP + 20, 8));

        // First line now; the rest get scheduled from the server tick.
        sendTitle(player, lines.get(0));
        int tick = player.tickCount;
        for (int i = 1; i < lines.size(); i++) {
            PENDING.add(new PendingLine(player, lines.get(i), tick + i * LINE_GAP));
        }

        // Full text in chat too, always legible.
        for (Component line : lines) {
            player.sendSystemMessage(line.copy().withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        }
    }

    /** Called every server tick to release any scheduled lines that are due. */
    public static void tick(ServerPlayer player) {
        if (PENDING.isEmpty()) return;
        PENDING.removeIf(pending -> {
            if (pending.player() == player && player.tickCount >= pending.showAtTick()) {
                sendTitle(player, pending.text());
                return true;
            }
            // Drop stale entries for players who logged out.
            return pending.player() == player && player.tickCount >= pending.showAtTick() + 200;
        });
    }

    private static void sendTitle(ServerPlayer player, Component line) {
        // Put the line in the SUBTITLE slot (half the size of the big title), title left empty.
        player.connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                line.copy().withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)));
    }

    private static List<Component> idealLines(RadiantOrder order, int oath) {
        List<Component> lines = new ArrayList<>();
        if (oath == 1) { // shared First Ideal
            lines.add(Component.literal("Life before Death"));
            lines.add(Component.literal("Strength before Weakness"));
            lines.add(Component.literal("Journey before Destination"));
            return lines;
        }
        switch (order) {
            case WINDRUNNER -> {
                switch (oath) {
                    case 2 -> lines.add(Component.literal("I will protect those who cannot protect themselves."));
                    case 3 -> lines.add(Component.literal("I will protect even those I hate, so long as it is right."));
                    default -> {}
                }
            }
            case EDGEDANCER -> {
                switch (oath) {
                    case 2 -> lines.add(Component.literal("I will remember those who have been forgotten."));
                    case 3 -> lines.add(Component.literal("I will listen to those who have been ignored."));
                    default -> {}
                }
            }
            default -> {}
        }
        return lines;
    }

    /** Full reset — the bond is severed and the spren returns to Shadesmar. */
    public static void breakBond(ServerPlayer player) {
        RadiantMode.deactivate(player);              // strips glow + speed/strength if lit
        RadiantAttachments.setOath(player, 0);
        RadiantAttachments.setOrder(player, RadiantOrder.NONE);
        StormlightAttachments.set(player, 0.0F);
        OathProgress.reset(player);
        RadiantNetwork.sendToClient(player);
    }
}