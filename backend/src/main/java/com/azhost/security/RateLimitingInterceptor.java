package com.azhost.security;

import com.azhost.config.AzHostBuildProperties;
import com.azhost.service.MetricsService;
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
    private final MetricsService metricsService;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitingInterceptor(AzHostBuildProperties properties, MetricsService metricsService) {
        this.properties = properties;
        this.metricsService = metricsService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!properties.getRateLimit().isEnabled()) {
            return true;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // Exclude GitHub webhook from raw IP rate limiting (signature/idempotency checked)
        if (path.startsWith("/api/webhooks/github")) {
            return true;
        }

        // Project creation limit: 5/minute
        // Build creation limit: 10/minute
        // Deployment creation limit: 10/minute
        double capacity = -1;
        
        if (method.equalsIgnoreCase("POST") && path.equals("/api/projects")) {
            capacity = 5.0;
        } else if (method.equalsIgnoreCase("POST") && path.matches("/api/projects/[^/]+/builds")) {
            capacity = 10.0;
        } else if (method.equalsIgnoreCase("POST") && path.matches("/api/projects/[^/]+/deployments")) {
            capacity = 10.0;
        } else if (path.contains("/deployments") || path.contains("/builds") || path.contains("/github")) {
            // General fallback limit
            capacity = properties.getRateLimit().getRequestsPerMinute();
        }

        if (capacity > 0) {
            String identityKey = getIdentityKey(request);
            String bucketKey = identityKey + ":" + path;

            // Bounded state: prevent map memory explosion
            if (buckets.size() > 5000) {
                buckets.clear();
            }

            double finalCap = capacity;
            TokenBucket bucket = buckets.computeIfAbsent(bucketKey, k -> new TokenBucket(
                    finalCap,
                    finalCap / 60.0
            ));

            if (!bucket.tryConsume()) {
                metricsService.incrementRateLimitRejections();
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Too many requests. Please try again later.");
                return false;
            }
        }

        return true;
    }

    private String getIdentityKey(HttpServletRequest request) {
        java.security.Principal principal = request.getUserPrincipal();
        if (principal != null && principal.getName() != null) {
            return "user:" + principal.getName();
        }
        return "ip:" + getClientIp(request);
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
