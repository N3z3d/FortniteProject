package com.fortnite.pronos.service.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fortnite.pronos.dto.IncidentReportRequest.IncidentType;

class GameIncidentServiceTest {

  private GameIncidentService service;

  @BeforeEach
  void setUp() {
    service = new GameIncidentService();
  }

  private IncidentEntry entry(UUID gameId, IncidentType type) {
    return IncidentEntry.builder()
        .id(UUID.randomUUID())
        .gameId(gameId)
        .gameName("Test Game")
        .reporterId(UUID.randomUUID())
        .reporterUsername("player1")
        .incidentType(type)
        .description("Test description")
        .timestamp(OffsetDateTime.now())
        .build();
  }

  @Nested
  class NominalCases {

    @Test
    void recordIncident_isRetrievableViaGetRecent() {
      UUID gameId = UUID.randomUUID();
      IncidentEntry incident = entry(gameId, IncidentType.BUG);

      service.recordIncident(incident);
      List<IncidentEntry> result = service.getRecentIncidents(10, null);

      assertThat(result)
          .hasSize(1)
          .first()
          .extracting(IncidentEntry::getId)
          .isEqualTo(incident.getId());
    }

    @Test
    void getRecentIncidents_filteredByGameId_returnsOnlyMatchingGame() {
      UUID gameId1 = UUID.randomUUID();
      UUID gameId2 = UUID.randomUUID();
      service.recordIncident(entry(gameId1, IncidentType.CHEATING));
      service.recordIncident(entry(gameId2, IncidentType.ABUSE));
      service.recordIncident(entry(gameId1, IncidentType.BUG));

      List<IncidentEntry> result = service.getRecentIncidents(10, gameId1);

      assertThat(result).hasSize(2).extracting(IncidentEntry::getGameId).containsOnly(gameId1);
    }

    @Test
    void getRecentIncidents_noFilter_returnsAll() {
      UUID gameId = UUID.randomUUID();
      service.recordIncident(entry(gameId, IncidentType.CHEATING));
      service.recordIncident(entry(UUID.randomUUID(), IncidentType.DISPUTE));

      List<IncidentEntry> result = service.getRecentIncidents(50, null);

      assertThat(result).hasSize(2);
    }

    @Test
    void getRecentIncidents_mostRecentFirst() {
      UUID gameId = UUID.randomUUID();
      IncidentEntry first = entry(gameId, IncidentType.BUG);
      IncidentEntry second = entry(gameId, IncidentType.ABUSE);
      service.recordIncident(first);
      service.recordIncident(second);

      List<IncidentEntry> result = service.getRecentIncidents(10, null);

      // addFirst → second recorded last is first in deque
      assertThat(result.get(0).getId()).isEqualTo(second.getId());
    }
  }

  @Nested
  class EdgeCases {

    @Test
    void getRecentIncidents_limitRespected() {
      UUID gameId = UUID.randomUUID();
      for (int i = 0; i < 10; i++) {
        service.recordIncident(entry(gameId, IncidentType.OTHER));
      }

      List<IncidentEntry> result = service.getRecentIncidents(3, null);

      assertThat(result).hasSize(3);
    }

    @Test
    void recordIncident_bufferFull_oldestEvicted() {
      UUID gameId = UUID.randomUUID();
      IncidentEntry oldest = entry(gameId, IncidentType.BUG);
      service.recordIncident(oldest);

      // Fill to max
      for (int i = 0; i < GameIncidentService.MAX_ENTRIES; i++) {
        service.recordIncident(entry(gameId, IncidentType.OTHER));
      }

      assertThat(service.getCurrentSize()).isEqualTo(GameIncidentService.MAX_ENTRIES);
      // oldest is evicted — it was recorded first (addFirst → lands at end → pollLast)
      List<IncidentEntry> all = service.getRecentIncidents(GameIncidentService.MAX_ENTRIES, null);
      assertThat(all).noneMatch(e -> e.getId().equals(oldest.getId()));
    }

    @Test
    void getRecentIncidents_emptyBuffer_returnsEmptyList() {
      assertThat(service.getRecentIncidents(50, null)).isEmpty();
    }
  }
}
