package com.fortnite.pronos.application.usecase;

import java.util.UUID;

import com.fortnite.pronos.dto.GameDetailDto;

public interface GameDetailUseCase {

  GameDetailDto getGameDetails(UUID gameId);
}
