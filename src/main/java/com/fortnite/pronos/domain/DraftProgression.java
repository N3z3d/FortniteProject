package com.fortnite.pronos.domain;

/** Domain logic for draft round/pick progression. Pure domain rules without JPA dependencies. */
public final class DraftProgression {

  private DraftProgression() {}

  /** Default number of players each team drafts. */
  public static final int DEFAULT_PLAYERS_PER_TEAM = 3;

  /**
   * Calculates total rounds needed for a draft.
   *
   * @param participantCount number of participants in the game
   * @param playersPerTeam players each team will draft
   * @return total rounds needed
   */
  public static int calculateTotalRounds(int participantCount, int playersPerTeam) {
    if (participantCount <= 0 || playersPerTeam <= 0) {
      throw new IllegalArgumentException("Participant count and players per team must be positive");
    }
    return (int) Math.ceil((double) (participantCount * playersPerTeam) / participantCount);
  }

  /**
   * Calculates total rounds with default players per team.
   *
   * @param participantCount number of participants
   * @return total rounds needed
   */
  public static int calculateTotalRounds(int participantCount) {
    return calculateTotalRounds(participantCount, DEFAULT_PLAYERS_PER_TEAM);
  }

  /**
   * Calculates next pick position after current pick.
   *
   * @param currentRound current round number (1-based)
   * @param currentPick current pick number within round (1-based)
   * @param participantCount total participants
   * @return next position as PickPosition record
   */
  public static PickPosition nextPick(int currentRound, int currentPick, int participantCount) {
    if (participantCount <= 0) {
      throw new IllegalArgumentException("Participant count must be positive");
    }
    int nextPick = currentPick + 1;
    int nextRound = currentRound;

    if (nextPick > participantCount) {
      nextRound++;
      nextPick = 1;
    }

    return new PickPosition(nextRound, nextPick);
  }

  /**
   * Checks if draft is complete based on current position.
   *
   * @param currentRound current round
   * @param totalRounds total rounds in draft
   * @return true if draft is complete
   */
  public static boolean isDraftComplete(int currentRound, int totalRounds) {
    return currentRound > totalRounds;
  }

  /**
   * Calculates remaining picks in the draft.
   *
   * @param currentRound current round
   * @param currentPick current pick
   * @param totalRounds total rounds
   * @param participantCount participants
   * @return remaining picks count
   */
  public static int remainingPicks(
      int currentRound, int currentPick, int totalRounds, int participantCount) {
    if (isDraftComplete(currentRound, totalRounds)) {
      return 0;
    }
    int remainingInCurrentRound = participantCount - currentPick + 1;
    int remainingFullRounds = totalRounds - currentRound;
    return remainingInCurrentRound + (remainingFullRounds * participantCount);
  }

  /**
   * Determines which participant's turn it is based on pick position and draft order direction.
   *
   * @param pickNumber pick number in round (1-based)
   * @param roundNumber current round (1-based)
   * @param participantCount total participants
   * @param useSnakeDraft true for snake draft (odd rounds forward, even rounds reverse)
   * @return participant index (0-based) whose turn it is
   */
  public static int getParticipantForPick(
      int pickNumber, int roundNumber, int participantCount, boolean useSnakeDraft) {
    if (pickNumber < 1 || pickNumber > participantCount) {
      throw new IllegalArgumentException("Pick number must be between 1 and participant count");
    }

    if (!useSnakeDraft || roundNumber % 2 == 1) {
      // Forward order (round 1, 3, 5...)
      return pickNumber - 1;
    } else {
      // Reverse order (round 2, 4, 6...) - snake draft
      return participantCount - pickNumber;
    }
  }

  /** Represents a position in the draft (round and pick). */
  public record PickPosition(int round, int pick) {
    public PickPosition {
      if (round < 1 || pick < 1) {
        throw new IllegalArgumentException("Round and pick must be at least 1");
      }
    }
  }
}
