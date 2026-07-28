package com.cageitem.plugin.managers;

import com.cageitem.plugin.Main;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.logging.Level;

/**
 * Carga y expone todos los valores definidos en config.yml.
 * <p>
 * Ademas de leer, tambien permite modificar y persistir en disco algunos
 * valores en caliente (material del item, cooldown, usos), usados por los
 * subcomandos de /cageitem para configurar el plugin sin editar el archivo
 * manualmente.
 * <p>
 * Cualquier valor invalido (material, sonido o particula mal escrita)
 * se registra como advertencia en consola y se reemplaza por un valor
 * por defecto seguro, para que un error de tipeo en el config nunca
 * impida que el plugin arranque.
 */
public class ConfigManager {

    private final Main plugin;

    // Item
    private Material itemMaterial;
    private String itemName;
    private List<String> itemLore;
    private int customModelData;
    private int itemMaxUses;

    // Jaula
    private int cageBaseSize;
    private int cageTopSize;
    private int cageBarsHeight;
    private Material cageBaseMaterial;
    private Material cageBarsMaterial;
    private int cageDuration;

    // Cooldown
    private int cooldownSeconds;

    // Sonidos
    private Sound createSound;
    private Sound removeSound;

    // Particulas
    private Particle particleType;
    private int particleCount;

    // Mensajes
    private String msgCooldown;
    private String msgCaged;
    private String msgCageRemoved;
    private String msgNoPermission;
    private String msgUsage;
    private String msgPlayerNotFound;
    private String msgItemGiven;
    private String msgItemReceived;
    private String msgConfigReloaded;
    private String msgItemMaterialSet;
    private String msgItemMaterialNeedHand;
    private String msgCooldownSet;
    private String msgUsesSet;
    private String msgInvalidNumber;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * (Re)carga toda la configuracion desde disco.
     */
    public void load() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        itemMaterial = parseMaterial(cfg.getString("item.material", "DIAMOND_SWORD"), Material.DIAMOND_SWORD);
        itemName = cfg.getString("item.name", "&c&lCage Blade");
        itemLore = cfg.getStringList("item.lore");
        customModelData = cfg.getInt("item.custom-model-data", 1001);
        itemMaxUses = cfg.getInt("item.max-uses", 5);

        cageBaseSize = Math.max(3, cfg.getInt("cage.base-size", 3));
        cageTopSize = Math.max(3, cfg.getInt("cage.top-size", 3));
        cageBarsHeight = Math.max(1, cfg.getInt("cage.bars-height", 3));
        cageBaseMaterial = parseMaterial(cfg.getString("cage.base-material", "STONE"), Material.STONE);
        cageBarsMaterial = parseMaterial(cfg.getString("cage.bars-material", "IRON_BARS"), Material.IRON_BARS);
        cageDuration = Math.max(1, cfg.getInt("cage.duration", 10));

        cooldownSeconds = Math.max(0, cfg.getInt("cooldown.seconds", 30));

        createSound = parseSound(cfg.getString("sounds.cage-create", "ENTITY_ENDER_DRAGON_GROWL"), Sound.ENTITY_ENDER_DRAGON_GROWL);
        removeSound = parseSound(cfg.getString("sounds.cage-remove", "BLOCK_GLASS_BREAK"), Sound.BLOCK_GLASS_BREAK);

        particleType = parseParticle(cfg.getString("particles.type", "PORTAL"), Particle.PORTAL);
        particleCount = Math.max(1, cfg.getInt("particles.count", 60));

        msgCooldown = cfg.getString("messages.cooldown", "&cDebes esperar &e{time}&c segundos.");
        msgCaged = cfg.getString("messages.caged", "&c¡Has quedado atrapado en una jaula!");
        msgCageRemoved = cfg.getString("messages.cage-removed", "&aLa jaula ha desaparecido.");
        msgNoPermission = cfg.getString("messages.no-permission", "&cNo tienes permiso para usar este comando.");
        msgUsage = cfg.getString("messages.usage", "&cUso: /cageitem <give|setitem|setcooldown|setuses|reload> ...");
        msgPlayerNotFound = cfg.getString("messages.player-not-found", "&cJugador no encontrado.");
        msgItemGiven = cfg.getString("messages.item-given", "&aLe diste el objeto especial a {player}.");
        msgItemReceived = cfg.getString("messages.item-received", "&a¡Has recibido el objeto especial!");
        msgConfigReloaded = cfg.getString("messages.config-reloaded", "&aConfiguracion recargada.");
        msgItemMaterialSet = cfg.getString("messages.item-material-set", "&aEl objeto especial ahora usa el material &e{material}&a.");
        msgItemMaterialNeedHand = cfg.getString("messages.item-material-need-hand", "&cDebes tener un item en la mano.");
        msgCooldownSet = cfg.getString("messages.cooldown-set", "&aCooldown actualizado a &e{seconds}&a segundos.");
        msgUsesSet = cfg.getString("messages.uses-set", "&aUsos del objeto actualizados a &e{uses}&a.");
        msgInvalidNumber = cfg.getString("messages.invalid-number", "&cDebes indicar un numero valido.");
    }

    // ---------------------------------------------------------------
    // Setters que persisten el cambio en config.yml y recargan en caliente.
    // Usados por los subcomandos administrativos de /cageitem.
    // ---------------------------------------------------------------

    public void setItemMaterial(Material material) {
        plugin.getConfig().set("item.material", material.name());
        plugin.saveConfig();
        load();
    }

    public void setCooldownSeconds(int seconds) {
        plugin.getConfig().set("cooldown.seconds", Math.max(0, seconds));
        plugin.saveConfig();
        load();
    }

    public void setMaxUses(int uses) {
        plugin.getConfig().set("item.max-uses", Math.max(0, uses));
        plugin.saveConfig();
        load();
    }

    private Material parseMaterial(String raw, Material fallback) {
        try {
            return Material.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().log(Level.WARNING,
                    "Material invalido '" + raw + "' en config.yml, usando " + fallback.name() + " por defecto.");
            return fallback;
        }
    }

    private Sound parseSound(String raw, Sound fallback) {
        try {
            return Sound.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().log(Level.WARNING,
                    "Sonido invalido '" + raw + "' en config.yml, usando " + fallback.name() + " por defecto.");
            return fallback;
        }
    }

    private Particle parseParticle(String raw, Particle fallback) {
        try {
            return Particle.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().log(Level.WARNING,
                    "Particula invalida '" + raw + "' en config.yml, usando " + fallback.name() + " por defecto.");
            return fallback;
        }
    }

    public Material getItemMaterial() {
        return itemMaterial;
    }

    public String getItemName() {
        return itemName;
    }

    public List<String> getItemLore() {
        return itemLore;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public int getItemMaxUses() {
        return itemMaxUses;
    }

    public int getCageBaseSize() {
        return cageBaseSize;
    }

    public int getCageTopSize() {
        return cageTopSize;
    }

    public int getCageBarsHeight() {
        return cageBarsHeight;
    }

    public Material getCageBaseMaterial() {
        return cageBaseMaterial;
    }

    public Material getCageBarsMaterial() {
        return cageBarsMaterial;
    }

    public int getCageDuration() {
        return cageDuration;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public Sound getCreateSound() {
        return createSound;
    }

    public Sound getRemoveSound() {
        return removeSound;
    }

    public Particle getParticleType() {
        return particleType;
    }

    public int getParticleCount() {
        return particleCount;
    }

    public String getMsgCooldown() {
        return msgCooldown;
    }

    public String getMsgCaged() {
        return msgCaged;
    }

    public String getMsgCageRemoved() {
        return msgCageRemoved;
    }

    public String getMsgNoPermission() {
        return msgNoPermission;
    }

    public String getMsgUsage() {
        return msgUsage;
    }

    public String getMsgPlayerNotFound() {
        return msgPlayerNotFound;
    }

    public String getMsgItemGiven() {
        return msgItemGiven;
    }

    public String getMsgItemReceived() {
        return msgItemReceived;
    }

    public String getMsgConfigReloaded() {
        return msgConfigReloaded;
    }

    public String getMsgItemMaterialSet() {
        return msgItemMaterialSet;
    }

    public String getMsgItemMaterialNeedHand() {
        return msgItemMaterialNeedHand;
    }

    public String getMsgCooldownSet() {
        return msgCooldownSet;
    }

    public String getMsgUsesSet() {
        return msgUsesSet;
    }

    public String getMsgInvalidNumber() {
        return msgInvalidNumber;
    }
}
