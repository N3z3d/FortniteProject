package com.fortnite.pronos.dto;

import java.util.UUID;

/** Request body for anonymous simultaneous-draft submission. */
public record SimultaneousSubmitRequest(UUID windowId, UUID participantId, UUID playerId) {}
