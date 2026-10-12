package com.james.taskflow.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    SecretKey jwtSecretKey(JwtProperties props) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(props.secret());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "JWT_SECRET is not valid base64. It must be the generated output (only A-Z a-z 0-9 + / =), not a command or placeholder.");
        }
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must decode to at least 32 bytes (256 bits).");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey, JwtProperties props) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // default validators: expiry (with 60s clock skew) + the issuer must match ours
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(props.issuer()));
        return decoder;
    }
}