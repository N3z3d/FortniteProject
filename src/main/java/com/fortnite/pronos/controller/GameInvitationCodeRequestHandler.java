package com.fortnite.pronos.controller;

import java.util.Locale;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fortnite.pronos.application.usecase.GameQueryUseCase;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.dto.JoinGameRequest;
import com.fortnite.pronos.dto.JoinGameWithCodeRequest;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.GameService;
import com.fortnite.pronos.service.InvitationCodeAttemptGuard;
import com.fortnite.pronos.service.UserResolver;
import com.fortnite.pronos.service.ValidationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
class GameInvitationCodeRequestHandler {

  private final GameService gameService;
  private final GameQueryUseCase gameQueryUseCase;
  private final ValidationService validationService;
  private final UserResolver userResolver;
  private final InvitationCodeAttemptGuard invitationCodeAttemptGuard;

  ResponseEntity<GameDto> joinGameWithCode(
      JoinGameWithCodeRequest payload, String username, HttpServletRequest httpRequest) {
    log.info(
        "GameController: joinGameWithCode requested - requestedUser={}",
        username != null ? username : "-");

    User user = userResolver.resolve(username, httpRequest);
    if (user == null) {
      log.warn(
          "GameController: joinGameWithCode unauthorized - requestedUser={}, remoteAddr={}",
          username != null ? username : "-",
          httpRequest.getRemoteAddr());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String invitationCode = payload == null ? null : payload.resolveInvitationCode();
    if (invitationCode == null || invitationCode.isBlank()) {
      log.warn("GameController: joinGameWithCode invalid payload - userId={}", user.getId());
      return ResponseEntity.badRequest().build();
    }

    String normalizedCode = normalizeInvitationCode(invitationCode);
    invitationCodeAttemptGuard.registerAttemptOrThrow(user.getId(), httpRequest.getRemoteAddr());
    return gameService
        .getGameByInvitationCode(normalizedCode)
        .map(game -> joinUserToGameByInvitationCode(game, user.getId(), normalizedCode))
        .orElseGet(() -> notFoundByInvitationCode(user.getId(), normalizedCode));
  }

  ResponseEntity<GameDto> regenerateInvitationCode(
      UUID id, String username, HttpServletRequest httpRequest, String duration) {
    log.info("GameController: regenerateInvitationCode requested - gameId={}", id);

    User user = userResolver.resolve(username, httpRequest);
    if (user == null) {
      log.warn("GameController: regenerateInvitationCode unauthorized - gameId={}", id);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    GameDto gameDto = gameQueryUseCase.getGameByIdOrThrow(id);
    if (!user.getId().equals(gameDto.getCreatorId())) {
      log.warn("GameController: regenerateInvitationCode forbidden - gameId={}", id);
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    GameDto game = gameService.regenerateInvitationCode(id, duration);
    log.info("GameController: regenerateInvitationCode succeeded - gameId={}", id);
    return ResponseEntity.ok(game);
  }

  ResponseEntity<GameDto> deleteInvitationCode(
      UUID id, String username, HttpServletRequest httpRequest) {
    log.info("GameController: deleteInvitationCode requested - gameId={}", id);

    User user = userResolver.resolve(username, httpRequest);
    if (user == null) {
      log.warn("GameController: deleteInvitationCode unauthorized - gameId={}", id);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    GameDto gameDto = gameQueryUseCase.getGameByIdOrThrow(id);
    if (!user.getId().equals(gameDto.getCreatorId())) {
      log.warn("GameController: deleteInvitationCode forbidden - gameId={}", id);
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    GameDto updatedGame = gameService.deleteInvitationCode(id);
    log.info("GameController: deleteInvitationCode succeeded - gameId={}", id);
    return ResponseEntity.ok(updatedGame);
  }

  private String normalizeInvitationCode(String invitationCode) {
    return invitationCode.trim().toUpperCase(Locale.ROOT);
  }

  private ResponseEntity<GameDto> joinUserToGameByInvitationCode(
      GameDto game, UUID userId, String normalizedCode) {
    JoinGameRequest joinRequest = new JoinGameRequest();
    joinRequest.setGameId(game.getId());
    joinRequest.setUserId(userId);
    joinRequest.setInvitationCode(normalizedCode);
    validationService.validateJoinGameRequest(joinRequest);
    gameService.joinGame(userId, joinRequest);
    log.info(
        "GameController: joinGameWithCode succeeded - gameId={}, userId={}", game.getId(), userId);
    return ResponseEntity.ok(gameService.getGameByIdOrThrow(game.getId()));
  }

  private ResponseEntity<GameDto> notFoundByInvitationCode(UUID userId, String normalizedCode) {
    log.warn(
        "GameController: joinGameWithCode notFound - userId={}, codeLength={}",
        userId,
        normalizedCode.length());
    return ResponseEntity.notFound().build();
  }
}
