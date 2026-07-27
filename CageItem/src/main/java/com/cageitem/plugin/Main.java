package com.cageitem.plugin;

import com.cageitem.plugin.commands.CageItemCommand;
import com.cageitem.plugin.commands.CageItemTabCompleter;
import com.cageitem.plugin.listeners.CageProtectionListener;
import com.cageitem.plugin.listeners.PlayerConnectionListener;
import com.cageitem.plugin.listeners.PlayerHitListener;
import com.cageitem.plugin.listeners.WorldUnloadListener;
import com.cageitem.plugin.managers.CageManager;
import com.cageitem.plugin.managers.ConfigManager;
import com.cageitem.plugin.managers.CooldownManager;
import com.cageitem.plugin.utils.ItemUtils;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Punto de entrada del plugin CageItem.
 * <p>
 * Responsabilidad unica: inicializar los managers, registrar listeners y
 * comandos, y garantizar una limpieza segura de todas las jaulas activas
 * al deshabilitarse (recarga del servidor, /reload, apagado, etc.).
 */
public final class Main extends JavaPlugin {

    private ConfigManager configManager;
    private CooldownManager cooldownManager;
    private CageManager cageManager;
    private ItemUtils itemUtils;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        try {
            configManager.load();
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Error cargando config.yml, se usaran valores por defecto.", ex);
        }

        this.itemUtils = new ItemUtils(this, configManager);
        this.cooldownManager = new CooldownManager(configManager);
        this.cageManager = new CageManager(this, configManager);

        registerListeners();
        registerCommands();

        getLogger().info("CageItem habilitado correctamente.");
    }

    @Override
    public void onDisable() {
        // Restauramos todas las jaulas activas para no dejar el mundo con
        // bloques modificados ni tareas programadas colgadas (memory leaks)
        // cuando el plugin se recarga o el servidor se apaga.
        if (cageManager != null) {
            cageManager.forceRemoveAll();
        }
        getLogger().info("CageItem deshabilitado. Todas las jaulas activas fueron restauradas.");
    }

    private void registerListeners() {
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new PlayerHitListener(this, configManager, cooldownManager, cageManager, itemUtils), this);
        pluginManager.registerEvents(new CageProtectionListener(cageManager), this);
        pluginManager.registerEvents(new PlayerConnectionListener(cageManager), this);
        pluginManager.registerEvents(new WorldUnloadListener(cageManager), this);
    }

    private void registerCommands() {
        PluginCommand command = getCommand("cageitem");
        if (command == null) {
            getLogger().severe("No se pudo registrar el comando 'cageitem'. Revisa plugin.yml.");
            return;
        }
        CageItemCommand executor = new CageItemCommand(this, configManager, itemUtils);
        command.setExecutor(executor);
        command.setTabCompleter(new CageItemTabCompleter());
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public CageManager getCageManager() {
        return cageManager;
    }

    public ItemUtils getItemUtils() {
        return itemUtils;
    }
}
