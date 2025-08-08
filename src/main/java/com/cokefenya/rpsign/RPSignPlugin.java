package com.cokefenya.rpsign;

import org.bukkit.plugin.java.JavaPlugin;
import com.cokefenya.rpsign.command.RPCommand;
import com.cokefenya.rpsign.listener.RPSignListener;
import com.cokefenya.rpsign.util.SchedulerAdapter;
import com.cokefenya.rpsign.util.SchedulerAdapter.Task;

public class RPSignPlugin extends JavaPlugin {
    private static RPSignPlugin instance;
    private ResourcepackManager resourcepackManager;
    private Task autoSaveTask;

    @Override
    public void onEnable() {
        try {
            instance = this;
            saveDefaultConfig();
            
            // Инициализация менеджера ресурс-паков
            this.resourcepackManager = new ResourcepackManager();
            resourcepackManager.init(getDataFolder());
            
            // Регистрация команд
            RPCommand rpCommand = new RPCommand(this, resourcepackManager);
            getCommand("rp").setExecutor(rpCommand);
            getCommand("rp").setTabCompleter(rpCommand);
            
            // Регистрация слушателей
            getServer().getPluginManager().registerEvents(new RPSignListener(resourcepackManager), this);
            
            // Запуск автосохранения
            startAutoSave();
            
            String serverType = SchedulerAdapter.isFolia() ? "Folia" : "Paper/Spigot/Purpur";
            getLogger().info("RP_Sign plugin enabled successfully on " + serverType + "!");
            
        } catch (Exception e) {
            getLogger().severe("Failed to enable RP_Sign plugin: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            // Остановка автосохранения
            stopAutoSave();
            
            // Сохранение данных
            if (resourcepackManager != null) {
                resourcepackManager.save();
                getLogger().info("Data saved successfully!");
            }
            
            getLogger().info("RP_Sign plugin disabled successfully!");
            
        } catch (Exception e) {
            getLogger().severe("Error while disabling RP_Sign plugin: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startAutoSave() {
        int interval = getConfig().getInt("performance.auto-save-interval", 300);
        if (interval > 0) {
            autoSaveTask = SchedulerAdapter.runTaskTimer(this, () -> {
                try {
                    if (resourcepackManager != null) {
                        resourcepackManager.save();
                        getLogger().fine("Auto-save completed");
                    }
                } catch (Exception e) {
                    getLogger().warning("Auto-save failed: " + e.getMessage());
                }
            }, interval * 20L, interval * 20L);
            
            String serverType = SchedulerAdapter.isFolia() ? "Folia" : "Paper/Spigot/Purpur";
            getLogger().info("Auto-save enabled with interval: " + interval + " seconds (Server: " + serverType + ")");
        }
    }

    private void stopAutoSave() {
        if (autoSaveTask != null) {
            SchedulerAdapter.cancelTask(autoSaveTask);
            autoSaveTask = null;
            getLogger().info("Auto-save disabled");
        }
    }

    public void reloadPlugin() {
        try {
            // Перезагрузка конфига
            reloadConfig();
            
            // Перезапуск автосохранения
            stopAutoSave();
            startAutoSave();
            
            getLogger().info("Plugin reloaded successfully!");
            
        } catch (Exception e) {
            getLogger().severe("Failed to reload plugin: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static RPSignPlugin getInstance() {
        return instance;
    }

    public ResourcepackManager getResourcepackManager() {
        return resourcepackManager;
    }
}
