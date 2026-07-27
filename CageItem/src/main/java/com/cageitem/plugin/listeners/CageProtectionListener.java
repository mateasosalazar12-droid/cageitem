package com.cageitem.plugin.listeners;

import com.cageitem.plugin.managers.CageManager;
import com.cageitem.plugin.models.CageData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Protege la integridad de la jaula mientras esta activa:
 * <ul>
 *     <li>La victima no puede moverse ni teletransportarse (ender pearl,
 *     fruta de corus, etc.) fuera del volumen de la jaula.</li>
 *     <li>La victima no puede romper los bloques de la jaula para escapar.</li>
 *     <li>Otros jugadores SI pueden romper los bloques desde afuera; la
 *     restauracion sigue funcionando porque CageManager guarda el estado
 *     original de cada bloque, sin importar si fue roto o no mientras tanto.</li>
 * </ul>
 */
public class CageProtectionListener implements Listener {

    private final CageManager cageManager;

    public CageProtectionListener(CageManager cageManager) {
        this.cageManager = cageManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        CageData data = cageManager.getCage(player.getUniqueId());
        if (data == null) {
            return;
        }

        Location to = event.getTo();
        if (to == null) {
            return;
        }

        if (!cageManager.isInsideCage(data, to)) {
            // Se intento salir del volumen de la jaula: se cancela el movimiento
            // devolviendo al jugador a su posicion anterior (valida).
            event.setTo(event.getFrom());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        CageData data = cageManager.getCage(player.getUniqueId());
        if (data == null) {
            return;
        }

        Location to = event.getTo();
        if (to == null) {
            return;
        }

        if (!cageManager.isInsideCage(data, to)) {
            // Bloquea intentos de escape via ender pearl, fruta de corus, comandos, etc.
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        CageData data = cageManager.findCageByBoundaryBlock(event.getBlock().getLocation());
        if (data == null) {
            return;
        }

        // La propia victima no puede romper su jaula para escapar.
        if (event.getPlayer().getUniqueId().equals(data.getVictim())) {
            event.setCancelled(true);
        }
        // Cualquier otro jugador puede romperla libremente para entrar/ayudar;
        // la restauracion final seguira siendo correcta.
    }
}
