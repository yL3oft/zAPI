package me.yleoft.zAPI.command;

/**
 * Represents the main command in a command structure.
 */
public interface Command extends CommandBasis {

    /**
     * Gets the cooldown time for the command in seconds.
     *
     * @return The cooldown time in seconds. Default is 0 seconds.
     */
    default double cooldownTime() {
        return 0D;
    }

    /**
     * Gets the permission node that allows bypassing the command's cooldown.
     *
     * @return The permission node as a String, or null if no bypass permission is set.
     */
    default String bypassCooldownPermission() {
        return null;
    }

}
