package com.skillstorm.finsight.identity_auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private LoginSuccessHandler loginSuccessHandler;
    private final JwtConfig jwtConfig;

    public SecurityConfig(LoginSuccessHandler loginSuccessHandler, JwtConfig jwtConfig) {
        this.loginSuccessHandler = loginSuccessHandler;
        this.jwtConfig = jwtConfig;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**").authenticated() // everything under /api requires JWT
                        .anyRequest().permitAll())
                .httpBasic(Customizer.withDefaults()) // basic login
                .oauth2Login(oauth2 -> oauth2.successHandler(loginSuccessHandler)) // OAuth login
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtConfig.jwtDecoder(jwtConfig.jwtPublicKey()))));

        return http.build();
    }
}
