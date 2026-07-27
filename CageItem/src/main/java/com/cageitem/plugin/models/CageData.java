package com.cageitem.plugin.models;

import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;

/**
 * Representa una jaula activa en el mundo.
 * <p>
 * Guarda todo lo necesario para poder restaurar exactamente el estado
 * original de cada bloque afectado, sin importar que haya sido un cofre,
 * un horno, bedrock o cualquier otro bloque con datos propios.
 */
public final class CageData {

    private final UUID victim;
    private final UUID attacker;
    private final String worldName;

    private final int minX, maxX;
    private final int minY, maxY;
    private final int minZ, maxZ;

    /**
     * Estado original de cada bloque afectado por la jaula, capturado
     * ANTES de modificarlo. La clave es la ubicacion exacta del bloque.
     */
    private final Map<Location, BlockState> originalStates;

    /**
     * Tarea programada que restaurara la jaula automaticamente.
     * Se guarda para poder cancelarla si la jaula se elimina antes de tiempo
     * (por ejemplo, al descargarse el mundo o al desconectarse la victima).
     */
    private BukkitTask restoreTask;

    public CageData(UUID victim, UUID attacker, String worldName,
                     int minX, int maxX, int minY, int maxY, int minZ, int maxZ,
                     Map<Location, BlockState> originalStates) {
        this.victim = victim;
        this.attacker = attacker;
        this.worldName = worldName;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.originalStates = originalStates;
    }

    public UUID getVictim() {
        return victim;
    }

    public UUID getAttacker() {
        return attacker;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getMinX() {
        return minX;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public Map<Location, BlockState> getOriginalStates() {
        return originalStates;
    }

    public BukkitTask getRestoreTask() {
        return restoreTask;
    }

    public void setRestoreTask(BukkitTask restoreTask) {
        this.restoreTask = restoreTask;
    }

    /**
     * Indica si un bloque en las coordenadas dadas pertenece al "cascaron"
     * exterior de la jaula (pared, techo o piso) en vez de al interior hueco.
     */
    public boolean isBoundary(int x, int y, int z) {
        return x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ;
    }

    /**
     * Indica si una ubicacion cae dentro del cubo que ocupa esta jaula.
     */
    public boolean contains(String world, int x, int y, int z) {
        return this.worldName.equals(world)
                && x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }
}
