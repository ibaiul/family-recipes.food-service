package eus.ibai.family.recipes.food.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("jwt")
public class JwtProperties {

    private AccessToken accessToken;

    private RefreshToken refreshToken;

    private Signature signature;

    private Encryption encryption;

    record AccessToken(int expirationTime) {}

    record RefreshToken(int expirationTime) {}

    record Signature(String secret) {}

    record Encryption(String secret) {}
}
