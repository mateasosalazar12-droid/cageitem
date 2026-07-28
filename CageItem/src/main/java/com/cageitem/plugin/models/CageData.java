package com.cageitem.plugin.models;

import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Representa una jaula activa en el mundo.
 * <p>
 * La jaula tiene 3 partes apiladas verticalmente: un piso solido, una
 * seccion de rejas (donde queda encerrada la victima) y un techo solido.
 * El piso y el techo pueden tener un ancho distinto entre si, lo que
 * permite lograr un efecto "hongo" (mas anchos que las rejas).
 * <p>
 * Guarda todo lo necesario para poder restaurar exactamente el estado
 * original de cada bloque afectado, sin importar que haya sido un cofre,
 * un horno, bedrock o cualquier otro bloque con datos propios.
 */
public final class CageData {

    private final UUID victim;
    private final UUID attacker;
    private final String worldName;

    // Limites del piso y del techo (pueden tener distinto ancho entre si).
    private final int baseMinX, baseMaxX, baseMinZ, baseMaxZ;
    private final int topMinX, topMaxX, topMinZ, topMaxZ;

    // Alturas: el piso esta en bottomY, el techo en topY, y la seccion de
    // rejas (hueca por dentro, solo paredes) ocupa desde barsMinY hasta barsMaxY.
    private final int bottomY;
    private final int barsMinY;
    private final int barsMaxY;
    private final int topY;

    /**
     * Estado original de cada bloque afectado por la jaula, capturado
     * ANTES de modificarlo. La clave es la ubicacion exacta del bloque.
     */
    private final Map<Location, BlockState> originalStates;

    /**
     * Ubicaciones de los bloques SOLIDOS que forman la jaula (piso, techo
     * y paredes de rejas) - es decir, todo lo que no es aire. Se usa para
     * saber si un bloque roto pertenece al "cascaron" de la jaula.
     */
    private final Set<Location> shellLocations;

    /**
     * Tarea programada que restaurara la jaula automaticamente.
     * Se guarda para poder cancelarla si la jaula se elimina antes de tiempo
     * (por ejemplo, al descargarse el mundo o al desconectarse la victima).
     */
    private BukkitTask restoreTask;

    public CageData(UUID victim, UUID attacker, String worldName,
                     int baseMinX, int baseMaxX, int baseMinZ, int baseMaxZ,
                     int topMinX, int topMaxX, int topMinZ, int topMaxZ,
                     int bottomY, int barsMinY, int barsMaxY, int topY,
                     Map<Location, BlockState> originalStates, Set<Location> shellLocations) {
        this.victim = victim;
        this.attacker = attacker;
        this.worldName = worldName;
        this.baseMinX = baseMinX;
        this.baseMaxX = baseMaxX;
        this.baseMinZ = baseMinZ;
        this.baseMaxZ = baseMaxZ;
        this.topMinX = topMinX;
        this.topMaxX = topMaxX;
        this.topMinZ = topMinZ;
        this.topMaxZ = topMaxZ;
        this.bottomY = bottomY;
        this.barsMinY = barsMinY;
        this.barsMaxY = barsMaxY;
        this.topY = topY;
        this.originalStates = originalStates;
        this.shellLocations = shellLocations;
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

    public int getBottomY() {
        return bottomY;
    }

    public int getTopY() {
        return topY;
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
     * Indica si una ubicacion cae dentro del espacio hueco (interior)
     * de la seccion de rejas, es decir, donde puede estar parada la victima.
     */
    public boolean isInsideInterior(String world, int x, int y, int z) {
        return this.worldName.equals(world)
                && y >= barsMinY && y <= barsMaxY
                && x > baseMinX && x < baseMaxX
                && z > baseMinZ && z < baseMaxZ;
    }

    /**
     * Indica si la ubicacion dada corresponde a un bloque SOLIDO de la
     * jaula (piso, techo o pared de rejas).
     */
    public boolean isShellBlock(Location location) {
        return shellLocations.contains(location);
    }
}
