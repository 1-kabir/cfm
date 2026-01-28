package com.cursorminecraft.worldedit;

import com.cursorminecraft.schema.VoxelSchemaParser;
import com.cursorminecraft.util.Logger;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.factory.BlockFactory;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.World;
import org.bukkit.entity.Player;
import java.util.List;

public class BlockPlacementEngine {

    public static void placeBuild(Player player, List<VoxelSchemaParser.VoxelBlock> blocks, BlockVector3 origin) {
        World world = player.getWorld();
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
            for (VoxelSchemaParser.VoxelBlock block : blocks) {
                BlockVector3 position = origin.add(block.getX(), block.getY(), block.getZ());

                try {
                    // Parse block data (e.g., minecraft:oak_planks[facing=north])
                    BlockState blockState = WorldEdit.getInstance().getBlockFactory()
                            .parseFromInput(block.getBlockData(), BukkitAdapter.adapt(player).getContext()).toBlock();
                    editSession.setBlock(position, blockState);
                } catch (Exception e) {
                    Logger.warn("Failed to parse block data: " + block.getBlockData());
                }
            }
            editSession.flushQueue();
            Logger.info("Build placed for player " + player.getName() + " (" + blocks.size() + " blocks)");
        }
    }
}
