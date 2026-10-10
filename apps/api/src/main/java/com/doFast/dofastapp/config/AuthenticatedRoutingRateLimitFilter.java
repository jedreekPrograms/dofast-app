package com.doFast.dofastapp.config;

import com.doFast.dofastapp.user.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Objects;
import java.util.regex.Pattern;

public class AuthenticatedRoutingRateLimitFilter extends OncePerRequestFilter {

    private static final String CREATE_QUOTE_PATH = "/routing/quotes";
    private static final Pattern MODE_ESTIMATES_PATH = Pattern.compile(
            "^/routing/quotes/[^/]+/mode-estimates$"
    );
    private static final int CREATE_QUOTE_PROVIDER_CALLS = 1;
    private static final int MODE_ESTIMATES_PROVIDER_CALLS = 2;

    private final Clock clock;
    private final InMemoryFixedWindowRateLimiter rateLimiter;

    public AuthenticatedRoutingRateLimitFilter(
            int maxProviderCalls,
            long windowSeconds,
            int maxEntries
    ) {
        this(maxProviderCalls, windowSeconds, maxEntries, Clock.systemUTC());
    }

    AuthenticatedRoutingRateLimitFilter(
            int maxProviderCalls,
            long windowSeconds,
            int maxEntries,
            Clock clock
    ) {
        if (maxProviderCalls < MODE_ESTIMATES_PROVIDER_CALLS || windowSeconds < 1 || maxEntries < 100) {
            throw new IllegalArgumentException("Invalid authenticated routing rate-limit configuration");
        }
        this.rateLimiter = new InMemoryFixedWindowRateLimiter(maxProviderCalls, windowSeconds, maxEntries);
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return providerCallCost(request) == 0;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User user)) {
            filterChain.doFilter(request, response);
            return;
        }

        int providerCallCost = providerCallCost(request);
        String accountKey = user.getId() == null ? "authenticated-without-id" : user.getId().toString();
        InMemoryFixedWindowRateLimiter.Decision decision = rateLimiter.register(
                accountKey,
                providerCallCost,
                clock.instant()
        );
        if (!decision.allowed()) {
            response.setStatus(429);
            response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private int providerCallCost(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (HttpMethod.POST.matches(request.getMethod()) && CREATE_QUOTE_PATH.equals(path)) {
            return CREATE_QUOTE_PROVIDER_CALLS;
        }
        if (HttpMethod.GET.matches(request.getMethod()) && MODE_ESTIMATES_PATH.matcher(path).matches()) {
            return MODE_ESTIMATES_PROVIDER_CALLS;
        }
        return 0;
    }

}
