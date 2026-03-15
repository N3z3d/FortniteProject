package com.fortnite.pronos.domain.player.alias.model;

import java.time.LocalDateTime;
import java.util.UUID;

public final class PlayerAliasEntry {

  private final UUID id;
  private final UUID playerId;
  private final String nickname;
  private final String source;
  private final boolean current;
  private final LocalDateTime createdAt;

  public PlayerAliasEntry(UUID playerId, String nickname, String source, LocalDateTime createdAt) {
    this.id = UUID.randomUUID();
    this.playerId = playerId;
    this.nickname = nickname;
    this.source = source;
    this.current = true;
    this.createdAt = createdAt;
  }

  public static PlayerAliasEntry restore(
      UUID id,
      UUID playerId,
      String nickname,
      String source,
      boolean current,
      LocalDateTime createdAt) {
    return new PlayerAliasEntry(id, playerId, nickname, source, current, createdAt);
  }

  private PlayerAliasEntry(
      UUID id,
      UUID playerId,
      String nickname,
      String source,
      boolean current,
      LocalDateTime createdAt) {
    this.id = id;
    this.playerId = playerId;
    this.nickname = nickname;
    this.source = source;
    this.current = current;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getPlayerId() {
    return playerId;
  }

  public String getNickname() {
    return nickname;
  }

  public String getSource() {
    return source;
  }

  public boolean isCurrent() {
    return current;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
