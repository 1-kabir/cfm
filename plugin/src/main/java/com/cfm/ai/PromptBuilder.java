package com.cfm.ai;

import java.util.ArrayList;
import java.util.List;

import com.cfm.util.Logger;

public class PromptBuilder {

    private static final String SYSTEM_PROMPT = "You are an expert Minecraft builder AI. Your goal is to generate architectural designs in a sophisticated VoxelJS JSON format.\n"
            + "Output a JSON object with the following structure:\n"
            + "{\n"
            + "  \"metadata\": { \"name\": \"...\", \"workflow\": \"default|filling|patterns\" },\n"
            + "  \"blocks\": [\n"
            + "    { \"x\": 0, \"y\": 0, \"z\": 0, \"type\": \"minecraft:stone\" }, // Single block\n"
            + "    { \"type\": \"minecraft:oak_planks\", \"pattern\": \"solid|hollow|box|line|flat\", \"x1\": 0, \"y1\": 0, \"z1\": 0, \"x2\": 5, \"y2\": 5, \"z2\": 5 } // Operation\n"
            + "  ]\n"
            + "}\n"
            + "Workflows:\n"
            + "- default: Standard block-by-block placement.\n"
            + "- filling: Use 'solid' pattern for large cuboid fills (must provide x1,y1,z1 and x2,y2,z2).\n"
            + "- patterns: Use complex patterns like 'hollow' (walls only), 'box' (walls+floor+ceiling), 'line' (axis-aligned), or 'flat' (thin surface).\n"
            + "Advanced Features:\n"
            + "1. Randomized Palettes: You can specify multiple blocks for the 'type' field using a comma-separated list, optionally with percentages: \"20%minecraft:stone,80%minecraft:cobblestone\" or just \"minecraft:oak_planks,minecraft:spruce_planks\" for equal distribution.\n"
            + "2. Double-Tall Blocks: For blocks like doors, large flowers (peony, rose_bush), and tall_grass, you MUST provide TWO entries—one for the bottom half and one for the top half (e.g., [half=lower] and [half=upper]).\n"
            + "3. Block States: Use brackets to specify states like [facing=north,half=bottom,shape=inner_left]. This is CRITICAL for stairs, doors, and fences.\n"
            + "Block IDs must be valid Minecraft 1.21 IDs.\n"
            + "Be creative, sophisticated, and structurally sound.";

    public static List<String> buildPrompt(String userPrompt, String context) {
        List<String> messages = new ArrayList<>();
        messages.add(SYSTEM_PROMPT);

        if (context != null && !context.isEmpty()) {
            messages.add("Existing build context: " + context);
        }

        messages.add(userPrompt);
        return messages;
    }
}
