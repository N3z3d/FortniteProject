package com.fortnite.pronos.dto.admin;

import java.util.UUID;

/** Request body for admin player assignment to a roster. */
public record AdminRosterAssignRequest(UUID participantUserId, UUID playerId) {}
