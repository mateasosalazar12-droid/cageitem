package com.cageitem.plugin.managers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona el cooldown de la habilidad de forma individual por jugador.
 * <p>
 * Se usa un {@link ConcurrentHashMap} porque los eventos de Bukkit pueden
 * dispararse desde distintos contextos y esta estructura es segura para
 * lecturas/escrituras concurrentes sin necesidad de sincronizar manualmente.
 */
public class CooldownManager {

    private final ConfigManager configManager;
    private final Map<UUID, Long> lastUse = new ConcurrentHashMap<>();

    public CooldownManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    /**
     * @return true si el jugador todavia debe esperar antes de volver a usar la habilidad.
     */
    public boolean isOnCooldown(UUID uuid) {
        Long last = lastUse.get(uuid);
        if (last == null) {
            return false;
        }
        long elapsedSeconds = (System.currentTimeMillis() - last) / 1000L;
        return elapsedSeconds < configManager.getCooldownSeconds();
    }

    /**
     * @return segundos restantes de cooldown (0 si ya esta disponible).
     */
    public long getRemainingSeconds(UUID uuid) {
        Long last = lastUse.get(uuid);
        if (last == null) {
            return 0L;
        }
        long elapsedSeconds = (System.currentTimeMillis() - last) / 1000L;
        long remaining = configManager.getCooldownSeconds() - elapsedSeconds;
        return Math.max(0L, remaining);
    }

    /**
     * Marca el instante actual como el ultimo uso de la habilidad para este jugador.
     */
    public void setCooldown(UUID uuid) {
        lastUse.put(uuid, System.currentTimeMillis());
    }

    /**
     * Elimina cualquier cooldown registrado para el jugador (por ejemplo, al salir del servidor).
     */
    public void clear(UUID uuid) {
        lastUse.remove(uuid);
    }
}
