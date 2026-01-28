package com.cursorminecraft.schema;

import com.google.gson.Gson;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.World;
import org.bukkit.block.Block;
import java.util.ArrayList;
import java.util.List;

public class WorldEditToVoxelParser {

    private static final Gson gson = new Gson();

    public static String convertRegionToJson(World world, Region region) {
        BlockVector3 min = region.getMinimumPoint();
        List<VoxelSchemaParser.VoxelBlock> blocks = new ArrayList<>();

        for (int x = region.getMinimumPoint().getX(); x <= region.getMaximumPoint().getX(); x++) {
            for (int y = region.getMinimumPoint().getY(); y <= region.getMaximumPoint().getY(); y++) {
                for (int z = region.getMinimumPoint().getZ(); z <= region.getMaximumPoint().getZ(); z++) {
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

                        blocks.add(new VoxelSchemaParser.VoxelBlock(
                                x - min.getX(),
                                y - min.getY(),
                                z - min.getZ(),
                                type));
                    }
                }
            }
        }
        return gson.toJson(blocks);
    }
}
