package me.yleoft.zAPI.hooks;

/**
 * Represents a hook instance for integrating with external plugins or systems.
 */
public interface HookInstance {

    /**
     * Checks if the hook exists (i.e., if the external plugin or system is available).
     *
     * @return true if the hook exists, false otherwise.
     */
    boolean exists();

    /**
     * Preloads the hook, preparing any necessary resources or configurations.
     */
    default void preload() {}

    /**
     * Loads the hook, initializing any necessary resources or connections.
     */
    void load();

    default String preloadMessage() {
        return "";
    }

    String message();

}
