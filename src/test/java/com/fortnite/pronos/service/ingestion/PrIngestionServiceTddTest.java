package com.fortnite.pronos.service.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.fortnite.pronos.adapter.out.persistence.player.alias.PlayerAliasEntityMapper;
import com.fortnite.pronos.adapter.out.persistence.player.alias.PlayerAliasRepositoryAdapter;
import com.fortnite.pronos.adapter.out.persistence.player.identity.PlayerIdentityEntityMapper;
import com.fortnite.pronos.adapter.out.persistence.player.identity.PlayerIdentityRepositoryAdapter;
import com.fortnite.pronos.model.IngestionRun;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.PrRegion;
import com.fortnite.pronos.model.PrSnapshot;
import com.fortnite.pronos.model.Score;
import com.fortnite.pronos.repository.IngestionRunRepository;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.PrSnapshotRepository;
import com.fortnite.pronos.repository.ScoreRepository;
import com.fortnite.pronos.service.ingestion.PrIngestionService.PrIngestionConfig;
import com.fortnite.pronos.service.ingestion.PrIngestionService.PrIngestionResult;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("test")
@Import({
  PrIngestionService.class,
  PrIngestionRowProcessor.class,
  PrIngestionServiceTddTest.TestConfig.class,
  PlayerIdentityRepositoryAdapter.class,
  PlayerIdentityEntityMapper.class,
  PlayerAliasRepositoryAdapter.class,
  PlayerAliasEntityMapper.class
})
class PrIngestionServiceTddTest {

  @TestConfiguration
  static class TestConfig {
    @Bean
    PrCsvParser prCsvParser() {
      return new PrCsvParser();
    }
  }

  @Autowired private PrIngestionService ingestionService;
  @Autowired private PlayerRepository playerRepository;
  @Autowired private PrSnapshotRepository prSnapshotRepository;
  @Autowired private ScoreRepository scoreRepository;
  @Autowired private IngestionRunRepository ingestionRunRepository;

  @Test
  void ingestsValidCsvAndWritesSnapshotsAndScores() {
    String csv = csv("pixie,EU,108022,1,2025-01-10", "Muz,NAC,125360,2,2025-01-10");

    PrIngestionResult result = ingestCsv(csv, true);

    assertThat(result.status()).isEqualTo(IngestionRun.Status.SUCCESS);
    assertThat(result.playersCreated()).isEqualTo(2);
    assertThat(result.snapshotsWritten()).isEqualTo(2);
    assertThat(result.scoresWritten()).isEqualTo(2);
    assertThat(playerRepository.count()).isEqualTo(2);
    assertThat(prSnapshotRepository.count()).isEqualTo(2);
    assertThat(scoreRepository.count()).isEqualTo(2);
  }

  @Test
  void createsUnknownPlayerFromGlobalRow() {
    String csv = csv("Ghost,GLOBAL,50000,3,2025-01-10");

    PrIngestionResult result = ingestCsv(csv, true);
    Player player = playerRepository.findByNickname("Ghost").orElseThrow();
    PrSnapshot snapshot = prSnapshotRepository.findAll().get(0);

    assertThat(result.status()).isEqualTo(IngestionRun.Status.SUCCESS);
    assertThat(result.playersCreated()).isEqualTo(1);
    assertThat(player.getRegion()).isEqualTo(Player.Region.UNKNOWN);
    assertThat(snapshot.getRegion()).isEqualTo(PrRegion.GLOBAL);
    assertThat(result.snapshotsWritten()).isEqualTo(1);
    assertThat(result.scoresWritten()).isEqualTo(1);
  }

  @Test
  void updatesExistingSnapshotAndScoreWhenReingesting() {
    String csv = csv("pixie,EU,108022,1,2025-01-10");
    ingestCsv(csv, true);

    String updated = csv("pixie,EU,120000,1,2025-01-10");
    PrIngestionResult result = ingestCsv(updated, true);

    Player player = playerRepository.findByNickname("pixie").orElseThrow();
    PrSnapshot.PrSnapshotId snapshotId =
        new PrSnapshot.PrSnapshotId(player.getId(), PrRegion.EU, LocalDate.parse("2025-01-10"));
    PrSnapshot snapshot = prSnapshotRepository.findById(snapshotId).orElseThrow();
    Score score = scoreRepository.findByPlayerAndSeason(player, 2025).orElseThrow();

    assertThat(result.snapshotsWritten()).isEqualTo(1);
    assertThat(snapshot.getPoints()).isEqualTo(120000);
    assertThat(score.getPoints()).isEqualTo(120000);
    assertThat(prSnapshotRepository.count()).isEqualTo(1);
  }

  @Test
  void classicRegionReplacesUnknown() {
    String globalCsv = csv("Ghost,GLOBAL,50000,3,2025-01-10");
    ingestCsv(globalCsv, true);

    String euCsv = csv("Ghost,EU,60000,2,2025-01-10");
    PrIngestionResult result = ingestCsv(euCsv, true);

    Player player = playerRepository.findByNickname("Ghost").orElseThrow();
    assertThat(result.status()).isEqualTo(IngestionRun.Status.SUCCESS);
    assertThat(player.getRegion()).isEqualTo(Player.Region.EU);
    assertThat(prSnapshotRepository.count()).isEqualTo(2);
  }

  @Test
  void globalDoesNotOverrideKnownRegion() {
    String euCsv = csv("Ghost,EU,60000,2,2025-01-10");
    ingestCsv(euCsv, true);

    String globalCsv = csv("Ghost,GLOBAL,50000,3,2025-01-10");
    PrIngestionResult result = ingestCsv(globalCsv, true);

    Player player = playerRepository.findByNickname("Ghost").orElseThrow();
    assertThat(result.status()).isEqualTo(IngestionRun.Status.SUCCESS);
    assertThat(player.getRegion()).isEqualTo(Player.Region.EU);
  }

  @Test
  void keepsSingleScorePerSeasonWhileSnapshotsTrackHistory() {
    String csv = csv("pixie,EU,108022,1,2025-01-10", "pixie,EU,120000,1,2025-01-17");

    PrIngestionResult result = ingestCsv(csv, true);

    Player player = playerRepository.findByNickname("pixie").orElseThrow();
    Score score = scoreRepository.findByPlayerAndSeason(player, 2025).orElseThrow();
    assertThat(result.status()).isEqualTo(IngestionRun.Status.SUCCESS);
    assertThat(prSnapshotRepository.count()).isEqualTo(2);
    assertThat(scoreRepository.count()).isEqualTo(1);
    assertThat(score.getPoints()).isEqualTo(120000);
    assertThat(score.getDate()).isEqualTo(LocalDate.parse("2025-01-17"));
  }

  @Test
  void skipsInvalidRowsAndCountsParseErrors() {
    String csv = csv("pixie,EU,108022,1,2025-01-10", "bad,EU,notanumber,1,2025-01-10");

    PrIngestionResult result = ingestCsv(csv, true);

    assertThat(result.status()).isEqualTo(IngestionRun.Status.PARTIAL);
    assertThat(result.parseErrors()).isEqualTo(1);
    assertThat(prSnapshotRepository.count()).isEqualTo(1);
  }

  @Test
  void failsWhenHeaderIsInvalid() {
    String csv = "nickname,points,rank,snapshot_date\npixie,108022,1,2025-01-10";

    PrIngestionResult result = ingestCsv(csv, true);

    IngestionRun run = ingestionRunRepository.findById(result.runId()).orElseThrow();
    assertThat(run.getStatus()).isEqualTo(IngestionRun.Status.FAILED);
    assertThat(run.getErrorMessage()).contains("invalid_header");
  }

  @Test
  void handlesDuplicateNicknameInSameCsvFile() {
    // Same player appears twice in CSV with different regions - should reuse player
    String csv = csv("pixie,EU,108022,1,2025-01-10", "pixie,NAC,105000,3,2025-01-10");

    PrIngestionResult result = ingestCsv(csv, true);

    assertThat(result.status()).isEqualTo(IngestionRun.Status.SUCCESS);
    assertThat(result.playersCreated()).isEqualTo(1);
    assertThat(result.playersUpdated()).isEqualTo(1);
    assertThat(playerRepository.count()).isEqualTo(1);
    assertThat(prSnapshotRepository.count()).isEqualTo(2);

    Player player = playerRepository.findByNickname("pixie").orElseThrow();
    assertThat(player.getRegion()).isEqualTo(Player.Region.NAC);
  }

  @Test
  void handlesExactDuplicateRowsGracefully() {
    // Exact same row appears twice - should upsert to same snapshot
    String csv = csv("pixie,EU,108022,1,2025-01-10", "pixie,EU,108022,1,2025-01-10");

    PrIngestionResult result = ingestCsv(csv, true);

    assertThat(result.status()).isEqualTo(IngestionRun.Status.SUCCESS);
    assertThat(playerRepository.count()).isEqualTo(1);
    assertThat(prSnapshotRepository.count()).isEqualTo(1);
    assertThat(scoreRepository.count()).isEqualTo(1);
  }

  @Test
  void assignsExpectedTrancheForRankBoundaries() {
    String csv =
        csv(
            "rank5,EU,1000,5,2025-01-10",
            "rank6,EU,1000,6,2025-01-10",
            "rank30,EU,1000,30,2025-01-10",
            "rank31,EU,1000,31,2025-01-10");

    PrIngestionResult result = ingestCsv(csv, true);

    assertThat(result.status()).isEqualTo(IngestionRun.Status.SUCCESS);
    assertThat(playerRepository.findByNickname("rank5").orElseThrow().getTranche())
        .isEqualTo("1-5");
    assertThat(playerRepository.findByNickname("rank6").orElseThrow().getTranche())
        .isEqualTo("6-10");
    assertThat(playerRepository.findByNickname("rank30").orElseThrow().getTranche())
        .isEqualTo("26-30");
    assertThat(playerRepository.findByNickname("rank31").orElseThrow().getTranche())
        .isEqualTo("31-infini");
  }

  @Test
  void buildsNormalizedUsernameFromNickname() {
    String csv = csv("PiXiE-42!,EU,108022,1,2025-01-10");

    ingestCsv(csv, true);

    Player player = playerRepository.findByNickname("PiXiE-42!").orElseThrow();
    assertThat(player.getUsername()).isEqualTo("pixie42");
  }

  @Test
  void fallsBackToGeneratedUsernameWhenNicknameHasNoAlphaNumericCharacter() {
    String nickname = "***";
    String csv = csv(nickname + ",EU,108022,1,2025-01-10");

    ingestCsv(csv, true);

    Player player = playerRepository.findByNickname(nickname).orElseThrow();
    assertThat(player.getUsername()).isEqualTo("player" + Math.abs(nickname.hashCode()));
  }

  private String csv(String... rows) {
    return "nickname,region,points,rank,snapshot_date\n" + String.join("\n", rows);
  }

  private PrIngestionResult ingestCsv(String csv, boolean writeScores) {
    return ingestionService.ingest(
        new StringReader(csv), new PrIngestionConfig("LOCAL_PR", 2025, writeScores));
  }
}
