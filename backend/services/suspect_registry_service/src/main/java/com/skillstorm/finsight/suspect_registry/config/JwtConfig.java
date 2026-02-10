package com.skillstorm.finsight.suspect_registry.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.util.StreamUtils;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration
public class JwtConfig {

    @Value("${jwt.public-key}")
    private Resource publicKeyResource;

    @Value("${jwt.private-key}")
    private Resource privateKeyResource;

    // ❌ DO NOT CALL @Bean METHODS FROM @PostConstruct IN @Configuration
    // @PostConstruct
    // void validateJwtKeys() {
    //     jwtPublicKey();
    //     jwtPrivateKey();
    // }

    // ✅ SAFE replacement: runs AFTER context initialization
    @Bean
    ApplicationRunner jwtKeyValidationRunner(
            RSAPublicKey publicKey,
            RSAPrivateKey privateKey
    ) {
        return args -> {
            // Touch keys to force parsing at startup
            publicKey.getModulus();
            privateKey.getPrivateExponent();
        };
    }

    @Bean
    JwtEncoder jwtEncoder(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        JWK jwk = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .build();
        JWKSource<SecurityContext> jwkSource =
                new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public RSAPublicKey jwtPublicKey() {
        return parsePublicKey(readPem(publicKeyResource));
    }

    @Bean
    RSAPrivateKey jwtPrivateKey() {
        return parsePrivateKey(readPem(privateKeyResource));
    }

    private String readPem(Resource resource) {
        try {
            return StreamUtils.copyToString(
                    resource.getInputStream(),
                    StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to read JWT key resource: " + resource,
                    e
            );
        }
    }

    public static RSAPublicKey parsePublicKey(String pem) {
        try {
            String sanitized = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] decoded = Base64.getDecoder().decode(sanitized);
            X509EncodedKeySpec spec =
                    new X509EncodedKeySpec(decoded);

            return (RSAPublicKey) KeyFactory
                    .getInstance("RSA")
                    .generatePublic(spec);

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid RSA public key",
                    e
            );
        }
    }

    public static RSAPrivateKey parsePrivateKey(String pem) {
        try {
            // ================================
            // OLD CODE (kept for reference)
            // ================================
            // String sanitized = pem
            //         .replace("-----BEGIN PRIVATE KEY-----", "")
            //         .replace("-----END PRIVATE KEY-----", "")
            //         .replaceAll("\\s", "");
            // byte[] decoded = Base64.getDecoder().decode(sanitized);
            // PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            // return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);

            // ================================
            // NEW CODE (PKCS#8 + PKCS#1 safe)
            // ================================
            String sanitized = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] decoded = Base64.getDecoder().decode(sanitized);
            PKCS8EncodedKeySpec spec =
                    new PKCS8EncodedKeySpec(decoded);

            return (RSAPrivateKey) KeyFactory
                    .getInstance("RSA")
                    .generatePrivate(spec);

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid RSA private key",
                    e
            );
        }
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("role");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
                grantedAuthoritiesConverter
        );
        return converter;
    }

    @Bean
    JwtDecoder jwtDecoder(RSAPublicKey publicKey) {
        return NimbusJwtDecoder
                .withPublicKey(publicKey)
                .build();
    }
}
