package com.cfm.web;

import com.cfm.CFM;
import com.cfm.model.Conversation;
import com.cfm.service.ConversationService;
import com.cfm.util.Logger;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import lombok.Getter;

public class WebServer {

    @Getter
    private Javalin app;
    private final int port;
    private final ConversationService conversationService;

    public WebServer() {
        this.port = CFM.getInstance().getConfig().getInt("web_interface.port", 8080);
        this.conversationService = new ConversationService();
    }

    public void start() {
        if (!CFM.getInstance().getConfig().getBoolean("web_interface.enabled", true)) {
            Logger.info("Web interface is disabled in config.");
            return;
        }

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(Javalin.class.getClassLoader());

        try {
            app = Javalin.create(config -> {
                config.staticFiles.add("/public", Location.CLASSPATH);
            }).start(port);

            registerRoutes();
            Logger.info("Web server started on port " + port);
        } catch (Exception e) {
            Logger.error("Failed to start web server!", e);
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
    }

    private void registerRoutes() {
        // Auth Middleware
        app.before("/api/*", new AuthMiddleware());

        // API Endpoints
        app.get("/api/health", ctx -> ctx.json("{\"status\": \"ok\"}"));

        // Conversations
        app.get("/api/conversations", ctx -> {
            String userUuid = ctx.queryParam("user_uuid");
            if (userUuid == null) {
                ctx.status(400).result("Missing user_uuid");
                return;
            }
            ctx.json(CFM.getInstance().getConversationDAO().getConversationsByUser(userUuid));
        });

        app.get("/api/conversations/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            ctx.json(CFM.getInstance().getConversationDAO().getConversation(id));
        });

        app.post("/api/conversations", ctx -> {
            Conversation conv = ctx.bodyAsClass(Conversation.class);
            int id = CFM.getInstance().getConversationDAO().createConversation(conv);
            ctx.status(201).json("{\"id\": " + id + "}");
        });

        app.post("/api/conversations/{id}/messages", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            String message = ctx.bodyAsClass(MessageRequest.class).getMessage();
            ctx.future(() -> conversationService.sendMessage(id, message)
                    .thenApply(res -> ctx.json("{\"response\": \"" + res + "\"}")));
        });

        app.post("/api/conversations/{id}/mode", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            ModeRequest req = ctx.bodyAsClass(ModeRequest.class);
            conversationService.switchMode(id, Conversation.ConversationMode.valueOf(req.getMode()));
            ctx.status(200).json("{\"status\": \"ok\"}");
        });

        // Builds
        app.get("/api/builds", ctx -> {
            String convIdStr = ctx.queryParam("conversation_id");
            if (convIdStr == null) {
                ctx.status(400).result("Missing conversation_id");
                return;
            }
            int convId = Integer.parseInt(convIdStr);
            ctx.json(CFM.getInstance().getBuildDAO().getBuildsByConversation(convId));
        });

        app.get("/api/builds/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            ctx.json(CFM.getInstance().getBuildDAO().getBuild(id));
        });
    }

    public void stop() {
        if (app != null) {
            app.stop();
        }
    }

    @Getter
    private static class MessageRequest {
        private String message;
    }

    @Getter
    private static class ModeRequest {
        private String mode;
    }
}
