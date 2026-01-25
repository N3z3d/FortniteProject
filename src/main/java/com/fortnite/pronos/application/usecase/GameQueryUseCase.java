package com.fortnite.pronos.application.usecase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameStatus;

/** Application use case for Game query operations. Defines the public API for querying games. */
public interface GameQueryUseCase {

  List<GameDto> getAllGames();

  List<GameDto> getAvailableGames();

  List<GameDto> getGamesByUser(UUID userId);

  Optional<GameDto> getGameById(UUID gameId);

  GameDto getGameByIdOrThrow(UUID gameId);

  Game getGameEntityById(UUID gameId);

  Optional<GameDto> getGameByInvitationCode(String invitationCode);

  List<GameDto> getGamesByStatus(GameStatus status);

  List<GameDto> getActiveGames();

  List<GameDto> searchGamesByName(String name);

  List<GameDto> getGamesCreatedByUser(UUID userId);

  boolean gameExists(UUID gameId);

  boolean gameExistsByInvitationCode(String invitationCode);

  long getGameCount();

  long getGameCountByStatus(GameStatus status);

  List<GameDto> getGamesBySeason(Integer season);

  Integer getCurrentSeason();
}
