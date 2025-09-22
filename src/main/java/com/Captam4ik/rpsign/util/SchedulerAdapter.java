package com.Captam4ik.rpsign.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import java.util.function.Consumer;

public class SchedulerAdapter {
    private static final boolean IS_FOLIA = checkFolia();
    
    private static boolean checkFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    public interface Task {
        void cancel();
    }
    
    private static class BukkitTaskWrapper implements Task {
        private final BukkitTask task;
        public BukkitTaskWrapper(BukkitTask task) { this.task = task; }
        @Override public void cancel() { if (task != null) task.cancel(); }
    }
    
    private static class FoliaTaskWrapper implements Task {
        private final Object task; // ScheduledTask
        public FoliaTaskWrapper(Object task) { this.task = task; }
        @Override public void cancel() {
            if (task != null) {
                try { task.getClass().getMethod("cancel").invoke(task); } 
                catch (Exception e) { /* Ignore */ }
            }
        }
    }
    
    public static Task runTaskTimer(Plugin plugin, Runnable task, long delay, long period) {
        if (IS_FOLIA) {
            try {
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Object scheduledTask = scheduler.getClass()
                    .getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class)
                    .invoke(scheduler, plugin, (Consumer<Object>) (t) -> task.run(), delay, period);
                return new FoliaTaskWrapper(scheduledTask);
            } catch (Exception e) {
                return new BukkitTaskWrapper(Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period));
            }
        } else {
            return new BukkitTaskWrapper(Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period));
        }
    }
    
    public static void runTaskAsync(Plugin plugin, Runnable task) {
        if (IS_FOLIA) {
            try {
                Object scheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                scheduler.getClass()
                    .getMethod("runNow", Plugin.class, Consumer.class)
                    .invoke(scheduler, plugin, (Consumer<Object>) (t) -> task.run());
            } catch (Exception e) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
            }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    public static void runTaskOnPlayer(Plugin plugin, Player player, Runnable task) {
        if (IS_FOLIA) {
            if (player == null || !player.isOnline()) return;
            try {
                player.getScheduler().run(plugin, (scheduledTask) -> task.run(), null);
            } catch (Exception e) {

                Bukkit.getScheduler().runTask(plugin, task);
            }
        } else {
            
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
    
    public static Task runTaskLater(Plugin plugin, Runnable task, long delay) {
        if (IS_FOLIA) {
            try {
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Object scheduledTask = scheduler.getClass()
                    .getMethod("runDelayed", Plugin.class, Consumer.class, long.class)
                    .invoke(scheduler, plugin, (Consumer<Object>) (t) -> task.run(), delay);
                return new FoliaTaskWrapper(scheduledTask);
            } catch (Exception e) {
                return new BukkitTaskWrapper(Bukkit.getScheduler().runTaskLater(plugin, task, delay));
            }
        } else {
            return new BukkitTaskWrapper(Bukkit.getScheduler().runTaskLater(plugin, task, delay));
        }
    }
    
    public static void cancelTask(Task task) {
        if (task != null) {
            task.cancel();
        }
    }
    
    public static boolean isFolia() {
        return IS_FOLIA;
    }
}