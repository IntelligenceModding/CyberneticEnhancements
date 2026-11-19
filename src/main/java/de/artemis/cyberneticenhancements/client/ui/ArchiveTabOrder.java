package de.artemis.cyberneticenhancements.client.ui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class ArchiveTabOrder {
    private ArchiveTabOrder() {
    }

    public static List<String> normalize(List<String> available, List<? extends String> saved) {
        var result = new LinkedHashSet<String>();
        for (String id : saved) if (available.contains(id)) result.add(id);
        result.addAll(available);
        return List.copyOf(result);
    }

    public static List<String> move(List<String> order, String id, int target) {
        var result = new ArrayList<>(order);
        if (!result.remove(id)) return List.copyOf(order);
        result.add(Math.clamp(target, 0, result.size()), id);
        return List.copyOf(result);
    }

    public static int dropIndex(List<String> order, List<Integer> centers, String dragged, double mouseX) {
        if (order.size() != centers.size()) throw new IllegalArgumentException("Tab geometry does not match order");
        int target = 0;
        for (int i = 0; i < order.size(); i++) {
            if (!order.get(i).equals(dragged) && mouseX > centers.get(i)) target++;
        }
        return target;
    }
}
