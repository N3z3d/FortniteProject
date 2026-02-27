package com.fortnite.pronos.service.ingestion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.player.identity.model.PlayerIdentityEntry;
import com.fortnite.pronos.domain.port.out.PlayerAliasRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerIdentityRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerRepositoryPort;
import com.fortnite.pronos.domain.port.out.ScoreRepositoryPort;
import com.fortnite.pronos.model.IngestionRun;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.repository.PrSnapshotRepository;
import com.fortnite.pronos.service.ingestion.PrIngestionService.PrIngestionConfig;

@ExtendWith(MockitoExtension.class)
@DisplayName("PrIngestionRowProcessor — UNRESOLVED identity queuing")
class PrIngestionRowProcessorUnresolvedTest {

  @Mock private PlayerRepositoryPort playerRepository;
  @Mock private PrSnapshotRepository prSnapshotRepository;
  @Mock private ScoreRepositoryPort scoreRepository;
  @Mock private PlayerIdentityRepositoryPort identityRepository;
  @Mock private PlayerAliasRepositoryPort aliasRepository;

  private PrIngestionRowProcessor processor;
  private PrIngestionConfig config;
  private IngestionRun run;

  @BeforeEach
  void setUp() {
    processor =
        new PrIngestionRowProcessor(
            playerRepository,
            prSnapshotRepository,
            scoreRepository,
            identityRepository,
            aliasRepository);
    config = new PrIngestionConfig("TEST", 2025, false);
    run = new IngestionRun();
  }

  private PrCsvParser.PrCsvRow row(String nickname, String region) {
    return new PrCsvParser.PrCsvRow(nickname, region, 100000, 1, LocalDate.of(2025, 1, 10));
  }

  @Nested
  @DisplayName("New player creation")
  class NewPlayerCreation {

    @Test
    @DisplayName("Queues UNRESOLVED identity entry when new player is created")
    void newPlayer_queuesUnresolvedIdentityEntry() {
      when(playerRepository.findByNickname("PlayerA")).thenReturn(Optional.empty());
      when(prSnapshotRepository.findById(any())).thenReturn(Optional.empty());

      processor.persistRows(List.of(row("PlayerA", "EU")), config, run);

      verify(identityRepository, times(1)).save(any(PlayerIdentityEntry.class));
    }

    @Test
    @DisplayName("Queues one identity entry per distinct new player")
    void multipleNewPlayers_queuesOneEntryEach() {
      when(playerRepository.findByNickname(any())).thenReturn(Optional.empty());
      when(prSnapshotRepository.findById(any())).thenReturn(Optional.empty());

      processor.persistRows(
          List.of(row("PlayerA", "EU"), row("PlayerB", "NAC"), row("PlayerC", "BR")), config, run);

      verify(identityRepository, times(3)).save(any(PlayerIdentityEntry.class));
    }
  }

  @Nested
  @DisplayName("Existing player update")
  class ExistingPlayerUpdate {

    @Test
    @DisplayName("Does not queue identity entry when player already exists")
    void existingPlayer_doesNotQueueIdentityEntry() {
      Player existing = new Player();
      existing.setNickname("ExistingPlayer");
      existing.setRegion(Player.Region.EU);
      existing.setTranche("1-5");
      existing.setCurrentSeason(2025);
      when(playerRepository.findByNickname("ExistingPlayer")).thenReturn(Optional.of(existing));
      when(prSnapshotRepository.findById(any())).thenReturn(Optional.empty());

      processor.persistRows(List.of(row("ExistingPlayer", "EU")), config, run);

      verify(identityRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Error resilience")
  class ErrorResilience {

    @Test
    @DisplayName("Continues row processing when identity save throws")
    void identitySaveThrows_rowIsStillProcessed() {
      when(playerRepository.findByNickname("PlayerA")).thenReturn(Optional.empty());
      when(prSnapshotRepository.findById(any())).thenReturn(Optional.empty());
      doThrow(new RuntimeException("DB constraint")).when(identityRepository).save(any());

      PrIngestionCounters counters =
          processor.persistRows(List.of(row("PlayerA", "EU")), config, run);

      // Player was still created despite identity save failure
      verify(playerRepository, times(1)).save(any(Player.class));
      assert counters.playersCreated() == 1;
    }

    @Test
    @DisplayName("Identity save failure does not abort subsequent rows in batch")
    void identitySaveFailsForFirst_secondRowStillProcessed() {
      when(playerRepository.findByNickname(any())).thenReturn(Optional.empty());
      when(prSnapshotRepository.findById(any())).thenReturn(Optional.empty());
      when(identityRepository.save(any()))
          .thenThrow(new RuntimeException("DB constraint"))
          .thenAnswer(invocation -> invocation.getArgument(0));

      PrIngestionCounters counters =
          processor.persistRows(List.of(row("PlayerA", "EU"), row("PlayerB", "NAC")), config, run);

      assert counters.playersCreated() == 2;
      verify(playerRepository, times(2)).save(any(Player.class));
    }
  }
}
