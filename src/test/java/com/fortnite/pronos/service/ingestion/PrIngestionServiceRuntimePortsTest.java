package com.fortnite.pronos.service.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.StringReader;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.port.out.PlayerRepositoryPort;
import com.fortnite.pronos.domain.port.out.ScoreRepositoryPort;
import com.fortnite.pronos.model.IngestionRun;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.PrSnapshot;
import com.fortnite.pronos.model.Score;
import com.fortnite.pronos.repository.IngestionRunRepository;
import com.fortnite.pronos.repository.PrSnapshotRepository;
import com.fortnite.pronos.service.ingestion.PrIngestionService.PrIngestionConfig;
import com.fortnite.pronos.service.ingestion.PrIngestionService.PrIngestionResult;

@ExtendWith(MockitoExtension.class)
class PrIngestionServiceRuntimePortsTest {

  private static final UUID RUN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID PLAYER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

  @Mock private PlayerRepositoryPort playerRepository;
  @Mock private PrSnapshotRepository prSnapshotRepository;
  @Mock private ScoreRepositoryPort scoreRepository;
  @Mock private IngestionRunRepository ingestionRunRepository;

  private PrIngestionService service;

  @BeforeEach
  void setUp() {
    service =
        new PrIngestionService(
            new PrCsvParser(),
            playerRepository,
            prSnapshotRepository,
            scoreRepository,
            ingestionRunRepository);

    when(ingestionRunRepository.save(any(IngestionRun.class)))
        .thenAnswer(
            invocation -> {
              IngestionRun run = invocation.getArgument(0);
              if (run.getId() == null) {
                run.setId(RUN_ID);
              }
              return run;
            });
    when(prSnapshotRepository.findById(any())).thenReturn(Optional.empty());
    when(prSnapshotRepository.save(any(PrSnapshot.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void createsPlayerAndScoreThroughPortsWhenNicknameIsUnknown() {
    when(playerRepository.findByNickname("pixie")).thenReturn(Optional.empty());
    when(playerRepository.save(any(Player.class)))
        .thenAnswer(
            invocation -> {
              Player player = invocation.getArgument(0);
              player.setId(PLAYER_ID);
              return player;
            });
    when(scoreRepository.findByPlayerAndSeason(any(Player.class), eq(2025)))
        .thenReturn(Optional.empty());
    when(scoreRepository.save(any(Score.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PrIngestionResult result = ingest(csv("pixie,EU,108022,1,2025-01-10"), true);

    assertThat(result.status()).isEqualTo(IngestionRun.Status.SUCCESS);
    assertThat(result.playersCreated()).isEqualTo(1);
    assertThat(result.snapshotsWritten()).isEqualTo(1);
    assertThat(result.scoresWritten()).isEqualTo(1);
    verify(playerRepository).findByNickname("pixie");
    verify(playerRepository).save(any(Player.class));
    verify(scoreRepository).findByPlayerAndSeason(any(Player.class), eq(2025));
    verify(scoreRepository).save(any(Score.class));
  }

  @Test
  void doesNotWriteScoresWhenWriteScoresIsDisabled() {
    Player existing = new Player();
    existing.setId(PLAYER_ID);
    existing.setNickname("pixie");
    existing.setRegion(Player.Region.EU);
    existing.setCurrentSeason(2025);

    when(playerRepository.findByNickname("pixie")).thenReturn(Optional.of(existing));

    PrIngestionResult result = ingest(csv("pixie,EU,108022,1,2025-01-10"), false);

    assertThat(result.status()).isEqualTo(IngestionRun.Status.SUCCESS);
    assertThat(result.scoresWritten()).isZero();
    verify(scoreRepository, never()).findByPlayerAndSeason(any(Player.class), eq(2025));
    verify(scoreRepository, never()).save(any(Score.class));
  }

  private String csv(String... rows) {
    return "nickname,region,points,rank,snapshot_date\n" + String.join("\n", rows);
  }

  private PrIngestionResult ingest(String csv, boolean writeScores) {
    return service.ingest(
        new StringReader(csv), new PrIngestionConfig("LOCAL_PR", 2025, writeScores));
  }
}
