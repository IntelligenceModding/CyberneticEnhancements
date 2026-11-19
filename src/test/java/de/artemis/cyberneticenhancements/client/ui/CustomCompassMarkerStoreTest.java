package de.artemis.cyberneticenhancements.client.ui;

import net.minecraft.core.BlockPos;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class CustomCompassMarkerStoreTest {
    @Test public void markersPersistPerWorldAndDimensionAndDeleteOnlyTheSelectedEntry(){
        var store=new CustomCompassMarkerStore();
        var first=store.add("world-a","minecraft:overworld",new BlockPos(10,64,20),"Home");
        assertEquals(first,store.add("world-a","minecraft:overworld",new BlockPos(10,64,20),"Duplicate"));
        store.add("world-a","minecraft:the_nether",new BlockPos(10,64,20),"Portal");
        store.add("world-b","minecraft:overworld",new BlockPos(10,64,20),"Other world");
        var restored=CustomCompassMarkerStore.parse(store.serialize());
        assertEquals(2,restored.all("world-a").size());assertEquals(1,restored.all("world-b").size());
        assertEquals(first,restored.all("world-a").getFirst());
        restored.delete("world-b",first.id());assertEquals(2,restored.all("world-a").size());
        restored.delete("world-a",first.id());assertEquals(1,restored.all("world-a").size());
        assertEquals(1,CustomCompassMarkerStore.parse(restored.serialize()).all("world-a").size());
    }
    @Test public void malformedRecordsAreIgnoredAndLimitsDoNotAffectOtherWorlds(){
        assertTrue(CustomCompassMarkerStore.parse(List.of("broken","{}","null")).all("a").isEmpty());
        var store=new CustomCompassMarkerStore();
        for(int i=0;i<CustomCompassMarkerStore.LIMIT;i++)assertNotNull(store.add("a","minecraft:overworld",new BlockPos(i,64,0),"Marker"));
        assertNull(store.add("a","minecraft:overworld",new BlockPos(999,64,0),"Too many"));
        assertNotNull(store.add("b","minecraft:overworld",BlockPos.ZERO,"Other world"));
        assertEquals(CustomCompassMarkerStore.LIMIT,CustomCompassMarkerStore.parse(store.serialize()).all("a").size());
    }
}
