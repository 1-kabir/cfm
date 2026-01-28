package com.cursorminecraft.database.dao;

import com.cursorminecraft.database.DatabaseManager;
import com.cursorminecraft.model.Conversation;
import com.cursorminecraft.util.Logger;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConversationDAO {

    private final DatabaseManager dbManager;

    public ConversationDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public int createConversation(Conversation conversation) {
        String sql = "INSERT INTO conversations (user_uuid, user_username, title, status, metadata) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, conversation.getUserUuid());
            pstmt.setString(2, conversation.getUserUsername());
            pstmt.setString(3, conversation.getTitle());
            pstmt.setString(4, conversation.getStatus().name());
            pstmt.setString(5, conversation.getMetadata());
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            Logger.error("Error creating conversation!", e);
        }
        return -1;
    }

    public Conversation getConversation(int id) {
        String sql = "SELECT * FROM conversations WHERE id = ?";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToConversation(rs);
                }
            }
        } catch (SQLException e) {
            Logger.error("Error getting conversation!", e);
        }
        return null;
    }

    public List<Conversation> getConversationsByUser(String userUuid) {
        List<Conversation> conversations = new ArrayList<>();
        String sql = "SELECT * FROM conversations WHERE user_uuid = ? ORDER BY created_at DESC";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, userUuid);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    conversations.add(mapResultSetToConversation(rs));
                }
            }
        } catch (SQLException e) {
            Logger.error("Error getting conversations by user!", e);
        }
        return conversations;
    }

    public void updateConversationStatus(int id, Conversation.ConversationStatus status) {
        String sql = "UPDATE conversations SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            Logger.error("Error updating conversation status!", e);
        }
    }

    private Conversation mapResultSetToConversation(ResultSet rs) throws SQLException {
        return Conversation.builder()
                .id(rs.getInt("id"))
                .userUuid(rs.getString("user_uuid"))
                .userUsername(rs.getString("user_username"))
                .title(rs.getString("title"))
                .status(Conversation.ConversationStatus.valueOf(rs.getString("status")))
                .createdAt(rs.getTimestamp("created_at"))
                .updatedAt(rs.getTimestamp("updated_at"))
                .metadata(rs.getString("metadata"))
                .build();
    }
}
