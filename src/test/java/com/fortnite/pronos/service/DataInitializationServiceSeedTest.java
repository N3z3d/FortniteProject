package com.fortnite.pronos.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fortnite.pronos.config.SeedProperties;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.ScoreRepository;
import com.fortnite.pronos.repository.TeamRepository;
import com.fortnite.pronos.repository.UserRepository;
import com.fortnite.pronos.service.seed.SeedDataProviderSelectorService;

@ExtendWith(MockitoExtension.class)
class DataInitializationServiceSeedTest {

  @Mock private UserRepository userRepository;
  @Mock private PlayerRepository playerRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private ScoreRepository scoreRepository;
  @Mock private GameRepository gameRepository;
  @Mock private Environment environment;
  @Mock private CsvDataLoaderService csvDataLoaderService;
  @Mock private SeedDataProviderSelectorService seedDataProviderSelector;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private SeedProperties seedProperties;

  @InjectMocks private DataInitializationService dataInitializationService;

  @Test
  void skipsWhenSeedDisabled() {
    when(seedProperties.isEnabled()).thenReturn(false);

    dataInitializationService.initializeTestData();

    verifyNoInteractions(
        userRepository,
        playerRepository,
        teamRepository,
        scoreRepository,
        gameRepository,
        seedDataProviderSelector,
        environment,
        csvDataLoaderService,
        passwordEncoder);
  }

  @Test
  void skipsWhenLegacySeedDisabled() {
    when(seedProperties.isEnabled()).thenReturn(true);
    when(seedProperties.isLegacyEnabled()).thenReturn(false);

    dataInitializationService.initializeTestData();

    verifyNoInteractions(
        userRepository,
        playerRepository,
        teamRepository,
        scoreRepository,
        gameRepository,
        seedDataProviderSelector,
        environment,
        csvDataLoaderService,
        passwordEncoder);
  }

  @Test
  void skipsWhenExistingDataAndResetDisabled() {
    when(seedProperties.isEnabled()).thenReturn(true);
    when(seedProperties.isLegacyEnabled()).thenReturn(true);
    when(seedProperties.isReset()).thenReturn(false);
    when(environment.getActiveProfiles()).thenReturn(new String[] {"dev"});
    when(userRepository.count()).thenReturn(1L);

    dataInitializationService.initializeTestData();

    verify(userRepository, never()).deleteAll();
    verify(teamRepository, never()).deleteAll();
    verify(scoreRepository, never()).deleteAll();
    verify(gameRepository, never()).deleteAll();
    verify(playerRepository, never()).deleteAll();
    verify(seedDataProviderSelector, never()).loadSeedData();
  }
}
