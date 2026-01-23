package com.fortnite.pronos.dto;

import java.util.UUID;

public record DraftNextParticipantResponse(
    UUID id, int draftOrder, UUID userId, int currentPick, int currentRound) {}
