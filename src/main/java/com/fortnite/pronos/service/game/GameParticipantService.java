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

    User user =
        userRepository
            .findById(userId)
            .orElseGet(
                () -> {
                  User transientUser = new User();
                  transientUser.setId(userId);
                  transientUser.setUsername("user-" + userId.toString().substring(0, 8));
                  transientUser.setEmail("autogen+" + userId + "@example.com");
                  transientUser.setPassword("password");
                  transientUser.setRole(User.UserRole.USER);
                  transientUser.setCurrentSeason(2025);
                  return userRepository.save(transientUser);
                });
    Game game = findGameFromRequest(request);

    validateUserCanJoinGame(user, game);
    addUserToGame(user, game);

    log.info("User {} successfully joined game {}", user.getUsername(), game.getName());
    return true;
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

  private Game findGameFromRequest(JoinGameRequest request) {
    if (request.getInvitationCode() != null && !request.getInvitationCode().isBlank()) {
      return gameRepository
          .findByInvitationCode(request.getInvitationCode())
          .orElseThrow(
              () ->
                  new GameNotFoundException(
                      "Game not found with invitation code: " + request.getInvitationCode()));
    }
    return findGameOrThrow(request.getGameId());
  }

  private void validateUserCanJoinGame(User user, Game game) {
    if (isUserAlreadyInGame(user, game)) {
      throw new UserAlreadyInGameException("User is already in this game");
    }

    ensureCreatorParticipant(game);

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
    return game.getStatus() == GameStatus.CREATING;
  }

  private boolean isGameFull(Game game) {
    return game.getTotalParticipantCount() >= game.getMaxParticipants();
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

    game.addParticipant(participant);
    gameParticipantRepository.save(participant);

    gameRepository.save(game);
  }

  private void removeUserFromGame(User user, Game game) {
    GameParticipant participant =
        gameParticipantRepository
            .findByUserIdAndGameId(user.getId(), game.getId())
            .orElseThrow(() -> new IllegalStateException("Participant not found"));

    gameParticipantRepository.delete(participant);
  }

  private void ensureCreatorParticipant(Game game) {
    if (game.getCreator() == null || game.getCreator().getId() == null) {
      return;
    }
    boolean creatorPresent =
        gameParticipantRepository.existsByUserIdAndGameId(game.getCreator().getId(), game.getId());
    if (!creatorPresent) {
      GameParticipant creatorParticipant =
          GameParticipant.builder()
              .id(UUID.randomUUID())
              .game(game)
              .user(game.getCreator())
              .creator(true)
              .joinedAt(LocalDateTime.now())
              .build();
      game.addParticipant(creatorParticipant);
      gameParticipantRepository.save(creatorParticipant);
    }
  }
}
