package com.cfm.worldedit;

import com.cfm.schema.VoxelSchemaParser;
import com.cfm.util.Logger;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BlockPlacementEngine {

    private static final Random RANDOM = new Random();

    public static void placeBuild(Player player, List<VoxelSchemaParser.BuildOperation> operations,
            BlockVector3 origin) {
        World world = player.getWorld();
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
            // Two-pass rendering for proper block connectivity
            List<PlacementRecord> records = new ArrayList<>();

            // Pass 1: Place all blocks
            for (VoxelSchemaParser.BuildOperation op : operations) {
                records.addAll(handleOperation(editSession, op, origin));
            }

            // Pass 2: Update connecting blocks (fences, panes, walls, stairs)
            updateConnectingBlocks(editSession, records, weWorld);

            editSession.close();
            Logger.info("Build placed for player " + player.getName() + " (" + operations.size() + " operations)");
        } catch (Exception e) {
            Logger.error("Failed to place build", e);
        }
    }

    private static List<PlacementRecord> handleOperation(EditSession editSession, VoxelSchemaParser.BuildOperation op,
            BlockVector3 origin) throws MaxChangedBlocksException {
        List<PlacementRecord> records = new ArrayList<>();
        String pattern = op.getPattern() != null ? op.getPattern().toLowerCase() : "single";

        WeightedPalette palette = new WeightedPalette(op.getBlockData());
        if (palette.isEmpty())
            return records;

        int x1 = op.getX1(), y1 = op.getY1(), z1 = op.getZ1();

        if (pattern.equals("single")) {
            BlockVector3 pos = origin.add(x1, y1, z1);
            BlockState state = palette.getRandomState();
            editSession.setBlock(pos, state);
            records.add(new PlacementRecord(pos, state));
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
                            BlockVector3 pos = origin.add(x, y, z);
                            BlockState state = palette.getRandomState();
                            editSession.setBlock(pos, state);
                            records.add(new PlacementRecord(pos, state));
                        }
                    }
                }
            }
        } else if (pattern.equals("door") || pattern.equals("layer")) {
            BlockVector3 pos = origin.add(x1, y1, z1);
            BlockState state = palette.getRandomState();
            editSession.setBlock(pos, state);
            records.add(new PlacementRecord(pos, state));
        }

        return records;
    }

    private static void updateConnectingBlocks(EditSession editSession, List<PlacementRecord> records,
            com.sk89q.worldedit.world.World world) throws MaxChangedBlocksException {
        for (PlacementRecord record : records) {
            BlockState state = record.state;
            BlockType type = state.getBlockType();
            String typeName = type.getId();

            // Update fences, panes, walls, iron bars
            if (typeName.contains("fence") || typeName.contains("pane") ||
                    typeName.contains("wall") || typeName.contains("bars") ||
                    typeName.contains("chain")) {

                BlockVector3 pos = record.position;
                BlockState updated = computeConnectingState(world, pos, state);
                if (updated != null) {
                    editSession.setBlock(pos, updated);
                }
            }
        }
    }

    private static BlockState computeConnectingState(com.sk89q.worldedit.world.World world,
            BlockVector3 pos, BlockState original) {
        try {
            BlockType type = original.getBlockType();
            Map<String, Object> states = new HashMap<>();

            // Check all 4 cardinal directions
            boolean north = canConnect(world, pos.add(0, 0, -1), type);
            boolean south = canConnect(world, pos.add(0, 0, 1), type);
            boolean east = canConnect(world, pos.add(1, 0, 0), type);
            boolean west = canConnect(world, pos.add(-1, 0, 0), type);

            String typeId = type.getId();

            if (typeId.contains("pane") || typeId.contains("bars")) {
                states.put("north", north ? "true" : "false");
                states.put("south", south ? "true" : "false");
                states.put("east", east ? "true" : "false");
                states.put("west", west ? "true" : "false");
            } else if (typeId.contains("fence") || typeId.contains("wall")) {
                states.put("north", north ? "true" : "false");
                states.put("south", south ? "true" : "false");
                states.put("east", east ? "true" : "false");
                states.put("west", west ? "true" : "false");
            }

            if (states.isEmpty())
                return null;

            StringBuilder stateString = new StringBuilder(typeId);
            stateString.append("[");
            boolean first = true;
            for (Map.Entry<String, Object> entry : states.entrySet()) {
                if (!first)
                    stateString.append(",");
                stateString.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
            stateString.append("]");

            return parseBlock(stateString.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean canConnect(com.sk89q.worldedit.world.World world, BlockVector3 pos, BlockType originalType) {
        try {
            BlockState neighbor = world.getBlock(pos);
            if (neighbor.getBlockType().getMaterial().isAir())
                return false;

            String neighborId = neighbor.getBlockType().getId();
            String originalId = originalType.getId();

            // Same type always connects
            if (neighborId.equals(originalId))
                return true;

            // Fences connect to solid blocks
            if (originalId.contains("fence") && neighbor.getBlockType().getMaterial().isSolid()) {
                return true;
            }

            // Glass panes only connect to panes
            if (originalId.contains("pane")) {
                return neighborId.contains("pane");
            }

            // Walls connect to walls and some solid blocks
            if (originalId.contains("wall")) {
                return neighborId.contains("wall") || neighbor.getBlockType().getMaterial().isSolid();
            }

            return false;
        } catch (Exception e) {
            return false;
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
        if (blockData == null || blockData.isEmpty())
            return null;
        try {
            return WorldEdit.getInstance().getBlockFactory().parseFromInput(blockData, null).toImmutableState();
        } catch (Exception e) {
            String materialName = blockData.contains("[") ? blockData.substring(0, blockData.indexOf("[")) : blockData;
            if (materialName.startsWith("minecraft:"))
                materialName = materialName.substring(10);

            org.bukkit.Material material = org.bukkit.Material.matchMaterial(materialName);
            if (material != null) {
                com.sk89q.worldedit.world.block.BlockType blockType = com.sk89q.worldedit.world.block.BlockType.REGISTRY
                        .get(material.getKey().toString());
                if (blockType != null)
                    return blockType.getDefaultState();
            }
        }
        return null;
    }

    private static class WeightedPalette {
        private final List<BlockState> states = new ArrayList<>();
        private final List<Double> cumulativeWeights = new ArrayList<>();
        private double totalWeight = 0;

        public WeightedPalette(String input) {
            if (input == null || input.isEmpty())
                return;

            String[] parts = input.split(",");
            for (String part : parts) {
                part = part.trim();
                double weight = 1.0;
                String blockData = part;

                if (part.contains("%")) {
                    try {
                        int percentIdx = part.indexOf("%");
                        weight = Double.parseDouble(part.substring(0, percentIdx));
                        blockData = part.substring(percentIdx + 1);
                    } catch (Exception e) {
                        // Ignore
                    }
                }

                BlockState state = parseBlock(blockData);
                if (state != null) {
                    states.add(state);
                    totalWeight += weight;
                    cumulativeWeights.add(totalWeight);
                }
            }
        }

        public boolean isEmpty() {
            return states.isEmpty();
        }

        public BlockState getRandomState() {
            if (isEmpty())
                return null;
            if (states.size() == 1)
                return states.get(0);

            double r = RANDOM.nextDouble() * totalWeight;
            for (int i = 0; i < cumulativeWeights.size(); i++) {
                if (r <= cumulativeWeights.get(i)) {
                    return states.get(i);
                }
            }
            return states.get(0);
        }
    }

    private static class PlacementRecord {
        final BlockVector3 position;
        final BlockState state;

        PlacementRecord(BlockVector3 position, BlockState state) {
            this.position = position;
            this.state = state;
        }
    }
}
