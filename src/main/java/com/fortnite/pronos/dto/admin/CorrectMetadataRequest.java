package com.fortnite.pronos.dto.admin;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CorrectMetadataRequest(
    @NotNull UUID playerId, @Size(max = 50) String newUsername, String newRegion) {}
