package me.yleoft.zAPI.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import static java.util.Objects.requireNonNull;

/**
 * Utility class for location-related operations.
 */
public abstract class LocationUtils {

    private static final Set<Material> blacklistedGround = new HashSet<>();

    static {
        addIfExists("CACTUS");
        addIfExists("FIRE");
        addIfExists("LAVA");
        addIfExists("MAGMA_BLOCK");
        addIfExists("CAMPFIRE");
        addIfExists("SOUL_FIRE");
    }

    /**
     * Serializes a Location object into a string format.
     * @param location the Location to serialize
     * @return a string representation of the Location
     */
    public static String serialize(@NotNull final Location location) {
        String world = requireNonNull(location.getWorld()).getName();
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        float yaw = location.getYaw();
        float pitch = location.getPitch();

        return serialize(world, x, y, z, yaw, pitch);
    }

    /**
     * Serializes the given parameters into a string format.
     * @param world the name of the world
     * @param x the x-coordinate
     * @param y the y-coordinate
     * @param z the z-coordinate
     * @param yaw the yaw angle
     * @param pitch the pitch angle
     * @return a string representation of the parameters
     */
    public static String serialize(@NotNull final String world, final double x, final double y, final double z, final float yaw, final float pitch) {
        StringBuilder sb = new StringBuilder(80);
        sb.append(world).append(';')
                .append(x).append(';')
                .append(y).append(';')
                .append(z).append(';')
                .append(yaw).append(';')
                .append(pitch);
        return sb.toString();
    }

    /**
     * Deserializes a string into a Location object.
     * @param serialized the serialized string
     * @return a Location object representing the serialized data
     */
    public static Location deserialize(@NotNull final String serialized) {
        StringTokenizer tokenizer = new StringTokenizer(serialized, ";");

        World w = Bukkit.getWorld(tokenizer.nextToken());
        double x = Double.parseDouble(tokenizer.nextToken());
        double y = Double.parseDouble(tokenizer.nextToken());
        double z = Double.parseDouble(tokenizer.nextToken());
        float yaw = Float.parseFloat(tokenizer.nextToken());
        float pitch = Float.parseFloat(tokenizer.nextToken());

        return new Location(w, x, y, z, yaw, pitch);
    }

    /**
     * Adds a material to the blacklist if it exists.
     * @param materialName the name of the material to add
     */
    private static void addIfExists(@NotNull final String materialName) {
        try {
            Material mat = Material.valueOf(materialName);
            blacklistedGround.add(mat);
        } catch (IllegalArgumentException ignored) {
        }
    }

    /**
     * Finds the nearest safe location from the given origin within a specified radius.
     * @param origin the starting location
     * @param radius the search radius
     * @param heightCheckRange the range to check for height variations
     * @return a safe location if found, otherwise null
     */
    @Nullable
    public static Location findNearestSafeLocation(@NotNull final Location origin, int radius, int heightCheckRange) {
        if(isSafeLocation(origin)) return origin;
        World world = origin.getWorld();
        if (world == null) return null;

        int ox = origin.getBlockX();
        int oy = origin.getBlockY();
        int oz = origin.getBlockZ();

        for (int r = 0; r <= radius; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.abs(x) != r && Math.abs(z) != r) continue; // spiral pattern edge only

                    for (int yOffset = -heightCheckRange; yOffset <= heightCheckRange; yOffset++) {
                        int y = oy + yOffset;
                        int minY = 0;
                        int maxY = 256;
                        try {
                            minY = world.getMinHeight();
                            maxY = world.getMaxHeight();
                        }catch (Throwable ignored) {
                        }
                        if (y < minY || y > maxY) continue;

                        Location check = new Location(world, ox + x, y, oz + z);
                        if (isSafeLocation(check)) {
                            return check.add(0.5, 0, 0.5); // center
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Checks if the given location is safe for a player to stand on.
     * @param loc the location to check
     * @return true if the location is safe, false otherwise
     */
    public static boolean isSafeLocation(@NotNull final Location loc) {
        World world = loc.getWorld();
        if (world == null) return false;

        Block feet = world.getBlockAt(loc);
        Block head = world.getBlockAt(loc.clone().add(0, 1, 0));
        Block ground = world.getBlockAt(loc.clone().add(0, -1, 0));

        return isAirOrNonSolid(feet) && isAirOrNonSolid(head) && isSafeGround(ground);
    }

    /**
     * Checks if the given block is air or a non-solid block.
     * @param block the block to check
     * @return true if the block is air or non-solid, false otherwise
     */
    private static boolean isAirOrNonSolid(@NotNull final Block block) {
        return !block.getType().isSolid() && !block.isLiquid();
    }

    /**
     * Checks if the given block is a solid block that is not blacklisted.
     * @param block the block to check
     * @return true if the block is solid and not blacklisted, false otherwise
     */
    private static boolean isSafeGround(@NotNull final Block block) {
        Material type = block.getType();
        return block.getType().isSolid() && !block.isLiquid() && !blacklistedGround.contains(type);
    }


}
