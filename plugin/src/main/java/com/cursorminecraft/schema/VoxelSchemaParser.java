package com.cursorminecraft.schema;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class VoxelSchemaParser {

    @Data
    @AllArgsConstructor
    public static class VoxelBlock {
        private final int x, y, z;
        private final String blockData;
    }

    @Data
    @Builder
    public static class BuildSchema {
        private final BuildMetadata metadata;
        private final List<VoxelBlock> blocks;
    }

    @Data
    @Builder
    public static class BuildMetadata {
        private final String name;
        private final String description;
        private final RegionBounds region;
    }

    @Data
    @AllArgsConstructor
    public static class RegionBounds {
        private final Vector3 min;
        private final Vector3 max;
    }

    @Data
    @AllArgsConstructor
    public static class Vector3 {
        private final int x, y, z;
    }

    /**
     * Parses the new sophisticated schema with metadata, or falls back to
     * array-only parsing.
     */
    public static BuildSchema parseFullSchema(String json) {
        JsonElement element = JsonParser.parseString(json);
        List<VoxelBlock> blocks = new ArrayList<>();
        BuildMetadata metadata = null;

        if (element.isJsonObject()) {
            JsonObject root = element.getAsJsonObject();

            // Parse Metadata if exists
            if (root.has("metadata")) {
                JsonObject metaObj = root.getAsJsonObject("metadata");
                BuildMetadata.BuildMetadataBuilder metaBuilder = BuildMetadata.builder();

                if (metaObj.has("name"))
                    metaBuilder.name(metaObj.get("name").getAsString());
                if (metaObj.has("description"))
                    metaBuilder.description(metaObj.get("description").getAsString());

                if (metaObj.has("region")) {
                    JsonObject reg = metaObj.getAsJsonObject("region");
                    Vector3 min = parseVector(reg.getAsJsonObject("min"));
                    Vector3 max = parseVector(reg.getAsJsonObject("max"));
                    metaBuilder.region(new RegionBounds(min, max));
                }
                metadata = metaBuilder.build();
            }

            // Parse Blocks
            if (root.has("blocks")) {
                JsonArray blockArray = root.getAsJsonArray("blocks");
                blocks.addAll(parseBlockArray(blockArray));
            }
        } else if (element.isJsonArray()) {
            // Fallback for simple array schemas
            blocks.addAll(parseBlockArray(element.getAsJsonArray()));
        }

        return BuildSchema.builder()
                .metadata(metadata)
                .blocks(blocks)
                .build();
    }

    /**
     * Maintain compatibility for simple array parsing
     */
    public static List<VoxelBlock> parseSchema(String jsonSchema) {
        return parseFullSchema(jsonSchema).getBlocks();
    }

    private static List<VoxelBlock> parseBlockArray(JsonArray array) {
        List<VoxelBlock> blocks = new ArrayList<>();
        for (JsonElement item : array) {
            if (item.isJsonObject()) {
                JsonObject obj = item.getAsJsonObject();
                int x = obj.get("x").getAsInt();
                int y = obj.get("y").getAsInt();
                int z = obj.get("z").getAsInt();
                String type = obj.get("type").getAsString();
                blocks.add(new VoxelBlock(x, y, z, type));
            }
        }
        return blocks;
    }

    private static Vector3 parseVector(JsonObject obj) {
        if (obj == null)
            return new Vector3(0, 0, 0);
        return new Vector3(
                obj.get("x").getAsInt(),
                obj.get("y").getAsInt(),
                obj.get("z").getAsInt());
    }
}
