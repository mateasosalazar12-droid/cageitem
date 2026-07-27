package com.cageitem.plugin.listeners;

import com.cageitem.plugin.managers.CageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Evita errores y fugas de memoria si la victima de una jaula se desconecta
 * mientras esta esta activa: restauramos el mundo de inmediato en vez de
 * dejar una tarea programada apuntando a un jugador offline.
 */
public class PlayerConnectionListener implements Listener {

    private final CageManager cageManager;

    public PlayerConnectionListener(CageManager cageManager) {
        this.cageManager = cageManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (cageManager.isCaged(player.getUniqueId())) {
            // No tiene sentido mantener a un jugador desconectado "atrapado":
            // restauramos el mundo de inmediato para no dejar bloques de mas
            // ni una tarea programada colgada.
            cageManager.removeCage(player.getUniqueId());
        }
    }
}
