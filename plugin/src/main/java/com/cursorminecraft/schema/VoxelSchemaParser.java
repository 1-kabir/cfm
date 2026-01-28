package com.cursorminecraft.schema;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

public class VoxelSchemaParser {

    @Data
    public static class VoxelBlock {
        private final int x, y, z;
        private final String blockData;
    }

    public static List<VoxelBlock> parseSchema(String jsonSchema) {
        List<VoxelBlock> blocks = new ArrayList<>();
        JsonElement element = JsonParser.parseString(jsonSchema);

        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
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
        }

        return blocks;
    }
}
