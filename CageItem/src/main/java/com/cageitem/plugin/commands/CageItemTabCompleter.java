package com.cageitem.plugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Autocompletado para /cageitem: sugiere los subcomandos disponibles y,
 * para "give", los nombres de jugadores conectados.
 */
public class CageItemTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("give", "reload");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(partial)) {
                    suggestions.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            String partial = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    suggestions.add(player.getName());
                }
            }
        }

        return suggestions;
    }
}
