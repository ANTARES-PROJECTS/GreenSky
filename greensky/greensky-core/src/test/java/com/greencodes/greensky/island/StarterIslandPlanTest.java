package com.greencodes.greensky.island;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class StarterIslandPlanTest {
    private static Map<String,Material> finalBlocks(StarterIslandPlan plan) {
        Map<String,Material> result=new HashMap<>();
        for(var block:plan.blocks()) result.put(block.x()+","+block.y()+","+block.z(),block.material());
        return result;
    }
    @Test void deterministicVariedAndSafeAcrossSupportedRadii() {
        Set<StarterIslandPlan.Theme> themes=EnumSet.noneOf(StarterIslandPlan.Theme.class);
        Set<Integer> fingerprints=new HashSet<>();
        for(int slot=1;slot<=40;slot++) {
            var region=IslandRegion.forSlot(slot,1200,100);
            var plan=StarterIslandPlan.create(region.centerX(),region.centerZ(),8);
            assertEquals(plan,StarterIslandPlan.create(region.centerX(),region.centerZ(),8));
            themes.add(plan.theme()); fingerprints.add(plan.blocks().hashCode());
        }
        assertEquals(4,themes.size());
        assertEquals(40,fingerprints.size());
        for(int radius=1;radius<=16;radius++) {
            var plan=StarterIslandPlan.create(-1200,1200,radius);
            var blocks=finalBlocks(plan);
            assertEquals(Material.BEDROCK,blocks.get("0,-3,0"));
            if (radius>=3) assertEquals(Material.CHEST,blocks.get("-2,1,2"));
            for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) {
                assertEquals(Material.GRASS_BLOCK,blocks.get(x+",0,"+z));
                assertEquals(Material.AIR,blocks.get(x+",1,"+z));
                assertEquals(Material.AIR,blocks.get(x+",2,"+z));
            }
            for(var block:plan.blocks()) {
                assertTrue(Math.abs(block.x())<=radius+3 && Math.abs(block.z())<=radius+3);
                assertTrue(block.y()>=-9 && block.y()<=8);
            }
        }
    }
    @Test void initialLakeHasOpenWaterLayersAndChestSurvivesDecoration() {
        var blocks=finalBlocks(StarterIslandPlan.create(1200,0,8));
        for(int x=-6;x<=-2;x++) for(int z=-5;z<=-1;z++) {
            assertEquals(Material.COBBLESTONE,blocks.get(x+",-2,"+z));
            assertEquals(Material.WATER,blocks.get(x+",-1,"+z));
            assertEquals(Material.WATER,blocks.get(x+",0,"+z));
            assertEquals(Material.AIR,blocks.get(x+",1,"+z));
        }
        assertEquals(Material.CHEST,blocks.get("-2,1,2"));
        assertEquals(Material.LANTERN,blocks.get("2,2,-2"));
    }
}
