package com.fortnite.pronos.domain.game.model;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class GameRegionRuleTest {

  @Test
  void createsValidRule() {
    GameRegionRule rule = new GameRegionRule(PlayerRegion.EU, 3);
    assertThat(rule.getRegion()).isEqualTo(PlayerRegion.EU);
    assertThat(rule.getMaxPlayers()).isEqualTo(3);
    assertThat(rule.isValid()).isTrue();
    assertThat(rule.getId()).isNull();
  }

  @Test
  void createsRuleWithId() {
    UUID id = UUID.randomUUID();
    GameRegionRule rule = new GameRegionRule(id, PlayerRegion.NAW, 5);
    assertThat(rule.getId()).isEqualTo(id);
  }

  @Test
  void rejectsNullRegion() {
    assertThatNullPointerException().isThrownBy(() -> new GameRegionRule(null, 3));
  }

  @Test
  void rejectsTooFewPlayers() {
    assertThatIllegalArgumentException().isThrownBy(() -> new GameRegionRule(PlayerRegion.EU, 0));
  }

  @Test
  void rejectsTooManyPlayers() {
    assertThatIllegalArgumentException().isThrownBy(() -> new GameRegionRule(PlayerRegion.EU, 11));
  }

  @Test
  void acceptsBoundaryValues() {
    assertThatCode(() -> new GameRegionRule(PlayerRegion.EU, 1)).doesNotThrowAnyException();
    assertThatCode(() -> new GameRegionRule(PlayerRegion.EU, 10)).doesNotThrowAnyException();
  }

  @Test
  void descriptionFormatsCorrectly() {
    GameRegionRule rule = new GameRegionRule(PlayerRegion.ASIA, 2);
    assertThat(rule.getDescription()).isEqualTo("ASIA: 2 joueurs max");
  }

  @Test
  void equalityByRegion() {
    GameRegionRule a = new GameRegionRule(UUID.randomUUID(), PlayerRegion.EU, 3);
    GameRegionRule b = new GameRegionRule(UUID.randomUUID(), PlayerRegion.EU, 5);
    assertThat(a).isEqualTo(b);
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
  }

  @Test
  void differentRegionsNotEqual() {
    GameRegionRule a = new GameRegionRule(PlayerRegion.EU, 3);
    GameRegionRule b = new GameRegionRule(PlayerRegion.NAW, 3);
    assertThat(a).isNotEqualTo(b);
  }
}
