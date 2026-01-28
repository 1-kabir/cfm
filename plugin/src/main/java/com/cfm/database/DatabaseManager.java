package com.cfm.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import com.cfm.CFM;
import com.cfm.util.Logger;

public class DatabaseManager {

    private Connection connection;
    private final String dbFile;

    public DatabaseManager() {
        this.dbFile = CFM.getInstance().getConfig().getString("database.sqlite.file", "data.db");
    }

    public void initialize() {
        try {
            File dataFolder = CFM.getInstance().getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:" + dataFolder.getAbsolutePath() + File.separator + dbFile;
            connection = DriverManager.getConnection(url);

            Logger.info("Successfully connected to SQLite database.");
            createTables();
        } catch (ClassNotFoundException | SQLException e) {
            Logger.error("Failed to connect to SQLite database!", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // Conversations Table
            statement.execute("CREATE TABLE IF NOT EXISTS conversations (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_uuid TEXT NOT NULL," +
                    "user_username TEXT NOT NULL," +
                    "title TEXT," +
                    "status TEXT DEFAULT 'ACTIVE'," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "metadata TEXT" +
                    ")");

            // Builds Table
            statement.execute("CREATE TABLE IF NOT EXISTS builds (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "conversation_id INTEGER NOT NULL," +
                    "iteration_number INTEGER NOT NULL DEFAULT 1," +
                    "build_name TEXT," +
                    "prompt TEXT," +
                    "schema_data TEXT," +
                    "image_url TEXT," +
                    "status TEXT DEFAULT 'PENDING'," +
                    "block_count INTEGER," +
                    "dimensions TEXT," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE," +
                    "UNIQUE(conversation_id, iteration_number)" +
                    ")");

            // Indexes
            statement.execute(
                    "CREATE INDEX IF NOT EXISTS idx_conversations_user_created ON conversations(user_uuid, created_at)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_conversations_status ON conversations(status)");
            statement.execute(
                    "CREATE INDEX IF NOT EXISTS idx_builds_conversation_iteration ON builds(conversation_id, iteration_number)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_builds_status ON builds(status)");
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                initialize();
            }
        } catch (SQLException e) {
            Logger.error("Error getting database connection!", e);
        }
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            Logger.error("Error closing database connection!", e);
        }
    }
}
