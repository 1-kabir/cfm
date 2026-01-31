package com.cfm.worldedit;

import com.cfm.schema.VoxelSchemaParser;
import com.cfm.util.Logger;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class BlockPlacementEngine {

    private static final Random RANDOM = new Random();

    public static void placeBuild(Player player, List<VoxelSchemaParser.BuildOperation> operations,
            BlockVector3 origin) {
        World world = player.getWorld();
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
        Set<BlockVector3> affectedPositions = new HashSet<>();

        // Phase 1: Place all blocks via WorldEdit
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
            for (VoxelSchemaParser.BuildOperation op : operations) {
                // Skip explicit upper doors if we are auto-generating them from lower doors
                // (Optional: can keep if we want to trust LLM, but user requested we handle it)
                if (op.getBlockData().contains("half=upper") && op.getBlockData().contains("door")) {
                    continue;
                }

                List<PlacementRecord> records = handleOperation(editSession, op, origin, weWorld);
                for (PlacementRecord record : records) {
                    affectedPositions.add(record.position);

                    // AUTO-DOOR LOGIC: If we placed a lower door, automatically place the upper
                    // half
                    if (isLowerDoor(record.state)) {
                        BlockVector3 upperPos = record.position.add(0, 1, 0);
                        BlockState upperState = getUpperDoorState(record.state);
                        if (upperState != null) {
                            editSession.setBlock(upperPos, upperState);
                            affectedPositions.add(upperPos);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.error("Failed to place build via WorldEdit", e);
            return;
        }

        // Phase 2: Connection Fixes & Physics
        Plugin plugin = Bukkit.getPluginManager().getPlugin("CFM");
        if (plugin != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Set<BlockVector3> toUpdate = new HashSet<>(affectedPositions);

                // Add neighbors for connection update
                for (BlockVector3 pos : affectedPositions) {
                    toUpdate.add(pos.add(1, 0, 0));
                    toUpdate.add(pos.add(-1, 0, 0));
                    toUpdate.add(pos.add(0, 0, 1));
                    toUpdate.add(pos.add(0, 0, -1));
                }

                for (BlockVector3 pos : toUpdate) {
                    try {
                        org.bukkit.Location loc = new org.bukkit.Location(world, pos.getX(), pos.getY(), pos.getZ());
                        Block block = loc.getBlock();

                        // Fix visual connections for Fences/Panes/etc.
                        if (isConnectable(block.getType())) {
                            fixVisualConnections(block);
                        }

                        // Force physics update
                        block.getState().update(true, true);
                    } catch (Exception e) {
                    }
                }
            });
        }
        Logger.info("Build placed for player " + player.getName());
    }

    // --- HELPER METHODS ---

    /**
     * Checks if the state represents the lower half of a door.
     */
    private static boolean isLowerDoor(BlockState state) {
        String id = state.getBlockType().id();
        if (!id.contains("door") || id.contains("trapdoor"))
            return false;
        // Check state properties manually or convert to Bukkit
        try {
            BlockData data = BukkitAdapter.adapt(state);
            return (data instanceof Door) && ((Door) data).getHalf() == Bisected.Half.BOTTOM;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Creates the matching Upper Door state for a given Lower Door state.
     */
    private static BlockState getUpperDoorState(BlockState lowerState) {
        try {
            BlockData lowerData = BukkitAdapter.adapt(lowerState);
            if (lowerData instanceof Door) {
                Door upperData = (Door) lowerData.clone();
                upperData.setHalf(Bisected.Half.TOP);
                return BukkitAdapter.adapt(upperData);
            }
        } catch (Exception e) {
        }
        return null;
    }

    private static boolean isConnectable(Material mat) {
        String name = mat.name();
        return name.contains("FENCE") || name.contains("GLASS_PANE") ||
                name.contains("WALL") || name.contains("IRON_BARS");
    }

    private static void fixVisualConnections(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof MultipleFacing) {
            MultipleFacing facing = (MultipleFacing) data;
            boolean changed = false;
            for (BlockFace face : facing.getAllowedFaces()) {
                if (shouldConnect(block, face)) {
                    if (!facing.hasFace(face)) {
                        facing.setFace(face, true);
                        changed = true;
                    }
                } else {
                    if (facing.hasFace(face)) {
                        facing.setFace(face, false);
                        changed = true;
                    }
                }
            }
            if (changed)
                block.setBlockData(facing, true);
        }
    }

    private static boolean shouldConnect(Block source, BlockFace face) {
        Block neighbor = source.getRelative(face);
        Material sourceMat = source.getType();
        Material targetMat = neighbor.getType();
        if (targetMat.isAir())
            return false;
        if (sourceMat == targetMat)
            return true;

        boolean isFence = sourceMat.name().contains("FENCE");
        boolean isPane = sourceMat.name().contains("GLASS_PANE") || sourceMat.name().contains("IRON_BARS");
        boolean isWall = sourceMat.name().contains("WALL");

        if (isFence)
            return (targetMat.isSolid() && targetMat.isOccluding()) || targetMat.name().contains("FENCE_GATE");
        if (isPane)
            return targetMat.name().contains("GLASS_PANE") || targetMat.name().contains("IRON_BARS")
                    || (targetMat.isSolid() && targetMat.isOccluding());
        if (isWall)
            return targetMat.name().contains("WALL") || targetMat.name().contains("FENCE_GATE")
                    || (targetMat.isSolid() && targetMat.isOccluding());
        return false;
    }

    private static List<PlacementRecord> handleOperation(EditSession editSession, VoxelSchemaParser.BuildOperation op,
            BlockVector3 origin, com.sk89q.worldedit.world.World weWorld) throws MaxChangedBlocksException {
        List<PlacementRecord> records = new ArrayList<>();
        String pattern = op.getPattern() != null ? op.getPattern().toLowerCase() : "single";

        WeightedPalette palette = new WeightedPalette(op.getBlockData());
        if (palette.isEmpty())
            return records;

        int x1 = op.getX1(), y1 = op.getY1(), z1 = op.getZ1();

        if (pattern.equals("single") || pattern.equals("door")) {
            BlockVector3 pos = origin.add(x1, y1, z1);
            if (pattern.equals("door") && !canPlaceDoor(weWorld, pos))
                return records;

            BlockState state = palette.getRandomState();
            editSession.setBlock(pos, state);
            records.add(new PlacementRecord(pos, state));

        } else if (op.getX2() != null) { // Box/Solid/etc logic
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
        }
        return records;
    }

    private static boolean canPlaceDoor(com.sk89q.worldedit.world.World world, BlockVector3 pos) {
        // Simple check: don't place if occupied?
        // Actually, with auto-door, we just want to ensure we don't overwrite weird
        // things.
        // For now, minimal validation to avoid blocking valid placements.
        return true;
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

    /**
     * CRITICAL FIX for state parsing (Stairs facing North bug).
     * Uses Bukkit's native parser which handles states much better than WorldEdit's
     * legacy parser.
     */
    private static BlockState parseBlock(String blockData) {
        if (blockData == null || blockData.isEmpty())
            return null;
        try {
            // 1. Try Bukkit Parser (Best for modern 1.13+ states like [facing=east])
            BlockData bukkitData = Bukkit.createBlockData(blockData);
            return BukkitAdapter.adapt(bukkitData);
        } catch (IllegalArgumentException e) {
            // 2. Fallback to WorldEdit parser if Bukkit fails (rare)
            try {
                return WorldEdit.getInstance().getBlockFactory().parseFromInput(blockData, null).toImmutableState();
            } catch (Exception ex) {
                // 3. Last resort: Match material name only (loses state)
                String materialName = blockData.contains("[") ? blockData.substring(0, blockData.indexOf("["))
                        : blockData;
                if (materialName.startsWith("minecraft:"))
                    materialName = materialName.substring(10);
                org.bukkit.Material material = org.bukkit.Material.matchMaterial(materialName);
                if (material != null) {
                    return BukkitAdapter.adapt(Bukkit.createBlockData(material));
                }
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
                    }
                }
                BlockState state = parseBlock(blockData); // Uses new robust parser
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
                if (r <= cumulativeWeights.get(i))
                    return states.get(i);
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
