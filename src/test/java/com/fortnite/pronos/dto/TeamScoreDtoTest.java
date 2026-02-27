package com.fortnite.pronos.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TeamScoreDtoTest {

  @Test
  void returnsZeroStatisticsWhenNoScore() {
    TeamScoreDto.PlayerScore playerScore =
        TeamScoreDto.fromScores(UUID.randomUUID(), "Player A", "EU", List.of());

    assertThat(playerScore.getTotalPoints()).isZero();
    assertThat(playerScore.getScoreCount()).isZero();
    assertThat(playerScore.getAverageScore()).isZero();
    assertThat(playerScore.getMedianScore()).isZero();
  }

  @Test
  void calculatesMedianForEvenScores() {
    TeamScoreDto.PlayerScore playerScore =
        TeamScoreDto.fromScores(UUID.randomUUID(), "Player A", "EU", List.of(10, 40, 20, 30));

    assertThat(playerScore.getTotalPoints()).isEqualTo(100);
    assertThat(playerScore.getScoreCount()).isEqualTo(4);
    assertThat(playerScore.getAverageScore()).isEqualTo(25.0);
    assertThat(playerScore.getMedianScore()).isEqualTo(25.0);
  }

  @Test
  void calculatesMedianForOddScores() {
    TeamScoreDto.PlayerScore playerScore =
        TeamScoreDto.fromScores(UUID.randomUUID(), "Player A", "EU", List.of(10, 30, 20));

    assertThat(playerScore.getTotalPoints()).isEqualTo(60);
    assertThat(playerScore.getScoreCount()).isEqualTo(3);
    assertThat(playerScore.getAverageScore()).isEqualTo(20.0);
    assertThat(playerScore.getMedianScore()).isEqualTo(20.0);
  }
}
