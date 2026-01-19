package com.fortnite.pronos.domain.facade;

import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.DraftProgression;
import com.fortnite.pronos.domain.DraftProgression.PickPosition;
import com.fortnite.pronos.domain.DraftStatus;
import com.fortnite.pronos.model.Draft;
import com.fortnite.pronos.model.Game;

/**
 * Facade bridging Draft entity with pure domain rules. Extracts entity data, applies domain logic,
 * and returns results or applies changes to entities.
 */
@Component
public class DraftDomainFacade {

  /**
   * Validates and attempts to start the draft.
   *
   * @param draft draft entity
   * @return result of the operation
   */
  public DraftStatus.TransitionResult tryStart(Draft draft) {
    DraftStatus.TransitionResult result = DraftStatus.canStart(draft.getStatus());

    if (result.allowed()) {
      draft.start();
    }
    return result;
  }

  /**
   * Validates and attempts to pause the draft.
   *
   * @param draft draft entity
   * @return result of the operation
   */
  public DraftStatus.TransitionResult tryPause(Draft draft) {
    DraftStatus.TransitionResult result = DraftStatus.canPause(draft.getStatus());

    if (result.allowed()) {
      draft.pause();
    }
    return result;
  }

  /**
   * Validates and attempts to resume the draft.
   *
   * @param draft draft entity
   * @return result of the operation
   */
  public DraftStatus.TransitionResult tryResume(Draft draft) {
    DraftStatus.TransitionResult result = DraftStatus.canResume(draft.getStatus());

    if (result.allowed()) {
      draft.resume();
    }
    return result;
  }

  /**
   * Validates and attempts to finish the draft.
   *
   * @param draft draft entity
   * @return result of the operation
   */
  public DraftStatus.TransitionResult tryFinish(Draft draft) {
    boolean isComplete = isDraftComplete(draft);
    DraftStatus.TransitionResult result = DraftStatus.canFinish(draft.getStatus(), isComplete);

    if (result.allowed()) {
      draft.finish();
    }
    return result;
  }

  /**
   * Validates and attempts to cancel the draft.
   *
   * @param draft draft entity
   * @return result of the operation
   */
  public DraftStatus.TransitionResult tryCancel(Draft draft) {
    DraftStatus.TransitionResult result = DraftStatus.canCancel(draft.getStatus());

    if (result.allowed()) {
      draft.cancel();
    }
    return result;
  }

  /**
   * Advances to the next pick in the draft.
   *
   * @param draft draft entity
   * @return the new pick position
   */
  public PickPosition advanceToNextPick(Draft draft) {
    int participantCount = getParticipantCount(draft);
    PickPosition nextPos =
        DraftProgression.nextPick(
            draft.getCurrentRound(), draft.getCurrentPick(), participantCount);

    draft.setCurrentRound(nextPos.round());
    draft.setCurrentPick(nextPos.pick());
    return nextPos;
  }

  /**
   * Checks if the draft is complete.
   *
   * @param draft draft entity
   * @return true if all picks are done
   */
  public boolean isDraftComplete(Draft draft) {
    return DraftProgression.isDraftComplete(draft.getCurrentRound(), draft.getTotalRounds());
  }

  /**
   * Calculates remaining picks in the draft.
   *
   * @param draft draft entity
   * @return number of picks remaining
   */
  public int getRemainingPicks(Draft draft) {
    int participantCount = getParticipantCount(draft);
    return DraftProgression.remainingPicks(
        draft.getCurrentRound(), draft.getCurrentPick(), draft.getTotalRounds(), participantCount);
  }

  /**
   * Gets which participant should pick next (0-indexed).
   *
   * @param draft draft entity
   * @param useSnakeDraft whether to use snake draft ordering
   * @return participant index (0-based)
   */
  public int getCurrentPickerIndex(Draft draft, boolean useSnakeDraft) {
    int participantCount = getParticipantCount(draft);
    return DraftProgression.getParticipantForPick(
        draft.getCurrentPick(), draft.getCurrentRound(), participantCount, useSnakeDraft);
  }

  /**
   * Checks if picks can be made in the current state.
   *
   * @param draft draft entity
   * @return true if picks are allowed
   */
  public boolean allowsPicks(Draft draft) {
    return DraftStatus.allowsPicks(draft.getStatus());
  }

  /**
   * Calculates total rounds needed for a new draft.
   *
   * @param game game entity
   * @return total rounds
   */
  public int calculateTotalRounds(Game game) {
    return DraftProgression.calculateTotalRounds(game.getTotalParticipantCount());
  }

  private int getParticipantCount(Draft draft) {
    if (draft.getGame() != null) {
      return draft.getGame().getTotalParticipantCount();
    }
    return 0;
  }
}
