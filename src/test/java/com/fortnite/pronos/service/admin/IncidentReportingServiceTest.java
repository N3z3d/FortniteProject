package com.fortnite.pronos.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.dto.IncidentReportRequest;
import com.fortnite.pronos.dto.IncidentReportRequest.IncidentType;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.exception.UnauthorizedAccessException;
import com.fortnite.pronos.service.game.GameParticipantService;

@ExtendWith(MockitoExtension.class)
@DisplayName("IncidentReportingService")
class IncidentReportingServiceTest {

  @Mock private GameDomainRepositoryPort gameDomainRepository;
  @Mock private GameParticipantService gameParticipantService;
  @Mock private GameIncidentService gameIncidentService;

  @InjectMocks private IncidentReportingService service;

  private final UUID gameId = UUID.randomUUID();
  private final UUID reporterId = UUID.randomUUID();
  private final String reporterUsername = "player1";

  private IncidentReportRequest buildRequest() {
    IncidentReportRequest req = new IncidentReportRequest();
    req.setIncidentType(IncidentType.BUG);
    req.setDescription("Game froze during pick");
    return req;
  }

  private Game mockGame(String name) {
    Game game = org.mockito.Mockito.mock(Game.class);
    when(game.getName()).thenReturn(name);
    return game;
  }

  @Nested
  @DisplayName("Nominal cases")
  class NominalCases {

    @Test
    @DisplayName("Valid participant — returns recorded IncidentEntry with correct fields")
    void validParticipant_returnsEntryWithCorrectFields() {
      Game game = mockGame("Test Game");
      when(gameDomainRepository.findById(gameId)).thenReturn(Optional.of(game));
      when(gameParticipantService.isUserParticipant(reporterId, gameId)).thenReturn(true);

      IncidentEntry result =
          service.reportIncident(gameId, reporterId, reporterUsername, buildRequest());

      assertThat(result.getGameId()).isEqualTo(gameId);
      assertThat(result.getReporterId()).isEqualTo(reporterId);
      assertThat(result.getReporterUsername()).isEqualTo(reporterUsername);
      assertThat(result.getIncidentType()).isEqualTo(IncidentType.BUG);
      assertThat(result.getDescription()).isEqualTo("Game froze during pick");
      assertThat(result.getGameName()).isEqualTo("Test Game");
      assertThat(result.getId()).isNotNull();
      assertThat(result.getTimestamp()).isNotNull();
      verify(gameIncidentService).recordIncident(any(IncidentEntry.class));
    }
  }

  @Nested
  @DisplayName("Edge cases")
  class EdgeCases {

    @Test
    @DisplayName("Unknown game — throws GameNotFoundException, incident not recorded")
    void unknownGame_throwsGameNotFoundException() {
      when(gameDomainRepository.findById(gameId)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> service.reportIncident(gameId, reporterId, reporterUsername, buildRequest()))
          .isInstanceOf(GameNotFoundException.class);

      verify(gameIncidentService, never()).recordIncident(any());
    }

    @Test
    @DisplayName("Non-participant — throws UnauthorizedAccessException, incident not recorded")
    void nonParticipant_throwsUnauthorizedAccessException() {
      // Do not stub game.getName() — exception is thrown before it is called
      Game game = org.mockito.Mockito.mock(Game.class);
      when(gameDomainRepository.findById(gameId)).thenReturn(Optional.of(game));
      when(gameParticipantService.isUserParticipant(reporterId, gameId)).thenReturn(false);

      assertThatThrownBy(
              () -> service.reportIncident(gameId, reporterId, reporterUsername, buildRequest()))
          .isInstanceOf(UnauthorizedAccessException.class);

      verify(gameIncidentService, never()).recordIncident(any());
    }
  }
}
