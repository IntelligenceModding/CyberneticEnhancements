package de.artemis.cyberneticenhancements.common.quest;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public enum QuestDimension {
    STANDARD("", 100), NETHER("minecraft:the_nether", 125), END("minecraft:the_end", 135);

    private final String id;
    private final int rewardPercent;

    QuestDimension(String id, int rewardPercent) {
        this.id = id;
        this.rewardPercent = rewardPercent;
    }

    public static QuestDimension of(String id) {
        return switch (id) {
            case "minecraft:the_nether" -> NETHER;
            case "minecraft:the_end" -> END;
            default -> STANDARD;
        };
    }

    public static QuestDimension byIndex(int index) {
        return switch (index) {
            case 1 -> NETHER;
            case 2 -> END;
            default -> STANDARD;
        };
    }

    public int reward(int base) {
        return (int) Math.min(Integer.MAX_VALUE, ((long) base * rewardPercent + 50) / 100);
    }

    public Component title(Component title) {
        return this == STANDARD ? title : Component.translatable("quest.cyberneticenhancements.title.dimension", title, name(id));
    }

    public Component displayName() {
        return name(levelKey().location().toString());
    }

    public static Component name(String dimension) {
        return switch (dimension) {
            case "minecraft:overworld" -> Component.translatable("quest.cyberneticenhancements.dimension.overworld");
            case "minecraft:the_nether" -> Component.translatable("quest.cyberneticenhancements.dimension.nether");
            case "minecraft:the_end" -> Component.translatable("quest.cyberneticenhancements.dimension.end");
            default -> Component.literal(dimension);
        };
    }

    net.minecraft.resources.ResourceKey<Level> levelKey() {
        return switch (this) {
            case STANDARD -> Level.OVERWORLD;
            case NETHER -> Level.NETHER;
            case END -> Level.END;
        };
    }
}
