package com.agastronix.stormlight.radiant;

import java.util.List;

public class ClientOathProgress {
    private static volatile int[] values = new int[OathTask.values().length];

    public static void set(List<Integer> list) {
        int[] next = new int[OathTask.values().length];
        for (int i = 0; i < next.length && i < list.size(); i++) next[i] = list.get(i);
        values = next;
    }

    public static int get(OathTask task) {
        return values[task.ordinal()];
    }

    /** Mirrors {@link OathProgress#isMet}, from synced data. Display only — the server re-checks. */
    public static boolean isMet(RadiantOrder order, int oath) {
        for (OathTask task : OathTask.tasksFor(order, oath)) {
            if (get(task) < task.target()) return false;
        }
        return true;
    }
}
