package me.yleoft.zAPI.hook;

import io.github.miniplaceholders.api.Expansion;
import me.yleoft.zAPI.placeholders.PlaceholderDefinition;
import me.yleoft.zAPI.placeholders.PlaceholderType;
import me.yleoft.zAPI.placeholders.PlaceholdersHandler;
import me.yleoft.zAPI.zAPI;
import net.kyori.adventure.text.minimessage.tag.Tag;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HookMiniPlaceholders implements HookInstance {

    public static String message = "MiniPlaceholders has been found, mini placeholders are enabled!";

    private final Map<PlaceholdersHandler, Expansion> expansions = new HashMap<>();
    private boolean exists = false;

    @Override
    public boolean exists() {
        return exists;
    }

    @Override
    public void load() {
        if(!Bukkit.getPluginManager().isPluginEnabled("MiniPlaceholders")) return;
        exists = true;
    }

    @Override
    public void unload() {
        if(!exists()) return;
        for(PlaceholdersHandler handler : new HashMap<>(expansions).keySet()) {
            unregisterPlaceholderExpansion(handler);
        }
    }

    @Override
    public String message() {
        return message;
    }

    /**
     * Registers all placeholders from a {@link PlaceholdersHandler} as a MiniPlaceholders Expansion.
     *
     * <p>Registration branches based on {@link PlaceholderType}:</p>
     * <ul>
     *   <li>{@link PlaceholderType#AUDIENCE} — registered as {@code audiencePlaceholder},
     *       the viewing player is passed to the handler.</li>
     *   <li>{@link PlaceholderType#GLOBAL} — registered as {@code globalPlaceholder},
     *       no audience/player is required. Player is passed as {@code null}.</li>
     *   <li>{@link PlaceholderType#PLAYER_TARGETED} — registered as {@code audiencePlaceholder},
     *       but the <b>first</b> argument is consumed as a player name. The resolved
     *       {@link OfflinePlayer} is passed to the handler instead of the viewing player.
     *       <br>Usage: {@code <zhomes_home:Steve:1>} — "Steve" is the target, "1" is the arg.</li>
     * </ul>
     */
    public void registerPlaceholderExpansion(PlaceholdersHandler handler) {
        if(!exists()) return;
        try {
            Expansion.Builder builder = Expansion.builder(handler.getIdentifier())
                    .author(handler.getAuthor())
                    .version(handler.getVersion());

            for (PlaceholderDefinition definition : handler.getPlaceholders()) {
                String miniKey = definition.getKey();
                int paramCount = definition.getParameterCount();
                PlaceholderType type = definition.getType();

                switch (type) {
                    case GLOBAL -> {
                        // Global placeholder: no audience needed, player is null
                        // Usage: <zhomes_version>  or  <zhomes_some_key:arg1>
                        builder.globalPlaceholder(miniKey, (queue, ctx) -> {
                            List<String> args = new ArrayList<>();
                            for (int i = 0; i < paramCount; i++) {
                                if (queue.hasNext()) {
                                    try {
                                        args.add(queue.pop().value());
                                    } catch (Exception e) {
                                        args.add("");
                                    }
                                }
                            }

                            String result = handler.onPlaceholderRequest(null, miniKey, args);
                            if (result == null) result = "";
                            return Tag.preProcessParsed(result);
                        });
                    }

                    case PLAYER_TARGETED -> {
                        // Player-targeted: first arg is the target player name,
                        // remaining args are the actual parameters.
                        // Usage: <zhomes_home:Steve:1>  →  player="Steve", args=["1"]
                        builder.globalPlaceholder(miniKey, (queue, ctx) -> {
                            OfflinePlayer targetPlayer = null;
                            if (queue.hasNext()) {
                                try {
                                    String targetName = queue.pop().value();
                                    if (targetName != null && !targetName.isEmpty()) {
                                        targetPlayer = Bukkit.getOfflinePlayer(targetName);
                                    }
                                } catch (Exception e) {
                                    return Tag.preProcessParsed("");
                                }
                            }

                            // Remaining arguments: actual placeholder parameters
                            List<String> args = new ArrayList<>();
                            for (int i = 0; i < paramCount; i++) {
                                if (queue.hasNext()) {
                                    try {
                                        args.add(queue.pop().value());
                                    } catch (Exception e) {
                                        args.add("");
                                    }
                                }
                            }

                            String result = handler.onPlaceholderRequest(targetPlayer, miniKey, args);
                            if (result == null) result = "";
                            return Tag.preProcessParsed(result);
                        });
                    }

                    case AUDIENCE -> {
                        // Standard audience placeholder: viewing player is the target
                        // Usage: <zhomes_home:1>
                        builder.audiencePlaceholder(Player.class, miniKey, (player, queue, ctx) -> {
                            List<String> args = new ArrayList<>();
                            for (int i = 0; i < paramCount; i++) {
                                if (queue.hasNext()) {
                                    try {
                                        args.add(queue.pop().value());
                                    } catch (Exception e) {
                                        args.add("");
                                    }
                                }
                            }

                            OfflinePlayer offlinePlayer = player;
                            String result = handler.onPlaceholderRequest(offlinePlayer, miniKey, args);
                            if (result == null) result = "";
                            return Tag.preProcessParsed(result);
                        });
                    }
                }
            }

            Expansion expansion = builder.build();
            expansion.register();
            expansions.put(handler, expansion);
        } catch (Exception exception) {
            zAPI.getLogger().warn("Failed to register MiniPlaceholders expansion for: " + handler.getIdentifier(), exception);
        }
    }

    /**
     * Unregisters a previously registered MiniPlaceholders expansion.
     */
    public void unregisterPlaceholderExpansion(PlaceholdersHandler handler) {
        if(!exists()) return;
        Expansion expansion = expansions.remove(handler);
        if (expansion != null) {
            try {
                expansion.unregister();
            } catch (Exception exception) {
                zAPI.getLogger().warn("Failed to unregister MiniPlaceholders expansion for: " + handler.getIdentifier(), exception);
            }
        }
    }

    public static void setMessage(String message) {
        HookMiniPlaceholders.message = message;
    }

}