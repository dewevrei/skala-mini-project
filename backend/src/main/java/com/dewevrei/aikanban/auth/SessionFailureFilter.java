package com.dewevrei.aikanban.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SessionFailureFilter extends OncePerRequestFilter {

    private static final String ERROR_JSON = "{\"success\":false,\"code\":\"SESSION_SERVICE_UNAVAILABLE\","
            + "\"message\":\"로그인 서비스를 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.\",\"data\":null}";

    private final SessionFailureClassifier classifier;
    private final String frontendUrl;

    public SessionFailureFilter(SessionFailureClassifier classifier, String frontendUrl) {
        this.classifier = classifier;
        this.frontendUrl = frontendUrl;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } catch (RuntimeException exception) {
            if (!classifier.isSessionStoreFailure(exception)) {
                throw exception;
            }
            if (request.getRequestURI().equals("/api/v1/auth/logout")) {
                AuthController.expireSessionCookie(response);
            }
            if (isOAuthFlow(request.getRequestURI())) {
                response.sendRedirect(frontendUrl + "/login?error=session-service-unavailable");
                return;
            }
            response.resetBuffer();
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(ERROR_JSON);
        }
    }

    private boolean isOAuthFlow(String uri) {
        return uri.startsWith("/oauth2/") || uri.startsWith("/login/oauth2/");
    }
}
