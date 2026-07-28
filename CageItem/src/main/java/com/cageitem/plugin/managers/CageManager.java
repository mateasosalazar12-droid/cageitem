package com.cageitem.plugin.managers;

import com.cageitem.plugin.models.CageData;
import com.cageitem.plugin.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Nucleo del plugin: crea la jaula alrededor de la victima, la mantiene
 * durante el tiempo configurado y restaura exactamente el estado original
 * del mundo al finalizar (o al ser forzada su eliminacion).
 * <p>
 * La jaula esta compuesta por 3 partes apiladas verticalmente: un piso
 * solido, una seccion de rejas (hueca por dentro, donde queda encerrada
 * la victima) y un techo solido. El piso y el techo pueden tener anchos
 * distintos entre si, logrando un efecto "hongo" si se desea.
 * <p>
 * Solo puede existir una jaula activa por victima a la vez (evita
 * duplicacion si el atacante logra golpear dos veces antes de que el
 * cooldown lo bloquee, o por cualquier condicion de carrera de eventos).
 */
public class CageManager {

    private final Plugin plugin;
    private final ConfigManager configManager;

    /**
     * Jaulas activas, indexadas por el UUID de la victima.
     */
    private final Map<UUID, CageData> activeCages = new ConcurrentHashMap<>();

    public CageManager(Plugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public boolean isCaged(UUID uuid) {
        return activeCages.containsKey(uuid);
    }

    public CageData getCage(UUID uuid) {
        return activeCages.get(uuid);
    }

    /**
     * Calcula el rango [min, max] de un eje centrado en una coordenada,
     * dado un ancho. Soporta anchos pares e impares.
     */
    private int[] computeBounds(int center, int size) {
        int min = center - (size - 1) / 2;
        int max = min + size - 1;
        return new int[]{min, max};
    }

    /**
     * Guarda el estado original del bloque, lo reemplaza por el material
     * indicado y, si no es aire, lo marca como parte del "cascaron" solido
     * de la jaula.
     */
    private void placeAndTrack(Block block, Material target, Map<Location, BlockState> saved, Set<Location> shell) {
        Location loc = block.getLocation();
        saved.put(loc, block.getState());
        try {
            // applyPhysics = false: evita actualizaciones en cascada mientras construimos la jaula.
            block.setType(target, false);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "No se pudo modificar el bloque en " + loc, ex);
        }
        if (target != Material.AIR) {
            shell.add(loc);
        }
    }

    /**
     * Crea una jaula centrada en la victima segun el tamano configurado.
     * No hace nada si la victima ya esta atrapada en otra jaula, para evitar
     * duplicaciones y fugas de memoria por tareas superpuestas.
     */
    public void createCage(Player victim, Player attacker) {
        if (isCaged(victim.getUniqueId())) {
            return;
        }

        World world = victim.getWorld();

        int baseSize = configManager.getCageBaseSize();
        int topSize = configManager.getCageTopSize();
        int barsHeight = configManager.getCageBarsHeight();
        Material baseMaterial = configManager.getCageBaseMaterial();
        Material barsMaterial = configManager.getCageBarsMaterial();

        Location victimLoc = victim.getLocation();
        int centerX = victimLoc.getBlockX();
        int centerZ = victimLoc.getBlockZ();
        int feetY = victimLoc.getBlockY();

        int[] baseX = computeBounds(centerX, baseSize);
        int[] baseZ = computeBounds(centerZ, baseSize);
        int[] topX = computeBounds(centerX, topSize);
        int[] topZ = computeBounds(centerZ, topSize);

        int bottomY = feetY - 1;
        int barsMinY = feetY;
        int barsMaxY = feetY + barsHeight - 1;
        int topY = barsMaxY + 1;

        Map<Location, BlockState> savedStates = new HashMap<>();
        Set<Location> shellLocations = new HashSet<>();

        // Piso solido.
        for (int x = baseX[0]; x <= baseX[1]; x++) {
            for (int z = baseZ[0]; z <= baseZ[1]; z++) {
                placeAndTrack(world.getBlockAt(x, bottomY, z), baseMaterial, savedStates, shellLocations);
            }
        }

        // Seccion de rejas: paredes en el perimetro del piso, hueco por dentro.
        for (int y = barsMinY; y <= barsMaxY; y++) {
            for (int x = baseX[0]; x <= baseX[1]; x++) {
                for (int z = baseZ[0]; z <= baseZ[1]; z++) {
                    boolean boundary = (x == baseX[0] || x == baseX[1] || z == baseZ[0] || z == baseZ[1]);
                    Material target = boundary ? barsMaterial : Material.AIR;
                    placeAndTrack(world.getBlockAt(x, y, z), target, savedStates, shellLocations);
                }
            }
        }

        // Techo solido.
        for (int x = topX[0]; x <= topX[1]; x++) {
            for (int z = topZ[0]; z <= topZ[1]; z++) {
                placeAndTrack(world.getBlockAt(x, topY, z), baseMaterial, savedStates, shellLocations);
            }
        }

        // Nos aseguramos de que la victima quede fisicamente dentro del hueco interior.
        Location center = new Location(world, centerX + 0.5, barsMinY, centerZ + 0.5,
                victimLoc.getYaw(), victimLoc.getPitch());
        victim.teleport(center);

        CageData data = new CageData(
                victim.getUniqueId(), attacker.getUniqueId(), world.getName(),
                baseX[0], baseX[1], baseZ[0], baseZ[1],
                topX[0], topX[1], topZ[0], topZ[1],
                bottomY, barsMinY, barsMaxY, topY,
                savedStates, shellLocations
        );
        activeCages.put(victim.getUniqueId(), data);

        playCreationEffects(victim);
        victim.sendMessage(MessageUtils.colorize(configManager.getMsgCaged()));

        // Programamos la restauracion automatica tras la duracion configurada.
        long delayTicks = configManager.getCageDuration() * 20L;
        var task = Bukkit.getScheduler().runTaskLater(plugin, () -> removeCage(victim.getUniqueId()), delayTicks);
        data.setRestoreTask(task);
    }

    /**
     * Restaura la jaula asociada a la victima (si existe), devolviendo cada
     * bloque exactamente a su estado original y cancelando la tarea programada.
     * Es seguro llamar este metodo varias veces o cuando no hay jaula activa.
     */
    public void removeCage(UUID victimUuid) {
        CageData data = activeCages.remove(victimUuid);
        if (data == null) {
            return;
        }

        if (data.getRestoreTask() != null) {
            data.getRestoreTask().cancel();
        }

        World world = Bukkit.getWorld(data.getWorldName());
        if (world != null) {
            for (Map.Entry<Location, BlockState> entry : data.getOriginalStates().entrySet()) {
                try {
                    // force = true, applyPhysics = false: restaura el bloque exactamente
                    // como estaba, sin importar que haya sido reemplazado o roto mientras
                    // tanto, y sin disparar actualizaciones fisicas indeseadas.
                    entry.getValue().update(true, false);
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.WARNING,
                            "No se pudo restaurar un bloque de la jaula en " + entry.getKey(), ex);
                }
            }
        }

        Player victim = Bukkit.getPlayer(victimUuid);
        if (victim != null && victim.isOnline()) {
            playRemovalEffects(victim);
            victim.sendMessage(MessageUtils.colorize(configManager.getMsgCageRemoved()));
        }
    }

    /**
     * @return true si la ubicacion dada esta dentro del hueco interior de la jaula.
     */
    public boolean isInsideCage(CageData data, Location location) {
        if (location.getWorld() == null) {
            return false;
        }
        return data.isInsideInterior(location.getWorld().getName(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /**
     * Busca si la ubicacion dada corresponde a un bloque solido (piso, techo
     * o pared de rejas) de alguna jaula activa. Se usa para permitir que
     * otros jugadores rompan la jaula desde afuera, pero no la propia victima.
     */
    public CageData findCageByBoundaryBlock(Location location) {
        for (CageData data : activeCages.values()) {
            if (data.getWorldName().equals(location.getWorld() != null ? location.getWorld().getName() : null)
                    && data.isShellBlock(location)) {
                return data;
            }
        }
        return null;
    }

    /**
     * Restaura inmediatamente todas las jaulas activas. Se usa al deshabilitar
     * el plugin para no dejar el mundo en un estado inconsistente.
     */
    public void forceRemoveAll() {
        for (UUID victimUuid : new HashSet<>(activeCages.keySet())) {
            removeCage(victimUuid);
        }
    }

    /**
     * Restaura inmediatamente todas las jaulas activas en un mundo especifico.
     * Se usa cuando ese mundo esta a punto de descargarse, para evitar
     * referencias invalidas y memory leaks.
     */
    public void forceRemoveInWorld(String worldName) {
        Set<UUID> toRemove = new HashSet<>();
        for (Map.Entry<UUID, CageData> entry : activeCages.entrySet()) {
            if (entry.getValue().getWorldName().equals(worldName)) {
                toRemove.add(entry.getKey());
            }
        }
        for (UUID victimUuid : toRemove) {
            removeCage(victimUuid);
        }
    }

    private void playCreationEffects(Player victim) {
        World world = victim.getWorld();
        Location loc = victim.getLocation().add(0, 1, 0);
        try {
            world.playSound(loc, configManager.getCreateSound(), 1.0f, 1.0f);
            world.spawnParticle(configManager.getParticleType(), loc,
                    configManager.getParticleCount(), 0.6, 1.0, 0.6, 0.02);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Error reproduciendo efectos de creacion de jaula.", ex);
        }
    }

    private void playRemovalEffects(Player victim) {
        World world = victim.getWorld();
        Location loc = victim.getLocation().add(0, 1, 0);
        try {
            world.playSound(loc, configManager.getRemoveSound(), 1.0f, 1.0f);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Error reproduciendo efectos de eliminacion de jaula.", ex);
        }
    }
}
