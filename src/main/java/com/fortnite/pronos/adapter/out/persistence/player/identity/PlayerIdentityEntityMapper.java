package com.fortnite.pronos.adapter.out.persistence.player.identity;

import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.player.identity.model.MetadataCorrection;
import com.fortnite.pronos.domain.player.identity.model.PlayerIdentityEntry;

@Component
public class PlayerIdentityEntityMapper {

  public PlayerIdentityEntry toDomain(PlayerIdentityEntity entity) {
    return PlayerIdentityEntry.restore(
        entity.getId(),
        entity.getPlayerId(),
        entity.getPlayerUsername(),
        entity.getPlayerRegion(),
        entity.getEpicId(),
        entity.getStatus(),
        entity.getConfidenceScore(),
        entity.getResolvedBy(),
        entity.getResolvedAt(),
        entity.getRejectedAt(),
        entity.getRejectionReason(),
        entity.getCreatedAt(),
        new MetadataCorrection(
            entity.getCorrectedUsername(),
            entity.getCorrectedRegion(),
            entity.getCorrectedBy(),
            entity.getCorrectedAt()));
  }

  public PlayerIdentityEntity toEntity(PlayerIdentityEntry domain) {
    return PlayerIdentityEntity.builder()
        .id(domain.getId())
        .playerId(domain.getPlayerId())
        .playerUsername(domain.getPlayerUsername())
        .playerRegion(domain.getPlayerRegion())
        .epicId(domain.getEpicId())
        .status(domain.getStatus())
        .confidenceScore(domain.getConfidenceScore())
        .resolvedBy(domain.getResolvedBy())
        .resolvedAt(domain.getResolvedAt())
        .rejectedAt(domain.getRejectedAt())
        .rejectionReason(domain.getRejectionReason())
        .createdAt(domain.getCreatedAt())
        .correctedUsername(domain.getCorrectedUsername())
        .correctedRegion(domain.getCorrectedRegion())
        .correctedBy(domain.getCorrectedBy())
        .correctedAt(domain.getCorrectedAt())
        .build();
  }
}
