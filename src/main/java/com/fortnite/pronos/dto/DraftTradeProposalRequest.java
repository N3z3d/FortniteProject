package com.fortnite.pronos.dto;

import java.util.UUID;

/** Request body for a 1v1 draft trade proposal (FR-34). */
public record DraftTradeProposalRequest(
    UUID targetParticipantId, UUID playerFromProposerId, UUID playerFromTargetId) {}
