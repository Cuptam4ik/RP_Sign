package com.Captam4ik.rpsign.config;

import com.Captam4ik.rpsign.RPSignPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class SettingsManager {

    private final RPSignPlugin plugin;

    public String prefix;
    public String successColor;
    public String errorColor;
    public String infoColor;
    public String warningColor;
    public String lang;

    public SettingsManager(RPSignPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        // Messages
        prefix = config.getString("messages.prefix", "<#00BFFF>[RP_Sign] <#FFFFFF>");
        successColor = config.getString("messages.success-color", "<#32CD32>");
        errorColor = config.getString("messages.error-color", "<#FF4500>");
        infoColor = config.getString("messages.info-color", "<#1E90FF>");
        warningColor = config.getString("messages.warning-color", "<#FFD700>");
        lang = config.getString("lang", "en");
    }
}