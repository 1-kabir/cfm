package com.cfm.command;

import org.bukkit.command.PluginCommand;

import com.cfm.CFM;

public class CommandManager {

    public void registerCommands() {
        CFMCommand executor = new CFMCommand();
        PluginCommand command = CFM.getInstance().getCommand("cfm");

        if (command != null) {
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }
}
