package com.cfm.schema;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class VoxelSchemaParser {

    @Data
    @AllArgsConstructor
    @Builder
    public static class BuildOperation {
        private final int x1, y1, z1;
        private final Integer x2, y2, z2;
        private final String blockData;
        private final String pattern; // single, solid, hollow, box, line, flat, door, trapdoor, layer

        // Helper for backwards compatibility
        public int getX() {
            return x1;
        }

        public int getY() {
            return y1;
        }

        public int getZ() {
            return z1;
        }
    }

    @Data
    @Builder
    public static class BuildSchema {
        private final BuildMetadata metadata;
        private final List<BuildOperation> operations;

        // Compatibility getter
        public List<BuildOperation> getBlocks() {
            return operations;
        }
    }

    @Data
    @Builder
    public static class BuildMetadata {
        private final String name;
        private final String description;
        private final String workflow; // default, filling, patterns
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
     * Extract JSON from LLM response that may be wrapped in markdown code blocks
     */
    private static String extractJSON(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        String trimmed = input.trim();

        // Check for triple backtick code blocks
        if (trimmed.startsWith("```")) {
            // Remove opening ```json or ```
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }

            // Remove closing ```
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.lastIndexOf("```"));
            }
        }

        trimmed = trimmed.trim();

        // Find the actual JSON object/array
        int jsonStart = trimmed.indexOf('{');
        if (jsonStart < 0) {
            jsonStart = trimmed.indexOf('[');
        }

        if (jsonStart > 0) {
            trimmed = trimmed.substring(jsonStart);
        }

        return trimmed;
    }

    public static BuildSchema parseFullSchema(String json) {
        // Extract JSON from potential markdown wrapper
        json = extractJSON(json);

        JsonElement element = JsonParser.parseString(json);
        List<BuildOperation> operations = new ArrayList<>();
        BuildMetadata metadata = null;

        if (element.isJsonObject()) {
            JsonObject root = element.getAsJsonObject();

            // Parse Metadata
            if (root.has("metadata")) {
                JsonObject metaObj = root.getAsJsonObject("metadata");
                BuildMetadata.BuildMetadataBuilder metaBuilder = BuildMetadata.builder();

                if (metaObj.has("name"))
                    metaBuilder.name(metaObj.get("name").getAsString());
                if (metaObj.has("description"))
                    metaBuilder.description(metaObj.get("description").getAsString());
                if (metaObj.has("workflow"))
                    metaBuilder.workflow(metaObj.get("workflow").getAsString());

                if (metaObj.has("region")) {
                    JsonObject reg = metaObj.getAsJsonObject("region");
                    Vector3 min = parseVector(reg.getAsJsonObject("min"));
                    Vector3 max = parseVector(reg.getAsJsonObject("max"));
                    metaBuilder.region(new RegionBounds(min, max));
                }
                metadata = metaBuilder.build();
            }

            // Parse Blocks (Operations)
            if (root.has("blocks")) {
                JsonArray blockArray = root.getAsJsonArray("blocks");
                operations.addAll(parseOperations(blockArray));
            }
        } else if (element.isJsonArray()) {
            operations.addAll(parseOperations(element.getAsJsonArray()));
        }

        return BuildSchema.builder()
                .metadata(metadata)
                .operations(operations)
                .build();
    }

    private static List<BuildOperation> parseOperations(JsonArray array) {
        List<BuildOperation> ops = new ArrayList<>();
        for (JsonElement item : array) {
            if (item.isJsonObject()) {
                JsonObject obj = item.getAsJsonObject();
                BuildOperation.BuildOperationBuilder builder = BuildOperation.builder();

                // Position 1 (mandatory for all)
                if (obj.has("x"))
                    builder.x1(obj.get("x").getAsInt());
                else if (obj.has("x1"))
                    builder.x1(obj.get("x1").getAsInt());

                if (obj.has("y"))
                    builder.y1(obj.get("y").getAsInt());
                else if (obj.has("y1"))
                    builder.y1(obj.get("y1").getAsInt());

                if (obj.has("z"))
                    builder.z1(obj.get("z").getAsInt());
                else if (obj.has("z1"))
                    builder.z1(obj.get("z1").getAsInt());

                // Position 2 (optional)
                if (obj.has("x2"))
                    builder.x2(obj.get("x2").getAsInt());
                if (obj.has("y2"))
                    builder.y2(obj.get("y2").getAsInt());
                if (obj.has("z2"))
                    builder.z2(obj.get("z2").getAsInt());

                // Material/Type
                if (obj.has("type"))
                    builder.blockData(obj.get("type").getAsString());

                // Pattern
                if (obj.has("pattern"))
                    builder.pattern(obj.get("pattern").getAsString());
                else
                    builder.pattern("single");

                ops.add(builder.build());
            }
        }
        return ops;
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
