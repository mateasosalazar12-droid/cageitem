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
     * Crea una jaula 5x5x5 (o el tamano configurado) centrada en la victima.
     * No hace nada si la victima ya esta atrapada en otra jaula, para evitar
     * duplicaciones y fugas de memoria por tareas superpuestas.
     */
    public void createCage(Player victim, Player attacker) {
        if (isCaged(victim.getUniqueId())) {
            return;
        }

        World world = victim.getWorld();
        int size = configManager.getCageSize();
        int radius = (size - 1) / 2;

        Location base = victim.getLocation();
        int baseX = base.getBlockX();
        int baseY = base.getBlockY();
        int baseZ = base.getBlockZ();

        int minX = baseX - radius;
        int maxX = baseX + radius;
        int minY = baseY - radius;
        int maxY = baseY + radius;
        int minZ = baseZ - radius;
        int maxZ = baseZ + radius;

        Material cageMaterial = configManager.getCageMaterial();
        Map<Location, BlockState> savedStates = new HashMap<>();

        // Recorremos todo el cubo: guardamos el estado ORIGINAL de cada bloque
        // (incluyendo tile-entities como cofres/hornos) antes de tocar nada,
        // para poder restaurarlo exactamente despues.
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    savedStates.put(block.getLocation(), block.getState());

                    boolean boundary = (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ);
                    Material target = boundary ? cageMaterial : Material.AIR;

                    try {
                        // applyPhysics = false: evita que se disparen actualizaciones en cascada
                        // (por ejemplo, arena/grava cayendo) mientras construimos la jaula.
                        block.setType(target, false);
                    } catch (Exception ex) {
                        plugin.getLogger().log(Level.WARNING,
                                "No se pudo modificar el bloque en " + x + "," + y + "," + z, ex);
                    }
                }
            }
        }

        // Nos aseguramos de que la victima quede fisicamente dentro del cubo hueco.
        Location center = new Location(world, baseX + 0.5, minY + 1, baseZ + 0.5,
                victim.getLocation().getYaw(), victim.getLocation().getPitch());
        victim.teleport(center);

        CageData data = new CageData(
                victim.getUniqueId(), attacker.getUniqueId(), world.getName(),
                minX, maxX, minY, maxY, minZ, maxZ, savedStates
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
     * @return true si la ubicacion dada esta dentro del volumen de la jaula.
     */
    public boolean isInsideCage(CageData data, Location location) {
        if (location.getWorld() == null) {
            return false;
        }
        return data.contains(location.getWorld().getName(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /**
     * Busca si la ubicacion dada corresponde a un bloque del "cascaron"
     * (pared/techo/piso) de alguna jaula activa. Se usa para permitir que
     * otros jugadores rompan la jaula desde afuera, pero no la propia victima.
     */
    public CageData findCageByBoundaryBlock(Location location) {
        if (location.getWorld() == null) {
            return null;
        }
        String worldName = location.getWorld().getName();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        for (CageData data : activeCages.values()) {
            if (data.contains(worldName, x, y, z) && data.isBoundary(x, y, z)) {
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
