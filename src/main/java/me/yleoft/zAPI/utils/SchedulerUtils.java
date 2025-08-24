package me.yleoft.zAPI.utils;

import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.yleoft.zAPI.folia.FoliaRunnable;
import me.yleoft.zAPI.zAPI;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static me.yleoft.zAPI.zAPI.isFolia;

/**
 * Utility class for scheduling tasks in a Paper/Folia server.
 */
public abstract class SchedulerUtils {

    /**
     * Schedules a task to run later on the main server thread.
     * @param loc The location where the task should run, or null for the main thread.
     * @param task   The task to run.
     * @param delay  The delay in ticks before the task runs.
     */
    public static void runTaskLater(@Nullable Location loc, @NotNull Runnable task, long delay) {
        JavaPlugin plugin = zAPI.getPlugin();
        if (isFolia()) {
            try {
                if (loc != null) {
                    Method getRegionScheduler = plugin.getServer().getClass().getMethod("getRegionScheduler");
                    RegionScheduler regionScheduler = (RegionScheduler) getRegionScheduler.invoke(plugin.getServer());
                    regionScheduler.runDelayed(
                            plugin,
                            loc,
                            (ScheduledTask scheduledTask) -> task.run(),
                            delay
                    );
                } else {
                    Method getGlobalScheduler = plugin.getServer().getClass().getMethod("getGlobalRegionScheduler");
                    GlobalRegionScheduler globalScheduler = (GlobalRegionScheduler) getGlobalScheduler.invoke(plugin.getServer());
                    globalScheduler.runDelayed(
                            plugin,
                            (ScheduledTask scheduledTask) -> task.run(),
                            delay
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, task, delay);
    }

    /**
     * Schedules a task to run repeatedly on the main server thread.
     * @param loc The location where the task should run, or null for the main thread.
     * @param runnable   The BukkitRunnable to run.
     * @param delay  The delay in ticks before the task runs.
     * @param period The period in ticks between subsequent runs of the task.
     */
    public static void runTaskTimer(@Nullable Location loc, @NotNull FoliaRunnable runnable, long delay, long period) {
        JavaPlugin plugin = zAPI.getPlugin();
        if (isFolia()) {
            try {
                ScheduledTask task;
                if (loc != null) {
                    Method getRegionScheduler = plugin.getServer().getClass().getMethod("getRegionScheduler");
                    RegionScheduler regionScheduler = (RegionScheduler) getRegionScheduler.invoke(plugin.getServer());
                    task = regionScheduler.runAtFixedRate(
                            plugin,
                            loc,
                            (ScheduledTask t) -> runnable.run(),
                            delay,
                            period
                    );
                } else {
                    Method getGlobalScheduler = plugin.getServer().getClass().getMethod("getGlobalRegionScheduler");
                    GlobalRegionScheduler globalScheduler = (GlobalRegionScheduler) getGlobalScheduler.invoke(plugin.getServer());
                    task = globalScheduler.runAtFixedRate(
                            plugin,
                            (ScheduledTask t) -> runnable.run(),
                            delay,
                            period
                    );
                }
                runnable.setScheduledTask(task);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        BukkitTask task = runnable.runTaskTimer(plugin, delay, period);
        runnable.setBukkitTask(task);
    }

    /**
     * Schedules a task to run later asynchronously on the main server thread.
     * @param loc The location where the task should run, or null for the main thread.
     * @param task   The task to run.
     * @param delay  The delay in ticks before the task runs.
     */
    public static void runTaskLaterAsynchronously(@Nullable Location loc, @NotNull Runnable task, long delay) {
        JavaPlugin plugin = zAPI.getPlugin();
        if (isFolia()) {
            try {
                if (loc != null) {
                    Method getRegionScheduler = plugin.getServer().getClass().getMethod("getRegionScheduler");
                    RegionScheduler regionScheduler = (RegionScheduler) getRegionScheduler.invoke(plugin.getServer());
                    regionScheduler.runDelayed(
                            plugin,
                            loc,
                            (ScheduledTask scheduledTask) -> task.run(),
                            delay
                    );
                } else {
                    Method getGlobalScheduler = plugin.getServer().getClass().getMethod("getGlobalRegionScheduler");
                    GlobalRegionScheduler globalScheduler = (GlobalRegionScheduler) getGlobalScheduler.invoke(plugin.getServer());
                    globalScheduler.runDelayed(
                            plugin,
                            (ScheduledTask scheduledTask) -> task.run(),
                            delay
                    );
                }
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
    }

    /**
     * Schedules a task to run repeatedly on the main server thread asynchronously.
     * @param runnable The FoliaRunnable to run.
     * @param delay  The delay in ticks before the task runs.
     * @param period The period in ticks between subsequent runs of the task.
     */
    public static void runTaskTimerAsynchronously(@NotNull FoliaRunnable runnable, long delay, long period) {
        JavaPlugin plugin = zAPI.getPlugin();
        if (isFolia()) {
            try {
                Method getGlobalScheduler = plugin.getServer().getClass().getMethod("getGlobalRegionScheduler");
                GlobalRegionScheduler globalScheduler = (GlobalRegionScheduler) getGlobalScheduler.invoke(plugin.getServer());
                class AsyncRepeatingTask {
                    void start(long initialDelay) {
                        ScheduledTask task = globalScheduler.runDelayed(plugin, (ScheduledTask t) -> {
                            runnable.run();
                            start(period);
                        }, initialDelay);
                        runnable.setScheduledTask(task);
                    }
                }
                new AsyncRepeatingTask().start(delay);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        BukkitTask task = runnable.runTaskTimerAsynchronously(plugin, delay, period);
        runnable.setBukkitTask(task);
    }

    /**
     * Runs a task asynchronously on the main server thread.
     * @param task The task to run.
     */
    public static void runTaskAsynchronously(@NotNull Runnable task) {
        JavaPlugin plugin = zAPI.getPlugin();
        if (isFolia()) {
            try {
                Method getGlobalScheduler = plugin.getServer().getClass().getMethod("getGlobalRegionScheduler");
                GlobalRegionScheduler globalScheduler = (GlobalRegionScheduler) getGlobalScheduler.invoke(plugin.getServer());
                globalScheduler.execute(plugin, task);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
    }

    /**
     * Runs a task on the main server thread at a specific location or globally.
     * @param loc The location where the task should run, or null for the main thread.
     * @param task The task to run.
     */
    public static void runTask(@Nullable Location loc, @NotNull Runnable task) {
        JavaPlugin plugin = zAPI.getPlugin();
        if (isFolia()) {
            try {
                if (loc != null) {
                    Method getRegionScheduler = plugin.getServer().getClass().getMethod("getRegionScheduler");
                    RegionScheduler regionScheduler = (RegionScheduler) getRegionScheduler.invoke(plugin.getServer());
                    regionScheduler.execute(plugin, loc, task);
                } else {
                    Method getGlobalScheduler = plugin.getServer().getClass().getMethod("getGlobalRegionScheduler");
                    GlobalRegionScheduler globalScheduler = (GlobalRegionScheduler) getGlobalScheduler.invoke(plugin.getServer());
                    globalScheduler.execute(plugin, task);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    public static <T> CompletableFuture<T> callSyncMethod(@Nullable Location loc, @NotNull Callable<T> task) {
        JavaPlugin plugin = zAPI.getPlugin();
        if (isFolia()) {
            CompletableFuture<T> future = new CompletableFuture<>();
            try {
                if (loc != null) {
                    Method getRegionScheduler = plugin.getServer().getClass().getMethod("getRegionScheduler");
                    RegionScheduler regionScheduler = (RegionScheduler) getRegionScheduler.invoke(plugin.getServer());
                    regionScheduler.execute(plugin, loc, () -> {
                        try {
                            future.complete(task.call());
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                        }
                    });
                } else {
                    Method getGlobalScheduler = plugin.getServer().getClass().getMethod("getGlobalRegionScheduler");
                    GlobalRegionScheduler globalScheduler = (GlobalRegionScheduler) getGlobalScheduler.invoke(plugin.getServer());
                    globalScheduler.execute(plugin, () -> {
                        try {
                            future.complete(task.call());
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                        }
                    });
                }
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
            return future;
        }
        CompletableFuture<T> cf = new CompletableFuture<>();
        try {
            Future<T> bukkitFuture = plugin.getServer().getScheduler().callSyncMethod(plugin, task);
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    T result = bukkitFuture.get();
                    cf.complete(result);
                } catch (Throwable ex) {
                    cf.completeExceptionally(ex);
                }
            });
        } catch (Throwable e) {
            cf.completeExceptionally(e);
        }
        return cf;
    }
}
