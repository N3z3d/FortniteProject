package com.fortnite.pronos.dto;

import java.util.UUID;

public record DraftOrderEntryResponse(UUID id, int draftOrder, UUID userId) {}
