package com.cfm.database.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.cfm.database.DatabaseManager;
import com.cfm.model.Build;
import com.cfm.util.Logger;

public class BuildDAO {

    private final DatabaseManager dbManager;

    public BuildDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public int createBuild(Build build) {
        String sql = "INSERT INTO builds (conversation_id, iteration_number, build_name, prompt, schema_data, image_url, status, block_count, dimensions) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, build.getConversationId());
            pstmt.setInt(2, build.getIterationNumber());
            pstmt.setString(3, build.getBuildName());
            pstmt.setString(4, build.getPrompt());
            pstmt.setString(5, build.getSchemaData());
            pstmt.setString(6, build.getImageUrl());
            pstmt.setString(7, build.getStatus().name());
            pstmt.setInt(8, build.getBlockCount());
            pstmt.setString(9, build.getDimensions());
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            Logger.error("Error creating build!", e);
        }
        return -1;
    }

    public Build getBuild(int id) {
        String sql = "SELECT * FROM builds WHERE id = ?";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBuild(rs);
                }
            }
        } catch (SQLException e) {
            Logger.error("Error getting build!", e);
        }
        return null;
    }

    public List<Build> getBuildsByConversation(int conversationId) {
        List<Build> builds = new ArrayList<>();
        String sql = "SELECT * FROM builds WHERE conversation_id = ? ORDER BY iteration_number ASC";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, conversationId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    builds.add(mapResultSetToBuild(rs));
                }
            }
        } catch (SQLException e) {
            Logger.error("Error getting builds by conversation!", e);
        }
        return builds;
    }

    public void updateBuildStatus(int id, Build.BuildStatus status) {
        String sql = "UPDATE builds SET status = ? WHERE id = ?";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            Logger.error("Error updating build status!", e);
        }
    }

    private Build mapResultSetToBuild(ResultSet rs) throws SQLException {
        return Build.builder()
                .id(rs.getInt("id"))
                .conversationId(rs.getInt("conversation_id"))
                .iterationNumber(rs.getInt("iteration_number"))
                .buildName(rs.getString("build_name"))
                .prompt(rs.getString("prompt"))
                .schemaData(rs.getString("schema_data"))
                .imageUrl(rs.getString("image_url"))
                .status(Build.BuildStatus.valueOf(rs.getString("status")))
                .blockCount(rs.getInt("block_count"))
                .dimensions(rs.getString("dimensions"))
                .createdAt(rs.getTimestamp("created_at"))
                .build();
    }
}
