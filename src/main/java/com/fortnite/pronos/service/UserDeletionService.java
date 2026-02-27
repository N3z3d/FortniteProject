package com.fortnite.pronos.service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameStatus;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.exception.AccountDeletionBlockedException;
import com.fortnite.pronos.exception.UserNotFoundException;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.game.GameParticipantService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Handles account deletion: roster liberation from active games then soft-delete. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserDeletionService {

  private static final Set<GameStatus> ACTIVE_STATUSES =
      EnumSet.of(GameStatus.CREATING, GameStatus.DRAFTING, GameStatus.ACTIVE);

  private final UserRepositoryPort userRepository;
  private final GameDomainRepositoryPort gameDomainRepository;
  private final GameParticipantService gameParticipantService;

  /**
   * Deletes the account of the given user.
   *
   * <p>Steps: 1. Validate user exists and is not already deleted. 2. Block if user owns any active
   * game. 3. Remove user from all active games where they are a participant (non-creator). 4.
   * Soft-delete the account.
   *
   * @param userId the ID of the user to delete
   * @throws UserNotFoundException if user does not exist or is already deleted
   * @throws AccountDeletionBlockedException if user owns active games
   */
  public void deleteAccount(UUID userId) {
    User user = findActiveUserOrThrow(userId);
    blockIfCreatorOfActiveGame(userId);
    leaveAllActiveGames(userId);
    softDeleteUser(userId);
    log.info("Account soft-deleted for user {}", user.getUsername());
  }

  private User findActiveUserOrThrow(UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    if (user.getDeletedAt() != null) {
      throw new UserNotFoundException("User not found: " + userId);
    }
    return user;
  }

  private void blockIfCreatorOfActiveGame(UUID userId) {
    List<Game> creatorGames = gameDomainRepository.findByCreatorId(userId);
    boolean hasActiveCreatorGame =
        creatorGames.stream().anyMatch(g -> ACTIVE_STATUSES.contains(g.getStatus()));
    if (hasActiveCreatorGame) {
      throw new AccountDeletionBlockedException(
          "Cannot delete account: you are the creator of one or more active games. "
              + "Please archive or transfer them first.");
    }
  }

  private void leaveAllActiveGames(UUID userId) {
    List<Game> participantGames = gameDomainRepository.findGamesByUserId(userId);
    participantGames.stream()
        .filter(g -> ACTIVE_STATUSES.contains(g.getStatus()))
        .filter(g -> !g.getCreatorId().equals(userId))
        .forEach(g -> gameParticipantService.leaveGame(userId, g.getId()));
  }

  private void softDeleteUser(UUID userId) {
    userRepository.softDelete(userId, LocalDateTime.now());
  }
}
