package com.agastronix.stormlight.radiant;

import java.util.ArrayList;
import java.util.List;

/**
 * Every task a spren asks of its Radiant before the next Ideal can be spoken.
 * All requirement thresholds live here. The First Ideal has no tasks.
 */
public enum OathTask {
    WINDRUNNER_ZOMBIES_DEFENDING_VILLAGERS("windrunner_zombies", RadiantOrder.WINDRUNNER, 2, 10,
            "Zombies slain defending villagers"),
    WINDRUNNER_RAIDS_WON("windrunner_raids", RadiantOrder.WINDRUNNER, 3, 1,
            "Raids won"),
    EDGEDANCER_MOBS_HEALED("edgedancer_heals", RadiantOrder.EDGEDANCER, 2, 8,
            "Injured creatures healed"),
    EDGEDANCER_WANDERER_TRADES("edgedancer_trades", RadiantOrder.EDGEDANCER, 3, 3,
            "Trades with wandering traders");

    private final String id;
    private final RadiantOrder order;
    private final int oath;      // the Ideal this task unlocks
    private final int target;
    private final String label;

    OathTask(String id, RadiantOrder order, int oath, int target, String label) {
        this.id = id;
        this.order = order;
        this.oath = oath;
        this.target = target;
        this.label = label;
    }

    public String id() { return id; }
    public RadiantOrder order() { return order; }
    public int oath() { return oath; }
    public int target() { return target; }
    public String label() { return label; }

    /** Tasks that must be finished before {@code order} may speak Ideal {@code oath}. */
    public static List<OathTask> tasksFor(RadiantOrder order, int oath) {
        List<OathTask> tasks = new ArrayList<>();
        for (OathTask task : values()) {
            if (task.order == order && task.oath == oath) tasks.add(task);
        }
        return tasks;
    }
}
