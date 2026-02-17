package com.fortnite.pronos.domain.game.model;

import java.util.Objects;
import java.util.UUID;

/** Domain model representing a regional player restriction for a game. Framework-free. */
public final class GameRegionRule {

  private final UUID id;
  private final PlayerRegion region;
  private final int maxPlayers;

  public GameRegionRule(UUID id, PlayerRegion region, int maxPlayers) {
    Objects.requireNonNull(region, "Region cannot be null");
    if (maxPlayers < 1 || maxPlayers > 10) {
      throw new IllegalArgumentException("Max players must be between 1 and 10");
    }
    this.id = id;
    this.region = region;
    this.maxPlayers = maxPlayers;
  }

  public GameRegionRule(PlayerRegion region, int maxPlayers) {
    this(null, region, maxPlayers);
  }

  public UUID getId() {
    return id;
  }

  public PlayerRegion getRegion() {
    return region;
  }

  public int getMaxPlayers() {
    return maxPlayers;
  }

  public boolean isValid() {
    return maxPlayers >= 1 && maxPlayers <= 10;
  }

  public String getDescription() {
    return String.format("%s: %d joueurs max", region.name(), maxPlayers);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof GameRegionRule that)) return false;
    return region == that.region;
  }

  @Override
  public int hashCode() {
    return Objects.hash(region);
  }
}
