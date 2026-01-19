package com.fortnite.pronos.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import com.fortnite.pronos.config.SeedProperties;
import com.fortnite.pronos.service.seed.GameSeedService;
import com.fortnite.pronos.service.seed.PlayerSeedService;
import com.fortnite.pronos.service.seed.TeamSeedService;
import com.fortnite.pronos.service.seed.UserSeedService;

/**
 * Unit tests for DataInitializationService seed configuration behavior. Tests the conditions under
 * which seeding is skipped or executed.
 */
@ExtendWith(MockitoExtension.class)
class DataInitializationServiceSeedTest {

  @Mock private SeedProperties seedProperties;
  @Mock private Environment environment;
  @Mock private UserSeedService userSeedService;
  @Mock private PlayerSeedService playerSeedService;
  @Mock private TeamSeedService teamSeedService;
  @Mock private GameSeedService gameSeedService;

  @InjectMocks private DataInitializationService dataInitializationService;

  @Test
  void skipsWhenSeedDisabled() {
    when(seedProperties.isEnabled()).thenReturn(false);

    dataInitializationService.initializeTestData();

    verifyNoInteractions(userSeedService, playerSeedService, teamSeedService, gameSeedService);
  }

  @Test
  void skipsWhenLegacySeedDisabled() {
    when(seedProperties.isEnabled()).thenReturn(true);
    when(seedProperties.isLegacyEnabled()).thenReturn(false);

    dataInitializationService.initializeTestData();

    verifyNoInteractions(userSeedService, playerSeedService, teamSeedService, gameSeedService);
  }

  @Test
  void skipsWhenExistingDataAndResetDisabled() {
    when(seedProperties.isEnabled()).thenReturn(true);
    when(seedProperties.isLegacyEnabled()).thenReturn(true);
    when(seedProperties.isReset()).thenReturn(false);
    when(environment.getActiveProfiles()).thenReturn(new String[] {"dev"});
    when(userSeedService.getAllUsers()).thenReturn(new ArrayList<>());
    when(playerSeedService.getPlayerCount()).thenReturn(1L);

    dataInitializationService.initializeTestData();

    verify(playerSeedService, never()).initializePlayers();
    verify(teamSeedService, never()).createTeamsFromCsvAssignments(null, null);
    verify(gameSeedService, never()).createTestGamesWithRealTeams(null, null);
  }
}
