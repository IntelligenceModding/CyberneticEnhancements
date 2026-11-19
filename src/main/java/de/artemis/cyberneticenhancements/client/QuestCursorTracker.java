package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.common.network.QuestCursorPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.network.PacketDistributor;

public final class QuestCursorTracker {
    private static QuestCursorPayload previous = new QuestCursorPayload("", "", 0);
    private static int heartbeat;

    private QuestCursorTracker() {}

    static void tick() {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null) {
            previous = new QuestCursorPayload("", "", 0);
            heartbeat = 0;
            return;
        }
        var menu = minecraft.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen
                ? screen.getMenu() : player.containerMenu;
        var data = menu.getCarried().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String contract = player.isCreative() ? data.getString("QuestContractId") : "";
        String type = player.isCreative() ? data.getString("QuestItemType") : "";
        var payload = new QuestCursorPayload(contract.length() <= 64 ? contract : "", type.length() <= 64 ? type : "", data.getLong("QuestItemRevision"));
        if (!payload.equals(previous) || !payload.contract().isEmpty() && ++heartbeat >= 10) {
            PacketDistributor.sendToServer(payload);
            previous = payload;
            heartbeat = 0;
        }
    }

    public static void invalidate(QuestCursorPayload payload) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        var menu = minecraft.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen
                ? screen.getMenu() : minecraft.player.containerMenu;
        var data = menu.getCarried().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (payload.contract().equals(data.getString("QuestContractId")) && payload.itemType().equals(data.getString("QuestItemType"))
                && payload.revision() == data.getLong("QuestItemRevision")) menu.setCarried(net.minecraft.world.item.ItemStack.EMPTY);
    }
}
