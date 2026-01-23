package com.fortnite.pronos.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.fortnite.pronos.model.IngestionRun;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.PrRegion;
import com.fortnite.pronos.model.PrSnapshot;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("test")
class PrSnapshotRepositoryTest {
  @Autowired private PlayerRepository playerRepository;
  @Autowired private PrSnapshotRepository prSnapshotRepository;
  @Autowired private IngestionRunRepository ingestionRunRepository;

  @Test
  void savesSnapshotWithRun() {
    Player player = savePlayer("pixie");
    IngestionRun run = new IngestionRun();
    run.setSource("FORTNITE_TRACKER");
    run = ingestionRunRepository.saveAndFlush(run);
    PrSnapshot snapshot =
        buildSnapshot(player, PrRegion.EU, LocalDate.parse("2025-01-10"), run, 108022, 1);
    PrSnapshot saved = prSnapshotRepository.saveAndFlush(snapshot);
    assertThat(saved.getPlayer().getId()).isEqualTo(player.getId());
    assertThat(saved.getRun().getId()).isEqualTo(run.getId());
    assertThat(saved.getRegion()).isEqualTo(PrRegion.EU);
  }

  @Test
  void allowsMultipleRegionsForSamePlayerAndDate() {
    Player player = savePlayer("muz");
    LocalDate date = LocalDate.parse("2025-01-10");
    PrSnapshot eu = buildSnapshot(player, PrRegion.EU, date, null, 100, 10);
    PrSnapshot nac = buildSnapshot(player, PrRegion.NAC, date, null, 200, 2);
    prSnapshotRepository.saveAndFlush(eu);
    prSnapshotRepository.saveAndFlush(nac);
    assertThat(prSnapshotRepository.count()).isEqualTo(2);
  }

  @Test
  void defaultsCollectedAtWhenMissing() {
    Player player = savePlayer("white");
    PrSnapshot snapshot =
        buildSnapshot(player, PrRegion.BR, LocalDate.parse("2025-01-10"), null, 82413, 2);
    snapshot.setCollectedAt(null);
    PrSnapshot saved = prSnapshotRepository.saveAndFlush(snapshot);
    assertThat(saved.getCollectedAt()).isNotNull();
  }

  @Test
  void allowsNullRun() {
    Player player = savePlayer("nuti");
    PrSnapshot snapshot =
        buildSnapshot(player, PrRegion.ME, LocalDate.parse("2025-01-10"), null, 85869, 5);
    PrSnapshot saved = prSnapshotRepository.saveAndFlush(snapshot);
    assertThat(saved.getRun()).isNull();
  }

  @Test
  void rejectsMissingPlayer() {
    PrSnapshot snapshot =
        buildSnapshot(null, PrRegion.EU, LocalDate.parse("2025-01-10"), null, 1, 1);
    assertThatThrownBy(() -> prSnapshotRepository.saveAndFlush(snapshot))
        .isInstanceOf(RuntimeException.class);
  }

  private Player savePlayer(String nickname) {
    Player player = new Player();
    player.setUsername(nickname.toLowerCase());
    player.setNickname(nickname);
    player.setRegion(Player.Region.EU);
    player.setTranche("1-5");
    player.setCurrentSeason(2025);
    return playerRepository.saveAndFlush(player);
  }

  private PrSnapshot buildSnapshot(
      Player player, PrRegion region, LocalDate date, IngestionRun run, int points, int rank) {
    PrSnapshot snapshot = new PrSnapshot();
    snapshot.setPlayer(player);
    snapshot.setRegion(region);
    snapshot.setSnapshotDate(date);
    snapshot.setPoints(points);
    snapshot.setRank(rank);
    snapshot.setCollectedAt(OffsetDateTime.parse("2025-01-10T10:15:30Z"));
    snapshot.setRun(run);
    return snapshot;
  }
}
