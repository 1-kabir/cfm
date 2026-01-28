package com.cfm.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;

import com.cfm.CFM;

public class Logger {

    private static final String PREFIX = "[CFM] ";

    public static void info(String message) {
        CFM.getInstance().getLogger().info(message);
    }

    public static void warn(String message) {
        CFM.getInstance().getLogger().warning(message);
    }

    public static void error(String message) {
        CFM.getInstance().getLogger().severe(message);
    }

    public static void error(String message, Throwable throwable) {
        CFM.getInstance().getLogger().log(java.util.logging.Level.SEVERE, message, throwable);
    }

    public static void debug(String message) {
        if (CFM.getInstance().getConfig().getBoolean("features.debug_logging", false)) {
            CFM.getInstance().getLogger().info("[DEBUG] " + message);
        }
    }

    public static void broadcast(String message) {
        Bukkit.broadcast(Component.text(PREFIX).color(NamedTextColor.GOLD)
                .append(Component.text(message).color(NamedTextColor.WHITE)));
    }
}
