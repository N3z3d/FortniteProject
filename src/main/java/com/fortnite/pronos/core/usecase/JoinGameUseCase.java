package com.fortnite.pronos.core.usecase;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameParticipant;
import com.fortnite.pronos.domain.game.model.GameStatus;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.dto.JoinGameRequest;
import com.fortnite.pronos.exception.GameFullException;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.exception.InvalidGameStateException;
import com.fortnite.pronos.exception.UserAlreadyInGameException;
import com.fortnite.pronos.exception.UserNotFoundException;
import com.fortnite.pronos.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Use Case for joining games. Uses domain models via GameDomainRepositoryPort. */
@Slf4j
@Component
@RequiredArgsConstructor
public class JoinGameUseCase {

  private final GameDomainRepositoryPort gameDomainRepositoryPort;
  private final UserRepositoryPort userRepositoryPort;

  @Transactional
  public boolean execute(UUID userId, JoinGameRequest request) {
    log.info("Executing JoinGameUseCase for user {} joining game {}", userId, request.getGameId());

    User user =
        userRepositoryPort
            .findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

    Game game =
        gameDomainRepositoryPort
            .findById(request.getGameId())
            .orElseThrow(
                () -> new GameNotFoundException("Game not found with ID: " + request.getGameId()));

    validateCanJoinGame(user, game);

    GameParticipant participant = new GameParticipant(user.getId(), user.getUsername(), false);
    game.addParticipant(participant);

    gameDomainRepositoryPort.save(game);

    log.info("User {} successfully joined game {}", userId, game.getId());
    return true;
  }

  private void validateCanJoinGame(User user, Game game) {
    if (!isGameJoinable(game.getStatus())) {
      throw new InvalidGameStateException("Cannot join game in status: " + game.getStatus());
    }

    if (game.isFull()) {
      throw new GameFullException(
          String.format(
              "Game is full. Max participants: %d, Current: %d",
              game.getMaxParticipants(), game.getParticipantCount()));
    }

    if (game.getCreatorId().equals(user.getId())) {
      throw new InvalidGameStateException("Game creator cannot join as participant");
    }

    if (game.isParticipant(user.getId())) {
      throw new UserAlreadyInGameException("User is already participating in this game");
    }
  }

  private boolean isGameJoinable(GameStatus status) {
    return status == GameStatus.CREATING || status == GameStatus.DRAFTING;
  }
}
