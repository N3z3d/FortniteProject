package com.fortnite.pronos.dto;

import com.fortnite.pronos.domain.player.model.FortnitePlayerData;

public record FortnitePlayerDataDto(
    String epicAccountId,
    String displayName,
    int battlePassLevel,
    int wins,
    int kills,
    int matches,
    double kd,
    double winRate,
    int minutesPlayed) {

  public static FortnitePlayerDataDto from(FortnitePlayerData domain) {
    return new FortnitePlayerDataDto(
        domain.epicAccountId(),
        domain.displayName(),
        domain.battlePassLevel(),
        domain.wins(),
        domain.kills(),
        domain.matches(),
        domain.kd(),
        domain.winRate(),
        domain.minutesPlayed());
  }
}
