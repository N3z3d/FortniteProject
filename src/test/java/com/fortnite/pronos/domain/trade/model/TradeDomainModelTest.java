package com.fortnite.pronos.domain.trade.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"java:S5778"})
class TradeDomainModelTest {

  private static final UUID TEAM_A = UUID.randomUUID();
  private static final UUID TEAM_B = UUID.randomUUID();
  private static final List<UUID> OFFERED = List.of(UUID.randomUUID());
  private static final List<UUID> REQUESTED = List.of(UUID.randomUUID());

  @Nested
  class Creation {

    @Test
    void createsTradeWithDefaults() {
      Trade trade = new Trade(TEAM_A, TEAM_B, OFFERED, REQUESTED);

      assertThat(trade.getId()).isNotNull();
      assertThat(trade.getFromTeamId()).isEqualTo(TEAM_A);
      assertThat(trade.getToTeamId()).isEqualTo(TEAM_B);
      assertThat(trade.getOfferedPlayerIds()).containsExactlyElementsOf(OFFERED);
      assertThat(trade.getRequestedPlayerIds()).containsExactlyElementsOf(REQUESTED);
      assertThat(trade.getStatus()).isEqualTo(TradeStatus.PENDING);
      assertThat(trade.getProposedAt()).isNotNull();
      assertThat(trade.getAcceptedAt()).isNull();
      assertThat(trade.getRejectedAt()).isNull();
      assertThat(trade.getCancelledAt()).isNull();
      assertThat(trade.getOriginalTradeId()).isNull();
    }

    @Test
    void rejectsNullFromTeamId() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new Trade(null, TEAM_B, OFFERED, REQUESTED));
    }

    @Test
    void rejectsNullToTeamId() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new Trade(TEAM_A, null, OFFERED, REQUESTED));
    }

    @Test
    void rejectsSameTeam() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new Trade(TEAM_A, TEAM_A, OFFERED, REQUESTED));
    }

    @Test
    void rejectsNullOfferedPlayers() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new Trade(TEAM_A, TEAM_B, null, REQUESTED));
    }

    @Test
    void rejectsEmptyOfferedPlayers() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new Trade(TEAM_A, TEAM_B, List.of(), REQUESTED));
    }

    @Test
    void rejectsNullRequestedPlayers() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new Trade(TEAM_A, TEAM_B, OFFERED, null));
    }

    @Test
    void rejectsEmptyRequestedPlayers() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new Trade(TEAM_A, TEAM_B, OFFERED, List.of()));
    }
  }

  @Nested
  class Restore {

    @Test
    void restoresAllFields() {
      UUID tradeId = UUID.randomUUID();
      UUID origId = UUID.randomUUID();
      LocalDateTime now = LocalDateTime.now();

      Trade trade =
          Trade.restore(
              tradeId,
              TEAM_A,
              TEAM_B,
              OFFERED,
              REQUESTED,
              TradeStatus.ACCEPTED,
              now,
              now.plusMinutes(5),
              null,
              null,
              origId);

      assertThat(trade.getId()).isEqualTo(tradeId);
      assertThat(trade.getFromTeamId()).isEqualTo(TEAM_A);
      assertThat(trade.getToTeamId()).isEqualTo(TEAM_B);
      assertThat(trade.getStatus()).isEqualTo(TradeStatus.ACCEPTED);
      assertThat(trade.getProposedAt()).isEqualTo(now);
      assertThat(trade.getAcceptedAt()).isEqualTo(now.plusMinutes(5));
      assertThat(trade.getOriginalTradeId()).isEqualTo(origId);
    }

    @Test
    void appliesSafeDefaults() {
      Trade trade =
          Trade.restore(null, TEAM_A, TEAM_B, null, null, null, null, null, null, null, null);

      assertThat(trade.getId()).isNotNull();
      assertThat(trade.getStatus()).isEqualTo(TradeStatus.PENDING);
      assertThat(trade.getProposedAt()).isNotNull();
      assertThat(trade.getOfferedPlayerIds()).isEmpty();
      assertThat(trade.getRequestedPlayerIds()).isEmpty();
    }

    @Test
    void restoresCounterOffer() {
      UUID origId = UUID.randomUUID();
      Trade trade =
          Trade.restore(
              UUID.randomUUID(),
              TEAM_A,
              TEAM_B,
              OFFERED,
              REQUESTED,
              TradeStatus.PENDING,
              LocalDateTime.now(),
              null,
              null,
              null,
              origId);

      assertThat(trade.isCounterOffer()).isTrue();
      assertThat(trade.getOriginalTradeId()).isEqualTo(origId);
    }

    @Test
    void restoresRejectedTrade() {
      LocalDateTime rejected = LocalDateTime.now();
      Trade trade =
          Trade.restore(
              UUID.randomUUID(),
              TEAM_A,
              TEAM_B,
              OFFERED,
              REQUESTED,
              TradeStatus.REJECTED,
              LocalDateTime.now(),
              null,
              rejected,
              null,
              null);

      assertThat(trade.getStatus()).isEqualTo(TradeStatus.REJECTED);
      assertThat(trade.getRejectedAt()).isEqualTo(rejected);
    }
  }

  @Nested
  class StatusTransitions {

    @Test
    void acceptsFromPending() {
      Trade trade = new Trade(TEAM_A, TEAM_B, OFFERED, REQUESTED);

      boolean result = trade.accept();

      assertThat(result).isTrue();
      assertThat(trade.getStatus()).isEqualTo(TradeStatus.ACCEPTED);
      assertThat(trade.getAcceptedAt()).isNotNull();
    }

    @Test
    void cannotAcceptNonPending() {
      Trade trade = new Trade(TEAM_A, TEAM_B, OFFERED, REQUESTED);
      trade.accept();

      boolean result = trade.accept();

      assertThat(result).isFalse();
      assertThat(trade.getStatus()).isEqualTo(TradeStatus.ACCEPTED);
    }

    @Test
    void rejectsFromPending() {
      Trade trade = new Trade(TEAM_A, TEAM_B, OFFERED, REQUESTED);

      boolean result = trade.reject();

      assertThat(result).isTrue();
      assertThat(trade.getStatus()).isEqualTo(TradeStatus.REJECTED);
      assertThat(trade.getRejectedAt()).isNotNull();
    }

    @Test
    void cannotRejectNonPending() {
      Trade trade = new Trade(TEAM_A, TEAM_B, OFFERED, REQUESTED);
      trade.reject();

      assertThat(trade.reject()).isFalse();
    }

    @Test
    void cancelsFromPending() {
      Trade trade = new Trade(TEAM_A, TEAM_B, OFFERED, REQUESTED);

      boolean result = trade.cancel();

      assertThat(result).isTrue();
      assertThat(trade.getStatus()).isEqualTo(TradeStatus.CANCELLED);
      assertThat(trade.getCancelledAt()).isNotNull();
    }

    @Test
    void cannotCancelNonPending() {
      Trade trade = new Trade(TEAM_A, TEAM_B, OFFERED, REQUESTED);
      trade.cancel();

      assertThat(trade.cancel()).isFalse();
    }

    @Test
    void countersFromPending() {
      Trade trade = new Trade(TEAM_A, TEAM_B, OFFERED, REQUESTED);

      boolean result = trade.counter();

      assertThat(result).isTrue();
      assertThat(trade.getStatus()).isEqualTo(TradeStatus.COUNTERED);
    }

    @Test
    void cannotCounterNonPending() {
      Trade trade = new Trade(TEAM_A, TEAM_B, OFFERED, REQUESTED);
      trade.accept();

      assertThat(trade.counter()).isFalse();
    }
  }

  @Nested
  class QueryMethods {

    @Test
    void isCounterOfferWhenOriginalTradeIdSet() {
      Trade trade =
          Trade.restore(
              UUID.randomUUID(),
              TEAM_A,
              TEAM_B,
              OFFERED,
              REQUESTED,
              TradeStatus.PENDING,
              LocalDateTime.now(),
              null,
              null,
              null,
              UUID.randomUUID());

      assertThat(trade.isCounterOffer()).isTrue();
    }

    @Test
    void isNotCounterOfferWhenNoOriginalTradeId() {
      Trade trade = new Trade(TEAM_A, TEAM_B, OFFERED, REQUESTED);

      assertThat(trade.isCounterOffer()).isFalse();
    }

    @Test
    void isTerminalForAccepted() {
      Trade trade = new Trade(TEAM_A, TEAM_B, OFFERED, REQUESTED);
      trade.accept();

      assertThat(trade.isTerminal()).isTrue();
    }

    @Test
    void isNotTerminalForPending() {
      Trade trade = new Trade(TEAM_A, TEAM_B, OFFERED, REQUESTED);

      assertThat(trade.isTerminal()).isFalse();
    }

    @Test
    void isNotTerminalForCountered() {
      Trade trade = new Trade(TEAM_A, TEAM_B, OFFERED, REQUESTED);
      trade.counter();

      assertThat(trade.isTerminal()).isFalse();
    }

    @Test
    void totalPlayersCount() {
      UUID p1 = UUID.randomUUID();
      UUID p2 = UUID.randomUUID();
      UUID p3 = UUID.randomUUID();
      Trade trade = new Trade(TEAM_A, TEAM_B, List.of(p1, p2), List.of(p3));

      assertThat(trade.getTotalPlayersCount()).isEqualTo(3);
    }

    @Test
    void isBalancedWhenEqualCounts() {
      Trade trade = new Trade(TEAM_A, TEAM_B, OFFERED, REQUESTED);

      assertThat(trade.isBalanced()).isTrue();
    }

    @Test
    void isNotBalancedWhenDifferentCounts() {
      Trade trade =
          new Trade(
              TEAM_A,
              TEAM_B,
              List.of(UUID.randomUUID(), UUID.randomUUID()),
              List.of(UUID.randomUUID()));

      assertThat(trade.isBalanced()).isFalse();
    }
  }

  @Nested
  class Immutability {

    @Test
    void offeredPlayerIdsAreImmutable() {
      Trade trade = new Trade(TEAM_A, TEAM_B, OFFERED, REQUESTED);

      org.assertj.core.api.Assertions.assertThatThrownBy(
              () -> trade.getOfferedPlayerIds().add(UUID.randomUUID()))
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void requestedPlayerIdsAreImmutable() {
      Trade trade = new Trade(TEAM_A, TEAM_B, OFFERED, REQUESTED);

      org.assertj.core.api.Assertions.assertThatThrownBy(
              () -> trade.getRequestedPlayerIds().add(UUID.randomUUID()))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Nested
  class Equality {

    @Test
    void sameIdAreEqual() {
      UUID id = UUID.randomUUID();
      Trade t1 =
          Trade.restore(
              id,
              TEAM_A,
              TEAM_B,
              OFFERED,
              REQUESTED,
              TradeStatus.PENDING,
              LocalDateTime.now(),
              null,
              null,
              null,
              null);
      Trade t2 =
          Trade.restore(
              id,
              TEAM_B,
              TEAM_A,
              REQUESTED,
              OFFERED,
              TradeStatus.ACCEPTED,
              LocalDateTime.now(),
              null,
              null,
              null,
              null);

      assertThat(t1).isEqualTo(t2).hasSameHashCodeAs(t2);
    }

    @Test
    void differentIdAreNotEqual() {
      Trade t1 = new Trade(TEAM_A, TEAM_B, OFFERED, REQUESTED);
      Trade t2 = new Trade(TEAM_A, TEAM_B, OFFERED, REQUESTED);

      assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    void notEqualToNull() {
      Trade trade = new Trade(TEAM_A, TEAM_B, OFFERED, REQUESTED);

      assertThat(trade).isNotEqualTo(null);
    }
  }

  @Nested
  class TradeStatusEnumTest {

    @Test
    void terminalStatuses() {
      assertThat(TradeStatus.ACCEPTED.isTerminal()).isTrue();
      assertThat(TradeStatus.REJECTED.isTerminal()).isTrue();
      assertThat(TradeStatus.CANCELLED.isTerminal()).isTrue();
    }

    @Test
    void nonTerminalStatuses() {
      assertThat(TradeStatus.PENDING.isTerminal()).isFalse();
      assertThat(TradeStatus.COUNTERED.isTerminal()).isFalse();
    }
  }
}
