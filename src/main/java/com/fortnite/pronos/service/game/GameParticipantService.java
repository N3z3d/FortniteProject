package com.fortnite.pronos.service.game;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.dto.JoinGameRequest;
import com.fortnite.pronos.exception.GameFullException;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.exception.InvalidGameStateException;
import com.fortnite.pronos.exception.UnauthorizedAccessException;
import com.fortnite.pronos.exception.UserAlreadyInGameException;
import com.fortnite.pronos.exception.UserNotFoundException;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameParticipantRepository;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for game participant management Handles joining, leaving, and
 * participant-related operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GameParticipantService {

  private final GameRepository gameRepository;
  private final GameParticipantRepository gameParticipantRepository;
  private final UserRepository userRepository;

  /** Adds a user to a game */
  public boolean joinGame(UUID userId, JoinGameRequest request) {
    log.debug("User {} attempting to join game with code {}", userId, request.getInvitationCode());

    try {
      User user = findUserOrThrow(userId);
      Game game = findGameByInvitationCode(request.getInvitationCode());

      validateUserCanJoinGame(user, game);
      addUserToGame(user, game);

      log.info("User {} successfully joined game {}", user.getUsername(), game.getName());
      return true;
    } catch (Exception e) {
      log.debug("Failed to join game: {}", e.getMessage());
      return false;
    }
  }

  /** Removes a user from a game */
  public void leaveGame(UUID userId, UUID gameId) {
    log.debug("User {} attempting to leave game {}", userId, gameId);

    User user = findUserOrThrow(userId);
    Game game = findGameOrThrow(gameId);

    validateUserCanLeaveGame(user, game);
    removeUserFromGame(user, game);

    log.info("User {} left game {}", user.getUsername(), game.getName());
  }

  /** Gets all participants for a game */
  @Transactional(readOnly = true)
  public List<GameParticipant> getGameParticipants(UUID gameId) {
    return gameParticipantRepository.findByGameIdOrderByJoinedAt(gameId);
  }

  /** Checks if user is participant in game */
  @Transactional(readOnly = true)
  public boolean isUserParticipant(UUID userId, UUID gameId) {
    return gameParticipantRepository.existsByUserIdAndGameId(userId, gameId);
  }

  /** Gets participant count for game */
  @Transactional(readOnly = true)
  public long getParticipantCount(UUID gameId) {
    return gameParticipantRepository.countByGameId(gameId);
  }

  // Private helper methods

  private User findUserOrThrow(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
  }

  private Game findGameOrThrow(UUID gameId) {
    return gameRepository
        .findById(gameId)
        .orElseThrow(() -> new GameNotFoundException("Game not found: " + gameId));
  }

  private Game findGameByInvitationCode(String invitationCode) {
    return gameRepository
        .findByInvitationCode(invitationCode)
        .orElseThrow(
            () ->
                new GameNotFoundException(
                    "Game not found with invitation code: " + invitationCode));
  }

  private void validateUserCanJoinGame(User user, Game game) {
    if (isUserAlreadyInGame(user, game)) {
      throw new UserAlreadyInGameException("User is already in this game");
    }

    if (!canJoinGame(game)) {
      throw new InvalidGameStateException("Game is not accepting new participants");
    }

    if (isGameFull(game)) {
      throw new GameFullException("Game is full");
    }
  }

  private void validateUserCanLeaveGame(User user, Game game) {
    if (!isUserAlreadyInGame(user, game)) {
      throw new IllegalStateException("User is not in this game");
    }

    if (isGameCreator(user, game)) {
      throw new UnauthorizedAccessException("Game creator cannot leave the game");
    }
  }

  private boolean isUserAlreadyInGame(User user, Game game) {
    return gameParticipantRepository.existsByUserIdAndGameId(user.getId(), game.getId());
  }

  private boolean canJoinGame(Game game) {
    return game.getStatus() == GameStatus.CREATING
        || game.getStatus() == GameStatus.WAITING_FOR_PLAYERS;
  }

  private boolean isGameFull(Game game) {
    long currentParticipants = gameParticipantRepository.countByGameId(game.getId());
    return currentParticipants >= game.getMaxParticipants();
  }

  private boolean isGameCreator(User user, Game game) {
    return game.getCreator().getId().equals(user.getId());
  }

  private void addUserToGame(User user, Game game) {
    GameParticipant participant =
        GameParticipant.builder()
            .id(UUID.randomUUID())
            .game(game)
            .user(user)
            .joinedAt(LocalDateTime.now())
            .creator(false)
            .build();

    gameParticipantRepository.save(participant);
  }

  private void removeUserFromGame(User user, Game game) {
    GameParticipant participant =
        gameParticipantRepository
            .findByUserIdAndGameId(user.getId(), game.getId())
            .orElseThrow(() -> new IllegalStateException("Participant not found"));

    gameParticipantRepository.delete(participant);
  }
}
