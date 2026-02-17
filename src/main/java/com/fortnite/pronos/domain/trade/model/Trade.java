package com.fortnite.pronos.domain.trade.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Pure domain model for Trade. No JPA, Spring, Hibernate, or Lombok dependencies. Represents a
 * trade proposal between two teams exchanging players.
 */
public final class Trade {

  private final UUID id;
  private final UUID fromTeamId;
  private final UUID toTeamId;
  private final List<UUID> offeredPlayerIds;
  private final List<UUID> requestedPlayerIds;
  private TradeStatus status;
  private final LocalDateTime proposedAt;
  private LocalDateTime acceptedAt;
  private LocalDateTime rejectedAt;
  private LocalDateTime cancelledAt;
  private final UUID originalTradeId;

  /** Business constructor for creating a new trade proposal. */
  public Trade(
      UUID fromTeamId, UUID toTeamId, List<UUID> offeredPlayerIds, List<UUID> requestedPlayerIds) {
    if (fromTeamId == null) {
      throw new IllegalArgumentException("fromTeamId cannot be null");
    }
    if (toTeamId == null) {
      throw new IllegalArgumentException("toTeamId cannot be null");
    }
    if (fromTeamId.equals(toTeamId)) {
      throw new IllegalArgumentException("Cannot trade with the same team");
    }
    if (offeredPlayerIds == null || offeredPlayerIds.isEmpty()) {
      throw new IllegalArgumentException("Must offer at least one player");
    }
    if (requestedPlayerIds == null || requestedPlayerIds.isEmpty()) {
      throw new IllegalArgumentException("Must request at least one player");
    }

    this.id = UUID.randomUUID();
    this.fromTeamId = fromTeamId;
    this.toTeamId = toTeamId;
    this.offeredPlayerIds = new ArrayList<>(offeredPlayerIds);
    this.requestedPlayerIds = new ArrayList<>(requestedPlayerIds);
    this.status = TradeStatus.PENDING;
    this.proposedAt = LocalDateTime.now();
    this.acceptedAt = null;
    this.rejectedAt = null;
    this.cancelledAt = null;
    this.originalTradeId = null;
  }

  /** Factory method for persistence reconstitution. */
  public static Trade restore(
      UUID id,
      UUID fromTeamId,
      UUID toTeamId,
      List<UUID> offeredPlayerIds,
      List<UUID> requestedPlayerIds,
      TradeStatus status,
      LocalDateTime proposedAt,
      LocalDateTime acceptedAt,
      LocalDateTime rejectedAt,
      LocalDateTime cancelledAt,
      UUID originalTradeId) {
    Trade trade =
        new Trade(
            id,
            fromTeamId,
            toTeamId,
            offeredPlayerIds,
            requestedPlayerIds,
            status,
            proposedAt,
            acceptedAt,
            rejectedAt,
            cancelledAt,
            originalTradeId);
    return trade;
  }

  private Trade(
      UUID id,
      UUID fromTeamId,
      UUID toTeamId,
      List<UUID> offeredPlayerIds,
      List<UUID> requestedPlayerIds,
      TradeStatus status,
      LocalDateTime proposedAt,
      LocalDateTime acceptedAt,
      LocalDateTime rejectedAt,
      LocalDateTime cancelledAt,
      UUID originalTradeId) {
    this.id = id != null ? id : UUID.randomUUID();
    this.fromTeamId = fromTeamId;
    this.toTeamId = toTeamId;
    this.offeredPlayerIds =
        offeredPlayerIds != null ? new ArrayList<>(offeredPlayerIds) : new ArrayList<>();
    this.requestedPlayerIds =
        requestedPlayerIds != null ? new ArrayList<>(requestedPlayerIds) : new ArrayList<>();
    this.status = status != null ? status : TradeStatus.PENDING;
    this.proposedAt = proposedAt != null ? proposedAt : LocalDateTime.now();
    this.acceptedAt = acceptedAt;
    this.rejectedAt = rejectedAt;
    this.cancelledAt = cancelledAt;
    this.originalTradeId = originalTradeId;
  }

  // --- Status transitions ---

  /** Accepts the trade. Returns true if transition was valid. */
  public boolean accept() {
    if (status != TradeStatus.PENDING) {
      return false;
    }
    status = TradeStatus.ACCEPTED;
    acceptedAt = LocalDateTime.now();
    return true;
  }

  /** Rejects the trade. Returns true if transition was valid. */
  public boolean reject() {
    if (status != TradeStatus.PENDING) {
      return false;
    }
    status = TradeStatus.REJECTED;
    rejectedAt = LocalDateTime.now();
    return true;
  }

  /** Cancels the trade. Returns true if transition was valid. */
  public boolean cancel() {
    if (status != TradeStatus.PENDING) {
      return false;
    }
    status = TradeStatus.CANCELLED;
    cancelledAt = LocalDateTime.now();
    return true;
  }

  /** Marks the trade as countered. Returns true if transition was valid. */
  public boolean counter() {
    if (status != TradeStatus.PENDING) {
      return false;
    }
    status = TradeStatus.COUNTERED;
    return true;
  }

  // --- Query methods ---

  /** Returns true if this trade is a counter-offer. */
  public boolean isCounterOffer() {
    return originalTradeId != null;
  }

  /** Returns true if the status is terminal (no further changes). */
  public boolean isTerminal() {
    return status.isTerminal();
  }

  /** Returns the total number of players involved in the trade. */
  public int getTotalPlayersCount() {
    return offeredPlayerIds.size() + requestedPlayerIds.size();
  }

  /** Returns true if the trade is balanced (equal number of offered and requested players). */
  public boolean isBalanced() {
    return offeredPlayerIds.size() == requestedPlayerIds.size();
  }

  // --- Getters ---

  public UUID getId() {
    return id;
  }

  public UUID getFromTeamId() {
    return fromTeamId;
  }

  public UUID getToTeamId() {
    return toTeamId;
  }

  public List<UUID> getOfferedPlayerIds() {
    return Collections.unmodifiableList(offeredPlayerIds);
  }

  public List<UUID> getRequestedPlayerIds() {
    return Collections.unmodifiableList(requestedPlayerIds);
  }

  public TradeStatus getStatus() {
    return status;
  }

  public LocalDateTime getProposedAt() {
    return proposedAt;
  }

  public LocalDateTime getAcceptedAt() {
    return acceptedAt;
  }

  public LocalDateTime getRejectedAt() {
    return rejectedAt;
  }

  public LocalDateTime getCancelledAt() {
    return cancelledAt;
  }

  public UUID getOriginalTradeId() {
    return originalTradeId;
  }

  // --- equals / hashCode on id ---

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Trade trade = (Trade) o;
    return Objects.equals(id, trade.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "Trade{id="
        + id
        + ", from="
        + fromTeamId
        + ", to="
        + toTeamId
        + ", status="
        + status
        + "}";
  }
}
