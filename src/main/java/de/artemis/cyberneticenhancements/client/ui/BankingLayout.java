package de.artemis.cyberneticenhancements.client.ui;

import java.util.ArrayList;
import java.util.List;

public final class BankingLayout {
    private BankingLayout() {}

    public record Box(int x, int y, int width, int height) {}
    public record Dashboard(List<Box> stats, Box graph, Box transfer, Box balance, int bottom) {}

    public static Dashboard dashboard(int width) {
        if (width < 240) throw new IllegalArgumentException("Banking panel is too narrow");
        int columns = width >= 620 ? 4 : 2;
        var stats = new ArrayList<Box>(4);
        for (int index = 0; index < 4; index++) {
            int column = index % columns;
            int left = column * (width + 10) / columns;
            int right = (column + 1) * (width + 10) / columns - 10;
            stats.add(new Box(left, index / columns * 84, right - left, 74));
        }
        int top = ((4 + columns - 1) / columns) * 84;
        boolean split = width >= 700;
        int formWidth = split ? Math.max(270, Math.min(340, width * 36 / 100)) : width;
        int chartWidth = split ? width - formWidth - 10 : width;
        var graph = new Box(0, top, chartWidth, 228);
        var transfer = new Box(split ? chartWidth + 10 : 0, split ? top : top + 238, formWidth, 228);
        var balance = new Box(0, transfer.y + transfer.height + 10, width, 228);
        return new Dashboard(List.copyOf(stats), graph, transfer, balance, balance.y + balance.height);
    }
}
