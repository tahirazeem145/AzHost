package com.azhost.security;

import com.azhost.config.AzHostBuildProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final AzHostBuildProperties properties;
    private final Map<String, TokenBucket> ipBuckets = new ConcurrentHashMap<>();

    public RateLimitingInterceptor(AzHostBuildProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!properties.getRateLimit().isEnabled()) {
            return true;
        }

        String path = request.getRequestURI();
        
        // Exclude GitHub webhook from raw IP rate limiting (already protected by HMAC/idempotency)
        if (path.startsWith("/api/webhooks/github")) {
            return true;
        }

        // Apply rate limit to deployment creation, manual build start, SCM operations
        if (path.contains("/deployments") || path.contains("/builds") || path.contains("/github")) {
            String clientIp = getClientIp(request);
            TokenBucket bucket = ipBuckets.computeIfAbsent(clientIp, k -> new TokenBucket(
                    properties.getRateLimit().getRequestsPerMinute(),
                    properties.getRateLimit().getRequestsPerMinute() / 60.0
            ));

            if (!bucket.tryConsume()) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Too many requests. Please try again later.");
                return false;
            }
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class TokenBucket {
        private final double capacity;
        private final double refillRatePerSec;
        private double tokens;
        private long lastRefillTime;

        public TokenBucket(double capacity, double refillRatePerSec) {
            this.capacity = capacity;
            this.refillRatePerSec = refillRatePerSec;
            this.tokens = capacity;
            this.lastRefillTime = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            double elapsedSec = (now - lastRefillTime) / 1000.0;
            tokens = Math.min(capacity, tokens + (elapsedSec * refillRatePerSec));
            lastRefillTime = now;
        }
    }
}
