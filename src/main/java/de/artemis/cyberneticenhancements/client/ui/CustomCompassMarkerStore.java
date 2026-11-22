package de.artemis.cyberneticenhancements.client.ui;

import com.google.gson.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public final class CustomCompassMarkerStore {
    public static final int LIMIT=128;
    public static final int DEATH_COLOR=0xFFFF4A4A;
    /** How long a death marker stays before it is dropped on its own. */
    public static final long DEATH_LIFETIME_MILLIS=30L*60*1000;
    /** {@code died} is when a death marker was made, in epoch milliseconds; 0 for any other marker. */
    private record Entry(String scope,CompassLandmark marker,long died){}
    private boolean unsaved;
    private final List<Entry> entries=new ArrayList<>();
    public static CustomCompassMarkerStore parse(List<? extends String> values){
        var result=new CustomCompassMarkerStore();
        for(String value:values)try{
            var j=JsonParser.parseString(value).getAsJsonObject();
            String scope=j.get("scope").getAsString(),dimension=j.get("dimension").getAsString();
            boolean death=j.has("death")&&j.get("death").getAsBoolean();
            String id="custom:"+UUID.fromString(j.get("id").getAsString());
            if(scope.isBlank()||ResourceLocation.tryParse(dimension)==null)continue;
            var pos=new BlockPos(j.get("x").getAsInt(),j.get("y").getAsInt(),j.get("z").getAsInt());
            if(Math.abs((long)pos.getX())>30_000_000||Math.abs((long)pos.getZ())>30_000_000)continue;
            if(result.entries.stream().anyMatch(e->e.scope.equals(scope)&&e.marker.id().equals(id))||result.all(scope).size()>=LIMIT)continue;
            // A death marker from before they expired starts its time now.
            long died=!death?0:j.has("died")?j.get("died").getAsLong():System.currentTimeMillis();
            if(death&&!j.has("died"))result.unsaved=true;
            result.entries.add(new Entry(scope,new CompassLandmark(id,clean(j.get("name").getAsString()),dimension,pos,death?CompassLandmark.Kind.DEATH:CompassLandmark.Kind.CUSTOM,death?DEATH_COLOR:0xFF8BDAF8),died));
        }catch(RuntimeException ignored){}
        return result;
    }
    public List<CompassLandmark> all(String scope){return entries.stream().filter(e->e.scope.equals(scope)).map(Entry::marker).toList();}
    public CompassLandmark add(String scope,String dimension,BlockPos pos,String name){
        var existing=all(scope).stream().filter(p->p.dimension().equals(dimension)&&p.pos().equals(pos)).findFirst();
        if(existing.isPresent())return existing.get();
        if(scope.isBlank()||all(scope).size()>=LIMIT)return null;
        var marker=new CompassLandmark("custom:"+UUID.randomUUID(),clean(name),dimension,pos.immutable(),CompassLandmark.Kind.CUSTOM,0xFF8BDAF8);
        entries.add(new Entry(scope,marker,0));return marker;
    }
    /** A death marker is always added (it replaces the oldest ordinary one when full), so the way back to your things is never lost. */
    public CompassLandmark addDeath(String scope,String dimension,BlockPos pos,String name){
        if(scope.isBlank())return null;
        var mine=all(scope);
        if(mine.size()>=LIMIT)mine.stream().filter(p->p.kind()!=CompassLandmark.Kind.DEATH).findFirst().or(()->mine.stream().findFirst()).ifPresent(p->delete(scope,p.id()));
        var marker=new CompassLandmark("custom:"+UUID.randomUUID(),clean(name),dimension,pos.immutable(),CompassLandmark.Kind.DEATH,DEATH_COLOR);
        entries.add(new Entry(scope,marker,System.currentTimeMillis()));return marker;
    }
    /** Drops death markers older than their lifetime; true when the stored list needs saving because of that. */
    public boolean expire(long now){
        boolean changed=entries.removeIf(e->e.died>0&&now-e.died>=DEATH_LIFETIME_MILLIS)|unsaved;
        unsaved=false;
        return changed;
    }
    public void delete(String scope,String id){entries.removeIf(e->e.scope.equals(scope)&&e.marker.id().equals(id));}
    public List<String> serialize(){
        return entries.stream().map(e->{
            var p=e.marker;var j=new JsonObject();j.addProperty("scope",e.scope);j.addProperty("id",p.id().substring(7));
            j.addProperty("dimension",p.dimension());j.addProperty("x",p.pos().getX());j.addProperty("y",p.pos().getY());j.addProperty("z",p.pos().getZ());j.addProperty("name",p.title());if(p.kind()==CompassLandmark.Kind.DEATH){j.addProperty("death",true);j.addProperty("died",e.died);}return j.toString();
        }).toList();
    }
    private static String clean(String name){var value=name.replaceAll("\\p{Cntrl}","").strip();return value.substring(0,Math.min(80,value.length()));}
}
