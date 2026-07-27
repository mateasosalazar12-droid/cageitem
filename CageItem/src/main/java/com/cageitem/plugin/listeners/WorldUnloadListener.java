package com.cageitem.plugin.listeners;

import com.cageitem.plugin.managers.CageManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;

/**
 * Si un mundo con jaulas activas se descarga (por un plugin multi-mundo,
 * un reinicio parcial, etc.), forzamos su restauracion inmediata para
 * evitar tareas programadas que referencien chunks/bloques de un mundo
 * ya descargado (lo cual causaria errores o fugas de memoria).
 */
public class WorldUnloadListener implements Listener {

    private final CageManager cageManager;

    public WorldUnloadListener(CageManager cageManager) {
        this.cageManager = cageManager;
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        cageManager.forceRemoveInWorld(event.getWorld().getName());
    }
}
