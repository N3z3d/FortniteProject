package com.fortnite.pronos.domain.player.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Pure domain model representing a daily rank snapshot for a player in a given region. Table is
 * append-only: one row per (player, region, date).
 */
public final class RankSnapshot {

  private final UUID id;
  private final UUID playerId;
  private final String region;
  private final int rank;
  private final int prValue;
  private final LocalDate snapshotDate;

  public RankSnapshot(UUID playerId, String region, int rank, int prValue, LocalDate snapshotDate) {
    Objects.requireNonNull(playerId, "playerId must not be null");
    Objects.requireNonNull(region, "region must not be null");
    Objects.requireNonNull(snapshotDate, "snapshotDate must not be null");
    if (rank < 1) throw new IllegalArgumentException("rank must be >= 1");
    this.id = UUID.randomUUID();
    this.playerId = playerId;
    this.region = region;
    this.rank = rank;
    this.prValue = prValue;
    this.snapshotDate = snapshotDate;
  }

  /** Reconstitution factory for persistence mapping. */
  public static RankSnapshot restore(
      UUID id, UUID playerId, String region, int rank, int prValue, LocalDate snapshotDate) {
    RankSnapshot s = new RankSnapshot(playerId, region, rank, prValue, snapshotDate);
    return new RankSnapshot(s, id);
  }

  private RankSnapshot(RankSnapshot source, UUID overrideId) {
    this.id = overrideId;
    this.playerId = source.playerId;
    this.region = source.region;
    this.rank = source.rank;
    this.prValue = source.prValue;
    this.snapshotDate = source.snapshotDate;
  }

  public UUID getId() {
    return id;
  }

  public UUID getPlayerId() {
    return playerId;
  }

  public String getRegion() {
    return region;
  }

  public int getRank() {
    return rank;
  }

  public int getPrValue() {
    return prValue;
  }

  public LocalDate getSnapshotDate() {
    return snapshotDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof RankSnapshot)) return false;
    RankSnapshot that = (RankSnapshot) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
