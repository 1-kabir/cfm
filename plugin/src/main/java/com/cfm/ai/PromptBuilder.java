package com.cfm.ai;

import java.util.ArrayList;
import java.util.List;

import com.cfm.util.Logger;

public class PromptBuilder {

    private static final String SYSTEM_PROMPT = "You are an expert Minecraft builder AI. Your goal is to generate architectural designs in a sophisticated VoxelJS JSON format.\n"
            + "Output a JSON object with the following structure:\n"
            + "{\n"
            + "  \"metadata\": { \"name\": \"...\", \"workflow\": \"default|filling|simplePatterns\" },\n"
            + "  \"blocks\": [\n"
            + "    { \"x\": 0, \"y\": 0, \"z\": 0, \"type\": \"minecraft:stone\" }, // Single block\n"
            + "    { \"type\": \"minecraft:oak_planks\", \"pattern\": \"solid|hollow|box|line|flat\", \"x1\": 0, \"y1\": 0, \"z1\": 0, \"x2\": 5, \"y2\": 5, \"z2\": 5 } // Operation\n"
            + "  ]\n"
            + "}\n"
            + "Workflows:\n"
            + "- default: Standard block-by-block placement.\n"
            + "- filling: Use 'solid' pattern for large cuboid fills (must provide x1,y1,z1 and x2,y2,z2).\n"
            + "- simplePatterns: Use complex patterns like 'hollow' (walls only), 'box' (walls+floor+ceiling), 'line' (axis-aligned), or 'flat' (thin surface).\n"
            + "Block IDs must be valid Minecraft 1.21 IDs (e.g., 'minecraft:oak_stairs[facing=north]').\n"
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
