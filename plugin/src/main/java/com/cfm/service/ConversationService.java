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

        // Get history (simplified for now)
        List<String> history = PromptBuilder.buildPrompt(userMessage, null);

        return CFM.getInstance().getAiClient().chat(history).thenApply(response -> {
            if (response != null) {
                // Check if response contains a build schema
                try {
                    VoxelSchemaParser.BuildSchema schema = VoxelSchemaParser.parseFullSchema(response);
                    if (!schema.getOperations().isEmpty()) {
                        // Save build for this conversation
                        Build build = Build.builder()
                                .conversationId(conversationId)
                                .schemaData(response)
                                .iterationNumber(1) // TODO: track iterations
                                .build();
                        CFM.getInstance().getBuildDAO().createBuild(build);
                    }
                } catch (Exception e) {
                    // Not a build schema, just a message
                }
            }
            return response;
        });
    }
}
