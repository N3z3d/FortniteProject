package com.fortnite.pronos.dto.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameParticipant;
import com.fortnite.pronos.domain.game.model.GameRegionRule;
import com.fortnite.pronos.domain.game.model.GameStatus;
import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.User;

class GameDtoMapperTest {

  @Test
  void fromDomainGameMapsMainFieldsParticipantsAndRules() {
    UUID gameId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();

    GameParticipant creator =
        GameParticipant.restore(
            UUID.randomUUID(), creatorId, "creator-user", 1, now, null, true, List.of());
    GameParticipant participant =
        GameParticipant.restore(
            UUID.randomUUID(), participantId, "participant-user", 2, now, null, false, List.of());

    Game domainGame =
        Game.restore(
            gameId,
            "Domain Game",
            "description",
            creatorId,
            8,
            GameStatus.CREATING,
            now,
            null,
            null,
            "INV12345",
            now.plusDays(1),
            List.of(new GameRegionRule(UUID.randomUUID(), PlayerRegion.EU, 3)),
            List.of(creator, participant),
            null,
            false,
            5,
            null,
            2026);

    GameDto dto = GameDtoMapper.fromDomainGame(domainGame);

    assertThat(dto.getId()).isEqualTo(gameId);
    assertThat(dto.getName()).isEqualTo("Domain Game");
    assertThat(dto.getCreatorId()).isEqualTo(creatorId);
    assertThat(dto.getCreatorUsername()).isEqualTo("creator-user");
    assertThat(dto.getStatus()).isEqualTo(com.fortnite.pronos.model.GameStatus.CREATING);
    assertThat(dto.getCurrentParticipantCount()).isEqualTo(2);
    assertThat(dto.getRegionRules()).containsEntry(Player.Region.EU, 3);
    assertThat(dto.getParticipants()).containsEntry(creatorId, "creator-user");
    assertThat(dto.getParticipants()).containsEntry(participantId, "participant-user");
  }

  @Test
  void fromDomainGameResolvesCreatorByCreatorIdWhenCreatorFlagMissing() {
    UUID gameId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();

    GameParticipant creatorWithoutFlag =
        GameParticipant.restore(
            UUID.randomUUID(), creatorId, "creator-user", 1, now, null, false, List.of());

    Game domainGame =
        Game.restore(
            gameId,
            "Domain Game",
            null,
            creatorId,
            8,
            GameStatus.CREATING,
            now,
            null,
            null,
            null,
            null,
            List.of(),
            List.of(creatorWithoutFlag),
            null,
            false,
            5,
            null,
            2026);

    GameDto dto = GameDtoMapper.fromDomainGame(domainGame);

    assertThat(dto.getCreatorId()).isEqualTo(creatorId);
    assertThat(dto.getCreatorUsername()).isEqualTo("creator-user");
    assertThat(dto.getCreatorName()).isEqualTo("creator-user");
  }

  @Test
  void fromEntityGameMapsCreatorParticipantsAndRulesAndIgnoresNullEntries() {
    UUID gameId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();

    User creator = new User();
    creator.setId(creatorId);
    creator.setUsername("entity-creator");

    User participantUser = new User();
    participantUser.setId(participantId);
    participantUser.setUsername("entity-participant");

    com.fortnite.pronos.model.GameParticipant participant =
        com.fortnite.pronos.model.GameParticipant.builder().user(participantUser).build();

    com.fortnite.pronos.model.GameRegionRule rule =
        com.fortnite.pronos.model.GameRegionRule.builder()
            .region(Player.Region.NA)
            .maxPlayers(4)
            .build();

    List<com.fortnite.pronos.model.GameParticipant> participants = new ArrayList<>();
    participants.add(null);
    participants.add(participant);

    List<com.fortnite.pronos.model.GameRegionRule> rules = new ArrayList<>();
    rules.add(null);
    rules.add(rule);

    com.fortnite.pronos.model.Game entityGame =
        com.fortnite.pronos.model.Game.builder()
            .id(gameId)
            .name("Entity Game")
            .description("entity description")
            .creator(creator)
            .status(com.fortnite.pronos.model.GameStatus.ACTIVE)
            .maxParticipants(10)
            .participants(participants)
            .regionRules(rules)
            .currentSeason(2026)
            .build();

    GameDto dto = GameDtoMapper.fromEntityGame(entityGame);

    assertThat(dto.getId()).isEqualTo(gameId);
    assertThat(dto.getCreatorId()).isEqualTo(creatorId);
    assertThat(dto.getCreatorUsername()).isEqualTo("entity-creator");
    assertThat(dto.getStatus()).isEqualTo(com.fortnite.pronos.model.GameStatus.ACTIVE);
    assertThat(dto.getCurrentParticipantCount()).isEqualTo(2);
    assertThat(dto.getRegionRules()).containsEntry(Player.Region.NA, 4);
    assertThat(dto.getParticipants()).containsEntry(participantId, "entity-participant");
  }

  @Test
  void fromDomainGameThrowsWhenInputIsNull() {
    assertThatThrownBy(() -> GameDtoMapper.fromDomainGame(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Game cannot be null");
  }

  @Test
  void fromEntityGameThrowsWhenInputIsNull() {
    assertThatThrownBy(() -> GameDtoMapper.fromEntityGame(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Game cannot be null");
  }
}
