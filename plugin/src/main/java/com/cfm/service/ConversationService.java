package com.cfm.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.cfm.CFM;
import com.cfm.ai.AIClient;
import com.cfm.ai.PromptBuilder;
import com.cfm.database.dao.BuildDAO;
import com.cfm.database.dao.ConversationDAO;
import com.cfm.model.Build;
import com.cfm.model.Conversation;
import com.cfm.schema.VoxelSchemaParser;
import com.cfm.util.Logger;

public class ConversationService {

    public CompletableFuture<String> sendMessage(int conversationId, String userMessage) {
        Conversation conv = CFM.getInstance().getConversationDAO().getConversation(conversationId);
        if (conv == null) {
            return CompletableFuture.completedFuture("Error: Conversation not found.");
        }

        Conversation.ConversationMode mode = conv.getCurrentMode();
        PromptBuilder.PromptMode promptMode = (mode == Conversation.ConversationMode.BUILDING)
                ? PromptBuilder.PromptMode.BUILDING
                : PromptBuilder.PromptMode.PLANNING;

        // Get history (TODO: Need real history fetching from DB/AIClient context)
        // For now, let's assume we pass the *last* assistant message as context if in
        // BUILDING mode
        // In a real system, we'd fetch the full chat log.
        // TEMPORARY: Just passing null as context for now, reliance on PromptBuilder to
        // set system prompt.
        List<String> promptMessages = PromptBuilder.buildPrompt(userMessage, null, promptMode);

        return CFM.getInstance().getAiClient().chat(promptMessages).thenApply(response -> {
            if (response != null) {
                // If in BUILDING mode, try to parse as JSON
                if (mode == Conversation.ConversationMode.BUILDING) {
                    try {
                        // Attempt to parse just to validate it's a build
                        VoxelSchemaParser.BuildSchema schema = VoxelSchemaParser.parseFullSchema(response);
                        if (!schema.getBlocks().isEmpty()) {
                            // Save build
                            Build build = Build.builder()
                                    .conversationId(conversationId)
                                    .schemaData(response)
                                    .iterationNumber(1) // TODO: track actual iteration
                                    .status(Build.BuildStatus.COMPLETED) // Mark valid
                                    .build();
                            CFM.getInstance().getBuildDAO().createBuild(build);
                        }
                    } catch (Exception e) {
                        // Valid JSON but invalid schema, or just text?
                        // With "BUILDING" mode, we EXPECT JSON.
                        Logger.warn("AI returned invalid JSON in BUILD mode: " + e.getMessage());
                    }
                }
                // In PLANNING mode, we just return the text (plan).
                // Future: Store the plan text in metadata or a 'plans' table?
                // For now, it stays in the chat history (which we need to persist to DB
                // eventually).
            }
            return response;
        });
    }

    public void switchMode(int conversationId, Conversation.ConversationMode newMode) {
        CFM.getInstance().getConversationDAO().updateConversationMode(conversationId, newMode);
    }
}
