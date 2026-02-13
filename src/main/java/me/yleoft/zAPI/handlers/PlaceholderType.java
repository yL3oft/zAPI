package me.yleoft.zAPI.handlers;

/**
 * Defines how a placeholder should be registered with MiniPlaceholders.
 */
public enum PlaceholderType {

    /**
     * Requires a player audience. The viewing player is the target.
     * <p>PAPI: {@code %identifier_key_args%}</p>
     * <p>Mini: {@code <identifier_key:args>} (audience-based)</p>
     */
    AUDIENCE,

    /**
     * Global/server placeholder. No player is required.
     * <p>PAPI: {@code %identifier_key%}</p>
     * <p>Mini: {@code <identifier_key>} (global, no audience needed)</p>
     */
    GLOBAL,

    /**
     * Targets a specific player by name (passed as the first argument).
     * <p>PAPI: {@code %identifier_player_<name>_key_args%}</p>
     * <p>Mini: {@code <identifier_key:playername:args>} (audience-based, first arg is target player name)</p>
     */
    PLAYER_TARGETED
}