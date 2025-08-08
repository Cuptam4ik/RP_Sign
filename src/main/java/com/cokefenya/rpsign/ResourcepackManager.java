package com.cokefenya.rpsign;

import java.util.*;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;

public class ResourcepackManager {
    private final Map<String, PackInfo> packs = new HashMap<>();
    private final Map<String, List<String>> userPacks = new HashMap<>();

    private File dataFile;
    private YamlConfiguration dataConfig;

    public void init(File dataFolder) {
        dataFile = new File(dataFolder, "packs.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        load();
    }

    public void save() {
        dataConfig.set("packs", null);
        dataConfig.set("userPacks", null);
        for (Map.Entry<String, PackInfo> entry : packs.entrySet()) {
            String key = entry.getKey();
            PackInfo p = entry.getValue();
            dataConfig.set("packs." + key + ".name", p.name);
            dataConfig.set("packs." + key + ".owner", p.owner);
            dataConfig.set("packs." + key + ".url", p.url);
            dataConfig.set("packs." + key + ".description", p.description);
        }
        for (Map.Entry<String, List<String>> entry : userPacks.entrySet()) {
            dataConfig.set("userPacks." + entry.getKey(), entry.getValue());
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        packs.clear();
        userPacks.clear();
        if (dataConfig.contains("packs")) {
            for (String key : dataConfig.getConfigurationSection("packs").getKeys(false)) {
                String name = dataConfig.getString("packs." + key + ".name");
                String owner = dataConfig.getString("packs." + key + ".owner");
                String url = dataConfig.getString("packs." + key + ".url");
                String desc = dataConfig.getString("packs." + key + ".description");
                packs.put(key, new PackInfo(name, owner, url, desc));
            }
        }
        if (dataConfig.contains("userPacks")) {
            for (String key : dataConfig.getConfigurationSection("userPacks").getKeys(false)) {
                List<String> list = dataConfig.getStringList("userPacks." + key);
                userPacks.put(key, list);
            }
        }
    }

    public void createPack(String owner, String name, String url, String description) {
        packs.put(name.toLowerCase(), new PackInfo(name, owner, url, description));
        userPacks.computeIfAbsent(owner.toLowerCase(), k -> new ArrayList<>()).add(name);
        save();
    }

    public PackInfo getPack(String name) {
        return packs.get(name.toLowerCase());
    }

    public void deletePack(String owner, String name) {
        packs.remove(name.toLowerCase());
        List<String> list = userPacks.get(owner.toLowerCase());
        if (list != null) list.remove(name);
        save();
    }

    public List<String> getUserPacks(String owner) {
        return userPacks.getOrDefault(owner.toLowerCase(), Collections.emptyList());
    }

    public Collection<PackInfo> getAllPacks() {
        return packs.values();
    }

    public static class PackInfo {
        public final String name;
        public final String owner;
        public final String url;
        public final String description;
        public PackInfo(String name, String owner, String url, String description) {
            this.name = name;
            this.owner = owner;
            this.url = url;
            this.description = description;
        }
    }
}
