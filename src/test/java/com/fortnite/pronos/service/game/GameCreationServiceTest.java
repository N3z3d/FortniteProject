package com.fortnite.pronos.service.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.game.model.DraftMode;
import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameStatus;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.dto.CreateGameRequest;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.InvitationCodeService;
import com.fortnite.pronos.service.ValidationService;

@ExtendWith(MockitoExtension.class)
class GameCreationServiceTest {

  @Mock private GameDomainRepositoryPort gameDomainRepository;
  @Mock private UserRepositoryPort userRepository;
  @Mock private ValidationService validationService;
  @Mock private InvitationCodeService invitationCodeService;

  @InjectMocks private GameCreationService service;

  private UUID creatorId;
  private User creator;

  @BeforeEach
  void setUp() {
    creatorId = UUID.randomUUID();
    creator = new User();
    creator.setId(creatorId);
    creator.setUsername("player1");
  }

  private void stubSuccessfulSave() {
    when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
    when(gameDomainRepository.save(any(Game.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Nested
  class NominalCases {

    @Test
    void createGame_withSnakeMode_persistsAllFields() {
      stubSuccessfulSave();
      CreateGameRequest request = new CreateGameRequest();
      request.setName("Snake Draft Game");
      request.setMaxParticipants(8);
      request.setDraftMode(DraftMode.SNAKE);
      request.setTeamSize(5);
      request.setTrancheSize(10);
      request.setTranchesEnabled(true);

      GameDto result = service.createGame(creatorId, request);

      assertThat(result.getName()).isEqualTo("Snake Draft Game");
      assertThat(result.getDraftMode()).isEqualTo(DraftMode.SNAKE);
      assertThat(result.getTeamSize()).isEqualTo(5);
      assertThat(result.getTrancheSize()).isEqualTo(10);
      assertThat(result.getTranchesEnabled()).isTrue();
      assertThat(result.getCompetitionStart()).isNull();
      assertThat(result.getCompetitionEnd()).isNull();
    }

    @Test
    void createGame_withSimultaneousMode_persistsDraftMode() {
      stubSuccessfulSave();
      CreateGameRequest request = new CreateGameRequest();
      request.setName("Simultaneous Game");
      request.setMaxParticipants(6);
      request.setDraftMode(DraftMode.SIMULTANEOUS);
      request.setTeamSize(3);
      request.setTrancheSize(10);
      request.setTranchesEnabled(true);

      GameDto result = service.createGame(creatorId, request);

      assertThat(result.getDraftMode()).isEqualTo(DraftMode.SIMULTANEOUS);
    }

    @Test
    void createGame_withTranchesDisabled_tranchesEnabledIsFalse() {
      stubSuccessfulSave();
      CreateGameRequest request = new CreateGameRequest();
      request.setName("No Tranches Game");
      request.setMaxParticipants(8);
      request.setDraftMode(DraftMode.SNAKE);
      request.setTeamSize(5);
      request.setTranchesEnabled(false);

      GameDto result = service.createGame(creatorId, request);

      assertThat(result.getTranchesEnabled()).isFalse();
    }

    @Test
    void createGame_withCompetitionDates_persistsDates() {
      stubSuccessfulSave();
      LocalDate start = LocalDate.of(2026, 3, 1);
      LocalDate end = LocalDate.of(2026, 5, 31);
      CreateGameRequest request = new CreateGameRequest();
      request.setName("Seasonal Game");
      request.setMaxParticipants(8);
      request.setDraftMode(DraftMode.SNAKE);
      request.setTeamSize(5);
      request.setTrancheSize(10);
      request.setTranchesEnabled(true);
      request.setCompetitionStart(start);
      request.setCompetitionEnd(end);

      GameDto result = service.createGame(creatorId, request);

      assertThat(result.getCompetitionStart()).isEqualTo(start);
      assertThat(result.getCompetitionEnd()).isEqualTo(end);
    }
  }

  @Nested
  class EdgeCases {

    @Test
    void createGame_withNullDraftMode_defaultsToSnake() {
      stubSuccessfulSave();
      CreateGameRequest request = new CreateGameRequest();
      request.setName("Default Mode Game");
      request.setMaxParticipants(8);
      request.setDraftMode(null);
      request.setTeamSize(5);
      request.setTrancheSize(10);
      request.setTranchesEnabled(true);

      GameDto result = service.createGame(creatorId, request);

      assertThat(result.getDraftMode()).isEqualTo(DraftMode.SNAKE);
    }

    @Test
    void createGame_withNullTeamSize_defaultsFive() {
      stubSuccessfulSave();
      CreateGameRequest request = new CreateGameRequest();
      request.setName("Default TeamSize Game");
      request.setMaxParticipants(8);
      request.setDraftMode(DraftMode.SNAKE);
      request.setTeamSize(null);
      request.setTrancheSize(10);
      request.setTranchesEnabled(true);

      GameDto result = service.createGame(creatorId, request);

      assertThat(result.getTeamSize()).isEqualTo(5);
    }

    @Test
    void createGame_withNullTranchesEnabled_defaultsTrue() {
      stubSuccessfulSave();
      CreateGameRequest request = new CreateGameRequest();
      request.setName("Default Tranches Game");
      request.setMaxParticipants(8);
      request.setDraftMode(DraftMode.SNAKE);
      request.setTeamSize(5);
      request.setTrancheSize(10);
      request.setTranchesEnabled(null);

      GameDto result = service.createGame(creatorId, request);

      assertThat(result.getTranchesEnabled()).isTrue();
    }
  }

  @Nested
  class RegenerateInvitationCode {

    private UUID gameId;
    private Game existingGame;

    @BeforeEach
    void setUpGame() {
      gameId = UUID.randomUUID();
      existingGame =
          Game.restore(
              gameId,
              "Test Game",
              null,
              creatorId,
              4,
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
              2026);
    }

    @Test
    void regenerateCode_withNullDuration_returnsCodeWithNullExpiry() {
      when(gameDomainRepository.findById(gameId)).thenReturn(Optional.of(existingGame));
      when(invitationCodeService.generateUniqueCode()).thenReturn("NEWCODE1");
      when(invitationCodeService.calculateExpirationDate(any())).thenReturn(null);
      when(gameDomainRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

      GameDto result = service.regenerateInvitationCode(gameId);

      assertThat(result.getInvitationCode()).isEqualTo("NEWCODE1");
      assertThat(result.getInvitationCodeExpiresAt()).isNull();
    }

    @Test
    void regenerateCode_withDuration_returnsCodeWithExpiry() {
      LocalDateTime expiry = LocalDateTime.now().plusHours(48);
      when(gameDomainRepository.findById(gameId)).thenReturn(Optional.of(existingGame));
      when(invitationCodeService.generateUniqueCode()).thenReturn("NEWCODE2");
      when(invitationCodeService.calculateExpirationDate(any())).thenReturn(expiry);
      when(gameDomainRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

      GameDto result = service.regenerateInvitationCode(gameId, "48h");

      assertThat(result.getInvitationCode()).isEqualTo("NEWCODE2");
      assertThat(result.getInvitationCodeExpiresAt()).isEqualTo(expiry);
    }

    @Test
    void regenerateCode_withUnknownGameId_throwsGameNotFoundException() {
      when(gameDomainRepository.findById(gameId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.regenerateInvitationCode(gameId))
          .isInstanceOf(GameNotFoundException.class);
    }
  }

  @Nested
  class DomainValidation {

    @Test
    void domainGame_withZeroTeamSize_throwsIllegalArgument() {
      assertThatThrownBy(() -> new Game("Bad Game", creatorId, 8, DraftMode.SNAKE, 0, 10, true))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Team size must be >= 1");
    }

    @Test
    void domainGame_withZeroTrancheSize_whenEnabled_throwsIllegalArgument() {
      assertThatThrownBy(() -> new Game("Bad Game", creatorId, 8, DraftMode.SNAKE, 5, 0, true))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Tranche size must be >= 1");
    }

    @Test
    void domainGame_withNullDraftMode_throwsIllegalArgument() {
      assertThatThrownBy(() -> new Game("Bad Game", creatorId, 8, null, 5, 10, true))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Draft mode cannot be null");
    }
  }
}
