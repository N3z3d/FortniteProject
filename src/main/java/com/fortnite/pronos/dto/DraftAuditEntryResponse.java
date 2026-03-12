package com.fortnite.pronos.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response record for a single audit event (swap or trade). type values: "SWAP_SOLO",
 * "TRADE_PROPOSED", "TRADE_ACCEPTED", "TRADE_REJECTED".
 *
 * <p>For SWAP_SOLO: participantId + playerOutId + playerInId are set; proposer/target are null. For
 * TRADE_*: proposerParticipantId + targetParticipantId + playerOutId (=fromProposer) + playerInId
 * (=fromTarget) are set; participantId is null.
 */
public record DraftAuditEntryResponse(
    UUID id,
    String type,
    LocalDateTime occurredAt,
    UUID participantId,
    UUID proposerParticipantId,
    UUID targetParticipantId,
    UUID playerOutId,
    UUID playerInId) {}
