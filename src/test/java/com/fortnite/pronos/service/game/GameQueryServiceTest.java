package com.fortnite.pronos.service.game;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameStatus;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.exception.GameNotFoundException;

@ExtendWith(MockitoExtension.class)
class GameQueryServiceTest {

  @Mock private GameDomainRepositoryPort gameRepository;
  @Mock private PlayerDomainRepositoryPort playerRepository;
  @Mock private UserRepositoryPort userRepository;
  @InjectMocks private GameQueryService service;

  private Game createDomainGame(UUID id, String name, UUID creatorId) {
    return Game.restore(
        id,
        name,
        null,
        creatorId,
        10,
        GameStatus.CREATING,
        LocalDateTime.now(),
        null,
        null,
        null,
        null,
        List.of(),
        List.of(),
        null,
        false,
        5,
        null,
        2025);
  }

  // --- getAllGames ---

  @Test
  void getAllGames_returnsAllGamesWithPlayerCount() {
    UUID creatorId = UUID.randomUUID();
    Game game = createDomainGame(UUID.randomUUID(), "Game 1", creatorId);
    when(gameRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(game));
    when(playerRepository.count()).thenReturn(42L);
    when(userRepository.findById(creatorId)).thenReturn(Optional.empty());

    List<GameDto> result = service.getAllGames();

    assertEquals(1, result.size());
    assertEquals("Game 1", result.get(0).getName());
    assertEquals(42L, result.get(0).getFortnitePlayerCount());
  }

  @Test
  void getAllGames_returnsEmptyList() {
    when(gameRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());
    when(playerRepository.count()).thenReturn(0L);

    List<GameDto> result = service.getAllGames();

    assertTrue(result.isEmpty());
  }

  // --- getAvailableGames ---

  @Test
  void getAvailableGames_returnsGamesWithSlots() {
    UUID creatorId = UUID.randomUUID();
    Game game = createDomainGame(UUID.randomUUID(), "Open Game", creatorId);
    when(gameRepository.findGamesWithAvailableSlots()).thenReturn(List.of(game));
    when(playerRepository.count()).thenReturn(10L);
    when(userRepository.findById(creatorId)).thenReturn(Optional.empty());

    List<GameDto> result = service.getAvailableGames();

    assertEquals(1, result.size());
  }

  // --- getGamesByUser ---

  @Test
  void getGamesByUser_returnsUserGames() {
    UUID userId = UUID.randomUUID();
    Game game = createDomainGame(UUID.randomUUID(), "My Game", userId);
    when(gameRepository.findGamesByUserId(userId)).thenReturn(List.of(game));
    when(playerRepository.count()).thenReturn(5L);
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    List<GameDto> result = service.getGamesByUser(userId);

    assertEquals(1, result.size());
    assertEquals("My Game", result.get(0).getName());
  }

  // --- getGameById ---

  @Test
  void getGameById_returnsGameWhenFound() {
    UUID gameId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    Game game = createDomainGame(gameId, "Found Game", creatorId);
    when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
    when(playerRepository.count()).thenReturn(20L);
    when(userRepository.findById(creatorId)).thenReturn(Optional.empty());

    Optional<GameDto> result = service.getGameById(gameId);

    assertTrue(result.isPresent());
    assertEquals("Found Game", result.get().getName());
    assertEquals(20L, result.get().getFortnitePlayerCount());
  }

  @Test
  void getGameById_returnsEmptyWhenNotFound() {
    UUID gameId = UUID.randomUUID();
    when(gameRepository.findById(gameId)).thenReturn(Optional.empty());

    Optional<GameDto> result = service.getGameById(gameId);

    assertTrue(result.isEmpty());
  }

  // --- getGameByIdOrThrow ---

  @Test
  void getGameByIdOrThrow_returnsGame() {
    UUID gameId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    Game game = createDomainGame(gameId, "Game", creatorId);
    when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
    when(playerRepository.count()).thenReturn(0L);
    when(userRepository.findById(creatorId)).thenReturn(Optional.empty());

    GameDto result = service.getGameByIdOrThrow(gameId);

    assertNotNull(result);
  }

  @Test
  void getGameByIdOrThrow_throwsWhenNotFound() {
    UUID gameId = UUID.randomUUID();
    when(gameRepository.findById(gameId)).thenReturn(Optional.empty());

    assertThrows(GameNotFoundException.class, () -> service.getGameByIdOrThrow(gameId));
  }

  // --- getGameByInvitationCode ---

  @Test
  void getGameByInvitationCode_returnsGame() {
    UUID creatorId = UUID.randomUUID();
    Game game = createDomainGame(UUID.randomUUID(), "Invited", creatorId);
    when(gameRepository.findByInvitationCode("ABC123")).thenReturn(Optional.of(game));
    when(userRepository.findById(creatorId)).thenReturn(Optional.empty());

    Optional<GameDto> result = service.getGameByInvitationCode("ABC123");

    assertTrue(result.isPresent());
  }

  @Test
  void getGameByInvitationCode_returnsEmptyWhenNotFound() {
    when(gameRepository.findByInvitationCode("MISSING")).thenReturn(Optional.empty());

    Optional<GameDto> result = service.getGameByInvitationCode("MISSING");

    assertTrue(result.isEmpty());
  }

  // --- getGamesByStatus ---

  @Test
  void getGamesByStatus_returnsFilteredGames() {
    UUID creatorId = UUID.randomUUID();
    Game game = createDomainGame(UUID.randomUUID(), "Active", creatorId);
    when(gameRepository.findByStatus(GameStatus.CREATING)).thenReturn(List.of(game));
    when(userRepository.findById(creatorId)).thenReturn(Optional.empty());

    List<GameDto> result = service.getGamesByStatus(GameStatus.CREATING);

    assertEquals(1, result.size());
  }

  // --- getActiveGames ---

  @Test
  void getActiveGames_excludesFinishedGames() {
    UUID creatorId = UUID.randomUUID();
    Game game = createDomainGame(UUID.randomUUID(), "Active", creatorId);
    when(gameRepository.findByStatusNot(GameStatus.FINISHED)).thenReturn(List.of(game));
    when(userRepository.findById(creatorId)).thenReturn(Optional.empty());

    List<GameDto> result = service.getActiveGames();

    assertEquals(1, result.size());
  }

  // --- searchGamesByName ---

  @Test
  void searchGamesByName_returnsMatchingGames() {
    UUID creatorId = UUID.randomUUID();
    Game game = createDomainGame(UUID.randomUUID(), "Tournament", creatorId);
    when(gameRepository.findByNameContainingIgnoreCase("tourn")).thenReturn(List.of(game));
    when(userRepository.findById(creatorId)).thenReturn(Optional.empty());

    List<GameDto> result = service.searchGamesByName("tourn");

    assertEquals(1, result.size());
  }

  // --- getGamesCreatedByUser ---

  @Test
  void getGamesCreatedByUser_returnsCreatorGames() {
    UUID creatorId = UUID.randomUUID();
    Game game = createDomainGame(UUID.randomUUID(), "Created", creatorId);
    when(gameRepository.findByCreatorId(creatorId)).thenReturn(List.of(game));
    when(userRepository.findById(creatorId)).thenReturn(Optional.empty());

    List<GameDto> result = service.getGamesCreatedByUser(creatorId);

    assertEquals(1, result.size());
  }

  // --- gameExists ---

  @Test
  void gameExists_returnsTrueWhenExists() {
    UUID gameId = UUID.randomUUID();
    when(gameRepository.existsById(gameId)).thenReturn(true);

    assertTrue(service.gameExists(gameId));
  }

  @Test
  void gameExists_returnsFalseWhenNotExists() {
    UUID gameId = UUID.randomUUID();
    when(gameRepository.existsById(gameId)).thenReturn(false);

    assertFalse(service.gameExists(gameId));
  }

  // --- gameExistsByInvitationCode ---

  @Test
  void gameExistsByInvitationCode_returnsTrue() {
    when(gameRepository.existsByInvitationCode("CODE")).thenReturn(true);

    assertTrue(service.gameExistsByInvitationCode("CODE"));
  }

  // --- getGameCount ---

  @Test
  void getGameCount_returnsCount() {
    when(gameRepository.count()).thenReturn(15L);

    assertEquals(15L, service.getGameCount());
  }

  // --- getGameCountByStatus ---

  @Test
  void getGameCountByStatus_returnsCountForStatus() {
    when(gameRepository.countByStatus(GameStatus.ACTIVE)).thenReturn(3L);

    assertEquals(3L, service.getGameCountByStatus(GameStatus.ACTIVE));
  }

  // --- getGamesBySeason ---

  @Test
  void getGamesBySeason_returnsSeasonGames() {
    UUID creatorId = UUID.randomUUID();
    Game game = createDomainGame(UUID.randomUUID(), "Season Game", creatorId);
    when(gameRepository.findByCurrentSeasonWithFetch(2025)).thenReturn(List.of(game));
    when(playerRepository.count()).thenReturn(100L);
    when(userRepository.findById(creatorId)).thenReturn(Optional.empty());

    List<GameDto> result = service.getGamesBySeason(2025);

    assertEquals(1, result.size());
    assertEquals(100L, result.get(0).getFortnitePlayerCount());
  }

  // --- getCurrentSeason ---

  @Test
  void getCurrentSeason_returnsCurrentYear() {
    Integer season = service.getCurrentSeason();

    assertEquals(java.time.Year.now().getValue(), season);
  }

  // --- enrichCreatorIdentity ---

  @Test
  void enrichCreatorIdentity_setsCreatorNameFromUsername() {
    UUID creatorId = UUID.randomUUID();
    com.fortnite.pronos.model.User user = mock(com.fortnite.pronos.model.User.class);
    when(user.getUsername()).thenReturn("PlayerOne");
    when(userRepository.findById(creatorId)).thenReturn(Optional.of(user));

    Game game = createDomainGame(UUID.randomUUID(), "Test", creatorId);
    when(gameRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(game));
    when(playerRepository.count()).thenReturn(0L);

    List<GameDto> result = service.getAllGames();

    assertEquals("PlayerOne", result.get(0).getCreatorUsername());
    assertEquals("PlayerOne", result.get(0).getCreatorName());
  }
}
