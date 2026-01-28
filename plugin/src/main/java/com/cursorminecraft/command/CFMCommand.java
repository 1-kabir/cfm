package com.cursorminecraft.command;

import com.cursorminecraft.CursorMinecraft;
import com.cursorminecraft.model.Conversation;
import com.cursorminecraft.schema.VoxelSchemaParser;
import com.cursorminecraft.schema.WorldEditToVoxelParser;
import com.cursorminecraft.service.ConversationService;
import com.cursorminecraft.util.Logger;
import com.cursorminecraft.worldedit.BlockPlacementEngine;
import com.cursorminecraft.worldedit.WorldEditSelectionHelper;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
            case "tool" -> handleTool(player);
            case "create" -> handleCreate(player, args);
            case "parse" -> handleParse(player);
            case "jsonparse" -> handleJsonParse(player, args);
            case "list" -> handleList(player);
            case "reload" -> handleReload(player);
            default -> player.sendMessage("§8[§bCFM§8] §cUnknown subcommand. Use §f/cfm help");
        }

        return true;
    }

    private void handleTool(Player player) {
        ItemStack wand = new ItemStack(Material.WOODEN_AXE);
        player.getInventory().addItem(wand);
        player.sendMessage("§8[§bCFM§8] §aProvided WorldEdit wand. §7Use left/right click to select a region.");
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§8[§bCFM§8] §cUsage: §f/cfm create <prompt>");
            return;
        }

        String prompt = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        if (!WorldEditSelectionHelper.hasValidSelection(player)) {
            player.sendMessage("§8[§bCFM§8] §cPlease select a region first with WorldEdit!");
            return;
        }

        Region region = WorldEditSelectionHelper.getPlayerSelection(player);
        BlockVector3 origin = region.getMinimumPoint();

        player.sendMessage("§8[§bCFM§8] §eThinking... §7Generating your structure...");

        Conversation conv = Conversation.builder()
                .userUuid(player.getUniqueId().toString())
                .userUsername(player.getName())
                .title(prompt.length() > 30 ? prompt.substring(0, 30) + "..." : prompt)
                .status(Conversation.ConversationStatus.ACTIVE)
                .build();

        int convId = CursorMinecraft.getInstance().getConversationDAO().createConversation(conv);

        conversationService.sendMessage(convId, prompt).thenAccept(response -> {
            if (response == null) {
                player.sendMessage("§8[§bCFM§8] §cAI failed to generate a build. Try again.");
                return;
            }

            try {
                VoxelSchemaParser.BuildSchema schema = VoxelSchemaParser.parseFullSchema(response);
                if (schema.getBlocks().isEmpty()) {
                    player.sendMessage("§8[§dAI§8] §f" + response);
                } else {
                    BlockPlacementEngine.placeBuild(player, schema.getBlocks(), origin);
                    player.sendMessage(
                            "§8[§bCFM§8] §aBuild complete! §7(" + schema.getBlocks().size() + " blocks placed)");
                }
            } catch (Exception e) {
                player.sendMessage("§8[§dAI§8] §f" + response);
            }
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

    private void handleJsonParse(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§8[§bCFM§8] §cUsage: §f/cfm jsonparse <url>");
            return;
        }

        String url = args[1];
        player.sendMessage("§8[§bCFM§8] §eFetching JSON from remote source...");

        fetchJson(url).thenAccept(json -> {
            if (json == null) {
                player.sendMessage("§8[§bCFM§8] §cFailed to fetch or read from URL.");
                return;
            }

            try {
                VoxelSchemaParser.BuildSchema schema = VoxelSchemaParser.parseFullSchema(json);
                if (schema.getBlocks().isEmpty()) {
                    player.sendMessage("§8[§bCFM§8] §cThe JSON provided contains no valid block data.");
                    return;
                }

                String name = schema.getMetadata() != null ? schema.getMetadata().getName() : "Remote Build";
                player.sendMessage("§8[§bCFM§8] §aManifesting §f" + name + " §7at your location...");

                // Place at player location as origin
                BlockVector3 origin = BlockVector3.at(
                        player.getLocation().getBlockX(),
                        player.getLocation().getBlockY(),
                        player.getLocation().getBlockZ());

                BlockPlacementEngine.placeBuild(player, schema.getBlocks(), origin);
                player.sendMessage(
                        "§8[§bCFM§8] §aConstruction finalized! §7(" + schema.getBlocks().size() + " blocks placed)");

            } catch (Exception e) {
                player.sendMessage("§8[§bCFM§8] §cError parsing schema: §7" + e.getMessage());
                Logger.error("Schema parse error", e);
            }
        });
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
        List<Conversation> conversations = CursorMinecraft.getInstance().getConversationDAO()
                .getConversationsByUser(player.getUniqueId().toString());
        if (conversations.isEmpty()) {
            player.sendMessage("§8[§bCFM§8] §7No active conversations found.");
            return;
        }
        player.sendMessage("§b--- Your AI Conversations ---");
        for (Conversation conv : conversations) {
            player.sendMessage("§8[§7#" + conv.getId() + "§8] §f" + conv.getTitle());
        }
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("cfm.admin")) {
            player.sendMessage("§cNo permission!");
            return;
        }
        player.sendMessage("§8[§bCFM§8] §eReloading configuration...");
        CursorMinecraft.getInstance().reloadPlugin();
        player.sendMessage("§8[§bCFM§8] §aReload complete!");
    }

    private void sendHelp(Player player) {
        player.sendMessage("§b§lCFM §8- §fAI Build Assistant");
        player.sendMessage(" ");
        player.sendMessage("§b/cfm tool §8- §7Get selection wand");
        player.sendMessage("§b/cfm create <prompt> §8- §7Generate build");
        player.sendMessage("§b/cfm parse §8- §7Selection to JSON (w/ Bounds)");
        player.sendMessage("§b/cfm jsonparse <url> §8- §7Build from URL");
        player.sendMessage("§b/cfm list §8- §7View your chats");
        player.sendMessage("§b/cfm reload §8- §7Reload plugin");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("tool", "create", "parse", "jsonparse", "list", "help", "reload").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
