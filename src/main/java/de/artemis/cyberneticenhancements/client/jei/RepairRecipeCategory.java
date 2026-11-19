package de.artemis.cyberneticenhancements.client.jei;

import de.artemis.cyberneticenhancements.common.cyberware.CyberwareUpgradeRegistry;
import de.artemis.cyberneticenhancements.common.registry.ModBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Laid out exactly like the Upgrade Station's page: implant, up to two materials, arrow, result, then what the cycle does. */
public final class RepairRecipeCategory implements IRecipeCategory<RepairJeiRecipe> {
    private final IDrawable arrow, icon;
    private int measuredRevision = -1, summaryHeight;
    private String measuredLanguage = "";
    private static final int WIDTH = 180, SUMMARY_Y = 42;
    public RepairRecipeCategory(IGuiHelper helper) {
        arrow = helper.getRecipeArrow();
        icon = helper.createDrawableItemLike(ModBlocks.TECH_STATION.get());
    }
    @Override public RecipeType<RepairJeiRecipe> getRecipeType() { return CyberneticEnhancementsJeiPlugin.REPAIRING; }
    @Override public Component getTitle() { return ModBlocks.TECH_STATION.get().getName(); }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() {
        var minecraft = Minecraft.getInstance();
        int revision = CyberwareUpgradeRegistry.clientRevision();
        String language = minecraft.getLanguageManager().getSelected();
        if (measuredRevision != revision || !measuredLanguage.equals(language)) {
            int maximumLines = 1;
            for (var recipe : RepairJeiRecipe.all()) {
                int lines = recipe.changes().stream().mapToInt(text -> minecraft.font.split(text, WIDTH).size()).sum();
                maximumLines = Math.max(maximumLines, lines);
            }
            summaryHeight = maximumLines * (minecraft.font.lineHeight + 2);
            measuredRevision = revision;
            measuredLanguage = language;
        }
        return SUMMARY_Y + summaryHeight;
    }
    @Override public IDrawable getIcon() { return icon; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, RepairJeiRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(25, 5).setSlotName("repair_input_0").setStandardSlotBackground().addItemStack(recipe.input());
        for (int i = 0; i < recipe.plan().materials().size(); i++) {
            var material = recipe.plan().materials().get(i);
            builder.addInputSlot(49 + i * 18, 5).setSlotName("repair_input_" + (i + 1))
                    .setStandardSlotBackground().addItemStack(material.stack().copyWithCount(material.count()));
        }
        builder.addOutputSlot(133, 5).setOutputSlotBackground().addItemStack(recipe.plan().output());
    }
    @Override public void draw(RepairJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, 97, (26 - arrow.getHeight()) / 2);
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, recipe.step(), 0, 29, 0xFF404040, false);
        int y = SUMMARY_Y;
        for (var change : recipe.changes()) for (var line : font.split(change, WIDTH)) {
            graphics.drawString(font, line, 0, y, 0xFF404040, false);
            y += font.lineHeight + 2;
        }
    }
}
