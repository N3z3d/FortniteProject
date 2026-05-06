package com.fortnite.pronos.dto.mapper;

import java.util.Objects;
import java.util.UUID;

import com.fortnite.pronos.domain.game.model.GameParticipant;
import com.fortnite.pronos.domain.game.model.GameRegionRule;
import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.model.Player;

/** Maps Game aggregate variants (domain and JPA entity) to {@link GameDto}. */
public final class GameDtoMapper {

  private GameDtoMapper() {}

  public static GameDto fromDomainGame(com.fortnite.pronos.domain.game.model.Game game) {
    if (game == null) {
      throw new IllegalArgumentException("Game cannot be null");
    }

    GameDto dto = new GameDto();
    dto.setId(game.getId());
    dto.setName(game.getName());
    dto.setDescription(game.getDescription());
    dto.setCreatorId(game.getCreatorId());
    dto.setMaxParticipants(game.getMaxParticipants());
    dto.setCurrentParticipantCount(countDomainParticipants(game));
    dto.setStatus(com.fortnite.pronos.model.GameStatus.valueOf(game.getStatus().name()));
    dto.setInvitationCode(game.getInvitationCode());
    dto.setInvitationCodeExpiresAt(game.getInvitationCodeExpiresAt());
    dto.setIsInvitationCodeExpired(game.isInvitationCodeExpired());
    dto.setCurrentSeason(game.getCurrentSeason());
    dto.setCreatedAt(game.getCreatedAt());
    dto.setDraftMode(game.getDraftMode());
    dto.setTeamSize(game.getTeamSize());
    dto.setTranchesEnabled(game.isTranchesEnabled());
    dto.setTrancheSize(game.getTrancheSize());
    dto.setCompetitionStart(game.getCompetitionStart());
    dto.setCompetitionEnd(game.getCompetitionEnd());
    dto.initializeCollections();

    mapDomainParticipants(dto, game.getParticipants());
    mapDomainCreator(dto, game.getCreatorId(), game.getParticipants());
    mapDomainRegionRules(dto, game.getRegionRules());
    return dto;
  }

  public static GameDto fromEntityGame(com.fortnite.pronos.model.Game game) {
    if (game == null) {
      throw new IllegalArgumentException("Game cannot be null");
    }

    GameDto dto = new GameDto();
    dto.setId(game.getId());
    dto.setName(game.getName());
    dto.setDescription(game.getDescription());
    dto.setMaxParticipants(game.getMaxParticipants());
    dto.setCurrentParticipantCount(countEntityParticipants(game));
    dto.setStatus(game.getStatus());
    dto.setInvitationCode(game.getInvitationCode());
    dto.setInvitationCodeExpiresAt(game.getInvitationCodeExpiresAt());
    dto.setIsInvitationCodeExpired(game.isInvitationCodeExpired());
    dto.setCurrentSeason(game.getCurrentSeason());
    dto.initializeCollections();

    mapEntityCreator(dto, game.getCreator());
    mapEntityRegionRules(dto, game.getRegionRules());
    mapEntityParticipants(dto, game.getParticipants());
    return dto;
  }

  private static void mapDomainCreator(
      GameDto dto,
      UUID creatorId,
      java.util.List<com.fortnite.pronos.domain.game.model.GameParticipant> participants) {
    if (participants == null) {
      return;
    }
    participants.stream()
        .filter(Objects::nonNull)
        .filter(
            participant ->
                participant.isCreator()
                    || (creatorId != null && creatorId.equals(participant.getUserId())))
        .filter(participant -> hasText(participant.getUsername()))
        .findFirst()
        .ifPresent(
            creator -> {
              dto.setCreatorUsername(creator.getUsername());
              dto.setCreatorName(creator.getUsername());
            });
  }

  private static void mapDomainRegionRules(
      GameDto dto,
      java.util.List<com.fortnite.pronos.domain.game.model.GameRegionRule> regionRules) {
    if (regionRules == null) {
      return;
    }
    for (GameRegionRule rule : regionRules) {
      if (rule == null || rule.getRegion() == null) {
        continue;
      }
      Player.Region region = toPlayerRegion(rule.getRegion());
      if (region != null) {
        dto.addRegionRule(region, rule.getMaxPlayers());
      }
    }
  }

  private static void mapDomainParticipants(
      GameDto dto,
      java.util.List<com.fortnite.pronos.domain.game.model.GameParticipant> participants) {
    if (participants == null) {
      return;
    }
    for (GameParticipant participant : participants) {
      if (participant == null || participant.getUserId() == null) {
        continue;
      }
      dto.addParticipant(participant.getUserId(), participant.getUsername());
    }
  }

  private static void mapEntityCreator(GameDto dto, com.fortnite.pronos.model.User creator) {
    if (creator == null) {
      return;
    }
    dto.setCreatorId(creator.getId());
    dto.setCreatorUsername(creator.getUsername());
    dto.setCreatorName(creator.getUsername());
  }

  private static void mapEntityRegionRules(
      GameDto dto, java.util.List<com.fortnite.pronos.model.GameRegionRule> regionRules) {
    if (regionRules == null) {
      return;
    }
    for (com.fortnite.pronos.model.GameRegionRule rule : regionRules) {
      if (rule == null || rule.getRegion() == null) {
        continue;
      }
      dto.addRegionRule(rule.getRegion(), rule.getMaxPlayers());
    }
  }

  private static void mapEntityParticipants(
      GameDto dto, java.util.List<com.fortnite.pronos.model.GameParticipant> participants) {
    if (participants == null) {
      return;
    }
    for (com.fortnite.pronos.model.GameParticipant participant : participants) {
      if (participant == null || participant.getUser() == null) {
        continue;
      }
      dto.addParticipant(participant.getUser().getId(), participant.getUser().getUsername());
    }
  }

  private static Player.Region toPlayerRegion(PlayerRegion region) {
    if (region == null) {
      return null;
    }
    try {
      return Player.Region.valueOf(region.name());
    } catch (IllegalArgumentException ignored) {
      return null;
    }
  }

  private static int countDomainParticipants(com.fortnite.pronos.domain.game.model.Game game) {
    return (int)
        game.getParticipants().stream()
            .filter(Objects::nonNull)
            .filter(participant -> participant.getUserId() != null)
            .count();
  }

  private static int countEntityParticipants(com.fortnite.pronos.model.Game game) {
    return (int)
        game.getParticipants().stream()
            .filter(Objects::nonNull)
            .filter(participant -> participant.getUser() != null)
            .count();
  }

  private static boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }
}
