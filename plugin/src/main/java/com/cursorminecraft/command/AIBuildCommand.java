package com.cursorminecraft.command;

import com.cursorminecraft.CursorMinecraft;
import com.cursorminecraft.ai.PromptBuilder;
import com.cursorminecraft.database.dao.ConversationDAO;
import com.cursorminecraft.model.Conversation;
import com.cursorminecraft.schema.VoxelSchemaParser;
import com.cursorminecraft.schema.WorldEditToVoxelParser;
import com.cursorminecraft.service.ConversationService;
import com.cursorminecraft.util.Logger;
import com.cursorminecraft.worldedit.BlockPlacementEngine;
import com.cursorminecraft.worldedit.WorldEditSelectionHelper;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import io.github.sashirestela.openai.domain.chat.Message;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AIBuildCommand implements CommandExecutor, TabCompleter {

    private final ConversationService conversationService = new ConversationService();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "tool" -> handleTool(player);
            case "create" -> handleCreate(player, args);
            case "parse" -> handleParse(player);
            case "jsonparse" -> handleJsonParse(player, args);
            case "list" -> handleList(player);
            default -> player.sendMessage("§cUnknown subcommand. Use /aibuild help");
        }

        return true;
    }

    private void handleTool(Player player) {
        ItemStack wand = new ItemStack(Material.WOODEN_AXE);
        player.getInventory().addItem(wand);
        player.sendMessage("§a[CursorAI] Provided WorldEdit wand. Use left/right click to select a region.");
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /aibuild create <prompt>");
            return;
        }

        String prompt = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        if (!WorldEditSelectionHelper.hasValidSelection(player)) {
            player.sendMessage("§c[CursorAI] Please select a region first with WorldEdit!");
            return;
        }

        Region region = WorldEditSelectionHelper.getPlayerSelection(player);
        BlockVector3 origin = region.getMinimumPoint();

        player.sendMessage("§e[CursorAI] Thinking... Generating structure...");

        // Start a new conversation and send message
        Conversation conv = Conversation.builder()
                .userUuid(player.getUniqueId().toString())
                .userUsername(player.getName())
                .title(prompt.length() > 30 ? prompt.substring(0, 30) + "..." : prompt)
                .status(Conversation.ConversationStatus.ACTIVE)
                .build();

        int convId = CursorMinecraft.getInstance().getConversationDAO().createConversation(conv);

        conversationService.sendMessage(convId, prompt).thenAccept(response -> {
            if (response == null) {
                player.sendMessage("§c[CursorAI] AI failed to generate a build. Try again.");
                return;
            }

            try {
                List<VoxelSchemaParser.VoxelBlock> blocks = VoxelSchemaParser.parseSchema(response);
                if (blocks.isEmpty()) {
                    player.sendMessage("§f[AI Chat]: " + response);
                } else {
                    BlockPlacementEngine.placeBuild(player, blocks, origin);
                    player.sendMessage("§a[CursorAI] Build complete! (" + blocks.size() + " blocks placed)");
                }
            } catch (Exception e) {
                player.sendMessage("§f[AI Chat]: " + response);
                Logger.error("Failed to parse as build, showing as chat message");
            }
        });
    }

    private void handleParse(Player player) {
        if (!WorldEditSelectionHelper.hasValidSelection(player)) {
            player.sendMessage("§c[CursorAI] Please select a region first!");
            return;
        }

        Region region = WorldEditSelectionHelper.getPlayerSelection(player);
        try {
            String json = WorldEditToVoxelParser.convertRegionToJson(player.getWorld(), region);
            Logger.info("Selection JSON: " + json);
            player.sendMessage("§a[CursorAI] Selection parsed! JSON outputted to console/logs.");
            player.sendMessage("§7(Check server logs for the full JSON schema)");
        } catch (Exception e) {
            player.sendMessage("§c[CursorAI] Error parsing selection: " + e.getMessage());
        }
    }

    private void handleJsonParse(Player player, String[] args) {
        player.sendMessage("§e[CursorAI] Feature coming soon via Web Interface!");
    }

    private void handleList(Player player) {
        List<Conversation> conversations = CursorMinecraft.getInstance().getConversationDAO()
                .getConversationsByUser(player.getUniqueId().toString());
        if (conversations.isEmpty()) {
            player.sendMessage("§e[CursorAI] No active conversations found.");
            return;
        }
        player.sendMessage("§b--- Your Conversations ---");
        for (Conversation conv : conversations) {
            player.sendMessage("§f- [ID: " + conv.getId() + "] " + conv.getTitle());
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage("§b--- Cursor for Minecraft Help ---");
        player.sendMessage("§f/aibuild tool §7- Get WorldEdit selection tool");
        player.sendMessage("§f/aibuild create <prompt> §7- Generate build in selection");
        player.sendMessage("§f/aibuild parse §7- Selection to JSON schema (Console output)");
        player.sendMessage("§f/aibuild list §7- List your AI conversations");
        player.sendMessage("§f/aibuild help §7- Show this message");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("tool", "create", "parse", "jsonparse", "list", "help").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
