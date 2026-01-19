package com.fortnite.pronos.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fortnite.pronos.domain.TradeRules.TransitionResult;
import com.fortnite.pronos.domain.TradeRules.ValidationResult;
import com.fortnite.pronos.model.Trade;

/** Unit tests for TradeRules domain rules. Pure domain tests - no Spring context required. */
@DisplayName("TradeRules")
class TradeRulesTest {

  @Nested
  @DisplayName("validateTradeProposal")
  class ValidateTradeProposal {

    private final UUID team1 = UUID.randomUUID();
    private final UUID team2 = UUID.randomUUID();
    private final UUID player1 = UUID.randomUUID();
    private final UUID player2 = UUID.randomUUID();

    @Test
    @DisplayName("accepts valid trade proposal")
    void acceptsValidProposal() {
      ValidationResult result =
          TradeRules.validateTradeProposal(
              team1, team2, Set.of(player1), Set.of(player2), Set.of(player1), Set.of(player2));

      assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("rejects null from team")
    void rejectsNullFromTeam() {
      ValidationResult result =
          TradeRules.validateTradeProposal(
              null, team2, Set.of(player1), Set.of(player2), Set.of(player1), Set.of(player2));

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("specified");
    }

    @Test
    @DisplayName("rejects same team trade")
    void rejectsSameTeam() {
      ValidationResult result =
          TradeRules.validateTradeProposal(
              team1, team1, Set.of(player1), Set.of(player2), Set.of(player1), Set.of(player2));

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("same team");
    }

    @Test
    @DisplayName("rejects empty offered players")
    void rejectsEmptyOffered() {
      ValidationResult result =
          TradeRules.validateTradeProposal(
              team1, team2, Set.of(), Set.of(player2), Set.of(), Set.of(player2));

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("offer");
    }

    @Test
    @DisplayName("rejects empty requested players")
    void rejectsEmptyRequested() {
      ValidationResult result =
          TradeRules.validateTradeProposal(
              team1, team2, Set.of(player1), Set.of(), Set.of(player1), Set.of());

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("request");
    }

    @Test
    @DisplayName("rejects offered player not on source team")
    void rejectsOfferedPlayerNotOnTeam() {
      ValidationResult result =
          TradeRules.validateTradeProposal(
              team1, team2, Set.of(player1), Set.of(player2), Set.of(), Set.of(player2));

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("source team");
    }

    @Test
    @DisplayName("rejects requested player not on target team")
    void rejectsRequestedPlayerNotOnTeam() {
      ValidationResult result =
          TradeRules.validateTradeProposal(
              team1, team2, Set.of(player1), Set.of(player2), Set.of(player1), Set.of());

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("target team");
    }
  }

  @Nested
  @DisplayName("canTrade")
  class CanTrade {

    @Test
    @DisplayName("allows trade when enabled and within limits")
    void allowsValidTrade() {
      ValidationResult result = TradeRules.canTrade(true, null, 0, 5);

      assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("rejects trade when disabled")
    void rejectsWhenDisabled() {
      ValidationResult result = TradeRules.canTrade(false, null, 0, 5);

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("not enabled");
    }

    @Test
    @DisplayName("rejects trade after deadline")
    void rejectsAfterDeadline() {
      LocalDateTime pastDeadline = LocalDateTime.now().minusHours(1);
      ValidationResult result = TradeRules.canTrade(true, pastDeadline, 0, 5);

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("deadline");
    }

    @Test
    @DisplayName("rejects trade at maximum count")
    void rejectsAtMaxCount() {
      ValidationResult result = TradeRules.canTrade(true, null, 5, 5);

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("limit");
    }
  }

  @Nested
  @DisplayName("canAccept")
  class CanAccept {

    @Test
    @DisplayName("allows accepting PENDING trade")
    void allowsAcceptingPending() {
      TransitionResult result = TradeRules.canAccept(Trade.Status.PENDING);

      assertThat(result.allowed()).isTrue();
      assertThat(result.newStatus()).isEqualTo(Trade.Status.ACCEPTED);
    }

    @Test
    @DisplayName("rejects accepting non-PENDING trade")
    void rejectsAcceptingNonPending() {
      TransitionResult result = TradeRules.canAccept(Trade.Status.ACCEPTED);

      assertThat(result.allowed()).isFalse();
      assertThat(result.errorMessage()).contains("PENDING");
    }
  }

  @Nested
  @DisplayName("canReject")
  class CanReject {

    @Test
    @DisplayName("allows rejecting PENDING trade")
    void allowsRejectingPending() {
      TransitionResult result = TradeRules.canReject(Trade.Status.PENDING);

      assertThat(result.allowed()).isTrue();
      assertThat(result.newStatus()).isEqualTo(Trade.Status.REJECTED);
    }

    @Test
    @DisplayName("rejects rejecting non-PENDING trade")
    void rejectsRejectingNonPending() {
      TransitionResult result = TradeRules.canReject(Trade.Status.REJECTED);

      assertThat(result.allowed()).isFalse();
    }
  }

  @Nested
  @DisplayName("canCancel")
  class CanCancel {

    @Test
    @DisplayName("allows cancelling PENDING trade")
    void allowsCancellingPending() {
      TransitionResult result = TradeRules.canCancel(Trade.Status.PENDING);

      assertThat(result.allowed()).isTrue();
      assertThat(result.newStatus()).isEqualTo(Trade.Status.CANCELLED);
    }

    @Test
    @DisplayName("rejects cancelling ACCEPTED trade")
    void rejectsCancellingAccepted() {
      TransitionResult result = TradeRules.canCancel(Trade.Status.ACCEPTED);

      assertThat(result.allowed()).isFalse();
    }
  }

  @Nested
  @DisplayName("canCounter")
  class CanCounter {

    @Test
    @DisplayName("allows countering PENDING trade")
    void allowsCounteringPending() {
      TransitionResult result = TradeRules.canCounter(Trade.Status.PENDING);

      assertThat(result.allowed()).isTrue();
      assertThat(result.newStatus()).isEqualTo(Trade.Status.COUNTERED);
    }

    @Test
    @DisplayName("rejects countering REJECTED trade")
    void rejectsCounteringRejected() {
      TransitionResult result = TradeRules.canCounter(Trade.Status.REJECTED);

      assertThat(result.allowed()).isFalse();
    }
  }

  @Nested
  @DisplayName("isTerminal")
  class IsTerminal {

    @Test
    @DisplayName("ACCEPTED is terminal")
    void acceptedIsTerminal() {
      assertThat(TradeRules.isTerminal(Trade.Status.ACCEPTED)).isTrue();
    }

    @Test
    @DisplayName("REJECTED is terminal")
    void rejectedIsTerminal() {
      assertThat(TradeRules.isTerminal(Trade.Status.REJECTED)).isTrue();
    }

    @Test
    @DisplayName("CANCELLED is terminal")
    void cancelledIsTerminal() {
      assertThat(TradeRules.isTerminal(Trade.Status.CANCELLED)).isTrue();
    }

    @Test
    @DisplayName("PENDING is not terminal")
    void pendingIsNotTerminal() {
      assertThat(TradeRules.isTerminal(Trade.Status.PENDING)).isFalse();
    }

    @Test
    @DisplayName("COUNTERED is not terminal")
    void counteredIsNotTerminal() {
      assertThat(TradeRules.isTerminal(Trade.Status.COUNTERED)).isFalse();
    }
  }
}
