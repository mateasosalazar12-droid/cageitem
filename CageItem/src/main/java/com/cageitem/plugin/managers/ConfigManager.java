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

    // Jaula
    private Material cageMaterial;
    private int cageSize;
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

        cageMaterial = parseMaterial(cfg.getString("cage.material", "IRON_BARS"), Material.IRON_BARS);
        int rawSize = cfg.getInt("cage.size", 5);
        cageSize = Math.max(3, rawSize);
        if (cageSize % 2 == 0) {
            // Necesitamos un tamano impar para poder centrar la jaula exactamente en la victima.
            cageSize++;
        }
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
        msgUsage = cfg.getString("messages.usage", "&cUso: /cageitem give <jugador>");
        msgPlayerNotFound = cfg.getString("messages.player-not-found", "&cJugador no encontrado.");
        msgItemGiven = cfg.getString("messages.item-given", "&aLe diste el objeto especial a {player}.");
        msgItemReceived = cfg.getString("messages.item-received", "&a¡Has recibido el objeto especial!");
        msgConfigReloaded = cfg.getString("messages.config-reloaded", "&aConfiguracion recargada.");
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

    public Material getCageMaterial() {
        return cageMaterial;
    }

    public int getCageSize() {
        return cageSize;
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
}
