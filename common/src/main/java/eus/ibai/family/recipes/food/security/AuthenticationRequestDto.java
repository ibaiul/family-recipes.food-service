package eus.ibai.family.recipes.food.security;

import jakarta.validation.constraints.NotBlank;

public record AuthenticationRequestDto(@NotBlank String username, @NotBlank String password) {}
