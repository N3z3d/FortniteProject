package com.fortnite.pronos.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Year;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Draft model season propagation")
class DraftModelTest {

  @Test
  @DisplayName("constructor copies current season from game")
  void constructorCopiesCurrentSeasonFromGame() {
    Game game = new Game();
    game.setCurrentSeason(2032);

    Draft draft = new Draft(game);

    assertThat(draft.getSeason()).isEqualTo(2032);
  }

  @Test
  @DisplayName("constructor falls back to current year when game season is missing")
  void constructorFallsBackToCurrentYearWhenGameSeasonIsMissing() {
    Game game = new Game();
    game.setCurrentSeason(null);

    Draft draft = new Draft(game);

    assertThat(draft.getSeason()).isEqualTo(Year.now().getValue());
  }

  @Test
  @DisplayName("setGame syncs season after default construction")
  void setGameSyncsSeasonAfterDefaultConstruction() {
    Draft draft = new Draft();
    Game game = new Game();
    game.setCurrentSeason(2040);

    draft.setGame(game);

    assertThat(draft.getSeason()).isEqualTo(2040);
  }

  @Test
  @DisplayName("default constructor initializes season to current year")
  void defaultConstructorInitializesSeasonToCurrentYear() {
    Draft draft = new Draft();

    assertThat(draft.getSeason()).isEqualTo(Year.now().getValue());
  }
}
