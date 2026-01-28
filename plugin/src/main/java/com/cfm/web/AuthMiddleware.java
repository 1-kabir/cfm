package com.cfm.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

import com.cfm.CFM;

import java.util.Base64;

public class AuthMiddleware implements Handler {

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        String authHeader = ctx.header("Authorization");

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            ctx.status(401).header("WWW-Authenticate", "Basic realm=\"CursorAI\"").result("Unauthorized");
            return;
        }

        String base64Credentials = authHeader.substring(6);
        String credentials = new String(Base64.getDecoder().decode(base64Credentials));
        String[] values = credentials.split(":", 2);

        if (values.length != 2) {
            ctx.status(401).result("Invalid credentials format");
            return;
        }

        String username = values[0];
        String password = values[1];

        String configUser = CFM.getInstance().getConfig().getString("web_interface.auth.username", "admin");
        String configPass = CFM.getInstance().getConfig().getString("web_interface.auth.password",
                "changeme");

        if (!username.equals(configUser) || !password.equals(configPass)) {
            ctx.status(401).result("Invalid username or password");
            return;
        }
    }
}
