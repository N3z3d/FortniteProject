package com.fortnite.pronos.service.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.game.model.DraftMode;
import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.exception.InvalidGameRequestException;
import com.fortnite.pronos.service.InvitationCodeService;
import com.fortnite.pronos.service.ValidationService;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameCreationService - configureCompetitionPeriod")
class GameCreationServicePeriodTest {

  @Mock private GameDomainRepositoryPort gameDomainRepository;
  @Mock private UserRepositoryPort userRepository;
  @Mock private ValidationService validationService;
  @Mock private InvitationCodeService invitationCodeService;

  private GameCreationService service;

  private static final UUID GAME_ID = UUID.randomUUID();
  private static final UUID CREATOR_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service =
        new GameCreationService(
            gameDomainRepository, userRepository, validationService, invitationCodeService);
  }

  private Game buildGame() {
    return new Game("Test Game", CREATOR_ID, 8, DraftMode.SNAKE, 5, 10, true);
  }

  @Nested
  @DisplayName("configureCompetitionPeriod")
  class ConfigureCompetitionPeriod {

    @Test
    void whenValidPeriod_savesAndReturnsGameDto() {
      Game game = buildGame();
      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
      when(gameDomainRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

      LocalDate start = LocalDate.of(2026, 3, 1);
      LocalDate end = LocalDate.of(2026, 3, 31);

      GameDto result = service.configureCompetitionPeriod(GAME_ID, start, end);

      assertThat(result).isNotNull();
      assertThat(result.getCompetitionStart()).isEqualTo(start);
      assertThat(result.getCompetitionEnd()).isEqualTo(end);
      verify(gameDomainRepository).save(any(Game.class));
    }

    @Test
    void whenStartEqualsEnd_acceptsAsSameDay() {
      Game game = buildGame();
      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
      when(gameDomainRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

      LocalDate sameDay = LocalDate.of(2026, 6, 15);

      GameDto result = service.configureCompetitionPeriod(GAME_ID, sameDay, sameDay);

      assertThat(result.getCompetitionStart()).isEqualTo(sameDay);
      assertThat(result.getCompetitionEnd()).isEqualTo(sameDay);
    }

    @Test
    void whenStartAfterEnd_throwsInvalidGameRequestException() {
      LocalDate start = LocalDate.of(2026, 4, 1);
      LocalDate end = LocalDate.of(2026, 3, 1);

      assertThatThrownBy(() -> service.configureCompetitionPeriod(GAME_ID, start, end))
          .isInstanceOf(InvalidGameRequestException.class)
          .hasMessageContaining("before or equal to end");

      verify(gameDomainRepository, never()).save(any());
    }

    @Test
    void whenGameNotFound_throwsGameNotFoundException() {
      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.empty());

      LocalDate start = LocalDate.of(2026, 3, 1);
      LocalDate end = LocalDate.of(2026, 3, 31);

      assertThatThrownBy(() -> service.configureCompetitionPeriod(GAME_ID, start, end))
          .isInstanceOf(GameNotFoundException.class);

      verify(gameDomainRepository, never()).save(any());
    }
  }
}
