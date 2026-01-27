package com.cursorminecraft;

import org.bukkit.plugin.java.JavaPlugin;

public class CursorMinecraft extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("CursorMinecraft has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("CursorMinecraft has been disabled!");
    }
}
