package com.cursorminecraft;

import com.cursorminecraft.ai.AIClient;
import com.cursorminecraft.command.CommandManager;
import com.cursorminecraft.database.DatabaseManager;
import com.cursorminecraft.database.dao.BuildDAO;
import com.cursorminecraft.database.dao.ConversationDAO;
import com.cursorminecraft.web.WebServer;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public class CursorMinecraft extends JavaPlugin {

    private static CursorMinecraft instance;

    @Getter
    private DatabaseManager databaseManager;
    @Getter
    private ConversationDAO conversationDAO;
    @Getter
    private BuildDAO buildDAO;
    @Getter
    private AIClient aiClient;
    @Getter
    private WebServer webServer;

    @Override
    public void onEnable() {
        instance = this;

        // Load configuration
        saveDefaultConfig();

        // Initialize Database
        databaseManager = new DatabaseManager();
        databaseManager.initialize();

        // Initialize DAOs
        conversationDAO = new ConversationDAO(databaseManager);
        buildDAO = new BuildDAO(databaseManager);

        // Initialize AI Client
        aiClient = new AIClient();

        // Register Commands
        new CommandManager().registerCommands();

        // Initialize Web Server
        webServer = new WebServer();
        webServer.start();

        getLogger().info("CFM v" + getDescription().getVersion() + " has been enabled!");

        // TODO: Register Listeners
    }

    @Override
    public void onDisable() {
        if (webServer != null) {
            webServer.stop();
        }

        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("CFM has been disabled!");
    }

    public static CursorMinecraft getInstance() {
        return instance;
    }
}
