package com.cokefenya.rpsign.listener;

import com.cokefenya.rpsign.RPSignPlugin;
import com.cokefenya.rpsign.ResourcepackManager;
import com.cokefenya.rpsign.util.SchedulerAdapter;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RPSignListener implements Listener {
    private final ResourcepackManager manager;
    private final RPSignPlugin plugin;
    private final Map<UUID, Long> lastClickTime = new HashMap<>();
    private final Map<UUID, String> pendingConfirmations = new HashMap<>();

    public RPSignListener(ResourcepackManager manager) {
        this.manager = manager;
        this.plugin = RPSignPlugin.getInstance();
    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        // Проверяем, включены ли таблички
        if (!plugin.getConfig().getBoolean("signs.enabled", true)) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (!(event.getClickedBlock().getState() instanceof Sign)) return;

        String[] lines = ((Sign) event.getClickedBlock().getState()).getLines();
        if (lines.length > 1 && lines[0].equalsIgnoreCase("[RP]")) {
            event.setCancelled(true);
            
            String packName = lines[1];
            ResourcepackManager.PackInfo pack = manager.getPack(packName);
            Player player = event.getPlayer();
            UUID playerId = player.getUniqueId();
            
            if (pack == null) {
                sendMessage(player, "error", "pack-not-found", packName);
                return;
            }

            // Проверяем подтверждение
            int confirmDelay = plugin.getConfig().getInt("signs.confirm-delay", 3);
            long currentTime = System.currentTimeMillis();
            Long lastClick = lastClickTime.get(playerId);
            
            if (lastClick != null && currentTime - lastClick < confirmDelay * 1000) {
                // Подтверждение получено, применяем пак
                applyPack(player, pack);
                pendingConfirmations.remove(playerId);
                lastClickTime.remove(playerId);
            } else {
                // Первый клик, запрашиваем подтверждение
                lastClickTime.put(playerId, currentTime);
                pendingConfirmations.put(playerId, packName);
                
                String confirmMessage = plugin.getConfig().getString("signs.confirm-message", "§aНажмите ещё раз для применения пака");
                player.sendMessage(getPrefix() + confirmMessage);
                
                // Удаляем подтверждение через время
                SchedulerAdapter.runTaskLater(plugin, () -> {
                    if (pendingConfirmations.containsKey(playerId) && 
                        pendingConfirmations.get(playerId).equals(packName)) {
                        pendingConfirmations.remove(playerId);
                        lastClickTime.remove(playerId);
                        player.sendMessage(getPrefix() + getWarningColor() + 
                            (plugin.getConfig().getString("lang", "en").equals("en") ? 
                                "Confirmation expired!" : "Подтверждение истекло!"));
                    }
                }, confirmDelay * 20L);
            }
        }
    }

    private void applyPack(Player player, ResourcepackManager.PackInfo pack) {
        player.setResourcePack(pack.url);
        sendMessage(player, "success", "pack-applied", pack.name);
    }

    private void sendMessage(Player player, String type, String key, String... args) {
        String prefix = getPrefix();
        String color = getColor(type);
        String message = getMessage(key, args);
        player.sendMessage(prefix + color + message);
    }

    private String getPrefix() {
        return plugin.getConfig().getString("messages.prefix", "§b[RP_Sign] §f");
    }

    private String getColor(String type) {
        switch (type) {
            case "success": return plugin.getConfig().getString("messages.success-color", "§a");
            case "error": return plugin.getConfig().getString("messages.error-color", "§c");
            case "warning": return plugin.getConfig().getString("messages.warning-color", "§e");
            default: return plugin.getConfig().getString("messages.info-color", "§b");
        }
    }

    private String getWarningColor() {
        return plugin.getConfig().getString("messages.warning-color", "§e");
    }

    private String getMessage(String key, String... args) {
        String lang = plugin.getConfig().getString("lang", "ru");
        boolean isEnglish = lang.equals("en");
        
        switch (key) {
            case "pack-not-found":
                return isEnglish ? 
                    "Pack '" + args[0] + "' not found!" : 
                    "Пак '" + args[0] + "' не найден!";
            case "pack-applied":
                return isEnglish ? 
                    "Resource pack '" + args[0] + "' applied successfully!" : 
                    "Ресурс-пак '" + args[0] + "' успешно установлен!";
            default:
                return key;
        }
    }
}
