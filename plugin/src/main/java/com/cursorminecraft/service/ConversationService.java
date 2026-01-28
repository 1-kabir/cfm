package com.cursorminecraft.service;

import com.cursorminecraft.CursorMinecraft;
import com.cursorminecraft.ai.PromptBuilder;
import com.cursorminecraft.database.dao.BuildDAO;
import com.cursorminecraft.database.dao.ConversationDAO;
import com.cursorminecraft.model.Build;
import com.cursorminecraft.model.Conversation;
import com.cursorminecraft.schema.VoxelSchemaParser;
import com.cursorminecraft.util.Logger;
import io.github.sashirestela.openai.domain.chat.Message;
import io.github.sashirestela.openai.domain.chat.Role;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ConversationService {

    private final ConversationDAO conversationDAO;
    private final BuildDAO buildDAO;

    public ConversationService() {
        this.conversationDAO = CursorMinecraft.getInstance().getConversationDAO();
        this.buildDAO = CursorMinecraft.getInstance().getBuildDAO();
    }

    public CompletableFuture<String> sendMessage(int conversationId, String userMessage) {
        Conversation conversation = conversationDAO.getConversation(conversationId);
        if (conversation == null) {
            return CompletableFuture.completedFuture("Conversation not found.");
        }

        // Get history (simplified for now)
        List<Message> history = PromptBuilder.buildPrompt(userMessage, null);

        return CursorMinecraft.getInstance().getAiClient().chat(history).thenApply(response -> {
            if (response != null) {
                // Check if response contains a build schema
                try {
                    List<VoxelSchemaParser.VoxelBlock> blocks = VoxelSchemaParser.parseSchema(response);
                    if (!blocks.isEmpty()) {
                        createBuildFromResponse(conversationId, userMessage, response, blocks.size());
                    }
                } catch (Exception ignored) {
                    // Not a build schema, just a normal chat message
                }
            }
            return response;
        });
    }

    private void createBuildFromResponse(int conversationId, String prompt, String schema, int blockCount) {
        Build build = Build.builder()
                .conversationId(conversationId)
                .iterationNumber(1) // TODO: Increment based on existing builds
                .buildName("AI Generated Build")
                .prompt(prompt)
                .schemaData(schema)
                .status(Build.BuildStatus.PENDING)
                .blockCount(blockCount)
                .build();
        buildDAO.createBuild(build);
    }
}
