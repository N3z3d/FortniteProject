package com.fortnite.pronos.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.User;

/**
 * Output port for GameParticipant persistence operations. Implemented by the persistence adapter
 * (GameParticipantRepository).
 */
public interface GameParticipantRepositoryPort {

  List<GameParticipant> findByGame(Game game);

  List<GameParticipant> findByUser(User user);

  Optional<GameParticipant> findByGameAndUser(Game game, User user);

  boolean existsByGameAndUser(Game game, User user);

  long countByGame(Game game);

  List<GameParticipant> findByGameOrderByDraftOrderAsc(Game game);

  List<GameParticipant> findByGameAndNoSelectedPlayers(Game game);

  List<GameParticipant> findByGameAndHasSelectedPlayers(Game game);

  List<GameParticipant> findByUserWithGameFetch(User user);

  List<GameParticipant> findByGameWithUserFetch(Game game);

  List<GameParticipant> findByGameIdOrderByJoinedAt(UUID gameId);

  boolean existsByUserIdAndGameId(UUID userId, UUID gameId);

  long countByGameId(UUID gameId);

  Optional<GameParticipant> findByUserIdAndGameId(UUID userId, UUID gameId);

  List<GameParticipant> findByGameIdWithUserFetch(UUID gameId);

  boolean existsByGameIdAndUserId(UUID gameId, UUID userId);

  List<GameParticipant> findByGameIdOrderByDraftOrderAsc(UUID gameId);

  Optional<GameParticipant> findById(UUID participantId);

  GameParticipant save(GameParticipant participant);

  void delete(GameParticipant participant);
}
