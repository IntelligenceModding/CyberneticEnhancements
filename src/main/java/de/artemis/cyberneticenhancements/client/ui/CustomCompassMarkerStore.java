package de.artemis.cyberneticenhancements.client.ui;

import com.google.gson.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public final class CustomCompassMarkerStore {
    public static final int LIMIT=128;
    private record Entry(String scope,CompassLandmark marker){}
    private final List<Entry> entries=new ArrayList<>();
    public static CustomCompassMarkerStore parse(List<? extends String> values){
        var result=new CustomCompassMarkerStore();
        for(String value:values)try{
            var j=JsonParser.parseString(value).getAsJsonObject();
            String scope=j.get("scope").getAsString(),dimension=j.get("dimension").getAsString();
            String id="custom:"+UUID.fromString(j.get("id").getAsString());
            if(scope.isBlank()||ResourceLocation.tryParse(dimension)==null)continue;
            var pos=new BlockPos(j.get("x").getAsInt(),j.get("y").getAsInt(),j.get("z").getAsInt());
            if(Math.abs((long)pos.getX())>30_000_000||Math.abs((long)pos.getZ())>30_000_000)continue;
            if(result.entries.stream().anyMatch(e->e.scope.equals(scope)&&e.marker.id().equals(id))||result.all(scope).size()>=LIMIT)continue;
            result.entries.add(new Entry(scope,new CompassLandmark(id,clean(j.get("name").getAsString()),dimension,pos,CompassLandmark.Kind.CUSTOM,0xFF8BDAF8)));
        }catch(RuntimeException ignored){}
        return result;
    }
    public List<CompassLandmark> all(String scope){return entries.stream().filter(e->e.scope.equals(scope)).map(Entry::marker).toList();}
    public CompassLandmark add(String scope,String dimension,BlockPos pos,String name){
        var existing=all(scope).stream().filter(p->p.dimension().equals(dimension)&&p.pos().equals(pos)).findFirst();
        if(existing.isPresent())return existing.get();
        if(scope.isBlank()||all(scope).size()>=LIMIT)return null;
        var marker=new CompassLandmark("custom:"+UUID.randomUUID(),clean(name),dimension,pos.immutable(),CompassLandmark.Kind.CUSTOM,0xFF8BDAF8);
        entries.add(new Entry(scope,marker));return marker;
    }
    public void delete(String scope,String id){entries.removeIf(e->e.scope.equals(scope)&&e.marker.id().equals(id));}
    public List<String> serialize(){
        return entries.stream().map(e->{
            var p=e.marker;var j=new JsonObject();j.addProperty("scope",e.scope);j.addProperty("id",p.id().substring(7));
            j.addProperty("dimension",p.dimension());j.addProperty("x",p.pos().getX());j.addProperty("y",p.pos().getY());j.addProperty("z",p.pos().getZ());j.addProperty("name",p.title());return j.toString();
        }).toList();
    }
    private static String clean(String name){var value=name.replaceAll("\\p{Cntrl}","").strip();return value.substring(0,Math.min(80,value.length()));}
}
