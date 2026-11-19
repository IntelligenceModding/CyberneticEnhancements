package de.artemis.cyberneticenhancements.client;

import de.artemis.cyberneticenhancements.client.ui.CustomCompassMarkerStore;
import de.artemis.cyberneticenhancements.client.ui.CompassLandmark;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import java.util.List;

public final class CustomCompassMarkers {
    private static List<? extends String> previous;
    private static CustomCompassMarkerStore store=new CustomCompassMarkerStore();
    private static CustomCompassMarkerStore store(){var values=ClientConfig.CUSTOM_COMPASS_MARKERS.get();if(values!=previous){store=CustomCompassMarkerStore.parse(values);previous=values;}return store;}
    public static List<CompassLandmark> all(){return store().all(CompassPreferences.scope());}
    public static void add(){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.screen!=null||!mc.player.isAlive()||mc.player.isSpectator())return;
        var hit=mc.player.pick(128,mc.getTimer().getGameTimeDeltaPartialTick(false),false);
        var pos=hit instanceof BlockHitResult block&&hit.getType()==HitResult.Type.BLOCK?block.getBlockPos():mc.player.blockPosition();
        var name=Component.translatable("message.cyberneticenhancements.custom_marker.name",pos.getX(),pos.getY(),pos.getZ()).getString();
        var point=store().add(CompassPreferences.scope(),mc.level.dimension().location().toString(),pos,name);
        if(point==null){mc.player.displayClientMessage(Component.translatable("message.cyberneticenhancements.custom_marker.full",CustomCompassMarkerStore.LIMIT),true);return;}
        save();mc.player.displayClientMessage(Component.translatable("message.cyberneticenhancements.custom_marker.added",point.title()),true);
        mc.player.playSound(net.minecraft.sounds.SoundEvents.LODESTONE_COMPASS_LOCK,.3F,1.15F);
    }
    public static void delete(String id){store().delete(CompassPreferences.scope(),id);save();}
    private static void save(){ClientConfig.CUSTOM_COMPASS_MARKERS.set(store.serialize());ClientConfig.SPEC.save();}
    private CustomCompassMarkers(){}
}
