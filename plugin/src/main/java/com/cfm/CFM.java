package com.cfm;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import com.cfm.ai.AIClient;
import com.cfm.command.CommandManager;
import com.cfm.database.DatabaseManager;
import com.cfm.database.dao.BuildDAO;
import com.cfm.database.dao.ConversationDAO;
import com.cfm.web.WebServer;

public class CFM extends JavaPlugin {

    private static CFM instance;

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

    public void reloadPlugin() {
        reloadConfig();
        if (aiClient != null) {
            aiClient = new AIClient();
        }
        if (webServer != null) {
            webServer.stop();
            webServer = new WebServer();
            webServer.start();
        }
        getLogger().info("CFM Configuration and services reloaded!");
    }

    public static CFM getInstance() {
        return instance;
    }
}
