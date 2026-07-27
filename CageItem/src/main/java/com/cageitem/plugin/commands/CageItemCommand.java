package com.cageitem.plugin.commands;

import com.cageitem.plugin.managers.ConfigManager;
import com.cageitem.plugin.utils.ItemUtils;
import com.cageitem.plugin.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Implementa el comando principal del plugin:
 * <pre>/cageitem give &lt;jugador&gt;</pre>
 * <pre>/cageitem reload</pre>
 * Ambos subcomandos requieren el permiso {@code cageitem.admin}.
 */
public class CageItemCommand implements CommandExecutor {

    private final Plugin plugin;
    private final ConfigManager configManager;
    private final ItemUtils itemUtils;

    public CageItemCommand(Plugin plugin, ConfigManager configManager, ItemUtils itemUtils) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.itemUtils = itemUtils;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cageitem.admin")) {
            sender.sendMessage(MessageUtils.colorize(configManager.getMsgNoPermission()));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(MessageUtils.colorize(configManager.getMsgUsage()));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give" -> handleGive(sender, args);
            case "reload" -> handleReload(sender);
            default -> sender.sendMessage(MessageUtils.colorize(configManager.getMsgUsage()));
        }

        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtils.colorize(configManager.getMsgUsage()));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(MessageUtils.colorize(configManager.getMsgPlayerNotFound()));
            return;
        }

        try {
            ItemStack item = itemUtils.createCageItem();
            var leftover = target.getInventory().addItem(item);

            // Si el inventario estaba lleno, dejamos el item en el suelo en vez de perderlo.
            if (!leftover.isEmpty()) {
                leftover.values().forEach(stack -> target.getWorld().dropItem(target.getLocation(), stack));
            }

            sender.sendMessage(MessageUtils.format(configManager.getMsgItemGiven(), "{player}", target.getName()));
            if (!sender.equals(target)) {
                target.sendMessage(MessageUtils.colorize(configManager.getMsgItemReceived()));
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Error entregando el objeto especial: " + ex.getMessage());
        }
    }

    private void handleReload(CommandSender sender) {
        try {
            configManager.load();
            sender.sendMessage(MessageUtils.colorize(configManager.getMsgConfigReloaded()));
        } catch (Exception ex) {
            plugin.getLogger().warning("Error recargando config.yml: " + ex.getMessage());
        }
    }
}
