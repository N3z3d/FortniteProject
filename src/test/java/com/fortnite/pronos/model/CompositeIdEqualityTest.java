package com.fortnite.pronos.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Composite ID equality")
class CompositeIdEqualityTest {

  @Test
  @DisplayName("PrSnapshotId compares by value")
  void prSnapshotIdShouldCompareByValue() {
    UUID playerId = UUID.randomUUID();
    LocalDate date = LocalDate.of(2024, 1, 1);

    PrSnapshot.PrSnapshotId id1 = new PrSnapshot.PrSnapshotId(playerId, PrRegion.EU, date);
    PrSnapshot.PrSnapshotId id2 = new PrSnapshot.PrSnapshotId(playerId, PrRegion.EU, date);

    assertEquals(id1, id2);
    assertEquals(id1.hashCode(), id2.hashCode());
  }

  @Test
  @DisplayName("PrSnapshotId handles nulls and mismatches")
  void prSnapshotIdShouldHandleNullAndMismatch() {
    UUID playerId = UUID.randomUUID();
    LocalDate date = LocalDate.of(2024, 1, 1);

    PrSnapshot.PrSnapshotId id = new PrSnapshot.PrSnapshotId(playerId, PrRegion.EU, date);

    assertNotEquals(id, null);
    assertNotEquals(id, "id");
    assertNotEquals(id, new PrSnapshot.PrSnapshotId(UUID.randomUUID(), PrRegion.EU, date));
    assertNotEquals(id, new PrSnapshot.PrSnapshotId(playerId, PrRegion.NAC, date));
    assertNotEquals(id, new PrSnapshot.PrSnapshotId(playerId, PrRegion.EU, date.plusDays(1)));
  }

  @Test
  @DisplayName("ScoreId compares by value")
  void scoreIdShouldCompareByValue() {
    UUID playerId = UUID.randomUUID();

    Score.ScoreId id1 = new Score.ScoreId(playerId, 1);
    Score.ScoreId id2 = new Score.ScoreId(playerId, 1);

    assertEquals(id1, id2);
    assertEquals(id1.hashCode(), id2.hashCode());
  }

  @Test
  @DisplayName("ScoreId handles nulls and mismatches")
  void scoreIdShouldHandleNullAndMismatch() {
    UUID playerId = UUID.randomUUID();

    Score.ScoreId id = new Score.ScoreId(playerId, 1);

    assertNotEquals(id, null);
    assertNotEquals(id, "id");
    assertNotEquals(id, new Score.ScoreId(UUID.randomUUID(), 1));
    assertNotEquals(id, new Score.ScoreId(playerId, 2));
  }

  @Test
  @DisplayName("TeamPlayerId compares by value")
  void teamPlayerIdShouldCompareByValue() {
    UUID teamId = UUID.randomUUID();
    UUID playerId = UUID.randomUUID();

    TeamPlayer.TeamPlayerId id1 = new TeamPlayer.TeamPlayerId(teamId, playerId);
    TeamPlayer.TeamPlayerId id2 = new TeamPlayer.TeamPlayerId(teamId, playerId);

    assertEquals(id1, id2);
    assertEquals(id1.hashCode(), id2.hashCode());
  }

  @Test
  @DisplayName("TeamPlayerId handles nulls and mismatches")
  void teamPlayerIdShouldHandleNullAndMismatch() {
    UUID teamId = UUID.randomUUID();
    UUID playerId = UUID.randomUUID();

    TeamPlayer.TeamPlayerId id = new TeamPlayer.TeamPlayerId(teamId, playerId);

    assertNotEquals(id, null);
    assertNotEquals(id, "id");
    assertNotEquals(id, new TeamPlayer.TeamPlayerId(UUID.randomUUID(), playerId));
    assertNotEquals(id, new TeamPlayer.TeamPlayerId(teamId, UUID.randomUUID()));
  }
}
