package com.fortnite.pronos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.dto.admin.GameSupervisionDto;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.service.admin.AdminGameSupervisionService;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminGameSupervisionController")
class AdminGameSupervisionControllerTest {

  @Mock private AdminGameSupervisionService supervisionService;

  private AdminGameSupervisionController controller;

  @BeforeEach
  void setUp() {
    controller = new AdminGameSupervisionController(supervisionService);
  }

  private GameSupervisionDto sampleDto() {
    return new GameSupervisionDto(
        UUID.randomUUID(), "Test Game", "ACTIVE", "SNAKE", 3, 10, "creator", OffsetDateTime.now());
  }

  @Nested
  @DisplayName("GET /games (no filter)")
  class GetAllGames {

    @Test
    @DisplayName("returns 200 with all active games")
    void returns200WithGames() {
      when(supervisionService.getAllActiveGames()).thenReturn(List.of(sampleDto()));

      var response = controller.getGames(null);

      assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
      assertThat(response.getBody()).hasSize(1);
      verify(supervisionService).getAllActiveGames();
    }

    @Test
    @DisplayName("returns 200 with empty list when no games")
    void returns200Empty() {
      when(supervisionService.getAllActiveGames()).thenReturn(List.of());

      var response = controller.getGames(null);

      assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
      assertThat(response.getBody()).isEmpty();
    }
  }

  @Nested
  @DisplayName("GET /games?status=DRAFTING")
  class GetGamesByStatus {

    @Test
    @DisplayName("delegates to getActiveGamesByStatus when status provided")
    void delegatesToFilteredService() {
      when(supervisionService.getActiveGamesByStatus(GameStatus.DRAFTING))
          .thenReturn(List.of(sampleDto()));

      var response = controller.getGames(GameStatus.DRAFTING);

      assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
      assertThat(response.getBody()).hasSize(1);
      verify(supervisionService).getActiveGamesByStatus(GameStatus.DRAFTING);
    }
  }
}
