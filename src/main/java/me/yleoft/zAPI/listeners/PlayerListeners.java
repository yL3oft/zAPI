package me.yleoft.zAPI.listeners;

import me.yleoft.zAPI.configuration.Messages;
import me.yleoft.zAPI.utility.PluginYAML;
import me.yleoft.zAPI.utility.TextFormatter;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;

import static me.yleoft.zAPI.utility.PluginYAML.cacheCooldown;

/**
 * PlayerListeners is a class that listens for player events.
 */
public class PlayerListeners implements Listener {

    /**
     * Listens for player command preprocess events.
     * For now used to handle command cooldowns.
     */
    @EventHandler
    public void onPlayerCommandPreprocessEvent(final PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String commandString = event.getMessage();
        String command = commandString.split(" ")[0].replace("/", "").toLowerCase(Locale.ENGLISH);
        PluginCommand pluginCommand = Bukkit.getPluginCommand(command);
        if (pluginCommand == null) return;

        if(PluginYAML.getCmds().containsKey(pluginCommand)) {
            long currentTime = System.currentTimeMillis();
            if(cacheCooldown.containsKey(player)) {
                long cooldown = cacheCooldown.get(player);
                if (currentTime >= cooldown) {
                    cacheCooldown.remove(player);
                }else {
                    event.setCancelled(true);
                    double seconds = (double) (cooldown-currentTime)/1000;
                    player.sendMessage(TextFormatter.transform(player, Messages.getCooldownMessage(seconds)));
                }
            }
            if(!cacheCooldown.containsKey(player)) {
                double cooldown = PluginYAML.getCmds().get(pluginCommand);
                if(cooldown == 0) return;
                long cooldownInMills = (long) (cooldown*1000);
                cacheCooldown.put(player, currentTime+cooldownInMills);
            }
        }
    }

}
