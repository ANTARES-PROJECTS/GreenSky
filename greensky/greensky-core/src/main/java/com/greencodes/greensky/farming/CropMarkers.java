package com.greencodes.greensky.farming;

import java.util.Arrays;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/** Identidade persistida no chunk, salva junto dos blocos. Acesso somente na thread do servidor. */
public class CropMarkers {
    private final NamespacedKey key;
    public CropMarkers(Plugin plugin, String name) { key = new NamespacedKey(plugin, name); }
    static long position(int x, int y, int z) {
        return ((long) y << 8) | ((x & 15) << 4) | (z & 15);
    }
    public boolean contains(Block block) {
        long[] plants = block.getChunk().getPersistentDataContainer().get(key, PersistentDataType.LONG_ARRAY);
        return plants != null && Arrays.stream(plants).anyMatch(p -> p == position(block.getX(), block.getY(), block.getZ()));
    }
    public void set(Block block, boolean planted) {
        var data = block.getChunk().getPersistentDataContainer();
        long[] old = data.get(key, PersistentDataType.LONG_ARRAY);
        if (old == null && !planted) return;
        long position = position(block.getX(), block.getY(), block.getZ());
        long[] filtered = old == null ? new long[0] : Arrays.stream(old).filter(p -> p != position).toArray();
        if (planted) {
            filtered = Arrays.copyOf(filtered, filtered.length + 1);
            filtered[filtered.length - 1] = position;
        }
        if (filtered.length == 0) data.remove(key);
        else data.set(key, PersistentDataType.LONG_ARRAY, filtered);
    }
    private static boolean isCrop(Material material) {
        return material == Material.WHEAT || material == Material.CARROTS || material == Material.POTATOES;
    }
    public boolean protectedBlock(Block block) {
        return isCrop(block.getType()) && contains(block)
                || block.getType() == Material.FARMLAND && contains(block.getRelative(org.bukkit.block.BlockFace.UP));
    }
}
