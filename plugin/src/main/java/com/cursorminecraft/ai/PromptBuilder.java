package com.cursorminecraft.ai;

import com.cursorminecraft.util.Logger;
import io.github.sashirestela.openai.domain.chat.Message;
import io.github.sashirestela.openai.domain.chat.Role;
import java.util.ArrayList;
import java.util.List;

public class PromptBuilder {

    private static final String SYSTEM_PROMPT = "You are an expert Minecraft builder AI. Your goal is to generate architectural designs in VoxelJS JSON format. "
            +
            "Output ONLY a JSON array of blocks where each block has {x, y, z, type}. " +
            "x, y, z are relative offsets from the build origin (0,0,0). " +
            "type is the Minecraft block ID (e.g., 'minecraft:oak_planks'). " +
            "Use valid Minecraft 1.21 block IDs. " +
            "Support directional blocks using states in brackets, e.g., 'minecraft:oak_stairs[facing=north]'. " +
            "Be creative, sophisticated, and structurally sound.";

    public static List<Message> buildPrompt(String userPrompt, String context) {
        List<Message> messages = new ArrayList<>();
        messages.add(Message.of(Role.SYSTEM, SYSTEM_PROMPT));

        if (context != null && !context.isEmpty()) {
            messages.add(Message.of(Role.ASSISTANT, "Existing build context: " + context));
        }

        messages.add(Message.of(Role.USER, userPrompt));
        return messages;
    }
}
