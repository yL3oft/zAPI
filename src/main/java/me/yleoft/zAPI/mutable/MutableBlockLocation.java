package me.yleoft.zAPI.mutable;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public class MutableBlockLocation {
    private final World world;
    private int x, y, z;

    public MutableBlockLocation(World world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void set(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Block getBlock() {
        return world.getBlockAt(x, y, z);
    }

    public Location toLocation() {
        return new Location(world, x, y, z);
    }
}