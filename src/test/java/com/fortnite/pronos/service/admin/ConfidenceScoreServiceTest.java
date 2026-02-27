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
}
