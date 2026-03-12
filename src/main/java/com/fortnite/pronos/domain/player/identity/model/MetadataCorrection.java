package com.fortnite.pronos.domain.player.identity.model;

import java.time.LocalDateTime;

/** Value Object grouping the four metadata-correction fields of a {@link PlayerIdentityEntry}. */
public record MetadataCorrection(
    String correctedUsername,
    String correctedRegion,
    String correctedBy,
    LocalDateTime correctedAt) {}
