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
 * Encargada de crear el objeto especial de la jaula y de verificar si un
 * ItemStack cualquiera es (o no) ese objeto.
 * <p>
 * La verificacion se basa PRINCIPALMENTE en el {@link org.bukkit.persistence.PersistentDataContainer},
 * ya que es el unico dato que un jugador no puede falsificar creando un
 * item identico con el mismo nombre/lore/custom-model-data mediante otros
 * plugins o comandos.
 */
public class ItemUtils {

    private final ConfigManager configManager;
    private final NamespacedKey cageItemKey;

    public ItemUtils(Plugin plugin, ConfigManager configManager) {
        this.configManager = configManager;
        this.cageItemKey = new NamespacedKey(plugin, "cage_item");
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

            List<Component> lore = new ArrayList<>();
            for (String line : configManager.getItemLore()) {
                lore.add(MessageUtils.colorize(line).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);

            meta.setCustomModelData(configManager.getCustomModelData());

            // Marca de autenticidad: imposible de falsificar sin acceso al plugin.
            meta.getPersistentDataContainer().set(cageItemKey, PersistentDataType.BYTE, (byte) 1);

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
}
