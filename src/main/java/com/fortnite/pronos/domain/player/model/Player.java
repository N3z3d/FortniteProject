package com.fortnite.pronos.domain.player.model;

import java.util.Objects;
import java.util.UUID;

import com.fortnite.pronos.domain.game.model.PlayerRegion;

/**
 * Pure domain model representing a Fortnite player. Contains business logic with zero framework
 * dependencies (no JPA, no Spring, no Lombok).
 */
@SuppressWarnings({"java:S107"})
public final class Player {

  private UUID id;
  private String fortniteId;
  private String username;
  private String nickname;
  private PlayerRegion region;
  private String tranche;
  private int currentSeason;
  private boolean locked;

  /** Business constructor for creating a new player. */
  public Player(String username, String nickname, PlayerRegion region, String tranche) {
    validateCreation(username, nickname, region, tranche);
    this.id = UUID.randomUUID();
    this.username = username.trim();
    this.nickname = nickname.trim();
    this.region = region;
    this.tranche = tranche.trim();
    this.currentSeason = 2025;
    this.locked = false;
  }

  /** Reconstitution factory for persistence mapping. */
  public static Player restore(
      UUID id,
      String fortniteId,
      String username,
      String nickname,
      PlayerRegion region,
      String tranche,
      int currentSeason,
      boolean locked) {
    Player player = new Player();
    player.id = id;
    player.fortniteId = fortniteId;
    player.username = username;
    player.nickname = nickname;
    player.region = region;
    player.tranche = tranche;
    player.currentSeason = currentSeason;
    player.locked = locked;
    return player;
  }

  /** Private no-arg constructor for restore(). */
  private Player() {}

  // ===============================
  // BUSINESS METHODS
  // ===============================

  /** Returns the display name: nickname if available, otherwise username. */
  public String getName() {
    return nickname != null ? nickname : username;
  }

  /** Returns the region name as string, or null if region is null. */
  public String getRegionName() {
    return region != null ? region.name() : null;
  }

  public void lock() {
    this.locked = true;
  }

  public void unlock() {
    this.locked = false;
  }

  public void updateRegion(PlayerRegion newRegion) {
    Objects.requireNonNull(newRegion, "Region cannot be null");
    this.region = newRegion;
  }

  public void updateTranche(String newTranche) {
    if (newTranche == null || newTranche.trim().isEmpty()) {
      throw new IllegalArgumentException("Tranche cannot be null or blank");
    }
    this.tranche = newTranche.trim();
  }

  public void updateNickname(String newNickname) {
    if (newNickname == null || newNickname.trim().isEmpty()) {
      throw new IllegalArgumentException("Nickname cannot be null or blank");
    }
    this.nickname = newNickname.trim();
  }

  public void setFortniteId(String fortniteId) {
    this.fortniteId = fortniteId;
  }

  // ===============================
  // GETTERS
  // ===============================

  public UUID getId() {
    return id;
  }

  public String getFortniteId() {
    return fortniteId;
  }

  public String getUsername() {
    return username;
  }

  public String getNickname() {
    return nickname;
  }

  public PlayerRegion getRegion() {
    return region;
  }

  public String getTranche() {
    return tranche;
  }

  public int getCurrentSeason() {
    return currentSeason;
  }

  public boolean isLocked() {
    return locked;
  }

  // ===============================
  // EQUALS / HASHCODE
  // ===============================

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Player that)) return false;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  // ===============================
  // PRIVATE HELPERS
  // ===============================

  private void validateCreation(
      String username, String nickname, PlayerRegion region, String tranche) {
    if (username == null || username.trim().isEmpty()) {
      throw new IllegalArgumentException("Username cannot be null or blank");
    }
    if (nickname == null || nickname.trim().isEmpty()) {
      throw new IllegalArgumentException("Nickname cannot be null or blank");
    }
    if (region == null) {
      throw new IllegalArgumentException("Region cannot be null");
    }
    if (tranche == null || tranche.trim().isEmpty()) {
      throw new IllegalArgumentException("Tranche cannot be null or blank");
    }
  }
}
