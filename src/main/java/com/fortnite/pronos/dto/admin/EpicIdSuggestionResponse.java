package com.fortnite.pronos.dto.admin;

import org.springframework.lang.Nullable;

/**
 * Response for the Epic ID suggestion endpoint. Carries the suggested Epic account ID with a
 * confidence score and the display name returned by the Fortnite API.
 *
 * @param suggestedEpicId Epic account ID found by the resolution adapter (null when not found)
 * @param displayName player display name returned by the Fortnite API (null when not found)
 * @param confidenceScore heuristic confidence in [0, 100] (0 when not found)
 * @param found true when the API resolved a player for this entry
 */
public record EpicIdSuggestionResponse(
    @Nullable String suggestedEpicId,
    @Nullable String displayName,
    int confidenceScore,
    boolean found) {

  public static EpicIdSuggestionResponse notFound() {
    return new EpicIdSuggestionResponse(null, null, 0, false);
  }
}
