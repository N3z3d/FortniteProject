package com.fortnite.pronos.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.fortnite.pronos.repository.PlayerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvBootstrapService {

  private final CsvDataLoaderService csvDataLoaderService;
  private final PlayerRepository playerRepository;

  @EventListener(ApplicationReadyEvent.class)
  public void importCsvDataIfEmpty() {
    long playerCount = playerRepository.count();
    if (playerCount > 0) {
      log.info("CSV bootstrap: {} joueurs déjà présents, import ignoré", playerCount);
      return;
    }

    log.info("CSV bootstrap: aucune donnée trouvée, import du CSV...");
    csvDataLoaderService.loadAllCsvData();
    log.info(
        "CSV bootstrap: import terminé (players={}, scores importés via service)",
        playerRepository.count());
  }
}
