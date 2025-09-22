package com.Captam4ik.rpsign;

import com.Captam4ik.rpsign.command.RPCommand;
import com.Captam4ik.rpsign.config.SettingsManager;
import com.Captam4ik.rpsign.listener.RPSignListener;
import com.Captam4ik.rpsign.util.SchedulerAdapter;
import com.Captam4ik.rpsign.util.SchedulerAdapter.Task;
import org.bukkit.plugin.java.JavaPlugin;

public class RPSignPlugin extends JavaPlugin {
    private static final int AUTO_SAVE_INTERVAL_SECONDS = 300;

    private static RPSignPlugin instance;
    private ResourcepackManager resourcepackManager;
    private SettingsManager settingsManager;
    private Task autoSaveTask;

    @Override
    public void onEnable() {
        try {
            instance = this;
            saveDefaultConfig();

            this.settingsManager = new SettingsManager(this);
            settingsManager.load();

            this.resourcepackManager = new ResourcepackManager(this);
            resourcepackManager.init(getDataFolder());

            RPCommand rpCommand = new RPCommand(this, resourcepackManager);
            getCommand("rp").setExecutor(rpCommand);
            getCommand("rp").setTabCompleter(rpCommand);
            
            getServer().getPluginManager().registerEvents(new RPSignListener(resourcepackManager), this);
            
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
            stopAutoSave();
            
            if (resourcepackManager != null) {
                resourcepackManager.save(false);
                getLogger().info("Data saved successfully!");
            }
            
            getLogger().info("RP_Sign plugin disabled successfully!");
            
        } catch (Exception e) {
            getLogger().severe("Error while disabling RP_Sign plugin: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startAutoSave() {
        if (AUTO_SAVE_INTERVAL_SECONDS > 0) {
            autoSaveTask = SchedulerAdapter.runTaskTimer(this, () -> {
                try {
                    if (resourcepackManager != null) {
                        resourcepackManager.save(true);
                        getLogger().fine("Auto-save task executed.");
                    }
                } catch (Exception e) {
                    getLogger().warning("Auto-save failed: " + e.getMessage());
                }
            }, AUTO_SAVE_INTERVAL_SECONDS * 20L, AUTO_SAVE_INTERVAL_SECONDS * 20L);
            
            String serverType = SchedulerAdapter.isFolia() ? "Folia" : "Paper/Spigot/Purpur";
            getLogger().info("Auto-save enabled with interval: " + AUTO_SAVE_INTERVAL_SECONDS + " seconds (Server: " + serverType + ")");
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
            settingsManager.load();
            
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
    
    public SettingsManager getSettingsManager() {
        return settingsManager;
    }
}