package com.cfm.worldedit;

import com.cfm.util.Logger;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import org.bukkit.entity.Player;

public class WorldEditSelectionHelper {

    public static Region getPlayerSelection(Player player) {
        com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(player);
        SessionManager manager = WorldEdit.getInstance().getSessionManager();
        LocalSession session = manager.get(wePlayer);

        try {
            return session.getSelection(wePlayer.getWorld());
        } catch (IncompleteRegionException e) {
            Logger.warn("Player " + player.getName() + " has an incomplete WorldEdit selection.");
            return null;
        }
    }

    public static boolean hasValidSelection(Player player) {
        Region region = getPlayerSelection(player);
        if (region == null)
            return false;

        // Ensure it's not too large to prevent server crash
        long volume = region.getVolume();
        int maxBlocks = 1000000; // 1 million blocks cap for now
        if (volume > maxBlocks) {
            Logger.warn("Player " + player.getName() + " selection is too large (" + volume + " blocks).");
            return false;
        }

        return true;
    }
}
