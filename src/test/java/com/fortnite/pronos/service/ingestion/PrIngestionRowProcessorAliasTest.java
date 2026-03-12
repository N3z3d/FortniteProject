package com.fortnite.pronos.service.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.player.alias.model.PlayerAliasEntry;
import com.fortnite.pronos.domain.port.out.PlayerAliasRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerIdentityRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerRepositoryPort;
import com.fortnite.pronos.domain.port.out.ScoreRepositoryPort;
import com.fortnite.pronos.model.IngestionRun;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.repository.PrSnapshotRepository;
import com.fortnite.pronos.service.ingestion.PrIngestionService.PrIngestionConfig;

@ExtendWith(MockitoExtension.class)
@DisplayName("PrIngestionRowProcessor — alias historisation")
class PrIngestionRowProcessorAliasTest {

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
  @DisplayName("Alias creation on new player")
  class AliasCreation {

    @Test
    @DisplayName("Records alias with FT_INGESTION source for new player")
    void newPlayer_recordsAliasWithCorrectSource() {
      when(playerRepository.findByNickname("PlayerA")).thenReturn(Optional.empty());
      when(prSnapshotRepository.findForUpsert(any(), any(), any())).thenReturn(Optional.empty());
      when(aliasRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      processor.persistRows(List.of(row("PlayerA", "EU")), config, run);

      ArgumentCaptor<PlayerAliasEntry> captor = ArgumentCaptor.forClass(PlayerAliasEntry.class);
      verify(aliasRepository, times(1)).save(captor.capture());
      PlayerAliasEntry alias = captor.getValue();
      assertThat(alias.getNickname()).isEqualTo("PlayerA");
      assertThat(alias.getSource()).isEqualTo("FT_INGESTION");
      assertThat(alias.isCurrent()).isTrue();
    }

    @Test
    @DisplayName("Records one alias per new player in batch")
    void multipleNewPlayers_oneAliasEach() {
      when(playerRepository.findByNickname(any())).thenReturn(Optional.empty());
      when(prSnapshotRepository.findForUpsert(any(), any(), any())).thenReturn(Optional.empty());
      when(aliasRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      processor.persistRows(List.of(row("PlayerA", "EU"), row("PlayerB", "NAC")), config, run);

      verify(aliasRepository, times(2)).save(any(PlayerAliasEntry.class));
    }
  }

  @Nested
  @DisplayName("Append-only behaviour")
  class AppendOnly {

    @Test
    @DisplayName("Does not record alias for existing player update (append-only)")
    void existingPlayer_doesNotRecordNewAlias() {
      Player existing = new Player();
      existing.setId(UUID.randomUUID());
      existing.setNickname("ExistingPlayer");
      existing.setRegion(Player.Region.EU);
      existing.setTranche("1-5");
      existing.setCurrentSeason(2025);
      when(playerRepository.findByNickname("ExistingPlayer")).thenReturn(Optional.of(existing));
      when(prSnapshotRepository.findForUpsert(any(), any(), any())).thenReturn(Optional.empty());

      processor.persistRows(List.of(row("ExistingPlayer", "EU")), config, run);

      verify(aliasRepository, never()).save(any());
    }

    @Test
    @DisplayName("Alias save failure does not abort row processing (non-blocking)")
    void aliasSaveThrows_rowIsStillProcessed() {
      when(playerRepository.findByNickname("PlayerA")).thenReturn(Optional.empty());
      when(prSnapshotRepository.findForUpsert(any(), any(), any())).thenReturn(Optional.empty());
      when(aliasRepository.save(any())).thenThrow(new RuntimeException("DB constraint"));

      PrIngestionCounters counters =
          processor.persistRows(List.of(row("PlayerA", "EU")), config, run);

      verify(playerRepository, times(1)).save(any(Player.class));
      assertThat(counters.playersCreated()).isEqualTo(1);
    }
  }
}
