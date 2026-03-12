package com.fortnite.pronos.service.scoring;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrates the daily PR delta computation, scheduled at 08:00 UTC — after the nightly PR
 * ingestion window (05:00–08:00 UTC). Delegates all computation logic to {@link
 * TeamScoreDeltaBatchService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamScoreDeltaOrchestrationService {

  private final TeamScoreDeltaBatchService batchService;

  @Scheduled(cron = "${scoring.team.delta.cron:0 0 8 * * *}")
  public void runDailyDeltaComputation() {
    log.info("TeamScoreDeltaOrchestration: starting daily delta computation");
    batchService.computeAllGameDeltas();
    log.info("TeamScoreDeltaOrchestration: completed daily delta computation");
  }
}
