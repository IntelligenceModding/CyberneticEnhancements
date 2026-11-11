package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.client.ui.*;
import de.artemis.cyberneticenhancements.common.fabrication.*;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.*;

import static de.artemis.cyberneticenhancements.common.fabrication.FabricatorLayout.*;

public final class FabricatorScreen extends ResponsiveContainerScreen<FabricatorMenu> {
    private EditBox search;
    private final Map<Integer, UiButton> actions = new HashMap<>();
    private final UiScrollbar scrollbar = new UiScrollbar();
    private final List<String> categories = new ArrayList<>();
    private int scroll, filter, category;
    private UiButton categoryButton, filterButton;

    public FabricatorScreen(FabricatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
    }

    @Override
    protected void init() {
        String query = search == null ? "" : search.getValue();
        super.init();
        actions.clear();
        categories.clear();
        categories.add("All categories");
        menu.availableRecipes().stream().map(r -> r.value().category()).distinct().sorted().forEach(categories::add);
        category = Math.min(category, categories.size() - 1);
        search = new UiEditBox(font, leftPos + LIST_X, topPos + 42, 202, 20, Component.literal("Search designs"));
        search.setHint(Component.literal("Search designs"));
        search.setMaxLength(80);
        search.setValue(query);
        search.setResponder(value -> scroll = 0);
        addRenderableWidget(search);
        categoryButton = addRenderableWidget(new UiButton(leftPos + LIST_X, topPos + 70, 124, 20, Component.literal(categories.get(category)), b -> {
            category = (category + 1) % categories.size();
            b.setMessage(Component.literal(categories.get(category)));
            ((UiButton) b).setSelected(category > 0);
            scroll = 0;
        }));
        categoryButton.setSelected(category > 0);
        filterButton = addRenderableWidget(new UiButton(leftPos + 150, topPos + 70, 72, 20, Component.literal(filterName()), b -> {
            filter = (filter + 1) % 2;
            b.setMessage(Component.literal(filterName()));
            ((UiButton) b).setSelected(filter > 0);
            scroll = 0;
        }));
        filterButton.setSelected(filter > 0);
        action("Fill grid", 252, ACTION_Y, 88, 0);
        action("Fabricate", 452, ACTION_Y, 96, 1);
        action("Cancel", 558, ACTION_Y, 82, 2);
        action("Clear grid", 350, ACTION_Y, 88, 4);
    }

    private String filterName() {
        return new String[]{"All tiers", "My tier"}[filter];
    }

    private void action(String label, int x, int y, int width, int id) {
        UiButton button = new UiButton(leftPos + x, topPos + y, width, 20, Component.literal(label), b -> {
            if (minecraft != null && minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        });
        actions.put(id, button);
        addRenderableWidget(button);
    }

    private int supportedTier() {
        return menu.capability() == 1 ? 2 : menu.capability() == 2 ? 4 : 5;
    }

    private String machineName() {
        return menu.capability() == 1 ? "Basic Assembly" : menu.capability() == 2 ? "Precision Assembly" : "Restricted Assembly";
    }

    private boolean outputFits(FabricationRecipe recipe) {
        ItemStack result = recipe.assemble(menu.station.input(), minecraft.level.registryAccess()), output = menu.station.getItem(26);
        return output.isEmpty() || ItemStack.isSameItemSameComponents(output, result) && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void updateActions() {
        if (minecraft == null || minecraft.level == null) return;
        refreshCategories();
        boolean busy = menu.isWorking();
        var selected = menu.selected();
        actions.get(0).active = !busy && selected != null && SchematicKnowledge.clientKnows(selected.value().schematic());
        actions.get(2).active = busy;
        actions.get(4).active = !busy && java.util.stream.IntStream.range(0, 25).anyMatch(i -> !menu.station.getItem(i).isEmpty());
        var candidate = selected != null ? selected : menu.availableRecipes().stream().filter(r -> r.value().matches(menu.station.input(), minecraft.level)).findFirst().orElse(null);
        boolean ready = !busy && candidate != null && candidate.value().tier() <= supportedTier() && SchematicKnowledge.clientKnows(candidate.value().schematic())
                && candidate.value().matches(menu.station.input(), minecraft.level) && outputFits(candidate.value());
        actions.get(1).active = ready;
        actions.get(1).setSelected(ready);
    }

    private List<RecipeHolder<FabricationRecipe>> visible() {
        String query = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT);
        return menu.availableRecipes().stream().filter(r -> {
            return (filter == 0 || r.value().tier() <= supportedTier()) && (category == 0 || r.value().category().equals(categories.get(category)))
                    && r.value().result().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query);
        }).toList();
    }

    private void refreshCategories() {
        List<String> next = new ArrayList<>();
        next.add("All categories");
        menu.availableRecipes().stream().map(r -> r.value().category()).distinct().sorted().forEach(next::add);
        if (categories.equals(next)) return;
        String previous = categories.get(category);
        categories.clear();
        categories.addAll(next);
        category = Math.max(0, categories.indexOf(previous));
        categoryButton.setMessage(Component.literal(categories.get(category)));
        categoryButton.setSelected(category > 0);
        scroll = 0;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        UiRenderer.panel(g, x, y, WIDTH, HEIGHT, StationScreenStyle.PANEL_DEEP);
        UiRenderer.panel(g, x + BROWSER_X, y + PANEL_Y, BROWSER_WIDTH, PANEL_HEIGHT, StationScreenStyle.PANEL_BG);
        UiRenderer.panel(g, x + ASSEMBLY_X, y + PANEL_Y, ASSEMBLY_WIDTH, PANEL_HEIGHT, StationScreenStyle.PANEL_BG);
        UiRenderer.panel(g, x + INVENTORY_X, y + INVENTORY_Y, INVENTORY_WIDTH, INVENTORY_HEIGHT, StationScreenStyle.PANEL_BG);
        g.drawString(font, "1. Choose a design", x + LIST_X, y + 20, UiRenderer.TEXT, false);
        g.drawString(font, "2. Assemble", x + ASSEMBLY_X + 8, y + 20, UiRenderer.TEXT, false);
        g.drawString(font, machineName(), x + ASSEMBLY_X + ASSEMBLY_WIDTH - 8 - font.width(machineName()), y + 20, UiRenderer.ACCENT, false);
        g.drawString(font, playerInventoryTitle, x + INVENTORY_X + 8, y + INVENTORY_Y + 8, UiRenderer.TEXT_SECONDARY, false);
        for (var slot : menu.slots)
            StationSlotRenderer.drawStandardSlot(g, x, y, slot.x, slot.y, StationScreenStyle.SLOT_BG);
        centered(g, "Assembly grid", x + GRID_X + 2 * GRID_STEP + 8, y + 75, UiRenderer.TEXT_SECONDARY);
        centered(g, "Output", x + OUTPUT_X + 8, y + OUTPUT_Y - 21, UiRenderer.TEXT_SECONDARY);
        UiRenderer.arrow(g, x + 476, y + OUTPUT_Y + 4, UiRenderer.ACCENT);
        var selected = menu.selected();
        if (selected == null) {
            g.drawString(font, "Select a design to preview its assembly.", x + ASSEMBLY_X + 8, y + 42, UiRenderer.TEXT_SECONDARY, false);
        } else {
            var r = selected.value();
            g.drawString(font, font.plainSubstrByWidth(r.result().getHoverName().getString(), ASSEMBLY_WIDTH - 16), x + ASSEMBLY_X + 8, y + 42, UiRenderer.TEXT, false);
            g.drawString(font, "Tier " + r.tier() + "  /  " + formatSeconds(r.ticks()) + " s  /  " + (SchematicKnowledge.clientKnows(r.schematic()) ? "Design known" : "Schematic required"), x + ASSEMBLY_X + 8, y + 58, UiRenderer.TEXT_SECONDARY, false);
            if (SchematicKnowledge.clientKnows(r.schematic())) for (var part : r.parts())
                if (menu.station.getItem(part.slot()).isEmpty()) {
                    ItemStack[] choices = part.ingredient().getItems();
                    if (choices.length == 0) continue;
                    var slot = menu.slots.get(part.slot());
                    g.renderFakeItem(choices[(int) (System.currentTimeMillis() / 1500 % choices.length)].copyWithCount(part.count()), x + slot.x, y + slot.y);
                    g.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0x99000000);
                    String count = Integer.toString(part.count());
                    g.drawString(font, count, x + slot.x + 17 - font.width(count), y + slot.y + 9, UiRenderer.TEXT_SECONDARY, false);
                }
            if (menu.station.getItem(26).isEmpty()) {
                g.renderFakeItem(r.result(), x + OUTPUT_X, y + OUTPUT_Y);
                g.fill(x + OUTPUT_X, y + OUTPUT_Y, x + OUTPUT_X + 16, y + OUTPUT_Y + 16, 0x88000000);
            }
        }
        double progress = selected == null ? 0 : Math.clamp(menu.progress() / (double) selected.value().ticks(), 0, 1);
        g.fill(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + PROGRESS_WIDTH, y + PROGRESS_Y + 6, UiRenderer.FIELD);
        g.fill(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + (int) (PROGRESS_WIDTH * progress), y + PROGRESS_Y + 6, UiRenderer.ACCENT);
        wrapped(g, status(), x + PROGRESS_X, y + STATUS_Y, PROGRESS_WIDTH, 2, actions.get(1).active ? UiRenderer.ACCENT : UiRenderer.TEXT_SECONDARY);
        renderDesigns(g, mouseX, mouseY);
    }

    private void renderDesigns(GuiGraphics g, int mouseX, int mouseY) {
        var list = visible();
        scroll = Math.clamp(scroll, 0, Math.max(0, list.size() - VISIBLE_ROWS));
        var selected = menu.selected();
        for (int row = 0; row < VISIBLE_ROWS && scroll + row < list.size(); row++) {
            var holder = list.get(scroll + row);
            int xx = leftPos + LIST_X, yy = topPos + LIST_Y + row * ROW_HEIGHT;
            boolean chosen = selected != null && selected.id().equals(holder.id()), known = SchematicKnowledge.clientKnows(holder.value().schematic());
            UiRenderer.control(g, xx, yy, LIST_WIDTH, ROW_HEIGHT - 2, true, chosen, inside(mouseX, mouseY, LIST_X, LIST_Y + row * ROW_HEIGHT, LIST_WIDTH, ROW_HEIGHT - 2), 0xFFFFFFFF);
            g.renderFakeItem(holder.value().result(), xx + 6, yy + 4);
            g.drawString(font, font.plainSubstrByWidth(holder.value().result().getHoverName().getString(), LIST_WIDTH - 34), xx + 28, yy + 3, known ? UiRenderer.TEXT : UiRenderer.TEXT_SECONDARY, false);
            g.drawString(font, "Tier " + holder.value().tier() + "  /  " + (known ? "Known" : "Locked"), xx + 28, yy + 14, known ? UiRenderer.ACCENT : UiRenderer.TEXT_SECONDARY, false);
        }
        if (list.isEmpty())
            wrapped(g, "No matching designs.", leftPos + LIST_X, topPos + LIST_Y + 12, LIST_WIDTH, 2, UiRenderer.TEXT_SECONDARY);
        UiScrollbar.draw(g, scrollTrack(), mouseX, mouseY, scrollbar.dragging());
        g.drawString(font, list.size() + " designs", leftPos + LIST_X, topPos + 290, UiRenderer.TEXT_SECONDARY, false);
    }

    private String status() {
        var holder = menu.selected();
        if (holder == null) return "Choose a design, fill the grid, then fabricate.";
        var r = holder.value();
        if (menu.isWorking())
            return !outputFits(r) ? "Paused: empty the output bay to continue." : "Fabricating: " + formatSeconds(menu.progress()) + " / " + formatSeconds(r.ticks()) + " s";
        if (r.tier() > supportedTier())
            return "Requires " + (r.tier() <= 4 ? "Precision" : "Restricted") + " Assembly. Use an upgrade module on this machine.";
        if (!SchematicKnowledge.clientKnows(r.schematic()))
            return "Right-click its schematic item to learn this design.";
        for (int i = 0; i < 25; i++) {
            final int slot = i;
            var part = r.parts().stream().filter(p -> p.slot() == slot).findFirst().orElse(null);
            var stack = menu.station.getItem(i);
            if (!stack.isEmpty() && (part == null || !part.ingredient().test(stack)))
                return "Some parts are in the wrong slots. Move them or clear the grid.";
        }
        int missing = r.parts().stream().mapToInt(p -> Math.max(0, p.count() - menu.station.getItem(p.slot()).getCount())).sum();
        if (missing > 0)
            return "Missing " + missing + " parts. Fill grid adds available materials from your inventory.";
        if (!outputFits(r)) return "Empty the output bay before starting another assembly.";
        return "Ready to fabricate. Materials are consumed when assembly completes.";
    }

    private String formatSeconds(int ticks) {
        return String.format(Locale.ROOT, "%.1f", ticks / 20.0);
    }

    private void wrapped(GuiGraphics g, String text, int x, int y, int width, int maxLines, int color) {
        var lines = font.split(Component.literal(text), width);
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++)
            g.drawString(font, lines.get(i), x, y + i * 11, color, false);
    }

    private void centered(GuiGraphics g, String text, int x, int y, int color) {
        g.drawString(font, text, x - font.width(text) / 2, y, color, false);
    }

    private boolean inside(double x, double y, int rx, int ry, int width, int height) {
        return x >= leftPos + rx && x < leftPos + rx + width && y >= topPos + ry && y < topPos + ry + height;
    }

    @Override
    protected void renderContent(GuiGraphics g, int x, int y, float partial) {
        updateActions();
        super.renderContent(g, x, y, partial);
        renderTooltip(g, x, y);
        var selected = menu.selected();
        if (selected != null && SchematicKnowledge.clientKnows(selected.value().schematic()))
            for (var part : selected.value().parts()) {
                var slot = menu.slots.get(part.slot());
                if (menu.station.getItem(part.slot()).isEmpty() && inside(x, y, slot.x, slot.y, 16, 16)) {
                    var values = part.ingredient().getItems();
                    if (values.length > 0)
                        g.renderTooltip(font, List.of(values[0].getHoverName(), Component.literal("Required: " + part.count())), Optional.empty(), x, y);
                }
            }
        var list = visible();
        if (inside(x, y, LIST_X, LIST_Y, LIST_WIDTH, VISIBLE_ROWS * ROW_HEIGHT)) {
            int index = scroll + (y - topPos - LIST_Y) / ROW_HEIGHT;
            if (index < list.size() && font.width(list.get(index).value().result().getHoverName()) > LIST_WIDTH - 34)
                g.renderTooltip(font, list.get(index).value().result().getHoverName(), x, y);
        }
    }

    @Override
    protected boolean mouseClickedContent(double x, double y, int button) {
        var track = scrollTrack();
        if (button == 0 && scrollbar.begin(track, x, y)) {
            scroll = (int) Math.round(scrollbar.value(track, x, y));
            return true;
        }
        if (button == 0 && inside(x, y, LIST_X, LIST_Y, LIST_WIDTH, VISIBLE_ROWS * ROW_HEIGHT)) {
            var list = visible();
            int index = scroll + (int) (y - topPos - LIST_Y) / ROW_HEIGHT;
            if (index < list.size() && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 100 + menu.recipes().indexOf(list.get(index)));
                return true;
            }
        }
        return super.mouseClickedContent(x, y, button);
    }

    private UiScrollbar.Track scrollTrack() {
        return UiScrollbar.layout(leftPos + SCROLL_X, topPos + LIST_Y, UiScrollbar.WIDTH, VISIBLE_ROWS * ROW_HEIGHT, false, VISIBLE_ROWS, Math.max(0, visible().size() - VISIBLE_ROWS), scroll);
    }

    @Override
    protected boolean mouseDraggedContent(double x, double y, int button, double dx, double dy) {
        if (button == 0 && scrollbar.dragging()) {
            scroll = (int) Math.round(scrollbar.value(scrollTrack(), x, y));
            return true;
        }
        return super.mouseDraggedContent(x, y, button, dx, dy);
    }

    @Override
    protected boolean mouseReleasedContent(double x, double y, int button) {
        scrollbar.release();
        return super.mouseReleasedContent(x, y, button);
    }

    @Override
    protected boolean mouseScrolledContent(double x, double y, double dx, double dy) {
        if (inside(x, y, LIST_X, LIST_Y, BROWSER_WIDTH - 16, VISIBLE_ROWS * ROW_HEIGHT)) {
            scroll = Math.clamp(scroll - (int) Math.round(dy * UiScrollState.WHEEL_STEP / ROW_HEIGHT), 0, Math.max(0, visible().size() - VISIBLE_ROWS));
            return true;
        }
        return super.mouseScrolledContent(x, y, dx, dy);
    }
}
