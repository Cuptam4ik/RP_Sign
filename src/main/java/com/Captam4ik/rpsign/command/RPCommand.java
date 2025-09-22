package com.Captam4ik.rpsign.command;

import com.Captam4ik.rpsign.RPSignPlugin;
import com.Captam4ik.rpsign.ResourcepackManager;
import com.Captam4ik.rpsign.config.SettingsManager;
import com.Captam4ik.rpsign.util.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RPCommand implements CommandExecutor, TabCompleter {
    private static final int MAX_PACK_NAME_LENGTH = 32;
    private static final int MAX_PACKS_PER_PLAYER = 10;
    private static final boolean VALIDATE_URLS = true;

    private final RPSignPlugin plugin;
    private final ResourcepackManager manager;
    private final SettingsManager settings;

    public RPCommand(RPSignPlugin plugin, ResourcepackManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.settings = plugin.getSettingsManager();
    }

    private String msg(String key) {
        String l = settings.lang;
        String prefix = settings.prefix;
        String success = settings.successColor;
        String error = settings.errorColor;
        String info = settings.infoColor;
        String warning = settings.warningColor;
        String cmdColor = "<#1E90FF>";
        String descColor = "<#AAAAAA>";

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
                return prefix + info + (l.equals("en") ? "Your resource packs:" : "Ваши ресурс-паки:");
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
            case "pack-checking":
                return prefix + info + (l.equals("en") ? "Checking URL... Please wait." : "Проверяем ссылку... Пожалуйста, подождите.");
            case "delete-hover":
                return info + (l.equals("en") ? "Click to delete this pack" : "Нажмите, чтобы удалить этот пак");
            case "help-title": 
                String titleText = l.equals("en") ? "RP_Sign Commands" : "RP_Sign Команды";
                return descColor + "&m        &r<#FFFFFF>[ " + info + titleText + " <#FFFFFF>]" + descColor + "&m         ";
            case "help-create": 
                return " " + cmdColor + "/rp create <name> <url>" + descColor + (l.equals("en") ? " — create pack" : " — создать пак");
            case "help-delete": 
                return " " + cmdColor + "/rp delete <name>" + descColor + (l.equals("en") ? " — delete pack" : " — удалить пак");
            case "help-list": 
                return " " + cmdColor + "/rp list" + descColor + (l.equals("en") ? " — your packs" : " — список паков");
            case "help-help": 
                return " " + cmdColor + "/rp help" + descColor + (l.equals("en") ? " — help" : " — справка");
            case "help-clear": 
                return " " + cmdColor + "/rp clear" + descColor + (l.equals("en") ? " — clear pack" : " — сбросить пак");
            case "help-reload": 
                return " " + cmdColor + "/rp reload" + descColor + (l.equals("en") ? " — reload config" : " — перезагрузить конфиг");
            case "help-lang": 
                return " " + cmdColor + "/rp lang <ru|en>" + descColor + (l.equals("en") ? " — change language" : " — сменить язык");
            case "usage-lang": 
                return prefix + info + (l.equals("en") ? "Usage: /rp lang <ru|en>" : "Использование: /rp lang <ru|en>");
            case "lang-changed": 
                return prefix + success + (l.equals("en") ? "Language changed successfully!" : "Язык успешно изменён!");
            default: 
                return prefix + info + key;
        }
    }

    private boolean isValidUrl(String url) {
        if (!VALIDATE_URLS) {
            return true;
        }
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private boolean isValidPackName(String name) {
        return name.length() <= MAX_PACK_NAME_LENGTH && name.matches("^[a-zA-Z0-9_\\-]+$");
    }

    private boolean canCreateMorePacks(String player) {
        List<String> playerPacks = manager.getUserPacks(player);
        return playerPacks.size() < MAX_PACKS_PER_PLAYER;
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
                sender.sendMessage(MessageUtils.colorize(msg("only-player")));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(MessageUtils.colorize(msg("usage-create")));
                return true;
            }
            
            String name = args[1];
            String url = args[2];
            String desc = "";
            
            if (!isValidPackName(name)) {
                sender.sendMessage(MessageUtils.colorize(msg("name-too-long")));
                return true;
            }
            
            if (!isValidUrl(url)) {
                sender.sendMessage(MessageUtils.colorize(msg("invalid-url")));
                return true;
            }
            
            if (!canCreateMorePacks(sender.getName())) {
                sender.sendMessage(MessageUtils.colorize(msg("too-many-packs")));
                return true;
            }
            
            if (manager.getPack(name) != null) {
                sender.sendMessage(MessageUtils.colorize(msg("pack-exists")));
                return true;
            }
            
            sender.sendMessage(MessageUtils.colorize(msg("pack-checking")));
            manager.validateAndCreatePack((Player) sender, name, url, desc);
            
        } else if (sub.equals("delete")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(MessageUtils.colorize(msg("only-player")));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(MessageUtils.colorize(msg("usage-delete")));
                return true;
            }
            
            ResourcepackManager.PackInfo pack = manager.getPack(args[1]);
            if (pack == null) {
                sender.sendMessage(MessageUtils.colorize(msg("not-found")));
                return true;
            }
            
            if (!pack.owner.equalsIgnoreCase(sender.getName())) {
                sender.sendMessage(MessageUtils.colorize(msg("not-owner")));
                return true;
            }
            
            manager.deletePack(sender.getName(), args[1]);
            manager.save(true);
            sender.sendMessage(MessageUtils.colorize(msg("deleted")));
            
        } else if (sub.equals("list")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(MessageUtils.colorize(msg("only-player")));
                return true;
            }
            Player player = (Player) sender;
            List<String> packNames = manager.getUserPacks(player.getName());

            if (packNames.isEmpty()) {
                player.sendMessage(MessageUtils.colorize(msg("no-packs")));
                return true;
            }

            player.sendMessage(MessageUtils.colorize(msg("your-packs")));

            for (String packName : packNames) {
                ResourcepackManager.PackInfo pack = manager.getPack(packName);
                if (pack == null) continue;

                Component packComponent = LegacyComponentSerializer.legacySection().deserialize(
                    MessageUtils.colorize(settings.infoColor + "• " + pack.name)
                );
                
                Component deleteComponent = LegacyComponentSerializer.legacySection().deserialize(
                    MessageUtils.colorize(settings.errorColor + " [" + (settings.lang.equals("ru") ? "Удалить" : "Delete") + "]")
                );

                deleteComponent = deleteComponent
                    .clickEvent(ClickEvent.suggestCommand("/rp delete " + pack.name))
                    .hoverEvent(HoverEvent.showText(
                        LegacyComponentSerializer.legacySection().deserialize(MessageUtils.colorize(msg("delete-hover")))
                    ));

                player.sendMessage(packComponent.append(Component.space()).append(deleteComponent));
            }
            return true;
            
        } else if (sub.equals("help")) {
            sendHelp(sender);
            
        } else if (sub.equals("clear")) {
            if (sender instanceof Player) {
                ((Player)sender).setResourcePack("");
                sender.sendMessage(MessageUtils.colorize(msg("cleared")));
            }
            
        } else if (sub.equals("reload")) {
            plugin.reloadPlugin();
            sender.sendMessage(MessageUtils.colorize(msg("reloaded")));
            
        } else if (sub.equals("lang")) {
            if (args.length < 2 || !(args[1].equalsIgnoreCase("ru") || args[1].equalsIgnoreCase("en"))) {
                sender.sendMessage(MessageUtils.colorize(msg("usage-lang")));
                return true;
            }
            plugin.getConfig().set("lang", args[1].toLowerCase());
            plugin.saveConfig();
            plugin.reloadPlugin();
            sender.sendMessage(MessageUtils.colorize(msg("lang-changed")));
            
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
        sender.sendMessage(MessageUtils.colorize(msg("help-title")));
        sender.sendMessage(MessageUtils.colorize(msg("help-create")));
        sender.sendMessage(MessageUtils.colorize(msg("help-delete")));
        sender.sendMessage(MessageUtils.colorize(msg("help-list")));
        sender.sendMessage(MessageUtils.colorize(msg("help-help")));
        sender.sendMessage(MessageUtils.colorize(msg("help-clear")));
        sender.sendMessage(MessageUtils.colorize(msg("help-reload")));
        sender.sendMessage(MessageUtils.colorize(msg("help-lang")));
    }
}