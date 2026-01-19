package com.fortnite.pronos.service.seed;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Score;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.ScoreRepository;
import com.fortnite.pronos.service.CsvDataLoaderService;
import com.fortnite.pronos.service.MockDataGeneratorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for seeding players and scores during initialization. Extracted from
 * DataInitializationService for SRP compliance and reduced coupling.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerSeedService {

  private final PlayerRepository playerRepository;
  private final ScoreRepository scoreRepository;
  private final CsvDataLoaderService csvDataLoaderService;
  private final SeedDataProviderSelectorService seedDataProviderSelector;

  /**
   * Initializes players from CSV data.
   *
   * @return list of saved players
   */
  public List<Player> initializePlayers() {
    log.info("Loading player data from CSV...");
    MockDataGeneratorService.MockDataSet mockData = seedDataProviderSelector.loadSeedData();

    if (mockData.total() == 0) {
      log.warn("No mock data loaded, falling back to CsvDataLoaderService");
      csvDataLoaderService.loadAllCsvData();
    } else {
      saveMockPlayersAndScores(mockData);
    }

    List<Player> savedPlayers = playerRepository.findAll();
    log.info("{} real players loaded from CSV with their complete scores", savedPlayers.size());
    return savedPlayers;
  }

  private void saveMockPlayersAndScores(MockDataGeneratorService.MockDataSet mockData) {
    List<Player> mockPlayers = mockData.getAllPlayers();
    List<Player> savedMockPlayers = playerRepository.saveAll(mockPlayers);

    List<Score> mockScores = mockData.getAllScores();
    for (int i = 0; i < savedMockPlayers.size() && i < mockScores.size(); i++) {
      mockScores.get(i).setPlayer(savedMockPlayers.get(i));
    }
    scoreRepository.saveAll(mockScores);

    log.info("{} mock players loaded from CSV", savedMockPlayers.size());
  }

  /** Returns all players in the repository. */
  public List<Player> getAllPlayers() {
    return playerRepository.findAll();
  }

  /** Returns the count of players. */
  public long getPlayerCount() {
    return playerRepository.count();
  }

  /** Returns the count of scores. */
  public long getScoreCount() {
    return scoreRepository.count();
  }

  /** Loads seed data for team creation. */
  public MockDataGeneratorService.MockDataSet loadSeedData() {
    return seedDataProviderSelector.loadSeedData();
  }
}
