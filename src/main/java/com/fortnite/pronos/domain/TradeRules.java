package com.fortnite.pronos.domain;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import com.fortnite.pronos.model.Trade;

/**
 * Domain logic for trade validation and status transitions. Pure domain rules without JPA
 * dependencies.
 */
public final class TradeRules {

  private TradeRules() {}

  /**
   * Validates if a trade proposal is valid.
   *
   * @param fromTeamId source team ID
   * @param toTeamId target team ID
   * @param offeredPlayerIds players being offered
   * @param requestedPlayerIds players being requested
   * @param fromTeamPlayerIds players on source team
   * @param toTeamPlayerIds players on target team
   * @return validation result
   */
  public static ValidationResult validateTradeProposal(
      UUID fromTeamId,
      UUID toTeamId,
      Set<UUID> offeredPlayerIds,
      Set<UUID> requestedPlayerIds,
      Set<UUID> fromTeamPlayerIds,
      Set<UUID> toTeamPlayerIds) {

    if (fromTeamId == null || toTeamId == null) {
      return ValidationResult.failure("Both teams must be specified");
    }
    if (fromTeamId.equals(toTeamId)) {
      return ValidationResult.failure("Cannot trade with the same team");
    }
    if (offeredPlayerIds == null || offeredPlayerIds.isEmpty()) {
      return ValidationResult.failure("Must offer at least one player");
    }
    if (requestedPlayerIds == null || requestedPlayerIds.isEmpty()) {
      return ValidationResult.failure("Must request at least one player");
    }

    // Verify offered players belong to from team
    for (UUID playerId : offeredPlayerIds) {
      if (!fromTeamPlayerIds.contains(playerId)) {
        return ValidationResult.failure("Offered player is not on the source team");
      }
    }

    // Verify requested players belong to to team
    for (UUID playerId : requestedPlayerIds) {
      if (!toTeamPlayerIds.contains(playerId)) {
        return ValidationResult.failure("Requested player is not on the target team");
      }
    }

    return ValidationResult.success();
  }

  /**
   * Validates if trading is allowed within a game context.
   *
   * @param tradingEnabled whether trading is enabled for the game
   * @param tradeDeadline deadline for trades (null = no deadline)
   * @param currentTradeCount current completed trades count
   * @param maxTradesPerTeam maximum trades allowed per team
   * @return validation result
   */
  public static ValidationResult canTrade(
      boolean tradingEnabled,
      LocalDateTime tradeDeadline,
      int currentTradeCount,
      int maxTradesPerTeam) {

    if (!tradingEnabled) {
      return ValidationResult.failure("Trading is not enabled for this game");
    }
    if (tradeDeadline != null && LocalDateTime.now().isAfter(tradeDeadline)) {
      return ValidationResult.failure("Trade deadline has passed");
    }
    if (currentTradeCount >= maxTradesPerTeam) {
      return ValidationResult.failure(
          "Team has reached maximum trade limit (" + maxTradesPerTeam + ")");
    }
    return ValidationResult.success();
  }

  /**
   * Validates if a trade can be accepted.
   *
   * @param currentStatus current trade status
   * @return transition result
   */
  public static TransitionResult canAccept(Trade.Status currentStatus) {
    if (currentStatus != Trade.Status.PENDING) {
      return TransitionResult.failure("Only PENDING trades can be accepted");
    }
    return TransitionResult.success(Trade.Status.ACCEPTED);
  }

  /**
   * Validates if a trade can be rejected.
   *
   * @param currentStatus current trade status
   * @return transition result
   */
  public static TransitionResult canReject(Trade.Status currentStatus) {
    if (currentStatus != Trade.Status.PENDING) {
      return TransitionResult.failure("Only PENDING trades can be rejected");
    }
    return TransitionResult.success(Trade.Status.REJECTED);
  }

  /**
   * Validates if a trade can be cancelled.
   *
   * @param currentStatus current trade status
   * @return transition result
   */
  public static TransitionResult canCancel(Trade.Status currentStatus) {
    if (currentStatus != Trade.Status.PENDING) {
      return TransitionResult.failure("Only PENDING trades can be cancelled");
    }
    return TransitionResult.success(Trade.Status.CANCELLED);
  }

  /**
   * Validates if a counter-offer can be made.
   *
   * @param currentStatus current trade status
   * @return transition result
   */
  public static TransitionResult canCounter(Trade.Status currentStatus) {
    if (currentStatus != Trade.Status.PENDING) {
      return TransitionResult.failure("Only PENDING trades can be countered");
    }
    return TransitionResult.success(Trade.Status.COUNTERED);
  }

  /**
   * Checks if a trade status is terminal (no more changes allowed).
   *
   * @param status status to check
   * @return true if terminal
   */
  public static boolean isTerminal(Trade.Status status) {
    return status == Trade.Status.ACCEPTED
        || status == Trade.Status.REJECTED
        || status == Trade.Status.CANCELLED;
  }

  /** Result of a trade validation. */
  public record ValidationResult(boolean valid, String errorMessage) {

    public static ValidationResult success() {
      return new ValidationResult(true, null);
    }

    public static ValidationResult failure(String errorMessage) {
      return new ValidationResult(false, errorMessage);
    }
  }

  /** Result of a trade status transition validation. */
  public record TransitionResult(boolean allowed, Trade.Status newStatus, String errorMessage) {

    public static TransitionResult success(Trade.Status newStatus) {
      return new TransitionResult(true, newStatus, null);
    }

    public static TransitionResult failure(String errorMessage) {
      return new TransitionResult(false, null, errorMessage);
    }
  }
}
