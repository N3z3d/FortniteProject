package com.fortnite.pronos.adapter.out.persistence.game;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fortnite.pronos.domain.game.model.GameParticipant;
import com.fortnite.pronos.domain.game.model.GameRegionRule;
import com.fortnite.pronos.domain.game.model.GameStatus;
import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.User;

@SuppressWarnings({"java:S5961"})
class GameEntityMapperTest {

  private GameEntityMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new GameEntityMapper();
  }

  @Test
  void toDomainMapsScalarFieldsAndNestedData() {
    UUID gameId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    UUID participantUserId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();
    UUID selectedPlayerId = UUID.randomUUID();
    UUID ruleId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();

    User creator = buildUser(creatorId, "creator");
    User participantUser = buildUser(participantUserId, "participant");

    Game entity = buildEntityGame(gameId, creator, now);
    entity.setInvitationCode("INV12345");
    entity.setInvitationCodeExpiresAt(now.minusMinutes(10));

    com.fortnite.pronos.model.GameParticipant creatorParticipant =
        new com.fortnite.pronos.model.GameParticipant();
    creatorParticipant.setId(UUID.randomUUID());
    creatorParticipant.setUser(creator);
    creatorParticipant.setDraftOrder(1);
    creatorParticipant.setJoinedAt(now.minusDays(2));
    creatorParticipant.setCreator(true);

    com.fortnite.pronos.model.GameParticipant participant =
        new com.fortnite.pronos.model.GameParticipant();
    participant.setId(participantId);
    participant.setUser(participantUser);
    participant.setDraftOrder(2);
    participant.setJoinedAt(now.minusDays(1));
    participant.setLastSelectionTime(now.minusMinutes(2));
    participant.setCreator(false);

    Player selectedPlayer = new Player();
    selectedPlayer.setId(selectedPlayerId);
    participant.setSelectedPlayers(List.of(selectedPlayer));

    com.fortnite.pronos.model.GameRegionRule regionRule =
        new com.fortnite.pronos.model.GameRegionRule();
    regionRule.setId(ruleId);
    regionRule.setGame(entity);
    regionRule.setRegion(Player.Region.EU);
    regionRule.setMaxPlayers(3);

    entity.setParticipants(List.of(creatorParticipant, participant));
    entity.setRegionRules(List.of(regionRule));

    com.fortnite.pronos.domain.game.model.Game domain = mapper.toDomain(entity);

    assertThat(domain).isNotNull();
    assertThat(domain.getId()).isEqualTo(gameId);
    assertThat(domain.getName()).isEqualTo("My Game");
    assertThat(domain.getDescription()).isEqualTo("description");
    assertThat(domain.getCreatorId()).isEqualTo(creatorId);
    assertThat(domain.getStatus()).isEqualTo(GameStatus.ACTIVE);
    assertThat(domain.getCreatedAt()).isEqualTo(now.minusHours(2));
    assertThat(domain.getFinishedAt()).isEqualTo(now.minusHours(1));
    assertThat(domain.getInvitationCode()).isEqualTo("INV12345");
    assertThat(domain.isInvitationCodeExpired()).isTrue();
    assertThat(domain.isTradingEnabled()).isTrue();
    assertThat(domain.getMaxTradesPerTeam()).isEqualTo(7);
    assertThat(domain.getCurrentSeason()).isEqualTo(2026);

    assertThat(domain.getParticipants()).hasSize(2);
    assertThat(domain.getParticipants())
        .anySatisfy(
            mappedParticipant -> {
              assertThat(mappedParticipant.getId()).isEqualTo(participantId);
              assertThat(mappedParticipant.getUserId()).isEqualTo(participantUserId);
              assertThat(mappedParticipant.getUsername()).isEqualTo("participant");
              assertThat(mappedParticipant.getDraftOrder()).isEqualTo(2);
              assertThat(mappedParticipant.getSelectedPlayerIds())
                  .containsExactly(selectedPlayerId);
            });
    assertThat(domain.getParticipants())
        .anySatisfy(
            mappedCreator -> {
              assertThat(mappedCreator.getUserId()).isEqualTo(creatorId);
              assertThat(mappedCreator.getUsername()).isEqualTo("creator");
              assertThat(mappedCreator.isCreator()).isTrue();
            });

    assertThat(domain.getRegionRules()).hasSize(1);
    GameRegionRule mappedRule = domain.getRegionRules().get(0);
    assertThat(mappedRule.getId()).isEqualTo(ruleId);
    assertThat(mappedRule.getRegion()).isEqualTo(PlayerRegion.EU);
    assertThat(mappedRule.getMaxPlayers()).isEqualTo(3);
  }

  @Test
  void toDomainHandlesNullCollections() {
    UUID creatorId = UUID.randomUUID();
    User creator = buildUser(creatorId, "creator");
    Game entity = buildEntityGame(UUID.randomUUID(), creator, LocalDateTime.now());
    entity.setParticipants(null);
    entity.setRegionRules(null);

    com.fortnite.pronos.domain.game.model.Game domain = mapper.toDomain(entity);

    assertThat(domain.getParticipants()).isEmpty();
    assertThat(domain.getRegionRules()).isEmpty();
  }

  @Test
  void toDomainDoesNotAddSyntheticCreatorParticipantWhenMissing() {
    UUID gameId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    UUID participantUserId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();

    User creator = buildUser(creatorId, "thibaut");
    User participantUser = buildUser(participantUserId, "teddy");

    Game entity = buildEntityGame(gameId, creator, now);

    com.fortnite.pronos.model.GameParticipant participant =
        com.fortnite.pronos.model.GameParticipant.builder()
            .user(participantUser)
            .creator(false)
            .build();
    entity.setParticipants(List.of(participant));

    com.fortnite.pronos.domain.game.model.Game domain = mapper.toDomain(entity);

    assertThat(domain.getParticipants()).hasSize(1);
    assertThat(domain.getParticipants()).noneMatch(p -> creatorId.equals(p.getUserId()));
  }

  @Test
  void toEntityMapsDomainToJpaEntity() {
    UUID gameId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();
    UUID participantUserId = UUID.randomUUID();
    UUID selectedPlayerId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();

    com.fortnite.pronos.domain.game.model.Game domain =
        com.fortnite.pronos.domain.game.model.Game.restore(
            gameId,
            "Domain Game",
            "domain description",
            creatorId,
            8,
            GameStatus.DRAFTING,
            now.minusHours(3),
            null,
            null,
            "CODE1234",
            now.plusHours(2),
            List.of(new GameRegionRule(UUID.randomUUID(), PlayerRegion.NAW, 2)),
            List.of(
                GameParticipant.restore(
                    participantId,
                    participantUserId,
                    "participant-user",
                    3,
                    now.minusDays(1),
                    now.minusMinutes(4),
                    false,
                    List.of(selectedPlayerId))),
            null,
            true,
            6,
            now.plusDays(1),
            2025);

    User creator = buildUser(creatorId, "creator-user");
    User participantUser = buildUser(participantUserId, "participant-user");

    Game entity = mapper.toEntity(domain, creator, Map.of(participantUserId, participantUser));

    assertThat(entity).isNotNull();
    assertThat(entity.getId()).isEqualTo(gameId);
    assertThat(entity.getName()).isEqualTo("Domain Game");
    assertThat(entity.getDescription()).isEqualTo("domain description");
    assertThat(entity.getCreator()).isEqualTo(creator);
    assertThat(entity.getStatus()).isEqualTo(com.fortnite.pronos.model.GameStatus.DRAFTING);
    assertThat(entity.getInvitationCode()).isEqualTo("CODE1234");
    assertThat(entity.getTradingEnabled()).isTrue();
    assertThat(entity.getMaxTradesPerTeam()).isEqualTo(6);
    assertThat(entity.getCurrentSeason()).isEqualTo(2025);

    assertThat(entity.getParticipants()).hasSize(1);
    com.fortnite.pronos.model.GameParticipant mappedParticipant = entity.getParticipants().get(0);
    assertThat(mappedParticipant.getId()).isEqualTo(participantId);
    assertThat(mappedParticipant.getUser()).isEqualTo(participantUser);
    assertThat(mappedParticipant.getGame()).isEqualTo(entity);
    assertThat(mappedParticipant.getSelectedPlayers()).hasSize(1);
    assertThat(mappedParticipant.getSelectedPlayers().get(0).getId()).isEqualTo(selectedPlayerId);

    assertThat(entity.getRegionRules()).hasSize(1);
    com.fortnite.pronos.model.GameRegionRule mappedRule = entity.getRegionRules().get(0);
    assertThat(mappedRule.getGame()).isEqualTo(entity);
    assertThat(mappedRule.getRegion()).isEqualTo(Player.Region.NAW);
    assertThat(mappedRule.getMaxPlayers()).isEqualTo(2);
  }

  @Test
  void enumMappingCoversAllStatusesAndRegions() {
    for (com.fortnite.pronos.model.GameStatus status :
        com.fortnite.pronos.model.GameStatus.values()) {
      assertThat(mapper.toEntityStatus(GameStatus.valueOf(status.name()))).isEqualTo(status);
      assertThat(mapper.toDomainStatus(status)).isEqualTo(GameStatus.valueOf(status.name()));
    }

    for (Player.Region region : Player.Region.values()) {
      assertThat(mapper.toEntityRegion(PlayerRegion.valueOf(region.name()))).isEqualTo(region);
      assertThat(mapper.toDomainRegion(region)).isEqualTo(PlayerRegion.valueOf(region.name()));
    }
  }

  @Test
  void toDomainReturnsNullWhenEntityIsNull() {
    assertThat(mapper.toDomain(null)).isNull();
  }

  @Test
  void toEntityReturnsNullWhenDomainIsNull() {
    User creator = buildUser(UUID.randomUUID(), "creator");
    assertThat(mapper.toEntity(null, creator, Map.of())).isNull();
  }

  private Game buildEntityGame(UUID id, User creator, LocalDateTime now) {
    Game game = new Game();
    game.setId(id);
    game.setName("My Game");
    game.setDescription("description");
    game.setCreator(creator);
    game.setMaxParticipants(10);
    game.setStatus(com.fortnite.pronos.model.GameStatus.ACTIVE);
    game.setCreatedAt(now.minusHours(2));
    game.setFinishedAt(now.minusHours(1));
    game.setTradingEnabled(true);
    game.setMaxTradesPerTeam(7);
    game.setTradeDeadline(now.plusDays(1));
    game.setCurrentSeason(2026);
    return game;
  }

  private User buildUser(UUID id, String username) {
    User user = new User();
    user.setId(id);
    user.setUsername(username);
    user.setEmail(username + "@test.com");
    user.setPassword("secret");
    user.setRole(User.UserRole.USER);
    user.setCurrentSeason(2025);
    return user;
  }
}
