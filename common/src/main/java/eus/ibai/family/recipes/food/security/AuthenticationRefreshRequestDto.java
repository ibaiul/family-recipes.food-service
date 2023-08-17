package eus.ibai.family.recipes.food.security;

import jakarta.validation.constraints.NotBlank;

public record AuthenticationRefreshRequestDto(@NotBlank String refreshToken) {}
