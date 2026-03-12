package com.fortnite.pronos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Minimal mapping of Fortnite-API.com /v2/stats/br/v2 response. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FortniteApiStatsResponse(int status, FortniteApiDataNode data) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record FortniteApiDataNode(
      FortniteApiAccountNode account,
      FortniteApiBattlePassNode battlePass,
      FortniteApiStatsNode stats) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record FortniteApiAccountNode(String id, String name) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record FortniteApiBattlePassNode(int level, int progress) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record FortniteApiStatsNode(FortniteApiInputNode all) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record FortniteApiInputNode(FortniteApiOverallStats overall) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record FortniteApiOverallStats(
      int score,
      int wins,
      int kills,
      int matches,
      double kd,
      double winRate,
      int top3,
      int top5,
      int top10,
      int top25,
      int minutesPlayed,
      int playersOutlived) {}
}
