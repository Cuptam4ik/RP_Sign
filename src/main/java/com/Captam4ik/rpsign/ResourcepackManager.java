package com.Captam4ik.rpsign;

import com.Captam4ik.rpsign.util.MessageUtils;
import com.Captam4ik.rpsign.util.SchedulerAdapter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResourcepackManager {
    private final RPSignPlugin plugin;
    private final Map<String, PackInfo> packs = new ConcurrentHashMap<>();
    private final Map<String, List<String>> userPacks = new ConcurrentHashMap<>();
    private File dataFile;
    private volatile boolean isDirty = false;

    public ResourcepackManager(RPSignPlugin plugin) {
        this.plugin = plugin;
    }

    public void init(File dataFolder) {
        dataFile = new File(dataFolder, "packs.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        load();
    }

    public void save(boolean async) {
        if (!isDirty) {
            return;
        }

        YamlConfiguration dataConfig = new YamlConfiguration();
        for (Map.Entry<String, PackInfo> entry : packs.entrySet()) {
            String key = "packs." + entry.getKey();
            PackInfo p = entry.getValue();
            dataConfig.set(key + ".name", p.name);
            dataConfig.set(key + ".owner", p.owner);
            dataConfig.set(key + ".url", p.url);
            dataConfig.set(key + ".description", p.description);
            dataConfig.set(key + ".hash", p.hash);
        }
        for (Map.Entry<String, List<String>> entry : userPacks.entrySet()) {
            dataConfig.set("userPacks." + entry.getKey(), entry.getValue());
        }

        this.isDirty = false;

        Runnable saveTask = () -> {
            try {
                dataConfig.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save packs.yml!");
                e.printStackTrace();
                this.isDirty = true;
            }
        };

        if (async) {
            SchedulerAdapter.runTaskAsync(plugin, saveTask);
        } else {
            saveTask.run();
        }
    }

    public void load() {
        YamlConfiguration dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        packs.clear();
        userPacks.clear();
        if (dataConfig.contains("packs")) {
            for (String key : dataConfig.getConfigurationSection("packs").getKeys(false)) {
                String name = dataConfig.getString("packs." + key + ".name");
                String owner = dataConfig.getString("packs." + key + ".owner");
                String url = dataConfig.getString("packs." + key + ".url");
                String desc = dataConfig.getString("packs." + key + ".description");
                String hash = dataConfig.getString("packs." + key + ".hash", null);
                packs.put(key, new PackInfo(name, owner, url, desc, hash));
            }
        }
        if (dataConfig.contains("userPacks")) {
            for (String key : dataConfig.getConfigurationSection("userPacks").getKeys(false)) {
                List<String> list = dataConfig.getStringList("userPacks." + key);
                userPacks.put(key, new ArrayList<>(list));
            }
        }
        this.isDirty = false;
    }

    public void createPack(String owner, String name, String url, String description, String hash) {
        packs.put(name.toLowerCase(), new PackInfo(name, owner, url, description, hash));
        userPacks.computeIfAbsent(owner.toLowerCase(), k -> new ArrayList<>()).add(name);
        this.isDirty = true;
    }

    public void validateAndCreatePack(Player player, String name, String url, String description) {
        SchedulerAdapter.runTaskAsync(plugin, () -> {
            try {
                
                HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
                connection.setRequestProperty("User-Agent", "RP_Sign_Plugin/2.0");
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(15000);

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    SchedulerAdapter.runTaskOnPlayer(plugin, player, () -> 
                        player.sendMessage(MessageUtils.colorize(plugin.getSettingsManager().prefix + plugin.getSettingsManager().errorColor + "Ошибка: Сервер вернул код " + responseCode + ". Убедитесь, что ссылка верна.")));
                    return;
                }

                String contentType = connection.getContentType();
                if (contentType == null || (!contentType.equals("application/zip") && !contentType.equals("application/octet-stream"))) {
                    SchedulerAdapter.runTaskOnPlayer(plugin, player, () -> 
                        player.sendMessage(MessageUtils.colorize(plugin.getSettingsManager().prefix + plugin.getSettingsManager().errorColor + "Ошибка: Ссылка должна вести напрямую на .zip файл, а не на веб-страницу.")));
                    return;
                }

                MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
                try (InputStream is = connection.getInputStream(); DigestInputStream dis = new DigestInputStream(is, sha1)) {
                    byte[] buffer = new byte[8192];
                    while (dis.read(buffer) != -1) {
                    }
                }
                byte[] hashBytes = sha1.digest();
                String hashString = bytesToHex(hashBytes);

                SchedulerAdapter.runTaskOnPlayer(plugin, player, () -> {
                    createPack(player.getName(), name, url, description, hashString);
                    save(true);
                    
                    String lang = plugin.getSettingsManager().lang;
                    String successMessage = lang.equals("ru") 
                        ? "Ресурс-пак '" + name + "' успешно создан!" 
                        : "Resource pack '" + name + "' successfully created!";

                    player.sendMessage(MessageUtils.colorize(plugin.getSettingsManager().prefix + plugin.getSettingsManager().successColor + successMessage));
                });

            } catch (Exception e) {
                SchedulerAdapter.runTaskOnPlayer(plugin, player, () -> 
                    player.sendMessage(MessageUtils.colorize(plugin.getSettingsManager().prefix + plugin.getSettingsManager().errorColor + "Произошла ошибка при проверке ссылки: " + e.getMessage())));
                e.printStackTrace();
            }
        });
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public PackInfo getPack(String name) {
        return packs.get(name.toLowerCase());
    }

    public void deletePack(String owner, String name) {
        packs.remove(name.toLowerCase());
        List<String> list = userPacks.get(owner.toLowerCase());
        if (list != null) {
            list.remove(name);
            if (list.isEmpty()) {
                userPacks.remove(owner.toLowerCase());
            }
        }
        this.isDirty = true;
    }

    public List<String> getUserPacks(String owner) {
        return userPacks.getOrDefault(owner.toLowerCase(), Collections.emptyList());
    }

    public Collection<PackInfo> getAllPacks() {
        return packs.values();
    }

    public static class PackInfo {
        public final String name, owner, url, description, hash;
        public PackInfo(String name, String owner, String url, String description, String hash) {
            this.name = name; this.owner = owner; this.url = url; this.description = description; this.hash = hash;
        }
    }
}