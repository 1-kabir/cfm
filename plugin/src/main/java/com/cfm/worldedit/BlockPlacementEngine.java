package com.cfm.worldedit;

import com.cfm.schema.VoxelSchemaParser;
import com.cfm.util.Logger;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.World;
import org.bukkit.entity.Player;
import java.util.List;

public class BlockPlacementEngine {

    public static void placeBuild(Player player, List<VoxelSchemaParser.BuildOperation> operations,
            BlockVector3 origin) {
        World world = player.getWorld();
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
            for (VoxelSchemaParser.BuildOperation op : operations) {
                handleOperation(player, editSession, op, origin);
            }
            editSession.close();
            Logger.info("Build placed for player " + player.getName() + " (" + operations.size() + " operations)");
        } catch (Exception e) {
            Logger.error("Failed to place build", e);
        }
    }

    private static void handleOperation(Player player, EditSession editSession, VoxelSchemaParser.BuildOperation op,
            BlockVector3 origin) throws MaxChangedBlocksException {
        String pattern = op.getPattern() != null ? op.getPattern().toLowerCase() : "single";
        BlockState blockState = parseBlock(op.getBlockData());
        if (blockState == null)
            return;

        int x1 = op.getX1(), y1 = op.getY1(), z1 = op.getZ1();

        if (pattern.equals("single")) {
            editSession.setBlock(origin.add(x1, y1, z1), blockState);
        } else if (op.getX2() != null && op.getY2() != null && op.getZ2() != null) {
            int x2 = op.getX2(), y2 = op.getY2(), z2 = op.getZ2();

            int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
            int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
            int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        boolean shouldPlace = switch (pattern) {
                            case "solid", "fill" -> true;
                            case "hollow" -> (x == minX || x == maxX || z == minZ || z == maxZ);
                            case "box" -> (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ);
                            case "flat" -> true;
                            case "line" -> isPointOnLine(x, y, z, x1, y1, z1, x2, y2, z2);
                            default -> true;
                        };

                        if (shouldPlace) {
                            editSession.setBlock(origin.add(x, y, z), blockState);
                        }
                    }
                }
            }
        } else if (pattern.equals("door")) {
            editSession.setBlock(origin.add(x1, y1, z1), blockState);
        } else if (pattern.equals("layer")) {
            editSession.setBlock(origin.add(x1, y1, z1), blockState);
        }
    }

    private static boolean isPointOnLine(int x, int y, int z, int x1, int y1, int z1, int x2, int y2, int z2) {
        if (x1 == x2 && y1 == y2)
            return x == x1 && y == y1 && z >= Math.min(z1, z2) && z <= Math.max(z1, z2);
        if (x1 == x2 && z1 == z2)
            return x == x1 && z == z1 && y >= Math.min(y1, y2) && y <= Math.max(y1, y2);
        if (y1 == y2 && z1 == z2)
            return y == y1 && z == z1 && x >= Math.min(x1, x2) && x <= Math.max(x1, x2);
        return true;
    }

    private static BlockState parseBlock(String blockData) {
        try {
            return WorldEdit.getInstance().getBlockFactory().parseFromInput(blockData, null).toImmutableState();
        } catch (Exception e) {
            org.bukkit.Material material = org.bukkit.Material.matchMaterial(blockData);
            if (material != null) {
                com.sk89q.worldedit.world.block.BlockType blockType = com.sk89q.worldedit.world.block.BlockType.REGISTRY
                        .get(material.getKey().toString());
                if (blockType != null)
                    return blockType.getDefaultState();
            }
        }
        return null;
    }
}
