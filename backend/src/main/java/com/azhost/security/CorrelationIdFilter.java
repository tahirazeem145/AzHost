package com.azhost.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class CorrelationIdFilter implements Filter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String MDC_KEY = "requestId";
    private static final Pattern UUID_OR_ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9-]{8,50}$");

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            String requestId = httpRequest.getHeader(REQUEST_ID_HEADER);
            if (requestId == null || requestId.trim().isEmpty() || !UUID_OR_ALPHANUMERIC_PATTERN.matcher(requestId).matches()) {
                requestId = UUID.randomUUID().toString();
            }

            MDC.put(MDC_KEY, requestId);
            httpResponse.setHeader(REQUEST_ID_HEADER, requestId);

            try {
                chain.doFilter(request, response);
            } finally {
                MDC.remove(MDC_KEY);
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {}
}
