package com.cursorminecraft.command;

import com.cursorminecraft.CursorMinecraft;
import org.bukkit.command.PluginCommand;

public class CommandManager {

    public void registerCommands() {
        registerAIBuildCommand();
    }

    private void registerAIBuildCommand() {
        AIBuildCommand executor = new AIBuildCommand();
        PluginCommand command = CursorMinecraft.getInstance().getCommand("aibuild");

        if (command != null) {
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }
}
