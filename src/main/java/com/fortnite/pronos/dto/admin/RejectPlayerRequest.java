package com.fortnite.pronos.dto.admin;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record RejectPlayerRequest(@NotNull UUID playerId, String reason) {}
