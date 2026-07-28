package com.cageitem.plugin.utils;

import com.cageitem.plugin.managers.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Encargada de crear el objeto especial de la jaula, verificar si un
 * ItemStack cualquiera es (o no) ese objeto, y gestionar sus usos limitados.
 * <p>
 * La verificacion se basa PRINCIPALMENTE en el {@link org.bukkit.persistence.PersistentDataContainer},
 * ya que es el unico dato que un jugador no puede falsificar creando un
 * item identico con el mismo nombre/lore/custom-model-data mediante otros
 * plugins o comandos.
 */
public class ItemUtils {

    private final ConfigManager configManager;
    private final NamespacedKey cageItemKey;
    private final NamespacedKey usesKey;

    public ItemUtils(Plugin plugin, ConfigManager configManager) {
        this.configManager = configManager;
        this.cageItemKey = new NamespacedKey(plugin, "cage_item");
        this.usesKey = new NamespacedKey(plugin, "cage_uses");
    }

    /**
     * Crea una nueva instancia del objeto especial segun la configuracion actual.
     */
    public ItemStack createCageItem() {
        ItemStack item = new ItemStack(configManager.getItemMaterial());
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            Component name = MessageUtils.colorize(configManager.getItemName())
                    .decoration(TextDecoration.ITALIC, false);
            meta.displayName(name);

            int maxUses = configManager.getItemMaxUses();
            meta.lore(buildLore(maxUses > 0 ? maxUses : -1));

            meta.setCustomModelData(configManager.getCustomModelData());

            // Marca de autenticidad: imposible de falsificar sin acceso al plugin.
            meta.getPersistentDataContainer().set(cageItemKey, PersistentDataType.BYTE, (byte) 1);

            // Solo guardamos el contador de usos si hay un limite configurado;
            // su ausencia se interpreta como usos ilimitados.
            if (maxUses > 0) {
                meta.getPersistentDataContainer().set(usesKey, PersistentDataType.INTEGER, maxUses);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Verifica si el ItemStack dado es el objeto especial autentico de la jaula.
     */
    public boolean isCageItem(ItemStack item) {
        if (item == null) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        Byte marker = meta.getPersistentDataContainer().get(cageItemKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    /**
     * Consume un uso del objeto especial dado, actualizando su lore y su
     * contador interno de usos restantes.
     *
     * @return true si el objeto se quedo sin usos y debe eliminarse del
     * inventario del jugador; false si sigue teniendo usos (o son ilimitados).
     */
    public boolean consumeUse(ItemStack item) {
        if (item == null) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        Integer current = meta.getPersistentDataContainer().get(usesKey, PersistentDataType.INTEGER);
        if (current == null) {
            // Sin contador guardado = usos ilimitados, no hay nada que hacer.
            return false;
        }

        int remaining = current - 1;
        if (remaining <= 0) {
            // Se agotaron los usos: el llamador debe quitar el item del inventario.
            return true;
        }

        meta.getPersistentDataContainer().set(usesKey, PersistentDataType.INTEGER, remaining);
        meta.lore(buildLore(remaining));
        item.setItemMeta(meta);
        return false;
    }

    /**
     * Construye el lore del objeto reemplazando el placeholder {uses} por
     * el numero de usos restantes (o el simbolo de infinito si es -1).
     */
    private List<Component> buildLore(int remainingUses) {
        String usesDisplay = remainingUses < 0 ? "∞" : String.valueOf(remainingUses);
        List<Component> lore = new ArrayList<>();
        for (String line : configManager.getItemLore()) {
            String replaced = line.replace("{uses}", usesDisplay);
            lore.add(MessageUtils.colorize(replaced).decoration(TextDecoration.ITALIC, false));
        }
        return lore;
    }
}
