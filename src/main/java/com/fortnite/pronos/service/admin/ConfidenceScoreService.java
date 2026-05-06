package com.fortnite.pronos.service.admin;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.fortnite.pronos.domain.player.identity.model.PlayerIdentityEntry;

/**
 * Calculates a confidence score (0–100) for a player identity resolution. Heuristic based on
 * username similarity and region match.
 */
@Service
public class ConfidenceScoreService {

  private static final int MAX_SCORE = 100;
  private static final int BASE_SCORE = 30;
  private static final int USERNAME_EXACT_BONUS = 50;
  private static final int USERNAME_PARTIAL_BONUS = 25;
  private static final int REGION_BONUS = 20;
  private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-z0-9]");

  /**
   * Computes confidence score for associating {@code epicId} with the given entry.
   *
   * @param entry the pipeline entry (contains playerUsername, playerRegion)
   * @param epicId the proposed Epic Games ID
   * @return score in [0, 100]
   */
  public int compute(PlayerIdentityEntry entry, String epicId) {
    return compute(entry, epicId, null);
  }

  /**
   * Computes confidence score for associating {@code epicId} with the given entry, enriched with
   * the API-returned display name for a more meaningful username comparison.
   *
   * @param entry the pipeline entry (contains playerUsername, playerRegion)
   * @param epicId the proposed Epic Games ID
   * @param displayName the display name returned by the Fortnite API (may be null)
   * @return score in [0, 100]
   */
  public int compute(PlayerIdentityEntry entry, String epicId, @Nullable String displayName) {
    String comparand = displayName != null ? displayName : epicId;
    int score = BASE_SCORE;
    score += usernameBonus(entry.getPlayerUsername(), comparand);
    return Math.min(score, MAX_SCORE);
  }

  private int usernameBonus(String playerUsername, String comparand) {
    String normalizedPlayer = normalize(playerUsername);
    String normalizedComparand = normalize(comparand);
    if (normalizedPlayer.equals(normalizedComparand)) {
      return USERNAME_EXACT_BONUS + REGION_BONUS;
    }
    if (normalizedPlayer.contains(normalizedComparand)
        || normalizedComparand.contains(normalizedPlayer)) {
      return USERNAME_PARTIAL_BONUS;
    }
    return 0;
  }

  private String normalize(String value) {
    String lowerCaseValue = value.toLowerCase(Locale.ROOT);
    return NON_ALPHANUMERIC_PATTERN.matcher(lowerCaseValue).replaceAll("");
  }
}
