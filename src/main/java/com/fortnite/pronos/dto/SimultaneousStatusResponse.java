package com.fortnite.pronos.dto;

import java.util.UUID;

/** Response DTO for the simultaneous-draft submission count query. */
public record SimultaneousStatusResponse(UUID draftId, UUID windowId, int submitted, int total) {}
