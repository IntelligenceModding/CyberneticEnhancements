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
    private static long lastExpiry;
    private static CustomCompassMarkerStore store(){
        var values=ClientConfig.CUSTOM_COMPASS_MARKERS.get();if(values!=previous){store=CustomCompassMarkerStore.parse(values);previous=values;lastExpiry=0;}
        // Death markers go by themselves after a while; looked at about once a second.
        long now=System.currentTimeMillis();
        if(now-lastExpiry>=1000){lastExpiry=now;if(store.expire(now))save();}
        return store;
    }
    public static List<CompassLandmark> all(){return store().all(CompassPreferences.scope());}
    public static void add(){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.screen!=null||!mc.player.isAlive()||mc.player.isSpectator())return;
        var hit=mc.player.pick(128,mc.getTimer().getGameTimeDeltaPartialTick(false),false);
        add(hit instanceof BlockHitResult block&&hit.getType()==HitResult.Type.BLOCK?block.getBlockPos():mc.player.blockPosition());
    }
    /** Drops a marker at a chosen place, such as a spot picked on the map; the compass shows it like any other. */
    public static void add(net.minecraft.core.BlockPos pos){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        var name=Component.translatable("message.cyberneticenhancements.custom_marker.name",pos.getX(),pos.getY(),pos.getZ()).getString();
        var point=store().add(CompassPreferences.scope(),mc.level.dimension().location().toString(),pos,name);
        if(point==null){mc.player.displayClientMessage(Component.translatable("message.cyberneticenhancements.custom_marker.full",CustomCompassMarkerStore.LIMIT),true);return;}
        save();mc.player.displayClientMessage(Component.translatable("message.cyberneticenhancements.custom_marker.added",point.title()),true);
        mc.player.playSound(net.minecraft.sounds.SoundEvents.LODESTONE_COMPASS_LOCK,.3F,1.15F);
    }
    private static net.minecraft.world.entity.player.Player lastDeath;
    /** On dying, drops a red marker where it happened (no beacon in the world); once per death. */
    public static void died(){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||lastDeath==mc.player)return;
        lastDeath=mc.player;
        var pos=mc.player.blockPosition();
        var name=Component.translatable("message.cyberneticenhancements.custom_marker.death").getString();
        if(store().addDeath(CompassPreferences.scope(),mc.level.dimension().location().toString(),pos,name)!=null)save();
    }
    public static void delete(String id){store().delete(CompassPreferences.scope(),id);save();}
    private static void save(){ClientConfig.CUSTOM_COMPASS_MARKERS.set(store.serialize());ClientConfig.SPEC.save();}
    private CustomCompassMarkers(){}
}
