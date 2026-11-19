package de.artemis.cyberneticenhancements.common.registry;

import de.artemis.cyberneticenhancements.CyberneticEnhancements;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, CyberneticEnhancements.MOD_ID);
    public static final DeferredHolder<SoundEvent, SoundEvent> FLATLINED = SOUNDS.register("player.flatlined",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "player.flatlined")));

    public static final DeferredHolder<SoundEvent, SoundEvent> HEALING = SOUNDS.register("player.healing",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "player.healing")));

    public static final DeferredHolder<SoundEvent, SoundEvent> HACKING_ACCESS = SOUNDS.register("player.hacking_access",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "player.hacking_access")));

    public static final DeferredHolder<SoundEvent, SoundEvent> MAXTEC_SPAWN = SOUNDS.register("npc.maxtec_spawn",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "npc.maxtec_spawn")));

    public static final DeferredHolder<SoundEvent, SoundEvent> INCOMING_CALL = SOUNDS.register("calls.incoming_call",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "calls.incoming_call")));

    public static final DeferredHolder<SoundEvent, SoundEvent> OUTGOING_CALL = SOUNDS.register("calls.outgoing_call",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "calls.outgoing_call")));

    public static final DeferredHolder<SoundEvent, SoundEvent> COMPLETE_CONTRACT = SOUNDS.register("contracts.complete_contract",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "contracts.complete_contract")));

    public static final DeferredHolder<SoundEvent, SoundEvent> NEW_CONTRACT = SOUNDS.register("contracts.new_contract",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "contracts.new_contract")));

    public static final DeferredHolder<SoundEvent, SoundEvent> NPC_AGGRESSION = SOUNDS.register("npc.npc_aggression",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "npc.npc_aggression")));

    public static final DeferredHolder<SoundEvent, SoundEvent> NPC_AGGRESSION_LOST = SOUNDS.register("npc.npc_aggression_lost",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "npc.npc_aggression_lost")));

    public static final DeferredHolder<SoundEvent, SoundEvent> BARRIER_SCAN = SOUNDS.register("world.barrier_scan",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "world.barrier_scan")));

    public static final DeferredHolder<SoundEvent, SoundEvent> CYBERPSYCHO_NEUTRALIZED = SOUNDS.register("contracts.cyberpsycho_neutralized",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "contracts.cyberpsycho_neutralized")));

    public static final DeferredHolder<SoundEvent, SoundEvent> LAUNCH_INTRO = SOUNDS.register("menu.launch_intro",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "menu.launch_intro")));

    public static final DeferredHolder<SoundEvent, SoundEvent> TITLE_SCREEN_MUSIC = SOUNDS.register("menu.title_screen_music",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "menu.title_screen_music")));

    public static final DeferredHolder<SoundEvent, SoundEvent> JOIN_LINK = SOUNDS.register("calls.join_link",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "calls.join_link")));

    public static final DeferredHolder<SoundEvent, SoundEvent> CYBERWARE_MALFUNCTION = SOUNDS.register("player.cyberware_malfunction",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "player.cyberware_malfunction")));

    public static final DeferredHolder<SoundEvent, SoundEvent> OPEN_CONTACTS = SOUNDS.register("calls.open_contacts",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "calls.open_contacts")));

    public static final DeferredHolder<SoundEvent, SoundEvent> CLOSE_CONTACTS = SOUNDS.register("calls.close_contacts",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "calls.close_contacts")));

    public static final DeferredHolder<SoundEvent, SoundEvent> SKILL_ADVANCE = SOUNDS.register("player.skill_advance",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "player.skill_advance")));

    public static final DeferredHolder<SoundEvent, SoundEvent> PAUSE = SOUNDS.register("menu.pause",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "menu.pause")));

    public static final DeferredHolder<SoundEvent, SoundEvent> RESUME = SOUNDS.register("menu.resume",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "menu.resume")));

    public static final DeferredHolder<SoundEvent, SoundEvent> CASH_TRANSFER = SOUNDS.register("player.cash_transfer",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CyberneticEnhancements.MOD_ID, "player.cash_transfer")));

    private ModSounds() {
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}
