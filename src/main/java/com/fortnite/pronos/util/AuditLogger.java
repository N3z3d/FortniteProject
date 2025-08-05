package com.fortnite.pronos.util;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/** Utilitaire pour l'audit et le logging des actions importantes */
@Component
@Slf4j
public class AuditLogger {

  private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("AUDIT");

  /** Log une action utilisateur pour audit */
  public void logUserAction(UUID userId, String action, String details) {
    AUDIT_LOGGER.info(
        "USER_ACTION|userId={}|action={}|details={}|timestamp={}",
        userId,
        action,
        details,
        OffsetDateTime.now());
  }

  /** Log une connexion utilisateur */
  public void logUserLogin(UUID userId, String email, boolean success) {
    AUDIT_LOGGER.info(
        "USER_LOGIN|userId={}|email={}|success={}|timestamp={}",
        userId,
        email,
        success,
        OffsetDateTime.now());
  }

  /** Log une tentative de connexion échouée */
  public void logFailedLogin(String email, String reason) {
    AUDIT_LOGGER.warn(
        "LOGIN_FAILED|email={}|reason={}|timestamp={}", email, reason, OffsetDateTime.now());
  }

  /** Log la création d'une équipe */
  public void logTeamCreation(UUID userId, UUID teamId, String teamName, int season) {
    AUDIT_LOGGER.info(
        "TEAM_CREATED|userId={}|teamId={}|teamName={}|season={}|timestamp={}",
        userId,
        teamId,
        teamName,
        season,
        OffsetDateTime.now());
  }

  /** Log un changement de joueur dans une équipe */
  public void logPlayerChange(UUID userId, UUID teamId, UUID playerOutId, UUID playerInId) {
    AUDIT_LOGGER.info(
        "PLAYER_CHANGE|userId={}|teamId={}|playerOut={}|playerIn={}|timestamp={}",
        userId,
        teamId,
        playerOutId,
        playerInId,
        OffsetDateTime.now());
  }

  /** Log une proposition d'échange */
  public void logTradeProposal(
      UUID fromUserId, UUID toUserId, UUID tradeId, UUID playerOutId, UUID playerInId) {
    AUDIT_LOGGER.info(
        "TRADE_PROPOSED|fromUser={}|toUser={}|tradeId={}|playerOut={}|playerIn={}|timestamp={}",
        fromUserId,
        toUserId,
        tradeId,
        playerOutId,
        playerInId,
        OffsetDateTime.now());
  }

  /** Log l'acceptation/rejet d'un échange */
  public void logTradeDecision(UUID userId, UUID tradeId, String decision) {
    AUDIT_LOGGER.info(
        "TRADE_DECISION|userId={}|tradeId={}|decision={}|timestamp={}",
        userId,
        tradeId,
        decision,
        OffsetDateTime.now());
  }

  /** Log une mise à jour de scores */
  public void logScoreUpdate(UUID playerId, int oldScore, int newScore, String source) {
    AUDIT_LOGGER.info(
        "SCORE_UPDATE|playerId={}|oldScore={}|newScore={}|source={}|timestamp={}",
        playerId,
        oldScore,
        newScore,
        source,
        OffsetDateTime.now());
  }

  /** Log une erreur système */
  public void logSystemError(String component, String error, String details) {
    AUDIT_LOGGER.error(
        "SYSTEM_ERROR|component={}|error={}|details={}|timestamp={}",
        component,
        error,
        details,
        OffsetDateTime.now());
  }

  /** Log les métriques de performance */
  public void logPerformanceMetric(String operation, long durationMs, String details) {
    AUDIT_LOGGER.info(
        "PERFORMANCE|operation={}|duration={}ms|details={}|timestamp={}",
        operation,
        durationMs,
        details,
        OffsetDateTime.now());
  }

  /** Log l'accès à des données sensibles */
  public void logDataAccess(UUID userId, String dataType, String action) {
    AUDIT_LOGGER.info(
        "DATA_ACCESS|userId={}|dataType={}|action={}|timestamp={}",
        userId,
        dataType,
        action,
        OffsetDateTime.now());
  }
}
