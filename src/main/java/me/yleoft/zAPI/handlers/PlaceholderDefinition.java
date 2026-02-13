package me.yleoft.zAPI.handlers;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a single placeholder entry for MiniPlaceholders registration.
 *
 * <p>This defines a key that will be registered as a MiniPlaceholders tag,
 * along with how many colon-separated arguments it expects and what
 * {@link PlaceholderType type} of placeholder it is.</p>
 *
 * <p>Example definitions and their resulting usage:</p>
 * <ul>
 *   <li>{@code ("version", 0, GLOBAL)}         → {@code <zhomes_version>}              (no audience needed)</li>
 *   <li>{@code ("home", 1, AUDIENCE)}           → {@code <zhomes_home:1>}               (uses viewing player)</li>
 *   <li>{@code ("home_world", 1, AUDIENCE)}     → {@code <zhomes_home_world:1>}</li>
 *   <li>{@code ("home", 1, PLAYER_TARGETED)}    → {@code <zhomes_home:Steve:1>}         (targets "Steve")</li>
 *   <li>{@code ("has_home", 1, PLAYER_TARGETED)} → {@code <zhomes_has_home:Steve:base>}</li>
 * </ul>
 *
 * <p>For {@link PlaceholderType#PLAYER_TARGETED}, the {@code parameterCount} does <b>not</b>
 * include the player-name argument — that is prepended automatically. So if you define
 * {@code ("home", 1, PLAYER_TARGETED)}, usage is {@code <zhomes_home:Steve:1>} (player + 1 param).</p>
 */
public class PlaceholderDefinition {

    private final String key;
    private final int parameterCount;
    private final PlaceholderType type;

    /**
     * Creates a placeholder definition with an explicit type.
     *
     * @param key            The placeholder key (e.g., "home_name", "home_world")
     * @param parameterCount The number of colon-separated arguments this placeholder expects
     *                       (excluding the player-name arg for PLAYER_TARGETED)
     * @param type           The placeholder type
     */
    public PlaceholderDefinition(@NotNull String key, int parameterCount, @NotNull PlaceholderType type) {
        this.key = key;
        this.parameterCount = parameterCount;
        this.type = type;
    }

    /**
     * Creates a placeholder definition (defaults to {@link PlaceholderType#AUDIENCE}).
     *
     * @param key            The placeholder key
     * @param parameterCount The number of colon-separated arguments this placeholder expects
     */
    public PlaceholderDefinition(@NotNull String key, int parameterCount) {
        this(key, parameterCount, PlaceholderType.AUDIENCE);
    }

    /**
     * Creates a placeholder definition with no parameters (defaults to {@link PlaceholderType#AUDIENCE}).
     *
     * @param key The placeholder key
     */
    public PlaceholderDefinition(@NotNull String key) {
        this(key, 0, PlaceholderType.AUDIENCE);
    }

    /**
     * Gets the placeholder key.
     *
     * @return The key (e.g., "home_world")
     */
    @NotNull
    public String getKey() {
        return key;
    }

    /**
     * Gets the number of parameters this placeholder expects.
     * For {@link PlaceholderType#PLAYER_TARGETED}, this does NOT include the player name.
     *
     * @return The parameter count
     */
    public int getParameterCount() {
        return parameterCount;
    }

    /**
     * Gets the placeholder type.
     *
     * @return The {@link PlaceholderType}
     */
    @NotNull
    public PlaceholderType getType() {
        return type;
    }

    /**
     * Whether this placeholder has parameters.
     *
     * @return true if it has at least one parameter
     */
    public boolean hasParameters() {
        return parameterCount > 0;
    }
}