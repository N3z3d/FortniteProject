package com.fortnite.pronos.dto;

import java.util.UUID;

/** Response returned after a draft trade proposal is created or updated (FR-34, FR-35). */
public record DraftTradeProposalResponse(
    UUID tradeId,
    UUID draftId,
    UUID proposerParticipantId,
    UUID targetParticipantId,
    UUID playerFromProposerId,
    UUID playerFromTargetId,
    String status) {}
