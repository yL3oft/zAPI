package me.yleoft.zAPI.folia;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class FoliaRunnable extends BukkitRunnable {

    private ScheduledTask foliaTask;
    private BukkitTask bukkitTask;

    @Override
    public synchronized void cancel() throws IllegalStateException {
        if (this.foliaTask != null) {
            this.foliaTask.cancel();
            this.foliaTask = null;
        } else if (this.bukkitTask != null) {
            // Legacy Bukkit task
            this.bukkitTask.cancel();
            this.bukkitTask = null;
        }
    }

    @Override
    public void run() {
    }

    public void setScheduledTask(ScheduledTask task) {
        this.foliaTask = task;
    }

    public void setBukkitTask(BukkitTask task) {
        this.bukkitTask = task;
    }
}
