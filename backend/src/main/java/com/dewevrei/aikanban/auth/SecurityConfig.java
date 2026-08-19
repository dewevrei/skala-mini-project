package com.dewevrei.aikanban.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    private static final String AUTH_REQUIRED_JSON = "{\"success\":false,\"code\":\"AUTHENTICATION_REQUIRED\","
            + "\"message\":\"로그인이 필요합니다.\",\"data\":null}";
    private static final String CSRF_INVALID_JSON = "{\"success\":false,\"code\":\"CSRF_TOKEN_INVALID\","
            + "\"message\":\"요청 보안 정보가 올바르지 않습니다. 다시 시도해 주세요.\",\"data\":null}";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            GoogleOAuth2UserService oauth2UserService,
            GoogleOidcUserService oidcUserService,
            SessionFailureClassifier sessionFailureClassifier,
            @Value("${app.frontend-origin}") String frontendOrigin,
            @Value("${app.frontend-url}") String frontendUrl) throws Exception {
        HttpSessionCsrfTokenRepository csrfRepository = new HttpSessionCsrfTokenRepository();
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource(frontendOrigin)))
                .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository).csrfTokenRequestHandler(csrfHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/oauth2/**", "/login/oauth2/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, AUTH_REQUIRED_JSON))
                        .accessDeniedHandler((request, response, exception) -> {
                            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                            if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
                                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, AUTH_REQUIRED_JSON);
                            } else {
                                writeJson(response, HttpServletResponse.SC_FORBIDDEN, CSRF_INVALID_JSON);
                            }
                        }))
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oauth2UserService)
                                .oidcUserService(oidcUserService))
                        .successHandler((request, response, authentication) ->
                                response.sendRedirect(frontendUrl + "/projects"))
                        .failureHandler((request, response, exception) -> {
                            if (sessionFailureClassifier.isSessionStoreFailure(exception)) {
                                response.sendRedirect(frontendUrl + "/login?error=session-service-unavailable");
                            } else {
                                response.sendRedirect(frontendUrl + "/login?error=oauth");
                            }
                        }));
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.frontend-origin}") String frontendOrigin) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendOrigin));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN", "Accept"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    FilterRegistrationBean<SessionFailureFilter> sessionFailureFilter(
            SessionFailureClassifier classifier, @Value("${app.frontend-url}") String frontendUrl) {
        FilterRegistrationBean<SessionFailureFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SessionFailureFilter(classifier, frontendUrl));
        registration.setOrder(SessionRepositoryFilter.DEFAULT_ORDER - 1);
        return registration;
    }

    @Bean
    CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setUseSecureCookie(false);
        serializer.setSameSite("Lax");
        serializer.setCookiePath("/");
        serializer.setCookieMaxAge((int) Duration.ofDays(7).getSeconds());
        return serializer;
    }

    private static void writeJson(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(body);
    }
}
