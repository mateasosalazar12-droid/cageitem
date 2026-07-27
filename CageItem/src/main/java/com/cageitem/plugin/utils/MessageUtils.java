package com.cageitem.plugin.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Utilidades para convertir texto plano (con codigos '&') en componentes
 * de Adventure, y para reemplazar placeholders simples como {player} o {time}.
 * <p>
 * Se usa Adventure en vez de la antigua API {@code ChatColor} porque es
 * el estandar actual de Paper/Spigot y evita codigo obsoleto.
 */
public final class MessageUtils {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private MessageUtils() {
        // Clase de utilidades, no instanciable.
    }

    /**
     * Convierte un texto con codigos de color '&' en un Component.
     */
    public static Component colorize(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return LEGACY_SERIALIZER.deserialize(text);
    }

    /**
     * Reemplaza placeholders (pares placeholder/valor) en el texto y
     * devuelve el resultado ya coloreado como Component.
     * <p>
     * Ejemplo: format("&cFaltan {time}s", "{time}", "5")
     */
    public static Component format(String text, String... replacements) {
        if (text == null) {
            return Component.empty();
        }
        String result = text;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            result = result.replace(replacements[i], replacements[i + 1]);
        }
        return colorize(result);
    }
}
