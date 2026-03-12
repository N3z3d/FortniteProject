package com.fortnite.pronos.dto;

import java.util.UUID;

/** Request body for a solo player swap operation. */
public record SwapSoloRequest(UUID playerOutId, UUID playerInId) {}
