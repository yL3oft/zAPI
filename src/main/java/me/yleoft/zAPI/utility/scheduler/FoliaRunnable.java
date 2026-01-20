package me.yleoft.zAPI.utility.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * A class for running tasks on both Bukkit and Folia. Without this class, you would need to check if the server is using Folia or not, and then run the task accordingly.
 */
public class FoliaRunnable extends BukkitRunnable {

    private ScheduledTask foliaTask;
    private BukkitTask bukkitTask;

    /**
     * Cancels the task if it is running.
     */
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

    /**
     * This method is not used. Instead, use the run() method in the subclass.
     */
    @Override
    public void run() {
    }

    /**
     * Sets the scheduled task for Folia.
     * @param task The {@link ScheduledTask}.
     */
    public void setScheduledTask(ScheduledTask task) {
        this.foliaTask = task;
    }

    /**
     * Sets the Bukkit task for legacy Bukkit.
     * @param task The {@link BukkitTask}.
     */
    public void setBukkitTask(BukkitTask task) {
        this.bukkitTask = task;
    }
}
