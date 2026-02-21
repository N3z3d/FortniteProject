package com.fortnite.pronos.service.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameStatus;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.dto.CreateGameRequest;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.exception.InvalidGameRequestException;
import com.fortnite.pronos.exception.UserNotFoundException;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.InvitationCodeService;
import com.fortnite.pronos.service.ValidationService;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"java:S5838"})
class GameCreationServiceDomainMigrationTest {

  @Mock private GameDomainRepositoryPort gameDomainRepository;
  @Mock private UserRepositoryPort userRepository;
  @Mock private ValidationService validationService;
  @Mock private InvitationCodeService invitationCodeService;

  @InjectMocks private GameCreationService service;

  private UUID gameId;
  private UUID creatorId;

  @BeforeEach
  void setUp() {
    gameId = UUID.randomUUID();
    creatorId = UUID.randomUUID();
  }

  @Test
  void deleteGameSoftDeletesWhenStatusCreating() {
    Game game = buildDomainGame(GameStatus.CREATING);
    when(gameDomainRepository.findById(gameId)).thenReturn(Optional.of(game));
    when(gameDomainRepository.save(any(Game.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.deleteGame(gameId);

    assertThat(game.isDeleted()).isTrue();
    verify(gameDomainRepository).save(game);
  }

  @Test
  void deleteGameThrowsWhenStatusIsNotCreating() {
    Game game = buildDomainGame(GameStatus.ACTIVE);
    when(gameDomainRepository.findById(gameId)).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> service.deleteGame(gameId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already started");

    verify(gameDomainRepository, never()).save(any(Game.class));
  }

  @Test
  void deleteGameThrowsWhenGameDoesNotExist() {
    when(gameDomainRepository.findById(gameId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.deleteGame(gameId))
        .isInstanceOf(GameNotFoundException.class)
        .hasMessageContaining(gameId.toString());
  }

  @Test
  void regenerateInvitationCodeUpdatesCodeAndExpiration() {
    Game game = buildDomainGame(GameStatus.CREATING);
    LocalDateTime fixedExpiration = LocalDateTime.of(2026, 2, 7, 12, 0);
    when(gameDomainRepository.findById(gameId)).thenReturn(Optional.of(game));
    when(invitationCodeService.generateUniqueCode()).thenReturn("NEWCODE1");
    when(invitationCodeService.calculateExpirationDate(InvitationCodeService.CodeDuration.HOURS_24))
        .thenReturn(fixedExpiration);
    when(gameDomainRepository.save(any(Game.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    GameDto result = service.regenerateInvitationCode(gameId, "24h");

    assertThat(result.getInvitationCode()).isEqualTo("NEWCODE1");
    assertThat(result.getInvitationCodeExpiresAt()).isEqualTo(fixedExpiration);
    verify(invitationCodeService)
        .calculateExpirationDate(InvitationCodeService.CodeDuration.HOURS_24);
  }

  @Test
  void regenerateInvitationCodeWithNullDurationUsesPermanent() {
    Game game = buildDomainGame(GameStatus.CREATING);
    when(gameDomainRepository.findById(gameId)).thenReturn(Optional.of(game));
    when(invitationCodeService.generateUniqueCode()).thenReturn("NEWCODE2");
    when(invitationCodeService.calculateExpirationDate(
            InvitationCodeService.CodeDuration.PERMANENT))
        .thenReturn(null);
    when(gameDomainRepository.save(any(Game.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    GameDto result = service.regenerateInvitationCode(gameId, null);

    assertThat(result.getInvitationCode()).isEqualTo("NEWCODE2");
    assertThat(result.getInvitationCodeExpiresAt()).isNull();
    verify(invitationCodeService)
        .calculateExpirationDate(InvitationCodeService.CodeDuration.PERMANENT);
  }

  @Test
  void regenerateInvitationCodeThrowsWhenGameDoesNotExist() {
    when(gameDomainRepository.findById(gameId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.regenerateInvitationCode(gameId, "24h"))
        .isInstanceOf(GameNotFoundException.class)
        .hasMessageContaining(gameId.toString());
  }

  @Test
  void renameGameUpdatesNameAndReturnsMappedDto() {
    Game game = buildDomainGame(GameStatus.CREATING);
    when(gameDomainRepository.findById(gameId)).thenReturn(Optional.of(game));
    when(gameDomainRepository.save(any(Game.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    GameDto result = service.renameGame(gameId, "New game name");

    assertThat(result.getName()).isEqualTo("New game name");
    verify(gameDomainRepository).save(game);
  }

  @Test
  void renameGameRejectsBlankName() {
    Game game = buildDomainGame(GameStatus.CREATING);
    when(gameDomainRepository.findById(gameId)).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> service.renameGame(gameId, "   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot be null or empty");

    verify(gameDomainRepository, never()).save(any(Game.class));
  }

  @Test
  void renameGameThrowsWhenGameDoesNotExist() {
    when(gameDomainRepository.findById(gameId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.renameGame(gameId, "Rename me"))
        .isInstanceOf(GameNotFoundException.class)
        .hasMessageContaining(gameId.toString());
  }

  // --- createGame ---

  @Test
  void createGameHappyPath() {
    User creator = mockUser(creatorId, "PlayerOne");
    CreateGameRequest request =
        CreateGameRequest.builder().name("My Game").maxParticipants(8).build();
    when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
    when(gameDomainRepository.save(any(Game.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    GameDto result = service.createGame(creatorId, request);

    assertThat(result.getName()).isEqualTo("My Game");
    assertThat(result.getCreatorUsername()).isEqualTo("PlayerOne");
    assertThat(result.getCreatorName()).isEqualTo("PlayerOne");
    verify(validationService).validateCreateGameRequest(request);
    verify(gameDomainRepository).save(any(Game.class));
  }

  @Test
  void createGameWithDescription() {
    User creator = mockUser(creatorId, "Builder");
    CreateGameRequest request =
        CreateGameRequest.builder()
            .name("Described Game")
            .maxParticipants(4)
            .description("A cool game")
            .build();
    when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
    when(gameDomainRepository.save(any(Game.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    GameDto result = service.createGame(creatorId, request);

    assertThat(result.getName()).isEqualTo("Described Game");
    assertThat(result.getDescription()).isEqualTo("A cool game");
  }

  @Test
  void createGameWithRegionRules() {
    User creator = mockUser(creatorId, "RegionFan");
    Map<Player.Region, Integer> regionRules = Map.of(Player.Region.EU, 3, Player.Region.NAW, 2);
    CreateGameRequest request =
        CreateGameRequest.builder()
            .name("Region Game")
            .maxParticipants(10)
            .regionRules(regionRules)
            .build();
    when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
    when(gameDomainRepository.save(any(Game.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    GameDto result = service.createGame(creatorId, request);

    assertThat(result.getName()).isEqualTo("Region Game");
    verify(validationService).validateRegionRules(regionRules);
  }

  @Test
  void createGameWithNullRegionRulesSkipsRegionValidation() {
    User creator = mockUser(creatorId, "NoRegion");
    CreateGameRequest request =
        CreateGameRequest.builder().name("No Region Game").maxParticipants(4).build();
    when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
    when(gameDomainRepository.save(any(Game.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.createGame(creatorId, request);

    verify(validationService, never()).validateRegionRules(any());
  }

  @Test
  void createGameThrowsWhenUserNotFound() {
    when(userRepository.findById(creatorId)).thenReturn(Optional.empty());
    CreateGameRequest request =
        CreateGameRequest.builder().name("Orphan Game").maxParticipants(4).build();

    assertThatThrownBy(() -> service.createGame(creatorId, request))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessageContaining(creatorId.toString());

    verify(gameDomainRepository, never()).save(any(Game.class));
  }

  @Test
  void createGameThrowsWhenRegionRulesInvalid() {
    User creator = mockUser(creatorId, "BadRegion");
    Map<Player.Region, Integer> badRules = Map.of(Player.Region.EU, 99);
    CreateGameRequest request =
        CreateGameRequest.builder()
            .name("Bad Region Game")
            .maxParticipants(4)
            .regionRules(badRules)
            .build();
    when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
    doThrow(new IllegalArgumentException("Too many players"))
        .when(validationService)
        .validateRegionRules(badRules);

    assertThatThrownBy(() -> service.createGame(creatorId, request))
        .isInstanceOf(InvalidGameRequestException.class)
        .hasMessageContaining("Invalid region rules");

    verify(gameDomainRepository, never()).save(any(Game.class));
  }

  @Test
  void createGameAddsCreatorAsHostParticipant() {
    User creator = mockUser(creatorId, "HostPlayer");
    CreateGameRequest request =
        CreateGameRequest.builder().name("Host Game").maxParticipants(6).build();
    when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
    when(gameDomainRepository.save(any(Game.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    GameDto result = service.createGame(creatorId, request);

    assertThat(result.getCurrentParticipantCount()).isGreaterThanOrEqualTo(1);
  }

  @Test
  void regenerateInvitationCodeWithNoDurationDelegatesToOverload() {
    Game game = buildDomainGame(GameStatus.CREATING);
    when(gameDomainRepository.findById(gameId)).thenReturn(Optional.of(game));
    when(invitationCodeService.generateUniqueCode()).thenReturn("OVERLOAD");
    when(invitationCodeService.calculateExpirationDate(
            InvitationCodeService.CodeDuration.PERMANENT))
        .thenReturn(null);
    when(gameDomainRepository.save(any(Game.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    GameDto result = service.regenerateInvitationCode(gameId);

    assertThat(result.getInvitationCode()).isEqualTo("OVERLOAD");
  }

  private User mockUser(UUID id, String username) {
    User user = new User();
    user.setId(id);
    user.setUsername(username);
    return user;
  }

  private Game buildDomainGame(GameStatus status) {
    return Game.restore(
        gameId,
        "Legacy name",
        "description",
        creatorId,
        8,
        status,
        LocalDateTime.of(2026, 2, 7, 10, 0),
        null,
        null,
        "OLDCODE1",
        null,
        List.of(),
        List.of(),
        null,
        false,
        5,
        null,
        2026);
  }
}
