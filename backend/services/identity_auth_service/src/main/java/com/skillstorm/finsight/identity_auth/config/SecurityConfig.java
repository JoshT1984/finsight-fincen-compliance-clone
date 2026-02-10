package com.skillstorm.finsight.identity_auth.config;

import com.skillstorm.finsight.identity_auth.aspects.SecurityAuditLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final SecurityAuditLogger securityAuditLogger;

    private final JwtConfig jwtConfig;

    public SecurityConfig(JwtConfig jwtConfig, SecurityAuditLogger securityAuditLogger) {
        this.jwtConfig = jwtConfig;
        this.securityAuditLogger = securityAuditLogger;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter,
            LoginSuccessHandler loginSuccessHandler)
            throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers("/auth/oauth/link/**").authenticated()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/error/**").permitAll()
                        .anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2.successHandler(loginSuccessHandler)) // OAuth
                // login
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtConfig.jwtDecoder(jwtConfig.jwtPublicKey()))
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)));

        return http.build();
    }

    @Bean
    public LoginSuccessHandler loginSuccessHandler(
            org.springframework.security.oauth2.jwt.JwtEncoder jwtEncoder,
            com.skillstorm.finsight.identity_auth.services.OauthIdentityService oauthIdentityService) {
        return new LoginSuccessHandler(jwtEncoder, oauthIdentityService, securityAuditLogger);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        configuration
                .setAllowedOrigins(java.util.List.of("http://localhost:4200", "https://d2aq49aewiq0a8.cloudfront.net"));
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}