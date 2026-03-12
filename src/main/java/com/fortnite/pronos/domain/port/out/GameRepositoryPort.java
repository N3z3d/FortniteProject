package com.fortnite.pronos.domain.port.out;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fortnite.pronos.domain.model.Pagination;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.User;

/**
 * Output port for Game persistence operations. Implemented by the persistence adapter
 * (GameRepository). Framework-agnostic - uses domain Pagination DTO (no Spring dependencies).
 */
public interface GameRepositoryPort {

  List<Game> findByCreator(User creator);

  List<Game> findByStatus(GameStatus status);

  List<Game> findActiveGames();

  long countByStatus(GameStatus status);

  boolean existsByNameAndCreator(String name, User creator);

  List<Game> findByCreatedAtAfter(LocalDateTime date);

  List<Game> findGamesWithAvailableSlots();

  Optional<Game> findByInvitationCode(String invitationCode);

  boolean existsByInvitationCode(String invitationCode);

  List<Game> findGamesBySeason(LocalDateTime seasonStart, LocalDateTime seasonEnd);

  List<Game> findByCurrentSeason(Integer season);

  List<Game> findByCurrentSeasonWithFetch(Integer season);

  List<Game> findByNameContainingIgnoreCase(String namePattern);

  List<Game> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

  List<Game> findAllWithFetch();

  List<Game> findByCreatorWithFetch(User creator);

  Optional<Game> findByIdWithFetch(UUID gameId);

  List<Game> findGamesByUserId(UUID userId);

  List<Game> findByStatusWithFetch(GameStatus status);

  List<Game> findByCreatorId(UUID creatorId);

  List<Game> findByStatusNot(GameStatus status);

  List<Game> findByStatusInWithFetch(Collection<GameStatus> statuses);

  Optional<Game> findByInvitationCodeWithFetch(String invitationCode);

  Optional<Game> findById(UUID gameId);

  long countParticipantsByGameId(UUID gameId);

  boolean isUserParticipant(UUID gameId, UUID userId);

  Game save(Game game);

  long countByCreatorAndStatusIn(User creator, List<GameStatus> statuses);

  long count();

  void deleteAll();

  List<Game> findAll();

  List<Game> findAllByOrderByCreatedAtDesc();

  List<Game> findAllGames(Pagination pagination);

  boolean existsById(UUID id);

  void deleteAllInBatch();

  Game saveAndFlush(Game game);
}
