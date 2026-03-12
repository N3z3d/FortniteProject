package com.fortnite.pronos.service.catalogue;

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

import com.fortnite.pronos.domain.player.model.FortnitePlayerData;
import com.fortnite.pronos.domain.port.out.FortniteApiPort;

@ExtendWith(MockitoExtension.class)
class FortnitePlayerSearchServiceTest {

  @Mock private FortniteApiPort fortniteApiPort;

  private FortnitePlayerSearchService service;

  @BeforeEach
  void setUp() {
    service = new FortnitePlayerSearchService(fortniteApiPort);
  }

  private FortnitePlayerData buildPlayer(String epicId, String name) {
    return new FortnitePlayerData(epicId, name, 100, 50, 500, 300, 2.0, 5.0, 120);
  }

  @Nested
  @DisplayName("searchByName")
  class SearchByName {

    @Test
    void shouldReturnPlayerFromPort() {
      when(fortniteApiPort.searchByName("Ninja"))
          .thenReturn(Optional.of(buildPlayer("epic-1", "Ninja")));

      Optional<FortnitePlayerData> result = service.searchByName("Ninja");

      assertThat(result).isPresent();
      assertThat(result.get().displayName()).isEqualTo("Ninja");
    }

    @Test
    void shouldReturnEmptyForBlankName() {
      Optional<FortnitePlayerData> result = service.searchByName("  ");
      assertThat(result).isEmpty();
      verifyNoInteractions(fortniteApiPort);
    }

    @Test
    void shouldReturnEmptyForNullName() {
      Optional<FortnitePlayerData> result = service.searchByName(null);
      assertThat(result).isEmpty();
      verifyNoInteractions(fortniteApiPort);
    }

    @Test
    void shouldTrimNameBeforeCallingPort() {
      when(fortniteApiPort.searchByName("Bugha"))
          .thenReturn(Optional.of(buildPlayer("epic-2", "Bugha")));

      service.searchByName("  Bugha  ");

      verify(fortniteApiPort).searchByName("Bugha");
    }

    @Test
    void shouldPropagateEmptyFromPort() {
      when(fortniteApiPort.searchByName("NoSuchPlayer")).thenReturn(Optional.empty());

      Optional<FortnitePlayerData> result = service.searchByName("NoSuchPlayer");

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("fetchByEpicId")
  class FetchByEpicId {

    @Test
    void shouldReturnPlayerFromPort() {
      when(fortniteApiPort.fetchByEpicId("epic-99"))
          .thenReturn(Optional.of(buildPlayer("epic-99", "SomePlayer")));

      Optional<FortnitePlayerData> result = service.fetchByEpicId("epic-99");

      assertThat(result).isPresent();
      assertThat(result.get().epicAccountId()).isEqualTo("epic-99");
    }

    @Test
    void shouldReturnEmptyForBlankEpicId() {
      Optional<FortnitePlayerData> result = service.fetchByEpicId("");
      assertThat(result).isEmpty();
      verifyNoInteractions(fortniteApiPort);
    }

    @Test
    void shouldReturnEmptyForNullEpicId() {
      Optional<FortnitePlayerData> result = service.fetchByEpicId(null);
      assertThat(result).isEmpty();
      verifyNoInteractions(fortniteApiPort);
    }
  }
}
