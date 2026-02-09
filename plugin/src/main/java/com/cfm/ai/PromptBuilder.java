package com.cfm.ai;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.cfm.CFM;
import com.cfm.util.Logger;

public class PromptBuilder {

    public enum PromptMode {
        PLANNING,
        BUILDING
    }

    public static List<String> buildPrompt(String userPrompt, String context, PromptMode mode) {
        List<String> messages = new ArrayList<>();

        String systemPrompt = loadPromptFile(mode);
        messages.add(systemPrompt);

        if (context != null && !context.isEmpty()) {
            messages.add("Context from previous turn:\n" + context);
        }

        messages.add(userPrompt);
        return messages;
    }

    private static String loadPromptFile(PromptMode mode) {
        String fileName = (mode == PromptMode.PLANNING) ? "plan.md" : "build.md";
        File file = new File(CFM.getInstance().getDataFolder().getParentFile(), fileName); // Plugin root

        try {
            if (file.exists()) {
                return Files.readString(file.toPath());
            } else {
                Logger.error("Prompt file not found: " + file.getAbsolutePath());
                return "Error: System prompt file missing.";
            }
        } catch (IOException e) {
            Logger.error("Failed to read prompt file: " + fileName, e);
            return "Error: Failed to read system prompt.";
        }
    }
}
