package com.beepit.server.controller;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.views.View;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;

import java.util.Map;

@Controller
public class TestController {

    @Get("/test-chat")
    @View("test-chat")
    public Map<String, String> testChat(@Nullable HttpRequest<?> request) {
        String protocol = request != null && request.isSecure() ? "https" : "http";
        String wsProtocol = request != null && request.isSecure() ? "wss" : "ws";
        
        // Forzar localhost para evitar problemas con IPv6
        String host = "localhost";
        int port = request != null ? request.getServerAddress().getPort() : 8080;
        
        String apiBase = String.format("%s://%s:%d", protocol, host, port);
        String wsBase = String.format("%s://%s:%d", wsProtocol, host, port);
        
        return Map.of(
            "apiBase", apiBase,
            "wsBase", wsBase
        );
    }
}
