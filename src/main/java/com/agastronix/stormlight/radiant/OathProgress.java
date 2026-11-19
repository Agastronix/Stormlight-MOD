package com.agastronix.stormlight.radiant;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Server-side bookkeeping for {@link OathTask} progress, stored in {@link RadiantAttachments#OATH_PROGRESS}. */
public class OathProgress {

    public static int get(Player player, OathTask task) {
        return player.getData(RadiantAttachments.OATH_PROGRESS.get()).getOrDefault(task.id(), 0);
    }

    private static void set(ServerPlayer player, OathTask task, int value) {
        Map<String, Integer> map = new HashMap<>(player.getData(RadiantAttachments.OATH_PROGRESS.get()));
        map.put(task.id(), Math.min(value, task.target()));
        player.setData(RadiantAttachments.OATH_PROGRESS.get(), map);
    }

    /** Counts one step, but only toward the player's NEXT Ideal in their CURRENT order. */
    public static void increment(ServerPlayer player, OathTask task) {
        if (task.order() != RadiantAttachments.getOrder(player)) return;
        if (task.oath() != RadiantAttachments.getOath(player) + 1) return;
        int current = get(player, task);
        if (current >= task.target()) return;
        set(player, task, current + 1);
        sendToClient(player);
    }

    /** True when every task for {@code oath} (in the player's order) is complete. */
    public static boolean isMet(Player player, int oath) {
        for (OathTask task : OathTask.tasksFor(RadiantAttachments.getOrder(player), oath)) {
            if (get(player, task) < task.target()) return false;
        }
        return true;
    }

    /** Debug: fill every task for the player's next Ideal. */
    public static void completeNext(ServerPlayer player) {
        int next = RadiantAttachments.getOath(player) + 1;
        for (OathTask task : OathTask.tasksFor(RadiantAttachments.getOrder(player), next)) {
            set(player, task, task.target());
        }
        sendToClient(player);
    }

    public static void reset(ServerPlayer player) {
        player.setData(RadiantAttachments.OATH_PROGRESS.get(), Map.of());
        sendToClient(player);
    }

    public static void sendToClient(ServerPlayer player) {
        List<Integer> values = new ArrayList<>();
        for (OathTask task : OathTask.values()) values.add(get(player, task));
        PacketDistributor.sendToPlayer(player, new OathProgressPayload(values));
    }
}
