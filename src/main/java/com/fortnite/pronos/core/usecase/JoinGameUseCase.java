package com.fortnite.pronos.core.usecase;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.domain.port.out.GameParticipantRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.dto.JoinGameRequest;
import com.fortnite.pronos.exception.GameFullException;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.exception.InvalidGameStateException;
import com.fortnite.pronos.exception.UserAlreadyInGameException;
import com.fortnite.pronos.exception.UserNotFoundException;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PHASE 2A: CLEAN ARCHITECTURE - Use Case for joining games
 *
 * <p>Business logic for joining a fantasy league game Encapsulates all the rules and validation
 * needed for a user to join a game
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JoinGameUseCase {

  private final GameRepositoryPort gameRepositoryPort;
  private final UserRepositoryPort userRepositoryPort;
  private final GameParticipantRepositoryPort gameParticipantRepositoryPort;

  /** Execute the join game use case */
  @Transactional
  public boolean execute(UUID userId, JoinGameRequest request) {
    log.info("Executing JoinGameUseCase for user {} joining game {}", userId, request.getGameId());

    // 1. Validate inputs and fetch entities
    User user =
        userRepositoryPort
            .findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

    Game game =
        gameRepositoryPort
            .findById(request.getGameId())
            .orElseThrow(
                () -> new GameNotFoundException("Game not found with ID: " + request.getGameId()));

    // 2. Apply business rules
    validateCanJoinGame(user, game);

    // 3. Create participation
    GameParticipant participant =
        GameParticipant.builder().game(game).user(user).joinedAt(LocalDateTime.now()).build();

    // 4. Persist the participation
    gameParticipantRepositoryPort.save(participant);

    // 5. Update game state if needed
    updateGameStateIfReady(game);

    log.info("User {} successfully joined game {}", userId, game.getId());
    return true;
  }

  /** Business rules for joining a game */
  private void validateCanJoinGame(User user, Game game) {
    // Rule 1: Game must be in a joinable state
    if (!isGameJoinable(game.getStatus())) {
      throw new InvalidGameStateException("Cannot join game in status: " + game.getStatus());
    }

    // Rule 2: Game must not be full
    long currentParticipants = gameParticipantRepositoryPort.countByGame(game);
    if (currentParticipants >= game.getMaxParticipants()) {
      throw new GameFullException(
          String.format(
              "Game is full. Max participants: %d, Current: %d",
              game.getMaxParticipants(), currentParticipants));
    }

    // Rule 3: User cannot already be in the game
    boolean alreadyParticipating = gameParticipantRepositoryPort.existsByGameAndUser(game, user);
    if (alreadyParticipating) {
      throw new UserAlreadyInGameException("User is already participating in this game");
    }

    // Rule 4: User cannot join their own game as participant
    if (game.getCreator().getId().equals(user.getId())) {
      throw new InvalidGameStateException("Game creator cannot join as participant");
    }
  }

  /** Check if game status allows joining */
  private boolean isGameJoinable(GameStatus status) {
    return status == GameStatus.CREATING || status == GameStatus.DRAFTING;
  }

  /** Update game state when conditions are met */
  private void updateGameStateIfReady(Game game) {
    // Ready flag: keep status as CREATING; draft will explicitly switch to DRAFTING
    gameRepositoryPort.save(game);
  }
}
