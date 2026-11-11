package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.client.render.SchematicIconRenderer;
import de.artemis.cyberneticenhancements.client.ui.UiRenderer;
import de.artemis.cyberneticenhancements.client.ui.UiEditBox;
import de.artemis.cyberneticenhancements.client.ui.UiScrollbar;
import de.artemis.cyberneticenhancements.client.ui.UiScrollState;
import de.artemis.cyberneticenhancements.common.cyberware.CyberwareCatalog;
import de.artemis.cyberneticenhancements.common.fabrication.*;
import de.artemis.cyberneticenhancements.common.registry.ModRecipeTypes;
import de.artemis.cyberneticenhancements.client.ui.UiEncryptedText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

final class SchematicBrowser {
    private static final int TILE_WIDTH = 136, TILE_HEIGHT = 104, GAP = 10, HEADER = 62, GRID_HEADER = 62;
    private String query = "", selected = "";
    private int filter, category, maxScroll;
    private final UiScrollState gridScroll = new UiScrollState(), detailScroll = new UiScrollState();
    private final UiScrollbar scrollbar = new UiScrollbar();
    private int mouseX, mouseY;
    private UiEditBox searchField;
    private int x, y, width, height;
    private int gridTop, gridBottom;
    private final List<String> categories = new ArrayList<>();
    private final List<Tile> tiles = new ArrayList<>();

    private record Tile(String id, int x, int y, int width, int height) {
    }

    private boolean inside(double mx, double my, int xx, int yy, int w, int h) {
        return mx >= xx && mx < xx + w && my >= yy && my < yy + h;
    }

    private int categoryWidth() {
        return Math.min(220, Math.max(120, (width - 2 * GAP) / 3));
    }

    private String name(String id) {
        return FabricationContent.schematicName(id).getString();
    }

    private String family(String id) {
        return CyberwareCatalog.get(id).slotType().name().replace('_', ' ');
    }

    private List<String> visible() {
        String search = query.toLowerCase(Locale.ROOT);
        return FabricationContent.SCHEMATICS.keySet().stream().filter(id -> {
            boolean owned = SchematicKnowledge.clientKnows(id);
            return (filter == 0 || filter == 1 && owned || filter == 2 && !owned)
                    && (category == 0 || family(id).equals(categories.get(category)))
                    && (owned ? name(id).toLowerCase(Locale.ROOT) : "encrypted schematic").contains(search);
        }).sorted(Comparator.comparingInt((String id) -> CyberwareCatalog.get(id).tier().ordinal()).thenComparing(id -> id)).toList();
    }

    void render(GuiGraphics g, Font font, int x, int y, int width, int height, int mx, int my) {
        this.mouseX = mx;
        this.mouseY = my;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        categories.clear();
        categories.add("All categories");
        FabricationContent.SCHEMATICS.keySet().stream().map(this::family).distinct().sorted().forEach(categories::add);
        category = Math.min(category, categories.size() - 1);
        tiles.clear();
        UiRenderer.panel(g, x, y, width, height, UiRenderer.PANEL);
        if (!selected.isEmpty() && !SchematicKnowledge.clientKnows(selected)) {
            selected = "";
            detailScroll.reset();
        }
        if (!selected.isEmpty()) {
            renderDetail(g, font, mx, my);
            return;
        }
        int searchWidth = Math.max(80, width - GAP * 3 - categoryWidth());
        if (searchField == null) {
            searchField = new UiEditBox(font, x + GAP, y + GAP, searchWidth, 22, Component.literal("Search learned schematics"));
            searchField.setMaxLength(80);
            searchField.setHint(Component.literal("Search learned schematics..."));
            searchField.setResponder(value -> {
                query = value;
                gridScroll.reset();
            });
        }
        searchField.setX(x + GAP);
        searchField.setY(y + GAP);
        searchField.setWidth(searchWidth);
        searchField.render(g, mx, my, 0);
        box(g, font, x + width - GAP - categoryWidth(), y + GAP, categoryWidth(), 22, "Category: " + categories.get(category), category > 0);
        int buttonWidth = 84;
        for (int i = 0; i < 3; i++)
            box(g, font, x + GAP + i * (buttonWidth + GAP), y + 42, buttonWidth, 20, new String[]{"All", "Owned", "Missing"}[i], filter == i);
        long owned = FabricationContent.SCHEMATICS.keySet().stream().filter(SchematicKnowledge::clientKnows).count();
        String count = owned + " / " + FabricationContent.SCHEMATICS.size() + " learned";
        g.drawString(font, count, x + width - GAP - font.width(count), y + 48, UiRenderer.TEXT_SECONDARY, false);
        var ids = visible();
        int viewport = Math.max(1, height - GRID_HEADER - GAP * 2);
        int pageRows = Math.max(1, (viewport + GAP) / (TILE_HEIGHT + GAP));

        int gridWidth = width - UiScrollbar.WIDTH - GAP;
        int columns = Math.max(1, (gridWidth - GAP) / (TILE_WIDTH + GAP));
        int rows = (ids.size() + columns - 1) / columns;
        double tileHeight = (viewport - (pageRows - 1) * GAP) / (double) pageRows;
        double tileWidth = (gridWidth - (columns + 1) * GAP) / (double) columns;
        gridTop = y + GRID_HEADER + GAP;
        gridBottom = y + height - GAP;
        maxScroll = Math.max(0, (int) Math.round(rows * tileHeight + Math.max(0, rows - 1) * GAP) - viewport);
        gridScroll.clamp(maxScroll);
        gridScroll.tick();
        g.enableScissor(x + GAP, gridTop, x + gridWidth - GAP, gridBottom);
        for (int i = 0; i < ids.size(); i++) {
            double columnStart = i % columns * (tileWidth + GAP);
            double rowStart = i / columns * (tileHeight + GAP);
            int tx = x + GAP + (int) Math.round(columnStart);
            int ty = gridTop + (int) Math.round(rowStart) - (int) Math.round(gridScroll.current);
            int cardWidth = (int) Math.round(columnStart + tileWidth) - (int) Math.round(columnStart);
            int cardHeight = (int) Math.round(rowStart + tileHeight) - (int) Math.round(rowStart);
            if (ty + cardHeight < gridTop || ty > gridBottom) continue;
            String id = ids.get(i);
            boolean known = SchematicKnowledge.clientKnows(id);
            boolean hovered = inside(mx, my, tx, ty, cardWidth, cardHeight) && my >= gridTop && my < gridBottom;
            UiRenderer.control(g, tx, ty, cardWidth, cardHeight, known, false, known && hovered, 0xFFFFFFFF);
            g.pose().pushPose();
            g.pose().translate(tx + (cardWidth - 32) / 2, ty + 10, 0);
            g.pose().scale(2, 2, 1);
            SchematicIconRenderer.drawWithShadow(g, new ItemStack(de.artemis.cyberneticenhancements.common.registry.ModItems.cyberware(id).get()), 0, 0, known);
            g.pose().popPose();
            Component label = Component.literal(name(id));
            var lines = font.split(label, cardWidth - 14);
            for (int n = 0; n < Math.min(2, lines.size()); n++) {
                var line = lines.get(n);
                int labelX = tx + (cardWidth - font.width(line)) / 2, labelY = ty + 51 + n * 11;
                if (known) g.drawString(font, line, labelX, labelY, UiRenderer.TEXT, false);
                else
                    UiEncryptedText.draw(g, font, line, labelX, labelY, id.hashCode() + n * 101, UiRenderer.TEXT_SECONDARY);
            }
            String status = known ? "Learned" : "Encrypted";
            g.drawString(font, status, tx + (cardWidth - font.width(status)) / 2, ty + cardHeight - 19, known ? UiRenderer.ACCENT : UiRenderer.TEXT_SECONDARY, false);
            tiles.add(new Tile(id, tx, ty, cardWidth, cardHeight));
        }
        if (ids.isEmpty())
            g.drawCenteredString(font, "No matching schematics", x + width / 2, y + HEADER + 20, UiRenderer.TEXT);
        g.disableScissor();
        drawScrollbar(g, gridScroll);
    }

    private void renderDetail(GuiGraphics g, Font font, int mx, int my) {
        boolean owned = SchematicKnowledge.clientKnows(selected);
        box(g, font, x + 10, y + 8, 90, 20, "< Collection", false);
        SchematicIconRenderer.drawWithShadow(g, new ItemStack(de.artemis.cyberneticenhancements.common.registry.ModItems.cyberware(selected).get()), x + 112, y + 10, owned);
        Component title = Component.literal(name(selected));
        if (owned) g.drawString(font, title, x + 138, y + 11, UiRenderer.TEXT, false);
        else
            UiEncryptedText.draw(g, font, title.getVisualOrderText(), x + 138, y + 11, selected.hashCode(), UiRenderer.TEXT_SECONDARY);
        g.drawString(font, owned ? "Learned design / recipes and discovery record" : "Not obtained / data encrypted", x + 112, y + 34, UiRenderer.TEXT_SECONDARY, false);
        var mc = Minecraft.getInstance();
        List<RecipeHolder<FabricationRecipe>> recipes = mc.level == null ? List.of() : mc.level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.FABRICATION.get()).stream().filter(r -> r.value().schematic().equals(selected)).sorted(Comparator.comparing(r -> r.id().toString())).toList();
        List<String> lines = new ArrayList<>();
        var history = SchematicKnowledge.clientHistory(selected);
        if (owned && history != null) {
            lines.add("Unlock method: " + history.source());
            if (history.epochMillis() > 0) {
                lines.add("Unlocked: " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(history.epochMillis())));
                lines.add("World day " + (history.gameTime() / 24000 + 1) + " / " + String.format(Locale.ROOT, "%02d:%02d", (history.gameTime() / 1000 + 6) % 24, (history.gameTime() % 1000) * 60 / 1000));
                lines.add("Location: " + history.dimension() + " / " + history.position().getX() + ", " + history.position().getY() + ", " + history.position().getZ());
            }
        } else {
            lines.add("This schematic has not been learned. Its name, contents and recipe data remain encrypted.");
            lines.add("Recover schematics from relic caches and specialist contracts, then right-click the schematic item to learn it.");
        }
        int textHeight = 0;
        for (String line : lines) textHeight += font.split(Component.literal(line), width - 40).size() * 12 + 6;
        int recipeHeight = 0;
        if (owned) for (var holder : recipes) recipeHeight += recipeHeight(font, holder.value());
        maxScroll = Math.max(0, textHeight + recipeHeight + 20 - (height - HEADER - 10));
        detailScroll.clamp(maxScroll);
        detailScroll.tick();
        int yy = y + HEADER + 8 - (int) Math.round(detailScroll.current);
        g.enableScissor(x + 6, y + HEADER, x + width - 12, y + height - 5);
        for (String line : lines) {
            for (var wrapped : font.split(Component.literal(line), width - 40)) {
                g.drawString(font, wrapped, x + 16, yy, UiRenderer.TEXT, false);
                yy += 12;
            }
            yy += 6;
        }
        ItemStack hovered = ItemStack.EMPTY;
        if (owned) {
            if (recipes.isEmpty()) {
                g.drawString(font, "No recipes currently reference this schematic in the server datapack.", x + 16, yy, UiRenderer.TEXT, false);
            }
            for (var holder : recipes) {
                var r = holder.value();
                int cardHeight = recipeHeight(font, r);
                UiRenderer.panel(g, x + 12, yy, width - 32, cardHeight - 10, UiRenderer.FIELD);
                g.renderItem(r.result(), x + 20, yy + 6);
                g.drawString(font, r.result().getHoverName(), x + 42, yy + 8, UiRenderer.TEXT, false);
                g.drawString(font, "Tier " + r.tier() + " / " + r.ticks() / 20.0 + "s / " + (r.tier() <= 2 ? "Basic" : r.tier() <= 4 ? "Precision" : "Restricted") + " Assembly", x + 20, yy + 29, UiRenderer.ACCENT, false);
                for (int slot = 0; slot < 25; slot++)
                    UiRenderer.outline(g, x + 20 + slot % 5 * 18, yy + 48 + slot / 5 * 18, 18, 18, UiRenderer.FIELD_EDGE);
                for (var part : r.parts()) {
                    var options = part.ingredient().getItems();
                    if (options.length == 0) continue;
                    ItemStack stack = options[(int) (System.currentTimeMillis() / 1500 % options.length)].copyWithCount(part.count());
                    int sx = x + 21 + part.slot() % 5 * 18, sy = yy + 49 + part.slot() / 5 * 18;
                    g.renderItem(stack, sx, sy);
                    g.renderItemDecorations(font, stack, sx, sy);
                    if (inside(mx, my, sx, sy, 16, 16) && my >= y + HEADER && my < y + height - 5) hovered = stack;
                }
                int by = yy + 48;
                for (String ingredient : bill(r))
                    for (var wrapped : font.split(Component.literal(ingredient), Math.max(50, width - 170))) {
                        g.drawString(font, wrapped, x + 128, by, UiRenderer.TEXT, false);
                        by += 11;
                    }
                yy += cardHeight;
            }
        }
        g.disableScissor();
        drawScrollbar(g, detailScroll);
        if (!hovered.isEmpty()) g.renderTooltip(font, hovered, mx, my);
    }

    private List<String> bill(FabricationRecipe recipe) {
        Map<String, Integer> totals = new LinkedHashMap<>();
        for (var part : recipe.parts()) {
            var values = part.ingredient().getItems();
            String label = values.length == 0 ? "Unavailable ingredient" : values[0].getHoverName().getString() + (values.length > 1 ? " (or alternative)" : "");
            totals.merge(label, part.count(), Integer::sum);
        }
        return totals.entrySet().stream().map(e -> e.getValue() + " x " + e.getKey()).toList();
    }

    private int recipeHeight(Font font, FabricationRecipe recipe) {
        int lines = 0;
        for (String text : bill(recipe)) lines += font.split(Component.literal(text), Math.max(50, width - 170)).size();
        return Math.max(154, lines * 11 + 62);
    }

    private void box(GuiGraphics g, Font font, int xx, int yy, int w, int h, String label, boolean active) {
        UiRenderer.button(g, font, xx, yy, w, h, Component.literal(label), true, active, inside(mouseX, mouseY, xx, yy, w, h), 0xFFFFFFFF);
    }

    private UiScrollbar.Track scrollTrack() {
        if (selected.isEmpty()) {
            int viewport = Math.max(0, height - GRID_HEADER - GAP * 2);
            return UiScrollbar.layout(x + width - UiScrollbar.WIDTH - GAP, y + GRID_HEADER + GAP, UiScrollbar.WIDTH, viewport, false, viewport, maxScroll, gridScroll.current);
        }
        int viewport = Math.max(0, height - HEADER - 10);
        return UiScrollbar.layout(x + width - UiScrollbar.WIDTH - 6, y + HEADER, UiScrollbar.WIDTH, viewport, false, viewport, maxScroll, detailScroll.current);
    }

    private UiScrollState activeScroll() {
        return selected.isEmpty() ? gridScroll : detailScroll;
    }

    private void drawScrollbar(GuiGraphics g, UiScrollState state) {
        UiScrollbar.draw(g, scrollTrack(), mouseX, mouseY, scrollbar.dragging());
    }

    boolean click(double mx, double my) {
        if (!inside(mx, my, x, y, width, height)) return false;
        var track = scrollTrack();
        if (scrollbar.begin(track, mx, my)) {
            unfocus();
            activeScroll().set(scrollbar.value(track, mx, my), maxScroll);
            return true;
        }
        if (!selected.isEmpty()) {
            if (inside(mx, my, x + 10, y + 8, 90, 20)) {
                playClick();
                selected = "";
                detailScroll.reset();
            }
            return true;
        }
        if (searchField != null) {
            searchField.setFocused(searchField.isMouseOver(mx, my));
            if (searchField.isFocused()) {
                searchField.mouseClicked(mx, my, 0);
                return true;
            }
        }
        for (int i = 0; i < 3; i++)
            if (inside(mx, my, x + GAP + i * (84 + GAP), y + 42, 84, 20)) {
                playClick();
                filter = i;
                gridScroll.reset();
                return true;
            }
        if (inside(mx, my, x + width - GAP - categoryWidth(), y + GAP, categoryWidth(), 22)) {
            playClick();
            category = (category + 1) % categories.size();
            gridScroll.reset();
            return true;
        }
        if (my >= gridTop && my < gridBottom) for (Tile tile : tiles)
            if (inside(mx, my, tile.x, tile.y, tile.width, tile.height)) {
                if (!SchematicKnowledge.clientKnows(tile.id)) return true;
                playClick();
                selected = tile.id;
                detailScroll.reset();
                break;
            }
        return true;
    }

    boolean scroll(double mx, double my, double delta) {
        if (!inside(mx, my, x, y + HEADER, width, height - HEADER)) return false;
        activeScroll().add(-delta * UiScrollState.WHEEL_STEP, maxScroll);
        return true;
    }

    boolean drag(double mx, double my) {
        if (!scrollbar.dragging())
            return selected.isEmpty() && searchField != null && searchField.isFocused() && searchField.mouseDragged(mx, my, 0, 0, 0);
        activeScroll().set(scrollbar.value(scrollTrack(), mx, my), maxScroll);
        return true;
    }

    void release() {
        scrollbar.release();
    }

    void unfocus() {
        if (searchField != null) searchField.setFocused(false);
    }

    private void playClick() {
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1F));
    }

    boolean key(int key, int scanCode, int modifiers) {
        if (key == 256) {
            unfocus();
            return false;
        }
        if (searchField == null || !searchField.isFocused() || !selected.isEmpty()) return false;
        searchField.keyPressed(key, scanCode, modifiers);
        return true;
    }

    boolean character(char value, int modifiers) {
        return selected.isEmpty() && searchField != null && searchField.isFocused() && searchField.charTyped(value, modifiers);
    }
}
