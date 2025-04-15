package me.yleoft.zAPI.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.Set;

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

    private static void addIfExists(String materialName) {
        try {
            Material mat = Material.valueOf(materialName);
            blacklistedGround.add(mat);
        } catch (IllegalArgumentException ignored) {
        }
    }

    public static Location findNearestSafeLocation(Location origin, int radius, int heightCheckRange) {
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


    public static boolean isSafeLocation(Location loc) {
        World world = loc.getWorld();
        if (world == null) return false;

        Block feet = world.getBlockAt(loc);
        Block head = world.getBlockAt(loc.clone().add(0, 1, 0));
        Block ground = world.getBlockAt(loc.clone().add(0, -1, 0));

        return isAirOrNonSolid(feet) && isAirOrNonSolid(head) && isSafeGround(ground);
    }

    private static boolean isAirOrNonSolid(Block block) {
        return !block.getType().isSolid() && !block.isLiquid();
    }

    private static boolean isSafeGround(Block block) {
        Material type = block.getType();
        return block.getType().isSolid() && !block.isLiquid() && !blacklistedGround.contains(type);
    }


}
