package com.fortnite.pronos.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fortnite.pronos.domain.DraftStatus.TransitionResult;
import com.fortnite.pronos.model.Draft;

/** Unit tests for DraftStatus domain rules. Pure domain tests - no Spring context required. */
@DisplayName("DraftStatus")
class DraftStatusTest {

  @Nested
  @DisplayName("canStart")
  class CanStart {

    @Test
    @DisplayName("allows starting from PENDING status")
    void allowsStartingFromPending() {
      TransitionResult result = DraftStatus.canStart(Draft.Status.PENDING);

      assertThat(result.allowed()).isTrue();
      assertThat(result.newStatus()).isEqualTo(Draft.Status.ACTIVE);
    }

    @Test
    @DisplayName("rejects starting from ACTIVE status")
    void rejectsStartingFromActive() {
      TransitionResult result = DraftStatus.canStart(Draft.Status.ACTIVE);

      assertThat(result.allowed()).isFalse();
      assertThat(result.errorMessage()).contains("PENDING");
    }

    @Test
    @DisplayName("rejects starting from FINISHED status")
    void rejectsStartingFromFinished() {
      TransitionResult result = DraftStatus.canStart(Draft.Status.FINISHED);

      assertThat(result.allowed()).isFalse();
    }
  }

  @Nested
  @DisplayName("canPause")
  class CanPause {

    @Test
    @DisplayName("allows pausing from ACTIVE status")
    void allowsPausingFromActive() {
      TransitionResult result = DraftStatus.canPause(Draft.Status.ACTIVE);

      assertThat(result.allowed()).isTrue();
      assertThat(result.newStatus()).isEqualTo(Draft.Status.PAUSED);
    }

    @Test
    @DisplayName("allows pausing from IN_PROGRESS status")
    void allowsPausingFromInProgress() {
      TransitionResult result = DraftStatus.canPause(Draft.Status.IN_PROGRESS);

      assertThat(result.allowed()).isTrue();
    }

    @Test
    @DisplayName("rejects pausing from PENDING status")
    void rejectsPausingFromPending() {
      TransitionResult result = DraftStatus.canPause(Draft.Status.PENDING);

      assertThat(result.allowed()).isFalse();
    }
  }

  @Nested
  @DisplayName("canResume")
  class CanResume {

    @Test
    @DisplayName("allows resuming from PAUSED status")
    void allowsResumingFromPaused() {
      TransitionResult result = DraftStatus.canResume(Draft.Status.PAUSED);

      assertThat(result.allowed()).isTrue();
      assertThat(result.newStatus()).isEqualTo(Draft.Status.ACTIVE);
    }

    @Test
    @DisplayName("rejects resuming from ACTIVE status")
    void rejectsResumingFromActive() {
      TransitionResult result = DraftStatus.canResume(Draft.Status.ACTIVE);

      assertThat(result.allowed()).isFalse();
    }
  }

  @Nested
  @DisplayName("canFinish")
  class CanFinish {

    @Test
    @DisplayName("allows finishing from ACTIVE when complete")
    void allowsFinishingFromActiveWhenComplete() {
      TransitionResult result = DraftStatus.canFinish(Draft.Status.ACTIVE, true);

      assertThat(result.allowed()).isTrue();
      assertThat(result.newStatus()).isEqualTo(Draft.Status.FINISHED);
    }

    @Test
    @DisplayName("rejects finishing when picks remaining")
    void rejectsFinishingWhenPicksRemaining() {
      TransitionResult result = DraftStatus.canFinish(Draft.Status.ACTIVE, false);

      assertThat(result.allowed()).isFalse();
      assertThat(result.errorMessage()).contains("picks remaining");
    }

    @Test
    @DisplayName("rejects finishing from PENDING status")
    void rejectsFinishingFromPending() {
      TransitionResult result = DraftStatus.canFinish(Draft.Status.PENDING, true);

      assertThat(result.allowed()).isFalse();
    }
  }

  @Nested
  @DisplayName("canCancel")
  class CanCancel {

    @Test
    @DisplayName("allows cancelling from PENDING status")
    void allowsCancellingFromPending() {
      TransitionResult result = DraftStatus.canCancel(Draft.Status.PENDING);

      assertThat(result.allowed()).isTrue();
      assertThat(result.newStatus()).isEqualTo(Draft.Status.CANCELLED);
    }

    @Test
    @DisplayName("allows cancelling from ACTIVE status")
    void allowsCancellingFromActive() {
      TransitionResult result = DraftStatus.canCancel(Draft.Status.ACTIVE);

      assertThat(result.allowed()).isTrue();
    }

    @Test
    @DisplayName("rejects cancelling from FINISHED status")
    void rejectsCancellingFromFinished() {
      TransitionResult result = DraftStatus.canCancel(Draft.Status.FINISHED);

      assertThat(result.allowed()).isFalse();
      assertThat(result.errorMessage()).contains("finished");
    }

    @Test
    @DisplayName("rejects cancelling from CANCELLED status")
    void rejectsCancellingFromCancelled() {
      TransitionResult result = DraftStatus.canCancel(Draft.Status.CANCELLED);

      assertThat(result.allowed()).isFalse();
      assertThat(result.errorMessage()).contains("cancelled");
    }
  }

  @Nested
  @DisplayName("isTerminal")
  class IsTerminal {

    @Test
    @DisplayName("FINISHED is terminal")
    void finishedIsTerminal() {
      assertThat(DraftStatus.isTerminal(Draft.Status.FINISHED)).isTrue();
    }

    @Test
    @DisplayName("CANCELLED is terminal")
    void cancelledIsTerminal() {
      assertThat(DraftStatus.isTerminal(Draft.Status.CANCELLED)).isTrue();
    }

    @Test
    @DisplayName("ACTIVE is not terminal")
    void activeIsNotTerminal() {
      assertThat(DraftStatus.isTerminal(Draft.Status.ACTIVE)).isFalse();
    }

    @Test
    @DisplayName("PENDING is not terminal")
    void pendingIsNotTerminal() {
      assertThat(DraftStatus.isTerminal(Draft.Status.PENDING)).isFalse();
    }
  }

  @Nested
  @DisplayName("allowsPicks")
  class AllowsPicks {

    @Test
    @DisplayName("ACTIVE allows picks")
    void activeAllowsPicks() {
      assertThat(DraftStatus.allowsPicks(Draft.Status.ACTIVE)).isTrue();
    }

    @Test
    @DisplayName("IN_PROGRESS allows picks")
    void inProgressAllowsPicks() {
      assertThat(DraftStatus.allowsPicks(Draft.Status.IN_PROGRESS)).isTrue();
    }

    @Test
    @DisplayName("PENDING does not allow picks")
    void pendingDoesNotAllowPicks() {
      assertThat(DraftStatus.allowsPicks(Draft.Status.PENDING)).isFalse();
    }

    @Test
    @DisplayName("PAUSED does not allow picks")
    void pausedDoesNotAllowPicks() {
      assertThat(DraftStatus.allowsPicks(Draft.Status.PAUSED)).isFalse();
    }

    @Test
    @DisplayName("FINISHED does not allow picks")
    void finishedDoesNotAllowPicks() {
      assertThat(DraftStatus.allowsPicks(Draft.Status.FINISHED)).isFalse();
    }
  }
}
