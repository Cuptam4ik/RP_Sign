package com.cokefenya.rpsign.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Адаптер планировщика для совместимости с Paper/Spigot/Purpur и Folia
 */
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
    
    /**
     * Интерфейс для задач, совместимый с обоими типами планировщиков
     */
    public interface Task {
        void cancel();
    }
    
    /**
     * Обертка для BukkitTask
     */
    private static class BukkitTaskWrapper implements Task {
        private final BukkitTask task;
        
        public BukkitTaskWrapper(BukkitTask task) {
            this.task = task;
        }
        
        @Override
        public void cancel() {
            if (task != null) {
                task.cancel();
            }
        }
    }
    
    /**
     * Обертка для Folia ScheduledTask
     */
    private static class FoliaTaskWrapper implements Task {
        private final Object task; // ScheduledTask
        
        public FoliaTaskWrapper(Object task) {
            this.task = task;
        }
        
        @Override
        public void cancel() {
            if (task != null) {
                try {
                    // Используем рефлексию для вызова cancel()
                    task.getClass().getMethod("cancel").invoke(task);
                } catch (Exception e) {
                    // Игнорируем ошибки рефлексии
                }
            }
        }
    }
    
    /**
     * Запланировать повторяющуюся задачу
     */
    public static Task runTaskTimer(Plugin plugin, Runnable task, long delay, long period) {
        if (IS_FOLIA) {
            try {
                // Используем рефлексию для вызова Folia API
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Object scheduledTask = scheduler.getClass()
                    .getMethod("runAtFixedRate", Plugin.class, java.util.function.Consumer.class, long.class, long.class)
                    .invoke(scheduler, plugin, (java.util.function.Consumer<Object>) (t) -> task.run(), delay, period);
                return new FoliaTaskWrapper(scheduledTask);
            } catch (Exception e) {
                // Fallback на стандартный планировщик
                return new BukkitTaskWrapper(Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period));
            }
        } else {
            // Для Paper/Spigot/Purpur используем стандартный BukkitScheduler
            return new BukkitTaskWrapper(Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period));
        }
    }
    
    /**
     * Запланировать задачу с задержкой
     */
    public static Task runTaskLater(Plugin plugin, Runnable task, long delay) {
        if (IS_FOLIA) {
            try {
                // Используем рефлексию для вызова Folia API
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Object scheduledTask = scheduler.getClass()
                    .getMethod("runDelayed", Plugin.class, java.util.function.Consumer.class, long.class)
                    .invoke(scheduler, plugin, (java.util.function.Consumer<Object>) (t) -> task.run(), delay);
                return new FoliaTaskWrapper(scheduledTask);
            } catch (Exception e) {
                // Fallback на стандартный планировщик
                return new BukkitTaskWrapper(Bukkit.getScheduler().runTaskLater(plugin, task, delay));
            }
        } else {
            // Для Paper/Spigot/Purpur используем стандартный BukkitScheduler
            return new BukkitTaskWrapper(Bukkit.getScheduler().runTaskLater(plugin, task, delay));
        }
    }
    
    /**
     * Отменить задачу
     */
    public static void cancelTask(Task task) {
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * Проверить, работает ли плагин на Folia
     */
    public static boolean isFolia() {
        return IS_FOLIA;
    }
}