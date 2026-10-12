package com.james.taskflow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)   // right after RequestIdFilter
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS =
            Set.of("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/refresh");

    private record Window(long startMillis, AtomicInteger count) {}

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowMillis;

    public RateLimitFilter(@Value("${app.rate-limit.max-requests:10}") int maxRequests,
                           @Value("${app.rate-limit.window:1m}") Duration window) {
        this.maxRequests = maxRequests;
        this.windowMillis = window.toMillis();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !LIMITED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long now = System.currentTimeMillis();
        String key = request.getRemoteAddr() + "|" + request.getRequestURI();

        Window window = windows.compute(key, (k, current) -> {
            if (current == null || now - current.startMillis() >= windowMillis) {
                return new Window(now, new AtomicInteger(1));
            }
            current.count().incrementAndGet();
            return current;
        });

        if (windows.size() > 10_000) {   // keep memory bounded
            windows.entrySet().removeIf(e -> now - e.getValue().startMillis() >= windowMillis);
        }

        if (window.count().get() > maxRequests) {
            long retryAfter = Math.max(1, (window.startMillis() + windowMillis - now) / 1000);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            ProblemJson.write(response, 429, "Too Many Requests", "Too many requests: try again later");
            return;
        }
        chain.doFilter(request, response);
    }
}