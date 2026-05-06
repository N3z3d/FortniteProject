package com.fortnite.pronos.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fortnite.pronos.domain.player.identity.model.PlayerIdentityEntry;

@DisplayName("ConfidenceScoreService")
class ConfidenceScoreServiceTest {

  private final ConfidenceScoreService service = new ConfidenceScoreService();

  @Test
  @DisplayName("uses locale root for case normalization")
  void usesLocaleRootForCaseNormalization() {
    Locale initialLocale = Locale.getDefault();
    try {
      Locale.setDefault(Locale.forLanguageTag("tr-TR"));
      PlayerIdentityEntry entry =
          new PlayerIdentityEntry(UUID.randomUUID(), "igamer", "EU", LocalDateTime.now());

      int score = service.compute(entry, "I-GAMER");

      assertThat(score).isEqualTo(100);
    } finally {
      Locale.setDefault(initialLocale);
    }
  }

  @Test
  @DisplayName("defines precompiled normalization pattern")
  void definesPrecompiledNormalizationPattern() {
    assertThatCode(() -> ConfidenceScoreService.class.getDeclaredField("NON_ALPHANUMERIC_PATTERN"))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("displayName exact match gives max score")
  void displayName_exact_match_gives_max_score() {
    PlayerIdentityEntry entry =
        new PlayerIdentityEntry(UUID.randomUUID(), "Bugha", "EU", LocalDateTime.now());

    int score = service.compute(entry, "33f85e8ed7124d15ae29cfaf53340239", "Bugha");

    assertThat(score).isEqualTo(100);
  }

  @Test
  @DisplayName("displayName partial match gives partial bonus")
  void displayName_partial_gives_partial_bonus() {
    PlayerIdentityEntry entry =
        new PlayerIdentityEntry(UUID.randomUUID(), "Bugha_EU", "EU", LocalDateTime.now());

    int score = service.compute(entry, "33f85e8ed7124d15ae29cfaf53340239", "Bugha");

    assertThat(score).isGreaterThan(30).isLessThan(100);
  }

  @Test
  @DisplayName("null displayName falls back to epicId comparison")
  void null_displayName_falls_back_to_epicId_comparison() {
    PlayerIdentityEntry entry =
        new PlayerIdentityEntry(UUID.randomUUID(), "igamer", "EU", LocalDateTime.now());

    int withNull = service.compute(entry, "igamer", null);
    int withoutDisplayName = service.compute(entry, "igamer");

    assertThat(withNull).isEqualTo(withoutDisplayName).isEqualTo(100);
  }
}
