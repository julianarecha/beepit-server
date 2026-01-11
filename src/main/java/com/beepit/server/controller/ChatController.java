package com.beepit.server.controller;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import reactor.core.publisher.Mono;

import java.util.Map;

@Controller("/api")
public class ChatController {
    
    @Get("/health")
    public Mono<Map<String, String>> health() {
        return Mono.just(Map.of(
            "status", "UP",
            "service", "Beepit Server",
            "version", "1.0.0"
        ));
    }
}
