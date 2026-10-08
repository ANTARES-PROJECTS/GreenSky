package com.greencodes.greensky.island;

import java.util.*;
import org.bukkit.Material;

/** Blueprint puro e determinístico; coordenadas relativas ao centro e à altura base. */
public record StarterIslandPlan(Theme theme, List<Block> blocks) {
    public record Block(int x, int y, int z, Material material) {}
    public enum Theme {
        WOODLAND("Bosque verde",Material.OAK_LOG,Material.OAK_LEAVES,Material.POPPY,Material.MOSSY_COBBLESTONE,Material.OAK_SAPLING),
        SAKURA("Jardim sakura",Material.CHERRY_LOG,Material.CHERRY_LEAVES,Material.PINK_TULIP,Material.CALCITE,Material.CHERRY_SAPLING),
        BOREAL("Refúgio boreal",Material.SPRUCE_LOG,Material.SPRUCE_LEAVES,Material.FERN,Material.ANDESITE,Material.SPRUCE_SAPLING),
        GOLDEN("Clareira dourada",Material.BIRCH_LOG,Material.BIRCH_LEAVES,Material.DANDELION,Material.GRANITE,Material.BIRCH_SAPLING);
        final String name;
        final Material log,leaves,flower,rock,sapling;
        Theme(String name,Material log,Material leaves,Material flower,Material rock,Material sapling) {
            this.name=name; this.log=log; this.leaves=leaves; this.flower=flower; this.rock=rock; this.sapling=sapling;
        }
        public String displayName() { return name; }
        public Material sapling() { return sapling; }
    }
    public StarterIslandPlan { blocks=List.copyOf(blocks); }
    public static StarterIslandPlan create(int centerX,int centerZ,int radius) {
        Random random=new Random(31L*centerX+centerZ);
        Theme theme=Theme.values()[random.nextInt(Theme.values().length)];
        List<Block> blocks=new ArrayList<>();
        int depth=Math.min(9,radius+3);
        for (int x=-radius;x<=radius;x++) for (int z=-radius;z<=radius;z++) {
            int distance=x*x+z*z;
            int edge=radius*radius-radius/2+random.nextInt(Math.max(1,radius));
            if (distance>edge && !(Math.abs(x)<=1&&Math.abs(z)<=1)) continue;
            boolean central=Math.abs(x)<=1&&Math.abs(z)<=1;
            add(blocks,x,0,z,central?Material.GRASS_BLOCK:random.nextInt(9)==0?Material.MOSS_BLOCK:Material.GRASS_BLOCK);
            for (int layer=1;layer<=depth;layer++) {
                double reach=radius*(1.0-(layer-1.0)/(depth+1));
                if (distance<=reach*reach) add(blocks,x,-layer,z,layer<3?Material.DIRT:
                        random.nextInt(5)==0?theme.rock:Material.STONE);
            }
            if (!central && radius>=3 && random.nextInt(7)==0)
                add(blocks,x,1,z,theme==Theme.GOLDEN && random.nextBoolean()?Material.OXEYE_DAISY:theme.flower);
        }
        add(blocks,0,-3,0,Material.BEDROCK);
        if (radius>=3) {
            int tree=Math.max(2,radius/2);
            int height=theme==Theme.BOREAL?6:4+random.nextInt(2);
            for (int y=1;y<=height;y++) add(blocks,tree,y,tree,theme.log);
            if (theme==Theme.BOREAL) {
                for (int y=3;y<=height+2;y++) {
                    int width=y>=height?1:2;
                    for (int x=-width;x<=width;x++) for(int z=-width;z<=width;z++)
                        if ((x!=0||z!=0)&&Math.abs(x)+Math.abs(z)<=width+1) add(blocks,tree+x,y,tree+z,theme.leaves);
                }
                add(blocks,tree,height+2,tree,theme.leaves);
            } else {
                for(int y=height-1;y<=height+2;y++) {
                    int width=y==height+2?1:2;
                    for(int x=-width;x<=width;x++) for(int z=-width;z<=width;z++)
                        if ((x!=0||z!=0||y>height)&&x*x+z*z<=width*width+1) add(blocks,tree+x,y,tree+z,theme.leaves);
                }
            }
            add(blocks,-2,0,2,Material.OAK_PLANKS);
            add(blocks,-2,1,2,Material.CHEST);
            add(blocks,2,0,-2,Material.COBBLESTONE);
            add(blocks,2,1,-2,Material.OAK_FENCE);
            add(blocks,2,2,-2,Material.LANTERN);
        }
        if (radius>=6) {
            for(int x=2;x<=radius-1;x++) add(blocks,x,0,0,Material.DIRT_PATH);
            add(blocks,-radius/2,1,radius/2,theme.rock);
            add(blocks,-radius/2,2,radius/2,Material.MOSS_BLOCK);
            add(blocks,-radius/2+1,1,radius/2,theme.rock);
            add(blocks,radius-2,-3,0,theme.leaves);
            add(blocks,radius-2,-4,0,theme.leaves);
        }
        if (radius>=8) {
            // Lago 5x5, duas camadas de água e margem sólida; centro de chegada fica livre.
            for(int x=-7;x<=-1;x++) for(int z=-6;z<=0;z++) {
                boolean water=x>=-6&&x<=-2&&z>=-5&&z<=-1;
                add(blocks,x,-2,z,Material.COBBLESTONE);
                add(blocks,x,-1,z,water?Material.WATER:Material.COBBLESTONE);
                add(blocks,x,0,z,water?Material.WATER:Material.MOSSY_COBBLESTONE);
                if (x!=0||z!=0) add(blocks,x,1,z,Material.AIR);
            }
        }
        // Sempre preservar a chegada: chão e dois blocos de altura sem obstáculos.
        for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) {
            add(blocks,x,0,z,Material.GRASS_BLOCK);
            add(blocks,x,1,z,Material.AIR);
            add(blocks,x,2,z,Material.AIR);
        }
        return new StarterIslandPlan(theme,blocks);
    }
    private static void add(List<Block> blocks,int x,int y,int z,Material material) { blocks.add(new Block(x,y,z,material)); }
}
