package me.yleoft.zAPI.mutable;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * MutableBlockLocation class to handle mutable block locations.
 * This class provides a way to set and get block locations.
 */
public class MutableBlockLocation {
    private final World world;
    private int x, y, z;

    /**
     * Constructor to create a new mutable block location.
     * @param world The world of the block location.
     * @param x The x coordinate of the block location.
     * @param y The y coordinate of the block location.
     * @param z The z coordinate of the block location.
     */
    public MutableBlockLocation(World world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Sets the coordinates of the block location.
     * @param x The x coordinate of the block location.
     * @param y The y coordinate of the block location.
     * @param z The z coordinate of the block location.
     */
    public void set(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Gets the block at the block location.
     * @return The block at the block location.
     */
    public Block getBlock() {
        return world.getBlockAt(x, y, z);
    }

    /**
     * Converts the block location to a location.
     * @return The location.
     */
    public Location toLocation() {
        return new Location(world, x, y, z);
    }
}