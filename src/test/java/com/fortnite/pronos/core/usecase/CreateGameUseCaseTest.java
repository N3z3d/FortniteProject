package com.fortnite.pronos.core.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.dto.CreateGameRequest;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.exception.InvalidGameRequestException;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.ValidationService;

@ExtendWith(MockitoExtension.class)
class CreateGameUseCaseTest {

  @Mock private GameRepositoryPort gameRepositoryPort;

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
    UUID gameId = UUID.randomUUID();

    when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
    when(gameRepositoryPort.countByCreatorAndStatusIn(eq(user), anyList())).thenReturn(0L);
    when(gameRepositoryPort.save(any(Game.class)))
        .thenAnswer(
            invocation -> {
              Game game = invocation.getArgument(0);
              game.setId(gameId);
              return game;
            });

    GameDto result = createGameUseCase.execute(userId, request);

    assertThat(result.getId()).isEqualTo(gameId);
    assertThat(result.getCreatorId()).isEqualTo(userId);
    assertThat(result.getStatus()).isEqualTo(GameStatus.CREATING);
    assertThat(result.getCurrentParticipantCount()).isEqualTo(1);
    assertThat(request.getCreatorId()).isEqualTo(userId);

    verify(validationService).validateCreateGameRequest(request);
    verify(userRepositoryPort).findById(userId);
    verify(gameRepositoryPort).countByCreatorAndStatusIn(eq(user), anyList());
    verify(gameRepositoryPort).save(any(Game.class));
  }

  @Test
  void shouldCreateFallbackUserWhenMissing() {
    UUID gameId = UUID.randomUUID();

    when(userRepositoryPort.findById(userId)).thenReturn(Optional.empty());
    when(userRepositoryPort.findByUsername(anyString())).thenReturn(Optional.empty());
    when(userRepositoryPort.save(any(User.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(gameRepositoryPort.countByCreatorAndStatusIn(any(User.class), anyList())).thenReturn(0L);
    when(gameRepositoryPort.save(any(Game.class)))
        .thenAnswer(
            invocation -> {
              Game game = invocation.getArgument(0);
              game.setId(gameId);
              return game;
            });

    GameDto result = createGameUseCase.execute(userId, request);

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepositoryPort).save(userCaptor.capture());
    User created = userCaptor.getValue();

    assertThat(created.getUsername()).startsWith("auto-");
    assertThat(created.getRole()).isEqualTo(User.UserRole.ADMIN);
    assertThat(request.getCreatorId()).isEqualTo(created.getId());
    assertThat(result.getCreatorId()).isEqualTo(created.getId());
  }

  @Test
  void shouldRejectUserWithoutRole() {
    user.setRole(null);
    when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> createGameUseCase.execute(userId, request))
        .isInstanceOf(InvalidGameRequestException.class)
        .hasMessageContaining("valid role");

    verify(gameRepositoryPort, never()).save(any(Game.class));
    verify(gameRepositoryPort, never()).countByCreatorAndStatusIn(any(User.class), anyList());
  }

  @Test
  void shouldRejectWhenActiveGameLimitReached() {
    user.setRole(User.UserRole.USER);
    when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
    when(gameRepositoryPort.countByCreatorAndStatusIn(eq(user), anyList())).thenReturn(5L);

    assertThatThrownBy(() -> createGameUseCase.execute(userId, request))
        .isInstanceOf(InvalidGameRequestException.class)
        .hasMessageContaining("more than 5 active games");

    verify(gameRepositoryPort, never()).save(any(Game.class));
  }
}
