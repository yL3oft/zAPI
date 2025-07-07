package me.yleoft.zAPI.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

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
                        if (y < world.getMinHeight() || y > world.getMaxHeight()) continue;

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
