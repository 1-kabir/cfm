package com.cfm.model;

import lombok.Builder;
import lombok.Data;
import java.sql.Timestamp;

@Data
@Builder
public class Conversation {
    private int id;
    private String userUuid;
    private String userUsername;
    private String title;
    private ConversationStatus status;
    private ConversationMode currentMode; // New field
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String metadata; // JSON metadata

    public enum ConversationStatus {
        ACTIVE, COMPLETED, CANCELLED
    }

    public enum ConversationMode {
        PLANNING, BUILDING
    }
}
