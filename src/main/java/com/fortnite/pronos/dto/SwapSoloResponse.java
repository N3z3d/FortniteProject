package com.fortnite.pronos.dto;

import java.util.UUID;

/** Response returned after a successful solo swap. */
public record SwapSoloResponse(
    UUID draftId, UUID participantId, UUID playerOutId, UUID playerInId) {}
