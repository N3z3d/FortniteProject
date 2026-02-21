package com.fortnite.pronos.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record NavigationTrackingRequestDto(@NotBlank String path) {}
