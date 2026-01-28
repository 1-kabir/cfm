package com.cursorminecraft.command;

import com.cursorminecraft.CursorMinecraft;
import org.bukkit.command.PluginCommand;

public class CommandManager {

    public void registerCommands() {
        CFMCommand executor = new CFMCommand();
        PluginCommand command = CursorMinecraft.getInstance().getCommand("cfm");

        if (command != null) {
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }
}
