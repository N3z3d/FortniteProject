package com.fortnite.pronos.service.admin;

import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.dto.admin.GameSupervisionDto;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameStatus;

@Service
public class AdminGameSupervisionService {

  private static final List<GameStatus> ACTIVE_STATUSES =
      List.of(GameStatus.CREATING, GameStatus.DRAFTING, GameStatus.ACTIVE);

  private final GameRepositoryPort gameRepository;

  public AdminGameSupervisionService(GameRepositoryPort gameRepository) {
    this.gameRepository = gameRepository;
  }

  public List<GameSupervisionDto> getAllActiveGames() {
    return gameRepository.findByStatusInWithFetch(ACTIVE_STATUSES).stream()
        .map(this::toDto)
        .toList();
  }

  public List<GameSupervisionDto> getActiveGamesByStatus(GameStatus status) {
    if (!ACTIVE_STATUSES.contains(status)) {
      throw new IllegalArgumentException(
          "Status must be one of CREATING, DRAFTING, ACTIVE — got: " + status);
    }
    return gameRepository.findByStatusInWithFetch(List.of(status)).stream()
        .map(this::toDto)
        .toList();
  }

  private GameSupervisionDto toDto(Game game) {
    return new GameSupervisionDto(
        game.getId(),
        game.getName(),
        game.getStatus().name(),
        game.getDraftMode().name(),
        game.getParticipants().size(),
        game.getMaxParticipants(),
        game.getCreator().getUsername(),
        game.getCreatedAt().atOffset(ZoneOffset.UTC));
  }
}
