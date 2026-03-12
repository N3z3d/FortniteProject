package com.fortnite.pronos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.fortnite.pronos.application.usecase.PlayerQueryUseCase;
import com.fortnite.pronos.domain.player.model.FortnitePlayerData;
import com.fortnite.pronos.service.RankSnapshotService;
import com.fortnite.pronos.service.catalogue.FortnitePlayerSearchService;
import com.fortnite.pronos.service.catalogue.PlayerCatalogueService;
import com.fortnite.pronos.service.catalogue.PlayerDetailService;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerController - Fortnite Search endpoint")
class PlayerFortniteSearchControllerTest {

  @Mock private PlayerQueryUseCase playerQueryUseCase;
  @Mock private RankSnapshotService rankSnapshotService;
  @Mock private PlayerCatalogueService playerCatalogueService;
  @Mock private PlayerDetailService playerDetailService;
  @Mock private FortnitePlayerSearchService fortnitePlayerSearchService;

  private PlayerController controller;

  @BeforeEach
  void setUp() {
    controller =
        new PlayerController(
            playerQueryUseCase,
            rankSnapshotService,
            playerCatalogueService,
            playerDetailService,
            fortnitePlayerSearchService);
  }

  private FortnitePlayerData buildPlayer(String epicId, String name) {
    return new FortnitePlayerData(epicId, name, 100, 50, 500, 300, 2.0, 5.0, 120);
  }

  @Nested
  @DisplayName("GET /players/fortnite-search")
  class SearchOnFortniteApi {

    @Test
    void shouldReturn200WithPlayerDataWhenFound() {
      when(fortnitePlayerSearchService.searchByName("Ninja"))
          .thenReturn(Optional.of(buildPlayer("epic-1", "Ninja")));

      var response = controller.searchOnFortniteApi("Ninja");

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().displayName()).isEqualTo("Ninja");
      assertThat(response.getBody().epicAccountId()).isEqualTo("epic-1");
      assertThat(response.getBody().wins()).isEqualTo(50);
    }

    @Test
    void shouldReturn200NullWhenPlayerNotFound() {
      when(fortnitePlayerSearchService.searchByName("UnknownPlayer")).thenReturn(Optional.empty());

      var response = controller.searchOnFortniteApi("UnknownPlayer");

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNull();
    }

    @Test
    void shouldDelegateToSearchServiceWithPassedName() {
      when(fortnitePlayerSearchService.searchByName("Bugha"))
          .thenReturn(Optional.of(buildPlayer("epic-2", "Bugha")));

      controller.searchOnFortniteApi("Bugha");

      verify(fortnitePlayerSearchService).searchByName("Bugha");
    }

    @Test
    void shouldReturnAllPlayerFields() {
      var player = new FortnitePlayerData("epic-3", "Clix", 200, 100, 2000, 800, 3.5, 12.5, 600);
      when(fortnitePlayerSearchService.searchByName("Clix")).thenReturn(Optional.of(player));

      var response = controller.searchOnFortniteApi("Clix");

      var dto = response.getBody();
      assertThat(dto).isNotNull();
      assertThat(dto.battlePassLevel()).isEqualTo(200);
      assertThat(dto.kills()).isEqualTo(2000);
      assertThat(dto.matches()).isEqualTo(800);
      assertThat(dto.kd()).isEqualTo(3.5);
      assertThat(dto.winRate()).isEqualTo(12.5);
      assertThat(dto.minutesPlayed()).isEqualTo(600);
    }
  }
}
