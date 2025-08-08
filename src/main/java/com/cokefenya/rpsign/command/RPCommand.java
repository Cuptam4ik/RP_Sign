package com.cokefenya.rpsign.command;

import com.cokefenya.rpsign.RPSignPlugin;
import com.cokefenya.rpsign.ResourcepackManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class RPCommand implements CommandExecutor, TabCompleter {
    private final RPSignPlugin plugin;
    private final ResourcepackManager manager;
    private final String blue = ChatColor.AQUA.toString();
    private final String blue2 = ChatColor.BLUE.toString();
    private final String gray = ChatColor.GRAY.toString();
    private final String green = ChatColor.GREEN.toString();
    private final String red = ChatColor.RED.toString();
    private final String yellow = ChatColor.YELLOW.toString();

    public RPCommand(RPSignPlugin plugin, ResourcepackManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private String lang() {
        return plugin.getConfig().getString("lang", "en").toLowerCase();
    }

    private String getPrefix() {
        return plugin.getConfig().getString("messages.prefix", "§b[RP_Sign] §f");
    }

    private String getSuccessColor() {
        return plugin.getConfig().getString("messages.success-color", "§a");
    }

    private String getErrorColor() {
        return plugin.getConfig().getString("messages.error-color", "§c");
    }

    private String getInfoColor() {
        return plugin.getConfig().getString("messages.info-color", "§b");
    }

    private String getWarningColor() {
        return plugin.getConfig().getString("messages.warning-color", "§e");
    }

    private String msg(String key) {
        String l = lang();
        String prefix = getPrefix();
        String success = getSuccessColor();
        String error = getErrorColor();
        String info = getInfoColor();
        String warning = getWarningColor();

        switch (key) {
            case "only-player": 
                return prefix + error + (l.equals("en") ? "Only players can use this command!" : "Только игрок может использовать эту команду!");
            case "created": 
                return prefix + success + (l.equals("en") ? "Pack created successfully!" : "Пак успешно создан!");
            case "usage-create": 
                return prefix + info + (l.equals("en") ? "Usage: /rp create <name> <url>" : "Использование: /rp create <name> <url>");
            case "not-found": 
                return prefix + error + (l.equals("en") ? "Pack not found!" : "Пак не найден!");
            case "usage-delete": 
                return prefix + info + (l.equals("en") ? "Usage: /rp delete <name>" : "Использование: /rp delete <name>");
            case "deleted": 
                return prefix + success + (l.equals("en") ? "Pack deleted successfully!" : "Пак успешно удалён!");
            case "no-packs": 
                return prefix + warning + (l.equals("en") ? "You have no packs." : "У вас нет паков.");
            case "your-packs": 
                return prefix + info + (l.equals("en") ? "Your packs: " : "Ваши паки: ");
            case "cleared": 
                return prefix + success + (l.equals("en") ? "Resource pack cleared!" : "Ресурс-пак сброшен!");
            case "reloaded": 
                return prefix + success + (l.equals("en") ? "Config reloaded successfully!" : "Конфиг успешно перезагружен!");
            case "invalid-url": 
                return prefix + error + (l.equals("en") ? "Invalid URL format!" : "Неверный формат URL!");
            case "name-too-long": 
                return prefix + error + (l.equals("en") ? "Pack name is too long!" : "Название пака слишком длинное!");

            case "too-many-packs": 
                return prefix + error + (l.equals("en") ? "You have too many packs!" : "У вас слишком много паков!");
            case "pack-exists": 
                return prefix + error + (l.equals("en") ? "Pack with this name already exists!" : "Пак с таким названием уже существует!");
            case "not-owner": 
                return prefix + error + (l.equals("en") ? "You don't own this pack!" : "Вы не владеете этим паком!");
            case "help-title": 
                return prefix + blue2 + (l.equals("en") ? "==== RP_Sign Commands ====" : "==== RP_Sign Команды ====");
            case "help-create": 
                return prefix + blue + "/rp create <name> <url>" + gray + (l.equals("en") ? " — create pack" : " — создать пак");
            case "help-delete": 
                return prefix + blue + "/rp delete <name>" + gray + (l.equals("en") ? " — delete pack" : " — удалить пак");
            case "help-list": 
                return prefix + blue + "/rp list" + gray + (l.equals("en") ? " — your packs" : " — список паков");
            case "help-help": 
                return prefix + blue + "/rp help" + gray + (l.equals("en") ? " — help" : " — справка");
            case "help-clear": 
                return prefix + blue + "/rp clear" + gray + (l.equals("en") ? " — clear pack" : " — сбросить пак");
            case "help-reload": 
                return prefix + blue + "/rp reload" + gray + (l.equals("en") ? " — reload config" : " — перезагрузить конфиг");
            case "help-lang": 
                return prefix + blue + "/rp lang <ru|en>" + gray + (l.equals("en") ? " — change language" : " — сменить язык");
            case "usage-lang": 
                return prefix + info + (l.equals("en") ? "Usage: /rp lang <ru|en>" : "Использование: /rp lang <ru|en>");
            case "lang-changed": 
                return prefix + success + (l.equals("en") ? "Language changed successfully!" : "Язык успешно изменён!");
            default: 
                return prefix + info + key;
        }
    }

    private boolean isValidUrl(String url) {
        if (!plugin.getConfig().getBoolean("security.validate-urls", true)) {
            return true;
        }
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private boolean isValidPackName(String name) {
        int maxLength = plugin.getConfig().getInt("security.max-pack-name-length", 32);
        return name.length() <= maxLength && name.matches("^[a-zA-Z0-9_\\-]+$");
    }



    private boolean canCreateMorePacks(String player) {
        int maxPacks = plugin.getConfig().getInt("security.max-packs-per-player", 10);
        List<String> playerPacks = manager.getUserPacks(player);
        return playerPacks.size() < maxPacks;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase();
        
        if (sub.equals("create")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(msg("only-player"));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(msg("usage-create"));
                return true;
            }
            
            String name = args[1];
            String url = args[2];
            String desc = "";
            
            // Валидация
            if (!isValidPackName(name)) {
                sender.sendMessage(msg("name-too-long"));
                return true;
            }
            
            if (!isValidUrl(url)) {
                sender.sendMessage(msg("invalid-url"));
                return true;
            }
            
            if (!canCreateMorePacks(sender.getName())) {
                sender.sendMessage(msg("too-many-packs"));
                return true;
            }
            
            if (manager.getPack(name) != null) {
                sender.sendMessage(msg("pack-exists"));
                return true;
            }
            
            manager.createPack(sender.getName(), name, url, desc);
            sender.sendMessage(msg("created"));
            
        } else if (sub.equals("delete")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(msg("only-player"));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(msg("usage-delete"));
                return true;
            }
            
            ResourcepackManager.PackInfo pack = manager.getPack(args[1]);
            if (pack == null) {
                sender.sendMessage(msg("not-found"));
                return true;
            }
            
            if (!pack.owner.equals(sender.getName())) {
                sender.sendMessage(msg("not-owner"));
                return true;
            }
            
            manager.deletePack(sender.getName(), args[1]);
            sender.sendMessage(msg("deleted"));
            
        } else if (sub.equals("list")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(msg("only-player"));
                return true;
            }
            List<String> packs = manager.getUserPacks(sender.getName());
            if (packs.isEmpty()) {
                sender.sendMessage(msg("no-packs"));
            } else {
                sender.sendMessage(msg("your-packs") + gray + String.join(", ", packs));
            }
            
        } else if (sub.equals("help")) {
            sendHelp(sender);
            
        } else if (sub.equals("clear")) {
            if (sender instanceof Player) {
                ((Player)sender).setResourcePack("");
                sender.sendMessage(msg("cleared"));
            }
            
        } else if (sub.equals("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(msg("reloaded"));
            
        } else if (sub.equals("lang")) {
            if (args.length < 2 || !(args[1].equalsIgnoreCase("ru") || args[1].equalsIgnoreCase("en"))) {
                sender.sendMessage(msg("usage-lang"));
                return true;
            }
            plugin.getConfig().set("lang", args[1].toLowerCase());
            plugin.saveConfig();
            plugin.reloadConfig(); // Перезагружаем конфиг для применения изменений
            sender.sendMessage(msg("lang-changed"));
            
        } else {
            sendHelp(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> subcommands = Arrays.asList("create", "delete", "list", "help", "clear", "reload", "lang");
        if (args.length == 1) {
            List<String> result = new ArrayList<>();
            for (String sub : subcommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    result.add(sub);
                }
            }
            return result;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("lang")) {
            return Arrays.asList("ru", "en");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            List<String> packs = manager.getUserPacks(sender.getName());
            List<String> result = new ArrayList<>();
            for (String pack : packs) {
                if (pack.toLowerCase().startsWith(args[1].toLowerCase())) {
                    result.add(pack);
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(msg("help-title"));
        sender.sendMessage(msg("help-create"));
        sender.sendMessage(msg("help-delete"));
        sender.sendMessage(msg("help-list"));
        sender.sendMessage(msg("help-help"));
        sender.sendMessage(msg("help-clear"));
        sender.sendMessage(msg("help-reload"));
        sender.sendMessage(msg("help-lang"));
    }
}
