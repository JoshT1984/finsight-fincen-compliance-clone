package com.skillstorm.finsight.documents_cases.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
// @EnableWebSecurity
// @Profile("!dev")
public class SecurityConfig {

    private final JwtConfig jwtConfig;

    public SecurityConfig(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        // CASES: all roles can READ; only analysts can change state (refer/close)
                        .requestMatchers(HttpMethod.GET, "/api/cases/**")
                        .hasAnyRole("ANALYST", "LAW_ENFORCEMENT_USER", "COMPLIANCE_USER")
                        .requestMatchers(HttpMethod.POST, "/api/cases/*/refer", "/api/cases/*/close")
                        .hasRole("ANALYST")
                        .requestMatchers(HttpMethod.POST, "/api/cases/**")
                        .hasRole("ANALYST")

                        // CASE NOTES: all roles can READ; only analysts can create/update/delete
                        .requestMatchers(HttpMethod.GET, "/api/case-notes/**")
                        .hasAnyRole("ANALYST", "LAW_ENFORCEMENT_USER", "COMPLIANCE_USER")
                        .requestMatchers(HttpMethod.POST, "/api/case-notes/**")
                        .hasRole("ANALYST")
                        .requestMatchers(HttpMethod.PATCH, "/api/case-notes/**")
                        .hasRole("ANALYST")
                        .requestMatchers(HttpMethod.DELETE, "/api/case-notes/**")
                        .hasRole("ANALYST")
                        .requestMatchers("/api/documents/**", "/api/audit-events/**").authenticated()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtConfig.jwtDecoder(jwtConfig.jwtPublicKey()))
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)));

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200", "https://d2aq49aewiq0a8.cloudfront.net"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}