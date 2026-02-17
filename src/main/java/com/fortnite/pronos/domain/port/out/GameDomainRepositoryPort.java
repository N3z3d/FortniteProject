package com.fortnite.pronos.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameStatus;
import com.fortnite.pronos.domain.model.Pagination;

/**
 * Output port for Game persistence migration (domain model contract).
 *
 * <p>This port coexists with the legacy {@link GameRepositoryPort} during ARCH-017/018 transition.
 */
public interface GameDomainRepositoryPort {

  Optional<Game> findById(UUID id);

  Game save(Game game);

  Optional<Game> findByInvitationCode(String invitationCode);

  boolean existsById(UUID id);

  long countByCreatorAndStatusIn(UUID creatorId, List<GameStatus> statuses);

  List<Game> findByStatus(GameStatus status);

  List<Game> findByCreatorId(UUID creatorId);

  List<Game> findAllByOrderByCreatedAtDesc();

  List<Game> findGamesWithAvailableSlots();

  List<Game> findGamesByUserId(UUID userId);

  List<Game> findByStatusNot(GameStatus status);

  List<Game> findAllGames(Pagination pagination);

  List<Game> findByNameContainingIgnoreCase(String name);

  boolean existsByInvitationCode(String code);

  long count();

  long countByStatus(GameStatus status);

  List<Game> findByCurrentSeasonWithFetch(Integer season);
}
