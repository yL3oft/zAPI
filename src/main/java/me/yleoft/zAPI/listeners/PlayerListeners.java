package me.yleoft.zAPI.listeners;

import me.yleoft.zAPI.mutable.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;

import static me.yleoft.zAPI.managers.PluginYAMLManager.cacheCooldown;
import static me.yleoft.zAPI.managers.PluginYAMLManager.getCmds;
import static me.yleoft.zAPI.utils.StringUtils.transform;

/**
 * PlayerListeners is a class that listens for player events.
 */
public class PlayerListeners implements Listener {

    @EventHandler
    public void onPlayerCommandPreprocessEvent(final PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String commandString = event.getMessage();
        String command = commandString.split(" ")[0].replace("/", "").toLowerCase(Locale.ENGLISH);
        PluginCommand pluginCommand = Bukkit.getPluginCommand(command);

        if(getCmds().containsKey(pluginCommand)) {
            long currentTime = System.currentTimeMillis();
            if(cacheCooldown.containsKey(player)) {
                long cooldown = cacheCooldown.get(player);
                if (currentTime >= cooldown) {
                    cacheCooldown.remove(player);
                }else {
                    event.setCancelled(true);
                    double seconds = (double) (cooldown-currentTime)/1000;
                    player.sendMessage(transform(Messages.getCooldownExpired(player, seconds)));
                }
            }
            if(!cacheCooldown.containsKey(player)) {
                double cooldown = getCmds().get(pluginCommand);
                if(cooldown == 0) return;
                long cooldownInMills = (long) (cooldown*1000);
                cacheCooldown.put(player, currentTime+cooldownInMills);
                return;
            }
        }
    }

}
