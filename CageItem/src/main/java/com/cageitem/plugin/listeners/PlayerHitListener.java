package com.cageitem.plugin.listeners;

import com.cageitem.plugin.managers.CageManager;
import com.cageitem.plugin.managers.ConfigManager;
import com.cageitem.plugin.managers.CooldownManager;
import com.cageitem.plugin.utils.ItemUtils;
import com.cageitem.plugin.utils.MessageUtils;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * Detecta cuando un jugador golpea a otro con el objeto especial y, si la
 * habilidad esta disponible, dispara la creacion de la jaula.
 * <p>
 * Solo reacciona ante golpes jugador-contra-jugador: los golpes contra mobs
 * quedan intactos (el item sigue siendo un arma normal contra ellos).
 */
public class PlayerHitListener implements Listener {

    private final Plugin plugin;
    private final ConfigManager configManager;
    private final CooldownManager cooldownManager;
    private final CageManager cageManager;
    private final ItemUtils itemUtils;

    public PlayerHitListener(Plugin plugin, ConfigManager configManager, CooldownManager cooldownManager,
                              CageManager cageManager, ItemUtils itemUtils) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.cooldownManager = cooldownManager;
        this.cageManager = cageManager;
        this.itemUtils = itemUtils;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        try {
            // Solo nos interesan los golpes de un jugador hacia otro jugador.
            if (!(event.getDamager() instanceof Player attacker)) {
                return;
            }
            if (!(event.getEntity() instanceof Player victim)) {
                return;
            }
            if (attacker.getUniqueId().equals(victim.getUniqueId())) {
                return;
            }

            // No activar la habilidad contra espectadores (no tiene sentido encerrarlos).
            if (victim.getGameMode() == GameMode.SPECTATOR) {
                return;
            }
            // El propio atacante debe ser un jugador "real" jugando, no en espectador.
            if (attacker.getGameMode() == GameMode.SPECTATOR) {
                return;
            }

            ItemStack weapon = attacker.getInventory().getItemInMainHand();
            if (!itemUtils.isCageItem(weapon)) {
                return;
            }

            // Evita duplicar una jaula si la victima ya esta atrapada en otra.
            if (cageManager.isCaged(victim.getUniqueId())) {
                return;
            }

            UUID attackerId = attacker.getUniqueId();
            if (cooldownManager.isOnCooldown(attackerId)) {
                long remaining = cooldownManager.getRemainingSeconds(attackerId);
                attacker.sendMessage(MessageUtils.format(configManager.getMsgCooldown(), "{time}", String.valueOf(remaining)));
                return;
            }

            cooldownManager.setCooldown(attackerId);
            cageManager.createCage(victim, attacker);
        } catch (Exception ex) {
            // Cualquier error inesperado aqui no debe romper el combate normal del servidor.
            plugin.getLogger().warning("Error procesando golpe con CageItem: " + ex.getMessage());
        }
    }
}
