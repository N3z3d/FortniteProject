package com.fortnite.pronos.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.ScoreRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvBootstrapService {

  private final CsvDataLoaderService csvDataLoaderService;
  private final PlayerRepository playerRepository;
  private final ScoreRepository scoreRepository;

  @EventListener(ApplicationReadyEvent.class)
  @Order(1)
  public void importCsvDataIfEmpty() {
    long playerCount = playerRepository.count();
    long scoreCount = scoreRepository.count();

    if (playerCount > 0 && scoreCount >= playerCount) {
      log.info("CSV bootstrap: players={}, scores={}, import skipped", playerCount, scoreCount);
      return;
    }

    log.info("CSV bootstrap: loading CSV (players={}, scores={})", playerCount, scoreCount);
    csvDataLoaderService.loadAllCsvData();
    log.info(
        "CSV bootstrap: import done (players={}, scores={})",
        playerRepository.count(),
        scoreRepository.count());
  }
}
