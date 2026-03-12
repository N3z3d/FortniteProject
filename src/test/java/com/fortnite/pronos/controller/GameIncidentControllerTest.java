package com.fortnite.pronos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.fortnite.pronos.dto.IncidentReportRequest;
import com.fortnite.pronos.dto.IncidentReportRequest.IncidentType;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.UserResolver;
import com.fortnite.pronos.service.admin.GameIncidentService;
import com.fortnite.pronos.service.admin.IncidentEntry;
import com.fortnite.pronos.service.admin.IncidentReportingService;

@ExtendWith(MockitoExtension.class)
class GameIncidentControllerTest {

  @Mock private IncidentReportingService incidentReportingService;
  @Mock private GameIncidentService gameIncidentService;
  @Mock private UserResolver userResolver;
  @Mock private HttpServletRequest httpRequest;

  private GameIncidentController controller;

  private final UUID gameId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final String username = "player1";

  @BeforeEach
  void setUp() {
    controller =
        new GameIncidentController(incidentReportingService, gameIncidentService, userResolver);
  }

  private User stubUser() {
    User user = mock(User.class);
    when(user.getId()).thenReturn(userId);
    when(user.getUsername()).thenReturn(username);
    return user;
  }

  private IncidentEntry buildEntry() {
    return IncidentEntry.builder()
        .id(UUID.randomUUID())
        .gameId(gameId)
        .gameName("Test Game")
        .reporterId(userId)
        .reporterUsername(username)
        .incidentType(IncidentType.CHEATING)
        .description("Suspected aimbot")
        .timestamp(OffsetDateTime.now())
        .build();
  }

  private IncidentReportRequest buildRequest() {
    IncidentReportRequest req = new IncidentReportRequest();
    req.setIncidentType(IncidentType.CHEATING);
    req.setDescription("Suspected aimbot");
    return req;
  }

  @Nested
  @DisplayName("Report Incident")
  class ReportIncident {

    @Test
    void withValidParticipant_returns201WithEntry() {
      User user = stubUser();
      when(userResolver.resolve(null, httpRequest)).thenReturn(user);
      IncidentEntry entry = buildEntry();
      when(incidentReportingService.reportIncident(gameId, userId, username, buildRequest()))
          .thenReturn(entry);

      var response = controller.reportIncident(gameId, buildRequest(), null, httpRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().isSuccess()).isTrue();
      assertThat(response.getBody().getData().getGameId()).isEqualTo(gameId);
    }

    @Test
    void whenUserNotResolved_returns401() {
      when(userResolver.resolve(null, httpRequest)).thenReturn(null);

      var response = controller.reportIncident(gameId, buildRequest(), null, httpRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
      verifyNoInteractions(incidentReportingService);
    }

    @Test
    void usernameParamIsForwardedToResolver() {
      when(userResolver.resolve("testUser", httpRequest)).thenReturn(null);

      controller.reportIncident(gameId, buildRequest(), "testUser", httpRequest);

      verify(userResolver).resolve("testUser", httpRequest);
    }
  }

  @Nested
  @DisplayName("Get Incidents")
  class GetIncidents {

    @Test
    void withDefaultLimit_returnsAll() {
      IncidentEntry entry = buildEntry();
      when(gameIncidentService.getRecentIncidents(50, null)).thenReturn(List.of(entry));

      var response = controller.getIncidents(50, null);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody().getData()).hasSize(1);
    }

    @Test
    void limitAboveMax_isClamped() {
      when(gameIncidentService.getRecentIncidents(500, null)).thenReturn(List.of());

      controller.getIncidents(9999, null);

      verify(gameIncidentService).getRecentIncidents(500, null);
    }

    @Test
    void limitBelowMin_isClamped() {
      when(gameIncidentService.getRecentIncidents(1, null)).thenReturn(List.of());

      controller.getIncidents(0, null);

      verify(gameIncidentService).getRecentIncidents(1, null);
    }

    @Test
    void filteredByGameId_passesGameIdToService() {
      when(gameIncidentService.getRecentIncidents(50, gameId)).thenReturn(List.of());

      controller.getIncidents(50, gameId);

      verify(gameIncidentService).getRecentIncidents(50, gameId);
    }
  }
}
