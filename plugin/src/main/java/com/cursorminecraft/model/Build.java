package com.cursorminecraft.model;

import lombok.Builder;
import lombok.Data;
import java.sql.Timestamp;

@Data
@Builder
public class Build {
    private int id;
    private int conversationId;
    private int iterationNumber;
    private String buildName;
    private String prompt;
    private String schemaData; // VoxelJS JSON
    private String imageUrl;
    private BuildStatus status;
    private int blockCount;
    private String dimensions; // JSON dimensions {width, height, depth}
    private Timestamp createdAt;

    public enum BuildStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }
}
