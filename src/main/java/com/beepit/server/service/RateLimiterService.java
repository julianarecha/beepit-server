package com.beepit.server.service;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class RateLimiterService {
    
    private static final Logger LOG = LoggerFactory.getLogger(RateLimiterService.class);
    
    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();
    private final RateLimiterConfig config;
    
    public RateLimiterService() {
        // Permitir 10 mensajes por segundo por usuario
        this.config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .limitForPeriod(10)
            .timeoutDuration(Duration.ofMillis(100))
            .build();
    }
    
    public boolean tryAcquire(String userId) {
        RateLimiter rateLimiter = rateLimiters.computeIfAbsent(userId, 
            id -> RateLimiter.of("user-" + id, config));
        
        boolean acquired = rateLimiter.acquirePermission();
        if (!acquired) {
            LOG.warn("Rate limit exceeded for user: {}", userId);
        }
        return acquired;
    }
    
    public void cleanup(String userId) {
        rateLimiters.remove(userId);
    }
}
