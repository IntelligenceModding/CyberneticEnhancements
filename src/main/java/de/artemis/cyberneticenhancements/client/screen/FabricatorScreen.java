package de.artemis.cyberneticenhancements.client.screen;

import de.artemis.cyberneticenhancements.client.ui.*;
import de.artemis.cyberneticenhancements.client.ClientConfig;
import de.artemis.cyberneticenhancements.client.render.SchematicIconRenderer;
import de.artemis.cyberneticenhancements.common.fabrication.*;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.inventory.Slot;

import java.util.*;

import static de.artemis.cyberneticenhancements.common.fabrication.FabricatorLayout.*;

public final class FabricatorScreen extends ResponsiveContainerScreen<FabricatorMenu> {
    private EditBox search;
    private final Map<Integer, UiButton> actions = new HashMap<>();
    private final UiScrollbar scrollbar = new UiScrollbar();
    private final WorkshopProgressAnimation fabricationAnimation = new WorkshopProgressAnimation();
    private static final String PINNED_GROUP = "pinned";
    private static final int GROUP_HEIGHT = 22;
    private final Set<String> expandedGroups = new HashSet<>(Set.of(PINNED_GROUP));
    private final Set<String> searchCollapsedGroups = new HashSet<>();
    private final Map<String, Float> groupExpansion = new HashMap<>();
    private final UiScrollState designScroll = new UiScrollState();
    private List<DesignGroup> designGroups = List.of();
    private double designContentHeight;
    private net.minecraft.resources.ResourceLocation revealDesign;
    private long listFrameMillis;
    private record DesignGroup(String id, Component title, List<RecipeHolder<FabricationRecipe>> designs, float expansion, double y) {}
    private record DesignHit(DesignGroup group, RecipeHolder<FabricationRecipe> recipe, boolean pin) {}
    private final Set<String> pinned = new HashSet<>();

    public FabricatorScreen(FabricatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        useMinecraftStationScale(WIDTH, HEIGHT, 26);
    }

    @Override
    protected void init() {
        String query = search == null ? "" : search.getValue();
        super.init();
        actions.clear();
        pinned.clear();
        pinned.addAll(ClientConfig.FABRICATOR_PINNED_DESIGNS.get());
        if (menu.selected() != null) expandedGroups.add("category:" + menu.selected().value().category());
        search = new UiEditBox(font, leftPos + LIST_X, topPos + HEADER_Y, 202, HEADER_HEIGHT, Component.literal("Search designs"));
        search.setHint(Component.literal("Search designs"));
        search.setMaxLength(80);
        search.setValue(query);
        search.setResponder(value -> { searchCollapsedGroups.clear(); designScroll.reset(); });
        addRenderableWidget(search);
        action("Fill grid", 252, ACTION_Y, 88, 0);
        action("Fabricate", 452, ACTION_Y, 96, 1);
        action("Stop", 558, ACTION_Y, 82, 2);
        action("Clear grid", 350, ACTION_Y, 88, 4);
    }

    private void action(String label, int x, int y, int width, int id) {
        UiButton button = new UiButton(leftPos + x, topPos + y, width, 20, Component.literal(label), b -> {
            if (minecraft != null && minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        });
        button.setTooltip(null);
        actions.put(id, button);
        addRenderableWidget(button);
    }

    private boolean outputFits(FabricationRecipe recipe) {
        ItemStack result = recipe.assemble(menu.station.input(), minecraft.level.registryAccess()), output = menu.station.getItem(26);
        return output.isEmpty() || ItemStack.isSameItemSameComponents(output, result) && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void updateActions() {
        if (minecraft == null || minecraft.level == null) return;
        boolean busy = menu.isWorking();
        var selected = menu.selected();
        actions.get(0).active = menu.canFill();
        actions.get(2).active = busy;
        actions.get(4).active = !busy && java.util.stream.IntStream.range(0, 25).anyMatch(i -> !menu.station.getItem(i).isEmpty());
        var candidate = selected != null ? selected : menu.availableRecipes().stream().filter(r -> r.value().matches(menu.station.input(), minecraft.level)).findFirst().orElse(null);
        boolean ready = !busy && candidate != null && SchematicKnowledge.clientKnows(candidate.value().schematic())
                && candidate.value().matches(menu.station.input(), minecraft.level) && outputFits(candidate.value());
        actions.get(1).active = ready;
    }

    private List<RecipeHolder<FabricationRecipe>> visible() {
        String query = search == null ? "" : search.getValue().strip().toLowerCase(Locale.ROOT);
        return menu.recipes().stream().filter(r -> query.isEmpty() || r.value().clientUnlocked()
                && r.value().result().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator.comparing((RecipeHolder<FabricationRecipe> r) -> !r.value().clientUnlocked())
                        .thenComparing(r -> r.id().toString())).toList();
    }

    private void updateDesignGroups() {
        var designs = visible();
        long now = net.minecraft.Util.getMillis();
        double seconds = listFrameMillis == 0 ? 0 : Math.clamp((now - listFrameMillis) / 1000D, 0, .1);
        listFrameMillis = now;
        float blend = (float) (1 - Math.exp(-seconds / .08));
        boolean searching = search != null && !search.getValue().isBlank();
        Map<String, List<RecipeHolder<FabricationRecipe>>> categories = new TreeMap<>();
        for (var recipe : designs) if (!pinned.contains(recipe.id().toString()))
            categories.computeIfAbsent(recipe.value().category(), key -> new ArrayList<>()).add(recipe);
        List<DesignGroup> groups = new ArrayList<>();
        double y = 0;
        var favorites = designs.stream().filter(r -> pinned.contains(r.id().toString())).toList();
        y = appendDesignGroup(groups, PINNED_GROUP, Component.translatable("screen.cyberneticenhancements.fabricator.pinned"), favorites, y, searching, blend);
        for (var category : categories.entrySet())
            y = appendDesignGroup(groups, "category:" + category.getKey(), Component.literal(category.getKey()), category.getValue(), y, searching, blend);
        designGroups = List.copyOf(groups);
        designContentHeight = Math.max(0, y - 4);
        if (revealDesign != null) for (var group : designGroups) {
            for (int i = 0; i < group.designs().size(); i++) if (group.designs().get(i).id().equals(revealDesign) && group.expansion() >= .98F) {
                double row = group.y() + GROUP_HEIGHT + i * ROW_HEIGHT;
                if (row < designScroll.target) designScroll.target = row;
                else if (row + ROW_HEIGHT > designScroll.target + LIST_HEIGHT) designScroll.target = row + ROW_HEIGHT - LIST_HEIGHT;
                revealDesign = null;
                break;
            }
        }
        designScroll.clamp(maxDesignScroll());
        designScroll.tick();
        if (maxDesignScroll() == 0) scrollbar.release();
    }

    private double appendDesignGroup(List<DesignGroup> groups, String id, Component title,
            List<RecipeHolder<FabricationRecipe>> recipes, double y, boolean searching, float blend) {
        float target = (searching ? !searchCollapsedGroups.contains(id) : expandedGroups.contains(id)) ? 1 : 0;
        float current = groupExpansion.getOrDefault(id, expandedGroups.contains(id) ? 1F : 0F);
        float expansion = current + (target - current) * blend;
        if (Math.abs(expansion - target) < .005F) expansion = target;
        groupExpansion.put(id, expansion);
        groups.add(new DesignGroup(id, title, recipes, expansion, y));
        return y + GROUP_HEIGHT + recipes.size() * ROW_HEIGHT * expansion + 4;
    }

    private double maxDesignScroll() { return Math.max(0, designContentHeight - LIST_HEIGHT); }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        UiRenderer.panel(g, x, y, WIDTH, HEIGHT, StationScreenStyle.PANEL_DEEP);
        UiRenderer.panel(g, x + BROWSER_X, y + PANEL_Y, BROWSER_WIDTH, PANEL_HEIGHT, StationScreenStyle.PANEL_BG);
        UiRenderer.panel(g, x + ASSEMBLY_X, y + PANEL_Y, ASSEMBLY_WIDTH, PANEL_HEIGHT, StationScreenStyle.PANEL_BG);
        for (var slot : menu.slots)
            if (slot.isActive() && slot.index < 25) StationSlotRenderer.drawStandardSlot(g, x, y, slot.x, slot.y, StationScreenStyle.SLOT_BG);
        UiRenderer.panel(g, x + OUTPUT_FRAME_X, y + OUTPUT_FRAME_Y, OUTPUT_FRAME_SIZE, OUTPUT_FRAME_SIZE,
                UiRenderer.FIELD, menu.slots.get(25).hasItem() ? UiRenderer.SELECTED_EDGE : UiRenderer.FIELD_EDGE);
        centered(g, "Assembly grid", x + GRID_X + 2 * GRID_STEP + 8, y + ASSEMBLY_LABEL_Y, UiRenderer.TEXT_SECONDARY);
        centered(g, "Output", x + OUTPUT_X + 8, y + ASSEMBLY_LABEL_Y, UiRenderer.TEXT_SECONDARY);
        var selected = menu.selected();
        if (selected == null) {
            g.drawString(font, "Select a design to preview its assembly.", x + ASSEMBLY_X + 8,
                    y + HEADER_Y + (HEADER_HEIGHT - font.lineHeight) / 2, UiRenderer.TEXT_SECONDARY, false);
        } else {
            var r = selected.value();
            drawRecipeHeading(g, r);
            if (SchematicKnowledge.clientKnows(r.schematic())) for (var part : r.parts())
                if (menu.station.getItem(part.slot()).isEmpty()) {
                    ItemStack[] choices = part.ingredient().getItems();
                    if (choices.length == 0) continue;
                    var slot = menu.slots.get(part.slot());
                    de.artemis.cyberneticenhancements.client.render.GhostItemRenderer.render(g,
                            choices[(int) (System.currentTimeMillis() / 1500 % choices.length)], x + slot.x, y + slot.y);
                    if (part.count() > 1) {
                        String count = Integer.toString(part.count());
                        g.drawString(font, count, x + slot.x + 17 - font.width(count), y + slot.y + 9, UiRenderer.TEXT_SECONDARY, false);
                    }
                }
            if (menu.station.getItem(26).isEmpty()) {
                g.pose().pushPose();
                g.pose().translate(x + OUTPUT_X + 8 - OUTPUT_ITEM_SIZE / 2, y + OUTPUT_Y + 8 - OUTPUT_ITEM_SIZE / 2, 0);
                g.pose().scale(OUTPUT_ITEM_SIZE / 16F, OUTPUT_ITEM_SIZE / 16F, 1);
                de.artemis.cyberneticenhancements.client.render.GhostItemRenderer.render(g, r.result(), 0, 0);
                g.pose().popPose();
            }
        }
        renderFabricationProgress(g);
        renderDesigns(g, mouseX, mouseY);
    }

    private void renderFabricationProgress(GuiGraphics g) {
        var selected = menu.selected();
        var frame = fabricationAnimation.sample(net.minecraft.Util.getMillis(), selected == null ? "" : selected.id().toString(),
                menu.progress(), selected == null ? 1 : selected.value().ticks(), menu.isWorking(), menu.station.getItem(26).getCount());
        int x = leftPos + PROGRESS_X, y = topPos + PROGRESS_Y;
        int accent = UiRenderer.blend(UiRenderer.ACCENT, 0xFF91E3B7, frame.completion());
        int fillColor = UiRenderer.blend(0xFF397E9B, 0xFF398D65, frame.completion());
        UiRenderer.panel(g, x, y - 1, PROGRESS_WIDTH, 10, UiRenderer.FIELD, UiRenderer.FIELD_EDGE);
        int barX = x + 2, barWidth = PROGRESS_WIDTH - 4;
        g.fill(barX, y + 1, barX + barWidth, y + 7, 0xFF202C38);
        int filled = Math.clamp(Math.round(barWidth * frame.progress()), 0, barWidth);
        if (filled > 0) {
            g.fill(barX, y + 1, barX + filled, y + 7, fillColor);
            g.fill(barX, y + 1, barX + filled, y + 2, accent);
        }
        for (int segment = 1; segment < 10; segment++) {
            int notch = barX + barWidth * segment / 10;
            g.fill(notch, y + 2, notch + 1, y + 7, 0xAA111820);
        }
    }

    private void drawRecipeHeading(GuiGraphics g, FabricationRecipe recipe) {
        Component name = recipe.result().getHoverName();
        String details = "Tier " + recipe.tier() + "  /  " + formatSeconds(recipe.ticks()) + " s";
        int nameWidth = font.width(name);
        float scale = Math.min(1F, (ASSEMBLY_WIDTH - 16F) / (nameWidth + 14 + font.width(details)));
        g.pose().pushPose();
        g.pose().translate(leftPos + ASSEMBLY_X + 8,
                topPos + HEADER_Y + (HEADER_HEIGHT - font.lineHeight * scale) / 2F, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, name, 0, 0, UiRenderer.TEXT, false);
        g.drawString(font, details, (ASSEMBLY_WIDTH - 16) / scale - font.width(details), 0F, UiRenderer.TEXT_SECONDARY, false);
        g.pose().popPose();
    }

    @Override
    protected void renderSlot(GuiGraphics g, Slot slot) {
        if (slot != menu.slots.get(25)) { super.renderSlot(g, slot); return; }
        g.pose().pushPose();
        g.pose().translate(OUTPUT_X + 8 - OUTPUT_ITEM_SIZE / 2, OUTPUT_Y + 8 - OUTPUT_ITEM_SIZE / 2, 0);
        g.pose().scale(OUTPUT_ITEM_SIZE / 16F, OUTPUT_ITEM_SIZE / 16F, 1);
        g.pose().translate(-slot.x, -slot.y, 0);
        super.renderSlot(g, slot);
        g.pose().popPose();
    }

    @Override
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        if (x == OUTPUT_X && y == OUTPUT_Y && width == 16 && height == 16)
            return super.isHovering(OUTPUT_FRAME_X, OUTPUT_FRAME_Y, OUTPUT_FRAME_SIZE, OUTPUT_FRAME_SIZE, mouseX, mouseY);
        return super.isHovering(x, y, width, height, mouseX, mouseY);
    }

    @Override
    protected void renderSlotHighlight(GuiGraphics g, Slot slot, int mouseX, int mouseY, float partial) {
        if (slot != menu.slots.get(25)) { super.renderSlotHighlight(g, slot, mouseX, mouseY, partial); return; }
        g.fill(OUTPUT_FRAME_X + 1, OUTPUT_FRAME_Y + 1, OUTPUT_FRAME_X + OUTPUT_FRAME_SIZE - 1,
                OUTPUT_FRAME_Y + OUTPUT_FRAME_SIZE - 1, 0x188BDAF8);
        UiRenderer.outline(g, OUTPUT_FRAME_X, OUTPUT_FRAME_Y, OUTPUT_FRAME_SIZE, OUTPUT_FRAME_SIZE, UiRenderer.SELECTED_EDGE);
    }

    private void renderDesigns(GuiGraphics g, int mouseX, int mouseY) {
        updateDesignGroups();
        int x = leftPos + LIST_X, top = topPos + LIST_Y, bottom = top + LIST_HEIGHT;
        var selected = menu.selected();
        g.enableScissor(x, top, x + LIST_WIDTH + 3, bottom);
        for (var group : designGroups) {
            int y = top + (int) Math.round(group.y() - designScroll.current);
            int childrenBottom = y + GROUP_HEIGHT + Math.round(group.designs().size() * ROW_HEIGHT * group.expansion());
            if (childrenBottom < top || y >= bottom) continue;
            boolean hovered = mouseX >= x && mouseX < x + LIST_WIDTH && mouseY >= Math.max(top, y) && mouseY < Math.min(bottom, y + GROUP_HEIGHT - 2);
            UiRenderer.control(g, x, y, LIST_WIDTH, GROUP_HEIGHT - 2, true, false, hovered, 0xFFFFFFFF);
            UiRenderer.disclosureChevron(g, x + 11, y + GROUP_HEIGHT / 2F, group.expansion(), hovered ? UiRenderer.ACCENT : UiRenderer.TEXT_SECONDARY);
            String count = Integer.toString(group.designs().size());
            int countWidth = font.width(count);
            g.drawString(font, font.plainSubstrByWidth(group.title().getString(), LIST_WIDTH - countWidth - 34), x + 22, y + 6, UiRenderer.TEXT_SECONDARY, false);
            g.drawString(font, count, x + LIST_WIDTH - 7 - countWidth, y + 6, UiRenderer.TEXT_SECONDARY, false);
            int clipTop = Math.max(top, y + GROUP_HEIGHT), clipBottom = Math.min(bottom, childrenBottom);
            if (clipBottom <= clipTop) continue;
            g.enableScissor(x, clipTop, x + LIST_WIDTH + 3, clipBottom);
            for (int index = 0; index < group.designs().size(); index++) {
                int rowY = y + GROUP_HEIGHT + index * ROW_HEIGHT;
                if (rowY + ROW_HEIGHT < clipTop || rowY >= clipBottom) continue;
                var holder = group.designs().get(index);
                boolean known = holder.value().clientUnlocked();
                boolean chosen = selected != null && selected.id().equals(holder.id());
                boolean rowHovered = mouseX >= x && mouseX < x + LIST_WIDTH && mouseY >= Math.max(rowY, clipTop) && mouseY < Math.min(rowY + ROW_HEIGHT - 2, clipBottom);
                UiRenderer.control(g, x + 4, rowY, LIST_WIDTH - 4, ROW_HEIGHT - 2, true, chosen, rowHovered, 0xFFFFFFFF);
                SchematicIconRenderer.drawWithShadow(g, holder.value().result(), x + 8, rowY + 4, known);
                Component label = Component.literal(font.plainSubstrByWidth(holder.value().result().getHoverName().getString(), LIST_WIDTH - 58));
                if (known) g.drawString(font, label, x + 30, rowY + 3, UiRenderer.TEXT, false);
                else UiEncryptedText.draw(g, font, label.getVisualOrderText(), x + 30, rowY + 3, holder.id().hashCode(), UiRenderer.TEXT_SECONDARY);
                g.drawString(font, known ? "Tier " + holder.value().tier() + "  /  Known" : "Encrypted", x + 30, rowY + 14,
                        known ? UiRenderer.ACCENT : UiRenderer.TEXT_SECONDARY, false);
                drawPin(g, x + LIST_WIDTH - 20, rowY + 2, pinned.contains(holder.id().toString()), rowHovered && mouseX >= x + LIST_WIDTH - 20);
            }
            g.disableScissor();
        }
        if (visible().isEmpty()) wrapped(g, "No matching designs.", x + 4, top + GROUP_HEIGHT + 8, LIST_WIDTH - 8, 2, UiRenderer.TEXT_SECONDARY);
        g.disableScissor();
        UiScrollbar.draw(g, scrollTrack(), mouseX, mouseY, scrollbar.dragging());
    }

    private DesignHit designAt(double mouseX, double mouseY) {
        if (!inside(mouseX, mouseY, LIST_X, LIST_Y, LIST_WIDTH, LIST_HEIGHT)) return null;
        double localY = mouseY - topPos - LIST_Y + designScroll.current;
        for (var group : designGroups) {
            double childY = localY - group.y() - GROUP_HEIGHT;
            if (localY >= group.y() && childY < -2) return new DesignHit(group, null, false);
            if (childY < 0 || childY >= group.designs().size() * ROW_HEIGHT * group.expansion()) continue;
            int index = (int) (childY / ROW_HEIGHT);
            if (childY % ROW_HEIGHT >= ROW_HEIGHT - 2) return null;
            return new DesignHit(group, group.designs().get(index), mouseX >= leftPos + LIST_X + LIST_WIDTH - 20);
        }
        return null;
    }

    private void drawPin(GuiGraphics g, int x, int y, boolean selected, boolean hovered) {
        UiRenderer.listPin(g, font, x, y, selected, hovered);
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
    protected void renderTooltip(GuiGraphics g, int x, int y) {
        if (menu.getCarried().isEmpty() && hoveredSlot != null && hoveredSlot.index < 25) {
            if (hoveredSlot.hasItem()) {
                g.renderTooltip(font, hoveredSlot.getItem().getHoverName(), x, y);
                return;
            }
            var selected = menu.selected();
            if (selected != null && SchematicKnowledge.clientKnows(selected.value().schematic())) {
                for (var part : selected.value().parts()) if (part.slot() == hoveredSlot.index) {
                    var values = part.ingredient().getItems();
                    if (values.length > 0) {
                        ItemStack preview = values[(int) (System.currentTimeMillis() / 1500 % values.length)];
                        g.renderTooltip(font, preview.getHoverName(), x, y);
                    }
                    return;
                }
            }
        }
        if (menu.getCarried().isEmpty() && hoveredSlot != null && hoveredSlot.index == 25) {
            ItemStack output = hoveredSlot.getItem();
            var selected = menu.selected();
            if (output.isEmpty() && selected != null && selected.value().clientUnlocked()) output = selected.value().result();
            if (!output.isEmpty()) { g.renderTooltip(font, output, x, y); return; }
        }
        super.renderTooltip(g, x, y);
    }

    @Override
    protected void renderContent(GuiGraphics g, int x, int y, float partial) {
        updateActions();
        super.renderContent(g, x, y, partial);
        renderTooltip(g, x, y);
        var hit = designAt(x, y);
        if (hit != null && hit.recipe() != null) {
            var holder = hit.recipe();
            if (hit.pin()) g.renderTooltip(font, Component.translatable("screen.cyberneticenhancements.fabricator."
                    + (pinned.contains(holder.id().toString()) ? "unpin" : "pin")), x, y);
            else if (holder.value().clientUnlocked() && font.width(holder.value().result().getHoverName()) > LIST_WIDTH - 58)
                g.renderTooltip(font, holder.value().result().getHoverName(), x, y);
        }
    }

    @Override
    protected boolean mouseClickedContent(double x, double y, int button) {
        var track = scrollTrack();
        if (button == 0 && scrollbar.begin(track, x, y)) {
            designScroll.set(scrollbar.value(track, x, y), maxDesignScroll());
            return true;
        }
        var hit = designAt(x, y);
        if (button == 0 && hit != null) {
            if (hit.recipe() == null) {
                if (search.getValue().isBlank()) {
                    if (!expandedGroups.remove(hit.group().id())) expandedGroups.add(hit.group().id());
                } else if (!searchCollapsedGroups.remove(hit.group().id())) searchCollapsedGroups.add(hit.group().id());
                playListClick();
            } else if (hit.pin()) {
                String id = hit.recipe().id().toString();
                if (!pinned.remove(id)) pinned.add(id);
                ClientConfig.FABRICATOR_PINNED_DESIGNS.set(pinned.stream().sorted().toList());
                ClientConfig.FABRICATOR_PINNED_DESIGNS.save();
                expandedGroups.add(PINNED_GROUP);
                designScroll.reset();
                playListClick();
            } else if (hit.recipe().value().clientUnlocked() && minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 100 + menu.recipes().indexOf(hit.recipe()));
            return true;
        }
        if (button == 0 && !hasShiftDown() && menu.getCarried().isEmpty() && !menu.isWorking() && minecraft.gameMode != null) {
            for (int i = 0; i < 25; i++) {
                Slot slot = menu.slots.get(i);
                if (!isHovering(slot.x, slot.y, 16, 16, x, y)) continue;
                if (slot.hasItem()) break;
                ItemStack ingredient = ItemStack.EMPTY;
                var selected = menu.selected();
                if (selected != null && selected.value().clientUnlocked()) {
                    for (var part : selected.value().parts()) if (part.slot() == i) {
                        var choices = part.ingredient().getItems();
                        if (choices.length > 0) ingredient = choices[(int) (System.currentTimeMillis() / 1500 % choices.length)];
                        break;
                    }
                }
                if (ingredient.isEmpty()) break;
                final ItemStack target = ingredient;
                var design = menu.recipes().stream().filter(r -> r.value().clientUnlocked() && r.value().result().is(target.getItem())).findFirst().orElse(null);
                if (design == null) break;
                search.setValue("");
                expandedGroups.add(pinned.contains(design.id().toString()) ? PINNED_GROUP : "category:" + design.value().category());
                revealDesign = design.id();
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 100 + menu.recipes().indexOf(design));
                playListClick();
                return true;
            }
        }
        return super.mouseClickedContent(x, y, button);
    }

    private void playListClick() {
        minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1));
    }

    private UiScrollbar.Track scrollTrack() {
        return UiScrollbar.layout(leftPos + SCROLL_X, topPos + LIST_Y, UiScrollbar.WIDTH, LIST_HEIGHT, false, LIST_HEIGHT, maxDesignScroll(), designScroll.current);
    }

    @Override
    protected boolean mouseDraggedContent(double x, double y, int button, double dx, double dy) {
        if (button == 0 && scrollbar.dragging()) {
            designScroll.set(scrollbar.value(scrollTrack(), x, y), maxDesignScroll());
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
        if (inside(x, y, LIST_X, LIST_Y, BROWSER_WIDTH - 16, LIST_HEIGHT)) {
            designScroll.add(-dy * UiScrollState.WHEEL_STEP, maxDesignScroll());
            return true;
        }
        return super.mouseScrolledContent(x, y, dx, dy);
    }
}
