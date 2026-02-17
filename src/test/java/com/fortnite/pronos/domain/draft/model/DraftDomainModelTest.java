package com.fortnite.pronos.domain.draft.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DraftDomainModelTest {

  @Nested
  class Creation {

    @Test
    void createsDraftWithDefaultState() {
      UUID gameId = UUID.randomUUID();

      Draft draft = new Draft(gameId, 4);

      assertThat(draft.getId()).isNotNull();
      assertThat(draft.getGameId()).isEqualTo(gameId);
      assertThat(draft.getStatus()).isEqualTo(DraftStatus.PENDING);
      assertThat(draft.getCurrentRound()).isEqualTo(1);
      assertThat(draft.getCurrentPick()).isEqualTo(1);
      assertThat(draft.getTotalRounds()).isEqualTo(4);
      assertThat(draft.getCreatedAt()).isNotNull();
      assertThat(draft.getUpdatedAt()).isNotNull();
    }

    @Test
    void rejectsNullGameId() {
      assertThatIllegalArgumentException().isThrownBy(() -> new Draft(null, 4));
    }

    @Test
    void rejectsInvalidTotalRounds() {
      assertThatIllegalArgumentException().isThrownBy(() -> new Draft(UUID.randomUUID(), 0));
    }
  }

  @Nested
  class Lifecycle {

    @Test
    void startsFromPending() {
      Draft draft = new Draft(UUID.randomUUID(), 3);

      boolean started = draft.start();

      assertThat(started).isTrue();
      assertThat(draft.getStatus()).isEqualTo(DraftStatus.ACTIVE);
      assertThat(draft.getStartedAt()).isNotNull();
    }

    @Test
    void cannotStartWhenAlreadyStarted() {
      Draft draft = new Draft(UUID.randomUUID(), 3);
      draft.start();

      boolean startedAgain = draft.start();

      assertThat(startedAgain).isFalse();
    }

    @Test
    void pausesAndResumesWhenAllowed() {
      Draft draft = new Draft(UUID.randomUUID(), 2);
      draft.start();

      boolean paused = draft.pause();
      boolean resumed = draft.resume();

      assertThat(paused).isTrue();
      assertThat(resumed).isTrue();
      assertThat(draft.getStatus()).isEqualTo(DraftStatus.ACTIVE);
    }

    @Test
    void nextPickMovesRoundAndFinishesDraft() {
      Draft draft = new Draft(UUID.randomUUID(), 1);

      draft.nextPick(2);
      draft.nextPick(2);

      assertThat(draft.getCurrentRound()).isEqualTo(2);
      assertThat(draft.getCurrentPick()).isEqualTo(1);
      assertThat(draft.getStatus()).isEqualTo(DraftStatus.FINISHED);
      assertThat(draft.isComplete()).isTrue();
      assertThat(draft.getFinishedAt()).isNotNull();
    }

    @Test
    void rejectsNonPositiveParticipantCount() {
      Draft draft = new Draft(UUID.randomUUID(), 2);
      assertThatIllegalArgumentException().isThrownBy(() -> draft.nextPick(0));
    }
  }

  @Nested
  class Restore {

    @Test
    void restoresAllFields() {
      UUID draftId = UUID.randomUUID();
      UUID gameId = UUID.randomUUID();
      LocalDateTime now = LocalDateTime.now();

      Draft restored =
          Draft.restore(
              draftId, gameId, DraftStatus.PAUSED, 2, 4, 7, now, now, now.minusMinutes(5), null);

      assertThat(restored.getId()).isEqualTo(draftId);
      assertThat(restored.getGameId()).isEqualTo(gameId);
      assertThat(restored.getStatus()).isEqualTo(DraftStatus.PAUSED);
      assertThat(restored.getCurrentRound()).isEqualTo(2);
      assertThat(restored.getCurrentPick()).isEqualTo(4);
      assertThat(restored.getTotalRounds()).isEqualTo(7);
      assertThat(restored.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void restoreAppliesSafeDefaults() {
      Draft restored =
          Draft.restore(
              UUID.randomUUID(), UUID.randomUUID(), null, 0, 0, 0, null, null, null, null);

      assertThat(restored.getStatus()).isEqualTo(DraftStatus.PENDING);
      assertThat(restored.getCurrentRound()).isEqualTo(1);
      assertThat(restored.getCurrentPick()).isEqualTo(1);
      assertThat(restored.getTotalRounds()).isEqualTo(1);
    }
  }
}
