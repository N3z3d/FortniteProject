package com.fortnite.pronos.dto;

import java.util.UUID;

public record DraftAdvanceResponse(
    boolean success,
    String message,
    int currentRound,
    int currentPick,
    boolean isComplete,
    UUID nextParticipantId,
    Integer nextDraftOrder) {}
