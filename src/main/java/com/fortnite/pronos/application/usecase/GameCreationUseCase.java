package com.fortnite.pronos.application.usecase;

import java.util.UUID;

import com.fortnite.pronos.dto.CreateGameRequest;
import com.fortnite.pronos.dto.GameDto;

/**
 * Application use case for Game creation operations. Defines the public API for creating and
 * managing games.
 */
public interface GameCreationUseCase {

  GameDto createGame(UUID creatorId, CreateGameRequest request);

  void deleteGame(UUID gameId);

  GameDto regenerateInvitationCode(UUID gameId);

  GameDto regenerateInvitationCode(UUID gameId, String duration);

  GameDto renameGame(UUID gameId, String newName);
}
