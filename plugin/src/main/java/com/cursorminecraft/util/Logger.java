package com.cursorminecraft.util;

import com.cursorminecraft.CursorMinecraft;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;

public class Logger {

    private static final String PREFIX = "[CFM] ";

    public static void info(String message) {
        CursorMinecraft.getInstance().getLogger().info(message);
    }

    public static void warn(String message) {
        CursorMinecraft.getInstance().getLogger().warning(message);
    }

    public static void error(String message) {
        CursorMinecraft.getInstance().getLogger().severe(message);
    }

    public static void error(String message, Throwable throwable) {
        CursorMinecraft.getInstance().getLogger().log(java.util.logging.Level.SEVERE, message, throwable);
    }

    public static void debug(String message) {
        if (CursorMinecraft.getInstance().getConfig().getBoolean("features.debug_logging", false)) {
            CursorMinecraft.getInstance().getLogger().info("[DEBUG] " + message);
        }
    }

    public static void broadcast(String message) {
        Bukkit.broadcast(Component.text(PREFIX).color(NamedTextColor.GOLD)
                .append(Component.text(message).color(NamedTextColor.WHITE)));
    }
}
