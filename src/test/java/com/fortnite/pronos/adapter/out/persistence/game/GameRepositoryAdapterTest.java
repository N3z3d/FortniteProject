package com.fortnite.pronos.adapter.out.persistence.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.CrudRepository;

import com.fortnite.pronos.domain.game.model.GameStatus;
import com.fortnite.pronos.domain.model.Pagination;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameParticipantRepository;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class GameRepositoryAdapterTest {

  @Mock private GameRepository gameRepository;
  @Mock private UserRepository userRepository;
  @Mock private GameParticipantRepository participantRepository;

  private GameRepositoryAdapter adapter;
  private CrudRepository<Game, UUID> gameCrudRepository;
  private CrudRepository<User, UUID> userCrudRepository;

  @BeforeEach
  void setUp() {
    adapter =
        new GameRepositoryAdapter(
            gameRepository, userRepository, new GameEntityMapper(), participantRepository);
    gameCrudRepository = gameRepository;
    userCrudRepository = userRepository;
  }

  @Test
  void findByIdReturnsEmptyWhenEntityMissing() {
    UUID gameId = UUID.randomUUID();
    when(gameCrudRepository.findById(gameId)).thenReturn(Optional.empty());

    Optional<com.fortnite.pronos.domain.game.model.Game> result = adapter.findById(gameId);

    assertThat(result).isEmpty();
  }

  @Test
  void findByIdReturnsMappedDomainGame() {
    UUID gameId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    Game entity = buildEntityGame(gameId, creatorId, com.fortnite.pronos.model.GameStatus.ACTIVE);
    when(gameCrudRepository.findById(gameId)).thenReturn(Optional.of(entity));

    Optional<com.fortnite.pronos.domain.game.model.Game> result = adapter.findById(gameId);

    assertThat(result).isPresent();
    com.fortnite.pronos.domain.game.model.Game game = result.orElseThrow();
    assertThat(game.getId()).isEqualTo(gameId);
    assertThat(game.getCreatorId()).isEqualTo(creatorId);
    assertThat(game.getStatus()).isEqualTo(GameStatus.ACTIVE);
  }

  @Test
  void saveCreatesOrUpdatesAndReturnsMappedDomainGame() {
    UUID gameId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    User creator = buildUser(creatorId, "creator");
    com.fortnite.pronos.domain.game.model.Game domainGame = buildDomainGame(gameId, creatorId);

    when(userCrudRepository.findById(creatorId)).thenReturn(Optional.of(creator));
    when(gameCrudRepository.save(any(Game.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    com.fortnite.pronos.domain.game.model.Game saved = adapter.save(domainGame);

    assertThat(saved.getId()).isEqualTo(gameId);
    assertThat(saved.getCreatorId()).isEqualTo(creatorId);
    assertThat(saved.getName()).isEqualTo("Domain Save");
    verify(gameCrudRepository).save(any(Game.class));
  }

  @Test
  void saveThrowsWhenCreatorIsMissing() {
    UUID creatorId = UUID.randomUUID();
    com.fortnite.pronos.domain.game.model.Game domainGame =
        buildDomainGame(UUID.randomUUID(), creatorId);
    when(userCrudRepository.findById(creatorId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> adapter.save(domainGame))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Creator not found");
  }

  @Test
  void findByInvitationCodeMapsDomainGame() {
    UUID gameId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    Game entity = buildEntityGame(gameId, creatorId, com.fortnite.pronos.model.GameStatus.CREATING);
    entity.setInvitationCode("INV-ABC");
    when(gameRepository.findByInvitationCode("INV-ABC")).thenReturn(Optional.of(entity));

    Optional<com.fortnite.pronos.domain.game.model.Game> result =
        adapter.findByInvitationCode("INV-ABC");

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().getInvitationCode()).isEqualTo("INV-ABC");
  }

  @Test
  void findByInvitationCodeForUpdateMapsDomainGame() {
    UUID gameId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    Game entity = buildEntityGame(gameId, creatorId, com.fortnite.pronos.model.GameStatus.CREATING);
    entity.setInvitationCode("INV-LOCK");
    when(gameRepository.findByInvitationCodeForUpdate("INV-LOCK")).thenReturn(Optional.of(entity));

    Optional<com.fortnite.pronos.domain.game.model.Game> result =
        adapter.findByInvitationCodeForUpdate("INV-LOCK");

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().getInvitationCode()).isEqualTo("INV-LOCK");
    verify(gameRepository).findByInvitationCodeForUpdate("INV-LOCK");
  }

  @Test
  void findByInvitationCodeForUpdateUsesPessimisticWriteLock() throws NoSuchMethodException {
    Method method = GameRepository.class.getMethod("findByInvitationCodeForUpdate", String.class);

    Lock lock = method.getAnnotation(Lock.class);

    assertThat(lock).isNotNull();
    assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
  }

  @Test
  void existsByIdDelegatesToJpaRepository() {
    UUID gameId = UUID.randomUUID();
    when(gameCrudRepository.existsById(gameId)).thenReturn(true);

    assertThat(adapter.existsById(gameId)).isTrue();
  }

  @Test
  void countByCreatorAndStatusInReturnsZeroWhenStatusesEmpty() {
    long count = adapter.countByCreatorAndStatusIn(UUID.randomUUID(), List.of());

    assertThat(count).isZero();
    verifyNoInteractions(gameRepository);
  }

  @Test
  void countByCreatorAndStatusInMapsStatuses() {
    UUID creatorId = UUID.randomUUID();
    User creator = buildUser(creatorId, "creator");
    when(userCrudRepository.findById(creatorId)).thenReturn(Optional.of(creator));
    when(gameRepository.countByCreatorAndStatusInAndDeletedAtIsNull(eq(creator), any()))
        .thenReturn(2L);

    long count =
        adapter.countByCreatorAndStatusIn(
            creatorId, List.of(GameStatus.CREATING, GameStatus.ACTIVE));

    assertThat(count).isEqualTo(2L);
    ArgumentCaptor<List<com.fortnite.pronos.model.GameStatus>> captor =
        ArgumentCaptor.forClass(List.class);
    verify(gameRepository)
        .countByCreatorAndStatusInAndDeletedAtIsNull(eq(creator), captor.capture());
    assertThat(captor.getValue())
        .containsExactly(
            com.fortnite.pronos.model.GameStatus.CREATING,
            com.fortnite.pronos.model.GameStatus.ACTIVE);
  }

  @Test
  void findByStatusMapsAllGames() {
    UUID creatorId = UUID.randomUUID();
    when(gameRepository.findByStatus(com.fortnite.pronos.model.GameStatus.ACTIVE))
        .thenReturn(
            List.of(
                buildEntityGame(
                    UUID.randomUUID(), creatorId, com.fortnite.pronos.model.GameStatus.ACTIVE),
                buildEntityGame(
                    UUID.randomUUID(), creatorId, com.fortnite.pronos.model.GameStatus.ACTIVE)));

    List<com.fortnite.pronos.domain.game.model.Game> games =
        adapter.findByStatus(GameStatus.ACTIVE);

    assertThat(games).hasSize(2).allMatch(game -> game.getStatus() == GameStatus.ACTIVE);
  }

  @Test
  void findByCreatorIdMapsAllGames() {
    UUID creatorId = UUID.randomUUID();
    when(gameRepository.findByCreatorId(creatorId))
        .thenReturn(
            List.of(
                buildEntityGame(
                    UUID.randomUUID(), creatorId, com.fortnite.pronos.model.GameStatus.DRAFTING)));

    List<com.fortnite.pronos.domain.game.model.Game> games = adapter.findByCreatorId(creatorId);

    assertThat(games).hasSize(1);
    assertThat(games.get(0).getCreatorId()).isEqualTo(creatorId);
  }

  @Test
  void findAllByOrderByCreatedAtDescDelegatesToRepository() {
    UUID creatorId = UUID.randomUUID();
    when(gameRepository.findAllByOrderByCreatedAtDesc())
        .thenReturn(
            List.of(
                buildEntityGame(
                    UUID.randomUUID(), creatorId, com.fortnite.pronos.model.GameStatus.CREATING)));

    List<com.fortnite.pronos.domain.game.model.Game> games =
        adapter.findAllByOrderByCreatedAtDesc();

    assertThat(games).hasSize(1);
  }

  @Test
  void findGamesWithAvailableSlotsDelegatesToRepository() {
    UUID creatorId = UUID.randomUUID();
    when(gameRepository.findGamesWithAvailableSlots())
        .thenReturn(
            List.of(
                buildEntityGame(
                    UUID.randomUUID(), creatorId, com.fortnite.pronos.model.GameStatus.CREATING)));

    List<com.fortnite.pronos.domain.game.model.Game> games = adapter.findGamesWithAvailableSlots();

    assertThat(games).hasSize(1);
  }

  @Test
  void findGamesByUserIdReturnsEmptyWhenNull() {
    assertThat(adapter.findGamesByUserId(null)).isEmpty();
  }

  @Test
  void findGamesByUserIdDelegatesToRepository() {
    UUID creatorId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(gameRepository.findGamesByUserId(userId))
        .thenReturn(
            List.of(
                buildEntityGame(
                    UUID.randomUUID(), creatorId, com.fortnite.pronos.model.GameStatus.ACTIVE)));

    List<com.fortnite.pronos.domain.game.model.Game> games = adapter.findGamesByUserId(userId);

    assertThat(games).hasSize(1);
  }

  @Test
  void findByStatusNotReturnsEmptyWhenNull() {
    assertThat(adapter.findByStatusNot(null)).isEmpty();
  }

  @Test
  void findByStatusNotDelegatesToRepository() {
    UUID creatorId = UUID.randomUUID();
    when(gameRepository.findByStatusNot(com.fortnite.pronos.model.GameStatus.FINISHED))
        .thenReturn(
            List.of(
                buildEntityGame(
                    UUID.randomUUID(), creatorId, com.fortnite.pronos.model.GameStatus.ACTIVE)));

    List<com.fortnite.pronos.domain.game.model.Game> games =
        adapter.findByStatusNot(GameStatus.FINISHED);

    assertThat(games).hasSize(1);
    assertThat(games.get(0).getStatus()).isEqualTo(GameStatus.ACTIVE);
  }

  @Test
  void findAllGamesDelegatesToPaginatedRepository() {
    UUID creatorId = UUID.randomUUID();
    Game entity =
        buildEntityGame(UUID.randomUUID(), creatorId, com.fortnite.pronos.model.GameStatus.ACTIVE);
    Page<Game> page = new PageImpl<>(List.of(entity));
    when(gameRepository.findAllWithFetchPaginated(any(PageRequest.class))).thenReturn(page);

    List<com.fortnite.pronos.domain.game.model.Game> games =
        adapter.findAllGames(Pagination.of(0, 10));

    assertThat(games).hasSize(1);
  }

  @Test
  void findByNameContainingIgnoreCaseReturnsEmptyWhenNull() {
    assertThat(adapter.findByNameContainingIgnoreCase(null)).isEmpty();
  }

  @Test
  void findByNameContainingIgnoreCaseDelegatesToRepository() {
    UUID creatorId = UUID.randomUUID();
    when(gameRepository.findByNameContainingIgnoreCase("test"))
        .thenReturn(
            List.of(
                buildEntityGame(
                    UUID.randomUUID(), creatorId, com.fortnite.pronos.model.GameStatus.CREATING)));

    List<com.fortnite.pronos.domain.game.model.Game> games =
        adapter.findByNameContainingIgnoreCase("test");

    assertThat(games).hasSize(1);
  }

  @Test
  void existsByInvitationCodeReturnsFalseWhenNull() {
    assertThat(adapter.existsByInvitationCode(null)).isFalse();
  }

  @Test
  void existsByInvitationCodeDelegatesToRepository() {
    when(gameRepository.existsByInvitationCode("CODE")).thenReturn(true);

    assertThat(adapter.existsByInvitationCode("CODE")).isTrue();
  }

  @Test
  void countDelegatesToRepository() {
    when(gameRepository.count()).thenReturn(42L);

    assertThat(adapter.count()).isEqualTo(42L);
  }

  @Test
  void countByStatusReturnsZeroWhenNull() {
    assertThat(adapter.countByStatus(null)).isZero();
  }

  @Test
  void countByStatusDelegatesToRepository() {
    when(gameRepository.countByStatus(com.fortnite.pronos.model.GameStatus.ACTIVE)).thenReturn(5L);

    assertThat(adapter.countByStatus(GameStatus.ACTIVE)).isEqualTo(5L);
  }

  @Test
  void findByCurrentSeasonWithFetchReturnsEmptyWhenNull() {
    assertThat(adapter.findByCurrentSeasonWithFetch(null)).isEmpty();
  }

  @Test
  void findByCurrentSeasonWithFetchDelegatesToRepository() {
    UUID creatorId = UUID.randomUUID();
    when(gameRepository.findByCurrentSeasonWithFetch(2026))
        .thenReturn(
            List.of(
                buildEntityGame(
                    UUID.randomUUID(), creatorId, com.fortnite.pronos.model.GameStatus.ACTIVE)));

    List<com.fortnite.pronos.domain.game.model.Game> games =
        adapter.findByCurrentSeasonWithFetch(2026);

    assertThat(games).hasSize(1);
  }

  private com.fortnite.pronos.domain.game.model.Game buildDomainGame(UUID gameId, UUID creatorId) {
    LocalDateTime now = LocalDateTime.now();
    return com.fortnite.pronos.domain.game.model.Game.restore(
        gameId,
        "Domain Save",
        "description",
        creatorId,
        6,
        GameStatus.CREATING,
        now.minusHours(1),
        null,
        null,
        "CODE1234",
        now.plusHours(3),
        List.of(),
        List.of(),
        null,
        true,
        5,
        now.plusDays(1),
        2026);
  }

  private Game buildEntityGame(
      UUID gameId, UUID creatorId, com.fortnite.pronos.model.GameStatus status) {
    Game game = new Game();
    game.setId(gameId);
    game.setName("Entity Game");
    game.setDescription("entity");
    game.setCreator(buildUser(creatorId, "creator"));
    game.setMaxParticipants(8);
    game.setStatus(status);
    game.setCreatedAt(LocalDateTime.now().minusHours(1));
    game.setCurrentSeason(2026);
    game.setParticipants(List.of());
    game.setRegionRules(List.of());
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
