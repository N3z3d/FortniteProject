package com.fortnite.pronos.adapter.out.resolution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.player.model.FortnitePlayerData;
import com.fortnite.pronos.domain.port.out.FortniteApiPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("FortniteApiResolutionAdapter")
class FortniteApiResolutionAdapterTest {

  @Mock private FortniteApiPort fortniteApiPort;

  private FortniteApiResolutionAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new FortniteApiResolutionAdapter(fortniteApiPort);
  }

  private static FortnitePlayerData playerData(String epicId, String displayName) {
    return new FortnitePlayerData(epicId, displayName, 0, 0, 0, 0, 0.0, 0.0, 0);
  }

  @Nested
  @DisplayName("resolvePlayer")
  class ResolvePlayer {

    @Test
    @DisplayName("returns full FortnitePlayerData when API finds the player")
    void resolves_when_api_returns_player() {
      when(fortniteApiPort.searchByName("Bugha_EU"))
          .thenReturn(Optional.of(playerData("33f85e8ed7124d15ae29cfaf53340239", "Bugha")));

      Optional<FortnitePlayerData> result = adapter.resolvePlayer("Bugha_EU", "EU");

      assertThat(result).isPresent();
      assertThat(result.get().epicAccountId()).isEqualTo("33f85e8ed7124d15ae29cfaf53340239");
      assertThat(result.get().displayName()).isEqualTo("Bugha");
    }

    @Test
    @DisplayName("returns empty when player not found in API")
    void returns_empty_when_not_found() {
      when(fortniteApiPort.searchByName("Unknown_Player")).thenReturn(Optional.empty());

      Optional<FortnitePlayerData> result = adapter.resolvePlayer("Unknown_Player", "EU");

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("returns empty when API is unavailable (exception)")
    void returns_empty_when_api_unavailable() {
      when(fortniteApiPort.searchByName("Aqua_EU"))
          .thenThrow(new RuntimeException("API unavailable"));

      Optional<FortnitePlayerData> result = adapter.resolvePlayer("Aqua_EU", "EU");

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("returns empty for null or blank pseudo")
    void returns_empty_for_blank_pseudo() {
      assertThat(adapter.resolvePlayer(null, "EU")).isEmpty();
      assertThat(adapter.resolvePlayer("", "EU")).isEmpty();
      assertThat(adapter.resolvePlayer("  ", "EU")).isEmpty();
    }
  }
}
