package com.fortnite.pronos.service.game;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameParticipant;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameParticipantRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.dto.JoinGameRequest;
import com.fortnite.pronos.exception.GameFullException;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.exception.InvalidGameStateException;
import com.fortnite.pronos.exception.UnauthorizedAccessException;
import com.fortnite.pronos.exception.UserAlreadyInGameException;
import com.fortnite.pronos.exception.UserNotFoundException;
import com.fortnite.pronos.model.User;

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

  private final GameDomainRepositoryPort gameRepository;
  private final GameRepositoryPort legacyGameRepository;
  private final GameParticipantRepositoryPort gameParticipantRepository;
  private final UserRepositoryPort userRepository;

  /** Adds a user to a game */
  public boolean joinGame(UUID userId, JoinGameRequest request) {
    log.debug("User {} attempting to join game with code {}", userId, request.getInvitationCode());

    User user = findUserOrThrow(userId);
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
  public List<com.fortnite.pronos.model.GameParticipant> getGameParticipants(UUID gameId) {
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
    if (game.getCreatorId().equals(user.getId()) || game.isParticipant(user.getId())) {
      throw new UserAlreadyInGameException("User is already in this game");
    }
    if (game.isFull()) {
      throw new GameFullException("Game is full");
    }
    if (!game.canAddParticipants()) {
      throw new InvalidGameStateException("Game is not accepting new participants");
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
    return game.isParticipant(user.getId());
  }

  private boolean isGameCreator(User user, Game game) {
    return game.getCreatorId().equals(user.getId());
  }

  private void addUserToGame(User user, Game game) {
    GameParticipant participant =
        GameParticipant.restore(
            UUID.randomUUID(),
            user.getId(),
            user.getUsername(),
            null,
            LocalDateTime.now(),
            null,
            false,
            List.of());
    if (!game.addParticipant(participant)) {
      throw new InvalidGameStateException("Game is not accepting new participants");
    }
    gameRepository.save(game);
    ensureCreatorParticipantPersisted(game);
  }

  private void removeUserFromGame(User user, Game game) {
    GameParticipant participant =
        game.getParticipants().stream()
            .filter(p -> p.getUserId().equals(user.getId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Participant not found"));
    game.removeParticipant(participant);
    gameRepository.save(game);
  }

  /**
   * Compatibility layer during migration: some legacy flows create games without persisting the
   * creator in game_participants. Keep creator row present for existing integrations relying on it.
   */
  private void ensureCreatorParticipantPersisted(Game domainGame) {
    UUID creatorId = domainGame.getCreatorId();
    UUID gameId = domainGame.getId();
    if (creatorId == null || gameId == null) {
      return;
    }
    if (gameParticipantRepository.existsByUserIdAndGameId(creatorId, gameId)) {
      return;
    }

    java.util.Optional<User> creatorOptional = userRepository.findById(creatorId);
    if (creatorOptional == null || creatorOptional.isEmpty()) {
      return;
    }
    java.util.Optional<com.fortnite.pronos.model.Game> legacyGameOptional =
        legacyGameRepository.findById(gameId);
    if (legacyGameOptional == null || legacyGameOptional.isEmpty()) {
      return;
    }

    com.fortnite.pronos.model.GameParticipant creatorParticipant =
        com.fortnite.pronos.model.GameParticipant.builder()
            .id(UUID.randomUUID())
            .game(legacyGameOptional.orElseThrow())
            .user(creatorOptional.orElseThrow())
            .creator(true)
            .joinedAt(LocalDateTime.now())
            .build();
    gameParticipantRepository.save(creatorParticipant);
  }
}
