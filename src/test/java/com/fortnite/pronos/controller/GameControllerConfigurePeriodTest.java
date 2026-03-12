package com.fortnite.pronos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.fortnite.pronos.application.usecase.GameQueryUseCase;
import com.fortnite.pronos.core.usecase.CreateGameUseCase;
import com.fortnite.pronos.dto.ConfigureCompetitionPeriodRequest;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.GameService;
import com.fortnite.pronos.service.InvitationCodeAttemptGuard;
import com.fortnite.pronos.service.UserResolver;
import com.fortnite.pronos.service.ValidationService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GameController - configureCompetitionPeriod")
class GameControllerConfigurePeriodTest {

  @Mock private GameService gameService;
  @Mock private GameQueryUseCase gameQueryUseCase;
  @Mock private ValidationService validationService;
  @Mock private UserResolver userResolver;
  @Mock private CreateGameUseCase createGameUseCase;
  @Mock private InvitationCodeAttemptGuard invitationCodeAttemptGuard;

  @InjectMocks private GameController controller;

  private static final UUID GAME_ID = UUID.randomUUID();
  private static final UUID CREATOR_ID = UUID.randomUUID();
  private static final UUID OTHER_USER_ID = UUID.randomUUID();

  private HttpServletRequest httpRequest;
  private ConfigureCompetitionPeriodRequest validRequest;
  private GameDto gameDto;

  @BeforeEach
  void setUp() {
    httpRequest = new MockHttpServletRequest();
    validRequest =
        new ConfigureCompetitionPeriodRequest(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
    gameDto = new GameDto();
    gameDto.setId(GAME_ID);
    gameDto.setCreatorId(CREATOR_ID);
  }

  private User buildUser(UUID userId, User.UserRole role) {
    User user = new User();
    user.setId(userId);
    user.setUsername("user-" + userId);
    user.setRole(role);
    return user;
  }

  @Nested
  @DisplayName("configureCompetitionPeriod endpoint")
  class ConfigureCompetitionPeriodEndpoint {

    @Test
    void whenCreator_returns200WithUpdatedGame() {
      User creator = buildUser(CREATOR_ID, User.UserRole.USER);
      when(userResolver.resolve(null, httpRequest)).thenReturn(creator);
      when(gameQueryUseCase.getGameByIdOrThrow(GAME_ID)).thenReturn(gameDto);
      GameDto updated = new GameDto();
      updated.setId(GAME_ID);
      updated.setCompetitionStart(validRequest.startDate());
      updated.setCompetitionEnd(validRequest.endDate());
      when(gameService.configureCompetitionPeriod(
              GAME_ID, validRequest.startDate(), validRequest.endDate()))
          .thenReturn(updated);

      ResponseEntity<GameDto> response =
          controller.configureCompetitionPeriod(GAME_ID, validRequest, null, httpRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCompetitionStart()).isEqualTo(validRequest.startDate());
      verify(gameService)
          .configureCompetitionPeriod(GAME_ID, validRequest.startDate(), validRequest.endDate());
    }

    @Test
    void whenAdminNotCreator_returns200() {
      User admin = buildUser(OTHER_USER_ID, User.UserRole.ADMIN);
      when(userResolver.resolve(null, httpRequest)).thenReturn(admin);
      when(gameQueryUseCase.getGameByIdOrThrow(GAME_ID)).thenReturn(gameDto);
      when(gameService.configureCompetitionPeriod(
              GAME_ID, validRequest.startDate(), validRequest.endDate()))
          .thenReturn(gameDto);

      ResponseEntity<GameDto> response =
          controller.configureCompetitionPeriod(GAME_ID, validRequest, null, httpRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      verify(gameService)
          .configureCompetitionPeriod(GAME_ID, validRequest.startDate(), validRequest.endDate());
    }

    @Test
    void whenUnauthenticated_returns401() {
      when(userResolver.resolve(null, httpRequest)).thenReturn(null);

      ResponseEntity<GameDto> response =
          controller.configureCompetitionPeriod(GAME_ID, validRequest, null, httpRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
      verifyNoInteractions(gameService);
    }

    @Test
    void whenNeitherAdminNorCreator_returns403() {
      User other = buildUser(OTHER_USER_ID, User.UserRole.USER);
      when(userResolver.resolve(null, httpRequest)).thenReturn(other);
      when(gameQueryUseCase.getGameByIdOrThrow(GAME_ID)).thenReturn(gameDto);

      ResponseEntity<GameDto> response =
          controller.configureCompetitionPeriod(GAME_ID, validRequest, null, httpRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
      verifyNoInteractions(gameService);
    }
  }
}
