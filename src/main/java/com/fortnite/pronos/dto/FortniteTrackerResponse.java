package com.fortnite.pronos.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FortniteTrackerResponse {

  private String accountId;
  private String epicUserHandle;
  private String platformId;
  private String platformName;
  private String platformNameLong;
  private String avatar;

  @JsonProperty("lifeTimeStats")
  private List<LifeTimeStat> lifeTimeStats;

  @JsonProperty("recentMatches")
  private List<RecentMatch> recentMatches;

  private Stats stats;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LifeTimeStat {
    private String key;
    private String value;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Stats {
    @JsonProperty("p2")
    private ModeStats p2; // Solo

    @JsonProperty("p10")
    private ModeStats p10; // Duo

    @JsonProperty("p9")
    private ModeStats p9; // Squad

    @JsonProperty("curr_p2")
    private ModeStats curr_p2; // Current season solo

    @JsonProperty("curr_p10")
    private ModeStats curr_p10; // Current season duo

    @JsonProperty("curr_p9")
    private ModeStats curr_p9; // Current season squad
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ModeStats {
    private TrnRating trnRating;
    private ValueStat score;
    private ValueStat top1;
    private ValueStat top3;
    private ValueStat top5;
    private ValueStat top6;
    private ValueStat top10;
    private ValueStat top12;
    private ValueStat top25;
    private ValueStat kd;
    private ValueStat winRatio;
    private ValueStat matches;
    private ValueStat kills;
    private ValueStat kpg;
    private ValueStat avgTimePlayed;
    private ValueStat scorePerMatch;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrnRating {
      private String label;
      private String field;
      private String category;
      private Integer valueInt;
      private Double value;
      private Integer rank;
      private Double percentile;
      private String displayValue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValueStat {
      private String label;
      private String field;
      private String category;
      private Integer valueInt;
      private Double valueDec;
      private String value;
      private Integer rank;
      private Double percentile;
      private String displayValue;
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RecentMatch {
    private Long id;
    private String accountId;
    private String playlist;
    private Integer kills;
    private Integer minutesPlayed;
    private Integer top1;
    private Integer top3;
    private Integer top5;
    private Integer top6;
    private Integer top10;
    private Integer top12;
    private Integer top25;
    private Integer matches;
    private Integer score;
    private String platform;
    private Integer trnRatingChange;
    private Integer trnRating;
    private String playlistId;
    private String dateCollected;
  }
}
