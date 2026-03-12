package com.fortnite.pronos.service.scoring;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Batch orchestrator: iterates eligible games and delegates per-game delta computation to {@link
 * TeamScoreDeltaGameService} which runs each game in its own transaction. This avoids the Spring
 * AOP self-invocation trap where {@code @Transactional} on a private/package-private method called
 * from the same class is silently bypassed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamScoreDeltaBatchService {

  private final GameDomainRepositoryPort gameDomainRepository;
  private final TeamScoreDeltaGameService gameProcessor;

  public void computeAllGameDeltas() {
    LocalDate today = LocalDate.now();
    List<Game> games = gameDomainRepository.findAllWithCompetitionPeriod();
    log.info("TeamScoreDeltaBatch: processing {} eligible games", games.size());

    for (Game game : games) {
      if (game.getDraftId() == null) {
        log.debug("TeamScoreDeltaBatch: skipping game {} — no draft", game.getId());
        continue;
      }
      try {
        gameProcessor.computeDeltasForGame(game, today);
      } catch (Exception e) {
        log.error(
            "TeamScoreDeltaBatch: error processing gameId={}: {}", game.getId(), e.getMessage(), e);
      }
    }
    log.info("TeamScoreDeltaBatch: completed");
  }
}
