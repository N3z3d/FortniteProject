package com.fortnite.pronos.service.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameParticipant;
import com.fortnite.pronos.domain.game.model.GameStatus;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.PlayerRepository;

@ExtendWith(MockitoExtension.class)
class GameQueryServiceCreatorFallbackTest {

  @Mock private GameDomainRepositoryPort gameRepository;
  @Mock private PlayerRepository playerRepository;
  @Mock private UserRepositoryPort userRepository;

  @InjectMocks private GameQueryService service;

  private UUID userId;
  private UUID creatorId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    creatorId = UUID.randomUUID();
  }

  @Test
  void getGamesByUserResolvesCreatorNameFromUserRepositoryWhenMissingInDomainParticipants() {
    Game domainGame = buildDomainGame(List.of());
    User creator = new User();
    creator.setId(creatorId);
    creator.setUsername("Thibaut");

    when(playerRepository.count()).thenReturn(147L);
    when(gameRepository.findGamesByUserId(userId)).thenReturn(List.of(domainGame));
    when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));

    List<GameDto> result = service.getGamesByUser(userId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getCreatorUsername()).isEqualTo("Thibaut");
    assertThat(result.get(0).getCreatorName()).isEqualTo("Thibaut");
    verify(userRepository).findById(creatorId);
  }

  @Test
  void getGamesByUserKeepsCreatorFromParticipantsWithoutRepositoryFallback() {
    GameParticipant creatorParticipant =
        GameParticipant.restore(
            UUID.randomUUID(),
            creatorId,
            "CreatorInParticipants",
            null,
            LocalDateTime.of(2026, 2, 8, 11, 0),
            null,
            true,
            List.of());
    Game domainGame = buildDomainGame(List.of(creatorParticipant));

    when(playerRepository.count()).thenReturn(147L);
    when(gameRepository.findGamesByUserId(userId)).thenReturn(List.of(domainGame));

    List<GameDto> result = service.getGamesByUser(userId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getCreatorUsername()).isEqualTo("CreatorInParticipants");
    assertThat(result.get(0).getCreatorName()).isEqualTo("CreatorInParticipants");
    verify(userRepository, never()).findById(any());
  }

  private Game buildDomainGame(List<GameParticipant> participants) {
    return Game.restore(
        UUID.randomUUID(),
        "Legacy game",
        "legacy description",
        creatorId,
        8,
        GameStatus.CREATING,
        LocalDateTime.of(2026, 2, 8, 10, 0),
        null,
        null,
        "INV12345",
        null,
        List.of(),
        participants,
        null,
        false,
        5,
        null,
        2026);
  }
}
