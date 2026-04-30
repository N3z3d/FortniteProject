package com.fortnite.pronos.core.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.dto.CreateGameRequest;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.exception.InvalidGameRequestException;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.ValidationService;

@ExtendWith(MockitoExtension.class)
class CreateGameUseCaseTest {

  @Mock private GameDomainRepositoryPort gameDomainRepositoryPort;

  @Mock private UserRepositoryPort userRepositoryPort;

  @Mock private ValidationService validationService;

  @InjectMocks private CreateGameUseCase createGameUseCase;

  private UUID userId;
  private CreateGameRequest request;
  private User user;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    request =
        CreateGameRequest.builder()
            .name("Test Game")
            .description("Test Description")
            .maxParticipants(10)
            .regionRules(Map.of(Player.Region.EU, 5, Player.Region.NAC, 5))
            .build();
    user = new User();
    user.setId(userId);
    user.setUsername("tester");
    user.setEmail("tester@example.com");
    user.setRole(User.UserRole.ADMIN);
    user.setCurrentSeason(2025);
  }

  @Test
  void shouldCreateGameWhenUserIsValid() {
    when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
    when(gameDomainRepositoryPort.countByCreatorAndStatusIn(eq(userId), anyList())).thenReturn(0L);
    when(gameDomainRepositoryPort.save(any(Game.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    GameDto result = createGameUseCase.execute(userId, request);

    assertThat(result.getName()).isEqualTo("Test Game");
    assertThat(result.getCreatorId()).isEqualTo(userId);
    assertThat(result.getStatus()).isEqualTo(com.fortnite.pronos.model.GameStatus.CREATING);
    assertThat(result.getCurrentParticipantCount()).isEqualTo(1);
    assertThat(result.getInvitationCode()).isNull();
    assertThat(request.getCreatorId()).isEqualTo(userId);

    ArgumentCaptor<Game> gameCaptor = ArgumentCaptor.forClass(Game.class);
    verify(validationService).validateCreateGameRequest(request);
    verify(userRepositoryPort).findById(userId);
    verify(gameDomainRepositoryPort).countByCreatorAndStatusIn(eq(userId), anyList());
    verify(gameDomainRepositoryPort).save(gameCaptor.capture());
    assertThat(gameCaptor.getValue().getParticipants())
        .singleElement()
        .satisfies(
            participant -> {
              assertThat(participant.getUserId()).isEqualTo(userId);
              assertThat(participant.isCreator()).isTrue();
              assertThat(participant.getDraftOrder()).isEqualTo(1);
            });
    // No invitation code generated at creation (manual generation policy)
  }

  @Test
  void shouldPersistRequestedRegionRulesInCreatedGame() {
    request.setRegionRules(
        Map.of(Player.Region.EU, 1, Player.Region.NAW, 1, Player.Region.ASIA, 1));

    when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
    when(gameDomainRepositoryPort.countByCreatorAndStatusIn(eq(userId), anyList())).thenReturn(0L);
    when(gameDomainRepositoryPort.save(any(Game.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    createGameUseCase.execute(userId, request);

    ArgumentCaptor<Game> gameCaptor = ArgumentCaptor.forClass(Game.class);
    verify(gameDomainRepositoryPort).save(gameCaptor.capture());

    Map<PlayerRegion, Integer> persistedRules =
        gameCaptor.getValue().getRegionRules().stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    rule -> rule.getRegion(), rule -> rule.getMaxPlayers()));

    assertThat(persistedRules)
        .containsEntry(PlayerRegion.EU, 1)
        .containsEntry(PlayerRegion.NAW, 1)
        .containsEntry(PlayerRegion.ASIA, 1)
        .hasSize(3);
  }

  @Test
  void shouldRejectWhenRequestOmitsRegionRules() {
    request.setRegionRules(null);
    when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
    when(gameDomainRepositoryPort.countByCreatorAndStatusIn(eq(userId), anyList())).thenReturn(0L);

    assertThatThrownBy(() -> createGameUseCase.execute(userId, request))
        .isInstanceOf(InvalidGameRequestException.class)
        .hasMessageContaining("regionRules");

    verify(userRepositoryPort).findById(userId);
    verify(gameDomainRepositoryPort, never()).save(any(Game.class));
  }

  @Test
  void shouldRejectWhenRequestContainsEmptyRegionRules() {
    request.setRegionRules(Map.of());
    when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
    when(gameDomainRepositoryPort.countByCreatorAndStatusIn(eq(userId), anyList())).thenReturn(0L);

    assertThatThrownBy(() -> createGameUseCase.execute(userId, request))
        .isInstanceOf(InvalidGameRequestException.class)
        .hasMessageContaining("regionRules");

    verify(userRepositoryPort).findById(userId);
    verify(gameDomainRepositoryPort, never()).save(any(Game.class));
  }

  @Test
  void shouldCreateFallbackUserWhenMissing() {
    when(userRepositoryPort.findById(userId)).thenReturn(Optional.empty());
    when(userRepositoryPort.findByUsername("auto-" + userId.toString().substring(0, 8)))
        .thenReturn(Optional.empty());
    when(userRepositoryPort.save(any(User.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(gameDomainRepositoryPort.countByCreatorAndStatusIn(any(UUID.class), anyList()))
        .thenReturn(0L);
    when(gameDomainRepositoryPort.save(any(Game.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    GameDto result = createGameUseCase.execute(userId, request);

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepositoryPort).save(userCaptor.capture());
    User created = userCaptor.getValue();

    assertThat(created.getId()).isEqualTo(userId);
    assertThat(created.getUsername()).isEqualTo("auto-" + userId.toString().substring(0, 8));
    assertThat(created.getRole()).isEqualTo(User.UserRole.ADMIN);
    assertThat(request.getCreatorId()).isEqualTo(userId);
    assertThat(result.getCreatorId()).isEqualTo(userId);
    assertThat(result.getInvitationCode()).isNull();
    // No invitation code generated at creation (manual generation policy)
  }

  @Test
  void shouldRejectUserWithoutRole() {
    user.setRole(null);
    when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> createGameUseCase.execute(userId, request))
        .isInstanceOf(InvalidGameRequestException.class)
        .hasMessageContaining("valid role");

    verify(gameDomainRepositoryPort, never()).save(any(Game.class));
    verify(gameDomainRepositoryPort, never()).countByCreatorAndStatusIn(any(UUID.class), anyList());
  }

  @Test
  void shouldRejectWhenActiveGameLimitReached() {
    user.setRole(User.UserRole.USER);
    when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
    when(gameDomainRepositoryPort.countByCreatorAndStatusIn(eq(userId), anyList())).thenReturn(5L);

    assertThatThrownBy(() -> createGameUseCase.execute(userId, request))
        .isInstanceOf(InvalidGameRequestException.class)
        .hasMessageContaining("more than 5 active games");

    verify(gameDomainRepositoryPort, never()).save(any(Game.class));
  }

  @Test
  void shouldDefineNamedConstantsForLimitsAndFallback() {
    assertThatCode(
            () -> {
              CreateGameUseCase.class.getDeclaredField("ADMIN_MAX_ACTIVE_GAMES");
              CreateGameUseCase.class.getDeclaredField("USER_MAX_ACTIVE_GAMES");
              CreateGameUseCase.class.getDeclaredField("AUTO_USERNAME_PREFIX_LENGTH");
              CreateGameUseCase.class.getDeclaredField("DEFAULT_FALLBACK_SEASON");
            })
        .doesNotThrowAnyException();
  }
}
