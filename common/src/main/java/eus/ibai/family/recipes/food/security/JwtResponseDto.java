package eus.ibai.family.recipes.food.security;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JwtResponseDto(@JsonProperty("access_token") String accessToken, @JsonProperty("token_type") String tokenType,
                             @JsonProperty("expires_in") int expiresIn, String scope, @JsonProperty("refresh_token") String refreshToken) {

    public JwtResponseDto(String accessToken, String tokenType, int expiresIn, String refreshToken) {
        this(accessToken, tokenType, expiresIn, "", refreshToken);
    }
}
