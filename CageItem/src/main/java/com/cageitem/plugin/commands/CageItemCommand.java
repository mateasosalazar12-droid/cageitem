package com.cageitem.plugin.commands;

import com.cageitem.plugin.managers.ConfigManager;
import com.cageitem.plugin.utils.ItemUtils;
import com.cageitem.plugin.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Implementa el comando principal del plugin:
 * <pre>/cageitem give &lt;jugador&gt;</pre>
 * <pre>/cageitem setitem</pre>
 * <pre>/cageitem setcooldown &lt;segundos&gt;</pre>
 * <pre>/cageitem setuses &lt;cantidad&gt;</pre>
 * <pre>/cageitem reload</pre>
 * Todos los subcomandos requieren el permiso {@code cageitem.admin}.
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
            case "setitem" -> handleSetItem(sender);
            case "setcooldown" -> handleSetCooldown(sender, args);
            case "setuses" -> handleSetUses(sender, args);
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

    /**
     * Permite elegir el objeto especial directamente desde el juego: toma
     * el material del item que el remitente tiene en la mano principal y
     * lo guarda como el material configurado para la habilidad.
     */
    private void handleSetItem(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtils.colorize(configManager.getMsgItemMaterialNeedHand()));
            return;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType() == Material.AIR) {
            sender.sendMessage(MessageUtils.colorize(configManager.getMsgItemMaterialNeedHand()));
            return;
        }

        try {
            Material material = held.getType();
            configManager.setItemMaterial(material);
            sender.sendMessage(MessageUtils.format(configManager.getMsgItemMaterialSet(), "{material}", material.name()));
        } catch (Exception ex) {
            plugin.getLogger().warning("Error guardando el material del objeto especial: " + ex.getMessage());
        }
    }

    private void handleSetCooldown(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtils.colorize(configManager.getMsgUsage()));
            return;
        }

        try {
            int seconds = Integer.parseInt(args[1]);
            configManager.setCooldownSeconds(seconds);
            sender.sendMessage(MessageUtils.format(configManager.getMsgCooldownSet(), "{seconds}", String.valueOf(seconds)));
        } catch (NumberFormatException ex) {
            sender.sendMessage(MessageUtils.colorize(configManager.getMsgInvalidNumber()));
        } catch (Exception ex) {
            plugin.getLogger().warning("Error guardando el cooldown: " + ex.getMessage());
        }
    }

    private void handleSetUses(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtils.colorize(configManager.getMsgUsage()));
            return;
        }

        try {
            int uses = Integer.parseInt(args[1]);
            configManager.setMaxUses(uses);
            sender.sendMessage(MessageUtils.format(configManager.getMsgUsesSet(), "{uses}", String.valueOf(uses)));
        } catch (NumberFormatException ex) {
            sender.sendMessage(MessageUtils.colorize(configManager.getMsgInvalidNumber()));
        } catch (Exception ex) {
            plugin.getLogger().warning("Error guardando los usos: " + ex.getMessage());
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
