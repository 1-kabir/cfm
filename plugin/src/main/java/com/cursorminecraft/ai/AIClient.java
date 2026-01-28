package com.cursorminecraft.ai;

import com.cursorminecraft.CursorMinecraft;
import com.cursorminecraft.util.Logger;
import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.chat.ChatRequest;
import io.github.sashirestela.openai.domain.chat.ChatResponse;
import io.github.sashirestela.openai.domain.chat.Message;
import lombok.Getter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AIClient {

    @Getter
    private SimpleOpenAI openAI;
    private final String modelName;

    public AIClient() {
        String endpoint = CursorMinecraft.getInstance().getConfig().getString("ai.endpoint_url");
        String apiKey = CursorMinecraft.getInstance().getConfig().getString("ai.api_key");
        this.modelName = CursorMinecraft.getInstance().getConfig().getString("ai.model_name");

        this.openAI = SimpleOpenAI.builder()
                .apiKey(apiKey)
                .baseUrl(endpoint)
                .build();
    }

    public CompletableFuture<String> chat(List<Message> messages) {
        ChatRequest chatRequest = ChatRequest.builder()
                .model(modelName)
                .messages(messages)
                .responseFormat(io.github.sashirestela.openai.domain.chat.ResponseFormat.JSON)
                .build();

        return openAI.chatCompletions().create(chatRequest)
                .thenApply(response -> {
                    if (response != null && !response.getChoices().isEmpty()) {
                        return response.firstContent();
                    }
                    return null;
                })
                .exceptionally(throwable -> {
                    Logger.error("AI Chat failed!", throwable);
                    return null;
                });
    }
}
