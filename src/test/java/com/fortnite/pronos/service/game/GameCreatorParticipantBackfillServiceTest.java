package com.fortnite.pronos.service.game;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.fortnite.pronos.adapter.out.persistence.game.GameEntityMapper;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.dto.mapper.GameDtoMapper;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameParticipantRepository;
import com.fortnite.pronos.repository.GameRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("test")
class GameCreatorParticipantBackfillServiceTest {

  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-04-30T10:15:00Z"), ZoneOffset.UTC);
  private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 4, 29, 12, 0);

  @Autowired private TestEntityManager entityManager;
  @Autowired private GameRepository gameRepository;
  @Autowired private GameParticipantRepository participantRepository;

  private GameCreatorParticipantBackfillService service;
  private GameEntityMapper mapper;

  @BeforeEach
  void setUp() {
    service =
        new GameCreatorParticipantBackfillService(
            gameRepository, participantRepository, FIXED_CLOCK);
    mapper = new GameEntityMapper();
  }

  @Test
  void auditFindsNonDeletedLegacyGameWithoutCreatorParticipantAndDoesNotWrite() {
    User creator = persistUser("creator-audit");
    Game legacyGame = persistGame("Legacy Audit", creator, 4, null);

    List<UUID> missingGameIds = service.findGameIdsMissingCreatorParticipant();

    assertThat(missingGameIds).containsExactly(legacyGame.getId());
    assertThat(participantRepository.countByGameId(legacyGame.getId())).isZero();
  }

  @Test
  void backfillCreatesOneCreatorParticipantAndIsIdempotent() {
    User creator = persistUser("creator-backfill");
    Game legacyGame = persistGame("Legacy Backfill", creator, 4, null);

    GameCreatorParticipantBackfillService.BackfillResult firstRun =
        service.backfillMissingCreatorParticipants();
    GameCreatorParticipantBackfillService.BackfillResult secondRun =
        service.backfillMissingCreatorParticipants();

    List<GameParticipant> participants =
        participantRepository.findByGameIdOrderByJoinedAt(legacyGame.getId());
    assertThat(firstRun.insertedCount()).isEqualTo(1);
    assertThat(secondRun.insertedCount()).isZero();
    assertThat(participants).hasSize(1);
    assertThat(participants.get(0).getUser().getId()).isEqualTo(creator.getId());
    assertThat(participants.get(0).getCreator()).isTrue();
    assertThat(participants.get(0).getJoinedAt()).isEqualTo(legacyGame.getCreatedAt());
  }

  @Test
  void backfillLeavesCanonicalGameWithoutDuplicate() {
    User creator = persistUser("creator-canonical");
    Game canonicalGame = persistGame("Canonical", creator, 4, null);
    persistParticipant(canonicalGame, creator, true, 1, CREATED_AT);

    GameCreatorParticipantBackfillService.BackfillResult result =
        service.backfillMissingCreatorParticipants();

    assertThat(result.insertedCount()).isZero();
    assertThat(participantRepository.findByGameIdOrderByJoinedAt(canonicalGame.getId())).hasSize(1);
  }

  @Test
  void backfillUsesNextDraftOrderWhenDraftOrderOneAlreadyExists() {
    User creator = persistUser("creator-order");
    User existingPlayer = persistUser("player-order");
    Game legacyGame = persistGame("Legacy Order", creator, 4, null);
    persistParticipant(legacyGame, existingPlayer, false, 1, CREATED_AT.plusMinutes(5));

    GameCreatorParticipantBackfillService.BackfillResult result =
        service.backfillMissingCreatorParticipants();

    GameParticipant creatorParticipant =
        participantRepository
            .findByUserIdAndGameId(creator.getId(), legacyGame.getId())
            .orElseThrow();
    assertThat(result.insertedCount()).isEqualTo(1);
    assertThat(creatorParticipant.getCreator()).isTrue();
    assertThat(creatorParticipant.getDraftOrder()).isEqualTo(2);
  }

  @Test
  void backfillMarksExistingCreatorParticipantAndKeepsCountsCapacityDraftAndDtoConsistent() {
    User creator = persistUser("creator-count");
    User player = persistUser("player-count");
    Game game = persistGame("Legacy Count", creator, 2, null);
    persistParticipant(game, creator, false, 1, null);
    persistParticipant(game, player, false, 2, CREATED_AT.plusMinutes(5));

    GameCreatorParticipantBackfillService.BackfillResult result =
        service.backfillMissingCreatorParticipants();
    flushAndClear();

    Game reloaded = reloadGameWithParticipants(game.getId());
    com.fortnite.pronos.domain.game.model.Game domain = mapper.toDomain(reloaded);
    GameDto dto = GameDtoMapper.fromDomainGame(domain);

    assertThat(result.updatedCreatorFlagCount()).isEqualTo(1);
    assertThat(participantRepository.findByUserIdAndGameId(creator.getId(), game.getId()))
        .get()
        .extracting(GameParticipant::getCreator, GameParticipant::getJoinedAt)
        .containsExactly(true, game.getCreatedAt());
    assertThat(domain.getTotalParticipantCount()).isEqualTo(2);
    assertThat(domain.isFull()).isTrue();
    assertThat(domain.canAddParticipants()).isFalse();
    assertThat(domain.startDraft()).isTrue();
    assertThat(dto.getCurrentParticipantCount()).isEqualTo(2);
  }

  @Test
  void backfilledCreatorAloneStillCannotStartDraft() {
    User creator = persistUser("creator-alone");
    Game game = persistGame("Creator Alone", creator, 2, null);

    service.backfillMissingCreatorParticipants();
    flushAndClear();

    Game reloaded = reloadGameWithParticipants(game.getId());
    com.fortnite.pronos.domain.game.model.Game domain = mapper.toDomain(reloaded);

    assertThat(domain.getTotalParticipantCount()).isEqualTo(1);
    assertThat(domain.startDraft()).isFalse();
  }

  @Test
  void backfillSkipsSoftDeletedGames() {
    User creator = persistUser("creator-deleted");
    Game deletedGame = persistGame("Deleted", creator, 4, LocalDateTime.of(2026, 4, 30, 8, 0));

    GameCreatorParticipantBackfillService.BackfillResult result =
        service.backfillMissingCreatorParticipants();

    assertThat(service.findGameIdsMissingCreatorParticipant()).doesNotContain(deletedGame.getId());
    assertThat(result.insertedCount()).isZero();
    assertThat(participantRepository.countByGameId(deletedGame.getId())).isZero();
  }

  private User persistUser(String username) {
    User user = new User();
    user.setUsername(username);
    user.setEmail(username + "@test.com");
    user.setPassword("secret");
    user.setRole(User.UserRole.USER);
    user.setCurrentSeason(2026);
    return entityManager.persistAndFlush(user);
  }

  private Game persistGame(
      String name, User creator, int maxParticipants, LocalDateTime deletedAt) {
    Game game = new Game();
    game.setName(name);
    game.setCreator(creator);
    game.setMaxParticipants(maxParticipants);
    game.setStatus(GameStatus.CREATING);
    game.setCreatedAt(CREATED_AT);
    game.setCurrentSeason(2026);
    game.setDeletedAt(deletedAt);
    return entityManager.persistAndFlush(game);
  }

  private GameParticipant persistParticipant(
      Game game, User user, Boolean creator, Integer draftOrder, LocalDateTime joinedAt) {
    GameParticipant participant =
        GameParticipant.builder()
            .game(game)
            .user(user)
            .creator(creator)
            .draftOrder(draftOrder)
            .joinedAt(joinedAt)
            .build();
    return entityManager.persistAndFlush(participant);
  }

  private Game reloadGameWithParticipants(UUID gameId) {
    Game persisted = gameRepository.findById(gameId).orElseThrow();
    Game snapshot = new Game();
    snapshot.setId(persisted.getId());
    snapshot.setName(persisted.getName());
    snapshot.setCreator(persisted.getCreator());
    snapshot.setMaxParticipants(persisted.getMaxParticipants());
    snapshot.setStatus(persisted.getStatus());
    snapshot.setCreatedAt(persisted.getCreatedAt());
    snapshot.setCurrentSeason(persisted.getCurrentSeason());
    snapshot.setParticipants(participantRepository.findByGameIdWithUserFetch(gameId));
    snapshot.setRegionRules(List.of());
    return snapshot;
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }
}
