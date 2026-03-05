package com.fortnite.pronos.adapter.out.persistence.support;

import java.util.UUID;

import com.fortnite.pronos.model.Draft;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.User;

/**
 * Factory for creating JPA entity shells (reference objects) that contain only an ID. Used by
 * adapter mappers to avoid loading full entities when only a foreign-key relationship is needed.
 * Centralizes the duplicated {@code createUserReference} / {@code createPlayerReference} methods
 * found across multiple mappers.
 */
public final class EntityReferenceFactory {

  private EntityReferenceFactory() {}

  /**
   * Creates a {@link User} shell containing only the given id.
   *
   * @param userId the user's id, or {@code null}
   * @return a User reference or {@code null} if userId is null
   */
  public static User userRef(UUID userId) {
    if (userId == null) {
      return null;
    }
    User user = new User();
    user.setId(userId);
    return user;
  }

  /**
   * Creates a {@link User} shell containing the given id and username.
   *
   * @param userId the user's id, or {@code null}
   * @param username the user's username (may be null)
   * @return a User reference or {@code null} if userId is null
   */
  public static User userRef(UUID userId, String username) {
    if (userId == null) {
      return null;
    }
    User user = new User();
    user.setId(userId);
    user.setUsername(username);
    return user;
  }

  /**
   * Creates a {@link Player} shell containing only the given id.
   *
   * @param playerId the player's id, or {@code null}
   * @return a Player reference or {@code null} if playerId is null
   */
  public static Player playerRef(UUID playerId) {
    if (playerId == null) {
      return null;
    }
    return Player.builder().id(playerId).build();
  }

  /**
   * Creates a {@link Game} shell containing only the given id.
   *
   * @param gameId the game's id, or {@code null}
   * @return a Game reference or {@code null} if gameId is null
   */
  public static Game gameRef(UUID gameId) {
    if (gameId == null) {
      return null;
    }
    Game game = new Game();
    game.setId(gameId);
    return game;
  }

  /**
   * Creates a {@link Draft} shell containing only the given id.
   *
   * @param draftId the draft's id, or {@code null}
   * @return a Draft reference or {@code null} if draftId is null
   */
  public static Draft draftRef(UUID draftId) {
    if (draftId == null) {
      return null;
    }
    Draft draft = new Draft();
    draft.setId(draftId);
    return draft;
  }
}
