package com.cfm.command;

import com.cfm.CFM;
import com.cfm.model.Build;
import com.cfm.model.Conversation;
import com.cfm.schema.VoxelSchemaParser;
import com.cfm.schema.WorldEditToVoxelParser;
import com.cfm.service.ConversationService;
import com.cfm.util.Logger;
import com.cfm.worldedit.BlockPlacementEngine;
import com.cfm.worldedit.WorldEditSelectionHelper;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CFMCommand implements CommandExecutor, TabCompleter {

    private final ConversationService conversationService = new ConversationService();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "create" -> handleCreate(player, args);
            case "parse" -> handleParse(player);
            case "paste" -> handlePaste(player, args); // Renamed from jsonparse
            case "list" -> handleList(player);
            case "reload" -> handleReload(player);
            default -> player.sendMessage("§8[§bCFM§8] §cUnknown subcommand. Use §f/cfm help");
        }

        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§8[§bCFM§8] §cUsage: §f/cfm create <prompt>");
            return;
        }

        String prompt = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        // No selection required. Uses player location as origin for relative terms if
        // needed.
        player.sendMessage("§8[§bCFM§8] §eThinking... §7Generating your plan...");

        Conversation conv = Conversation.builder()
                .userUuid(player.getUniqueId().toString())
                .userUsername(player.getName())
                .title(prompt.length() > 30 ? prompt.substring(0, 30) + "..." : prompt)
                .status(Conversation.ConversationStatus.ACTIVE)
                .currentMode(Conversation.ConversationMode.PLANNING) // Default to Planning
                .build();

        int convId = CFM.getInstance().getConversationDAO().createConversation(conv);

        conversationService.sendMessage(convId, prompt).thenAccept(response -> {
            if (response == null) {
                player.sendMessage("§8[§bCFM§8] §cAI failed to respond. Try again.");
                return;
            }
            // In Planning mode, we just show the plan info
            player.sendMessage("§8[§bCFM§8] §aPlan generated! §7ID: #" + convId);
            player.sendMessage("§7Check the Web UI to review and build.");
        });
    }

    private void handleParse(Player player) {
        if (!WorldEditSelectionHelper.hasValidSelection(player)) {
            player.sendMessage("§8[§bCFM§8] §cPlease select a region first!");
            return;
        }

        Region region = WorldEditSelectionHelper.getPlayerSelection(player);
        try {
            String json = WorldEditToVoxelParser.convertRegionToJson(player.getWorld(), region);
            Logger.info("Parsed Build JSON:\n" + json);
            player.sendMessage("§8[§bCFM§8] §aSelection parsed! §7JSON with bounds outputted to console.");
        } catch (Exception e) {
            player.sendMessage("§8[§bCFM§8] §cError: " + e.getMessage());
        }
    }

    private void handlePaste(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§8[§bCFM§8] §cUsage: §f/cfm paste <build_id|url>");
            return;
        }

        String input = args[1];
        BlockVector3 origin = BlockVector3.at(
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ());

        // Check if input is integer (Build ID)
        try {
            int buildId = Integer.parseInt(input);
            Build build = CFM.getInstance().getBuildDAO().getBuild(buildId);
            if (build != null && build.getSchemaData() != null) {
                player.sendMessage("§8[§bCFM§8] §eLoading build #" + buildId + "...");
                placeSchema(player, build.getSchemaData(), origin);
                return;
            } else {
                player.sendMessage("§8[§bCFM§8] §cBuild #" + buildId + " not found or empty.");
                return;
            }
        } catch (NumberFormatException e) {
            // Not an ID, treat as URL
            player.sendMessage("§8[§bCFM§8] §eFetching JSON from remote source...");
            fetchJson(input).thenAccept(json -> {
                if (json == null) {
                    player.sendMessage("§8[§bCFM§8] §cFailed to fetch from URL.");
                    return;
                }
                placeSchema(player, json, origin);
            });
        }
    }

    private void placeSchema(Player player, String json, BlockVector3 origin) {
        try {
            VoxelSchemaParser.BuildSchema schema = VoxelSchemaParser.parseFullSchema(json);
            if (schema.getBlocks().isEmpty()) {
                player.sendMessage("§8[§bCFM§8] §cThe JSON provided contains no valid block data.");
                return;
            }

            String name = schema.getMetadata() != null ? schema.getMetadata().getName() : "Structure";
            player.sendMessage("§8[§bCFM§8] §aManifesting §f" + name + " §7at your location...");

            BlockPlacementEngine.placeBuild(player, schema.getBlocks(), origin);
            player.sendMessage(
                    "§8[§bCFM§8] §aConstruction finalized! §7(" + schema.getBlocks().size() + " blocks placed)");

        } catch (Exception e) {
            player.sendMessage("§8[§bCFM§8] §cError parsing schema: §7" + e.getMessage());
            Logger.error("Schema parse error", e);
        }
    }

    private CompletableFuture<String> fetchJson(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return response.body();
                }
            } catch (Exception e) {
                Logger.error("Failed to fetch JSON from " + url, e);
            }
            return null;
        });
    }

    private void handleList(Player player) {
        List<Conversation> conversations = CFM.getInstance().getConversationDAO()
                .getConversationsByUser(player.getUniqueId().toString());
        if (conversations.isEmpty()) {
            player.sendMessage("§8[§bCFM§8] §7No active conversations found.");
            return;
        }
        player.sendMessage("§b--- Your AI Conversations ---");
        for (Conversation conv : conversations) {
            player.sendMessage(
                    "§8[§7#" + conv.getId() + "§8] §f" + conv.getTitle() + " §7[" + conv.getCurrentMode() + "]");
        }
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("cfm.admin")) {
            player.sendMessage("§cNo permission!");
            return;
        }
        player.sendMessage("§8[§bCFM§8] §eReloading configuration...");
        CFM.getInstance().reloadPlugin();
        player.sendMessage("§8[§bCFM§8] §aReload complete!");
    }

    private void sendHelp(Player player) {
        player.sendMessage("§b§lCFM §8- §fAI Build Assistant");
        player.sendMessage(" ");
        player.sendMessage("§b/cfm create <prompt> §8- §7Start new plan");
        player.sendMessage("§b/cfm paste <id|url> §8- §7Paste build at feet");
        player.sendMessage("§b/cfm parse §8- §7Selection to JSON");
        player.sendMessage("§b/cfm list §8- §7View chats");
        player.sendMessage("§b/cfm reload §8- §7Reload plugin");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "paste", "parse", "list", "help", "reload").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
