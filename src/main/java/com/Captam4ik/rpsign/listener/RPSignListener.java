package com.Captam4ik.rpsign.listener;

import com.Captam4ik.rpsign.RPSignPlugin;
import com.Captam4ik.rpsign.ResourcepackManager;
import com.Captam4ik.rpsign.config.SettingsManager;
import com.Captam4ik.rpsign.util.MessageUtils;
import com.Captam4ik.rpsign.util.SchedulerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RPSignListener implements Listener {
    private static final boolean SIGNS_ENABLED = true;
    private static final int CONFIRM_DELAY_SECONDS = 3;

    private final ResourcepackManager manager;
    private final RPSignPlugin plugin;
    private final SettingsManager settings;
    private final Map<UUID, Long> lastClickTime = new HashMap<>();
    private final Map<UUID, String> pendingConfirmations = new HashMap<>();

    public RPSignListener(ResourcepackManager manager) {
        this.manager = manager;
        this.plugin = RPSignPlugin.getInstance();
        this.settings = plugin.getSettingsManager();
    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        if (!SIGNS_ENABLED) return;
        
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        if (event.getClickedBlock() == null || !event.getClickedBlock().getType().name().contains("SIGN")) return;

        Sign sign = (Sign) event.getClickedBlock().getState();
        List<Component> componentLines = sign.getSide(Side.FRONT).lines();
        
        String[] lines = new String[componentLines.size()];
        for (int i = 0; i < componentLines.size(); i++) {
            lines[i] = PlainTextComponentSerializer.plainText().serialize(componentLines.get(i));
        }

        if (lines.length > 1 && lines[0].equalsIgnoreCase("[RP]")) {
            event.setCancelled(true);
            
            String packName = lines[1];
            ResourcepackManager.PackInfo pack = manager.getPack(packName);
            Player player = event.getPlayer();
            UUID playerId = player.getUniqueId();
            
            if (pack == null) {
                sendPluginMessage(player, "error", "pack-not-found", packName);
                return;
            }

            long currentTime = System.currentTimeMillis();
            Long lastClick = lastClickTime.get(playerId);
            
            if (lastClick != null && currentTime - lastClick < CONFIRM_DELAY_SECONDS * 1000) {
                applyPack(player, pack);
                pendingConfirmations.remove(playerId);
                lastClickTime.remove(playerId);
            } else {
                lastClickTime.put(playerId, currentTime);
                pendingConfirmations.put(playerId, packName);
                
                String lang = settings.lang;
                String confirmMessageText = lang.equals("ru")
                    ? "<#32CD32>Нажмите ещё раз для применения пака"
                    : "<#32CD32>Click again to apply the pack";
                
                player.sendMessage(MessageUtils.colorize(settings.prefix + confirmMessageText));
                
                SchedulerAdapter.runTaskLater(plugin, () -> {
                    if (pendingConfirmations.containsKey(playerId) && 
                        pendingConfirmations.get(playerId).equals(packName)) {
                        pendingConfirmations.remove(playerId);
                        lastClickTime.remove(playerId);
                        sendPluginMessage(player, "warning", "confirmation-expired");
                    }
                }, CONFIRM_DELAY_SECONDS * 20L);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        lastClickTime.remove(playerId);
        pendingConfirmations.remove(playerId);
    }

    private void applyPack(Player player, ResourcepackManager.PackInfo pack) {
        if (pack.hash != null && !pack.hash.isEmpty()) {
            player.setResourcePack(pack.url, pack.hash);
        } else {
            player.setResourcePack(pack.url, (String) null);
        }
        sendPluginMessage(player, "success", "pack-applied", pack.name);
    }

    private void sendPluginMessage(Player player, String type, String key, String... args) {
        String prefix = settings.prefix;
        String color;
        switch (type) {
            case "success": color = settings.successColor; break;
            case "error": color = settings.errorColor; break;
            case "warning": color = settings.warningColor; break;
            default: color = settings.infoColor; break;
        }

        String lang = settings.lang;
        boolean isEnglish = lang.equals("en");
        String messageText;

        switch (key) {
            case "pack-not-found":
                messageText = isEnglish ? "Pack '" + args[0] + "' not found!" : "Пак '" + args[0] + "' не найден!";
                break;
            case "pack-applied":
                messageText = isEnglish ? "Resource pack '" + args[0] + "' applied successfully!" : "Ресур-пак '" + args[0] + "' успешно установлен!";
                break;
            case "confirmation-expired":
                 messageText = isEnglish ? "Confirmation expired!" : "Подтверждение истекло!";
                 break;
            default:
                messageText = key;
        }
        
        player.sendMessage(MessageUtils.colorize(prefix + color + messageText));
    }
}