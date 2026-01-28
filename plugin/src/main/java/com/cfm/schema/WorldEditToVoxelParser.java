package com.cfm.schema;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.World;
import org.bukkit.block.Block;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldEditToVoxelParser {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static String convertRegionToJson(World world, Region region) {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        List<VoxelSchemaParser.BuildOperation> blocks = new ArrayList<>();

        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!block.getType().isAir()) {
                        String blockData = block.getBlockData().getAsString();
                        // Simplify name to include brackets if present
                        String type = blockData.replace("minecraft:", "");
                        if (type.contains("[")) {
                            type = "minecraft:" + type;
                        } else {
                            type = block.getType().getKey().toString();
                        }

                        blocks.add(VoxelSchemaParser.BuildOperation.builder()
                                .x1(x - min.getBlockX())
                                .y1(y - min.getBlockY())
                                .z1(z - min.getBlockZ())
                                .blockData(type)
                                .pattern("single")
                                .build());
                    }
                }
            }
        }

        // Construct sophisticated schema
        Map<String, Object> root = new HashMap<>();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", "Exported Build");
        metadata.put("version", "1.2.0");
        metadata.put("workflow", "default");

        Map<String, Object> regionMap = new HashMap<>();
        regionMap.put("min", createVectorMap(0, 0, 0)); // Relative min
        regionMap.put("max", createVectorMap(
                max.getBlockX() - min.getBlockX(),
                max.getBlockY() - min.getBlockY(),
                max.getBlockZ() - min.getBlockZ())); // Relative max

        metadata.put("region", regionMap);

        root.put("metadata", metadata);
        root.put("blocks", blocks);

        return gson.toJson(root);
    }

    private static Map<String, Integer> createVectorMap(int x, int y, int z) {
        Map<String, Integer> map = new HashMap<>();
        map.put("x", x);
        map.put("y", y);
        map.put("z", z);
        return map;
    }
}
