package com.fortnite.pronos.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fortnite.pronos.application.usecase.GameStatisticsUseCase;
import com.fortnite.pronos.model.Player;

@ExtendWith(MockitoExtension.class)
class GameStatisticsControllerTest {

  @Mock private GameStatisticsUseCase gameStatisticsService;

  @InjectMocks private GameStatisticsController gameStatisticsController;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(gameStatisticsController).build();
  }

  @Test
  @DisplayName("devrait retourner la distribution par région")
  void shouldReturnRegionDistribution() throws Exception {
    // Given
    UUID gameId = UUID.randomUUID();
    Map<Player.Region, Integer> distribution = new HashMap<>();
    distribution.put(Player.Region.EU, 4);
    distribution.put(Player.Region.NAW, 2);
    distribution.put(Player.Region.BR, 2);
    distribution.put(Player.Region.ASIA, 1);

    when(gameStatisticsService.getPlayerDistributionByRegion(gameId)).thenReturn(distribution);

    // When & Then
    mockMvc
        .perform(
            get("/api/games/{gameId}/statistics/region-distribution", gameId)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.EU").value(4))
        .andExpect(jsonPath("$.NAW").value(2))
        .andExpect(jsonPath("$.BR").value(2))
        .andExpect(jsonPath("$.ASIA").value(1));
  }

  @Test
  @DisplayName("devrait retourner la distribution par région en pourcentage")
  void shouldReturnRegionDistributionPercentage() throws Exception {
    // Given
    UUID gameId = UUID.randomUUID();
    Map<Player.Region, Double> percentages = new HashMap<>();
    percentages.put(Player.Region.EU, 44.4);
    percentages.put(Player.Region.NAW, 22.2);
    percentages.put(Player.Region.BR, 22.2);
    percentages.put(Player.Region.ASIA, 11.1);

    when(gameStatisticsService.getPlayerDistributionByRegionPercentage(gameId))
        .thenReturn(percentages);

    // When & Then
    mockMvc
        .perform(
            get("/api/games/{gameId}/statistics/region-distribution-percentage", gameId)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.EU").value(44.4))
        .andExpect(jsonPath("$.NAW").value(22.2))
        .andExpect(jsonPath("$.BR").value(22.2))
        .andExpect(jsonPath("$.ASIA").value(11.1));
  }

  @Test
  @DisplayName("devrait retourner 404 si la game n'existe pas")
  void shouldReturn404IfGameNotFound() throws Exception {
    // Given
    UUID gameId = UUID.randomUUID();
    when(gameStatisticsService.getPlayerDistributionByRegion(gameId))
        .thenThrow(new IllegalArgumentException("Game non trouvée"));

    // When & Then
    mockMvc
        .perform(
            get("/api/games/{gameId}/statistics/region-distribution", gameId)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Game non trouvée"));
  }

  @Test
  @DisplayName("devrait retourner un objet vide si aucun joueur dans la game")
  void shouldReturnEmptyObjectIfNoPlayers() throws Exception {
    // Given
    UUID gameId = UUID.randomUUID();
    when(gameStatisticsService.getPlayerDistributionByRegion(gameId)).thenReturn(new HashMap<>());

    // When & Then
    mockMvc
        .perform(
            get("/api/games/{gameId}/statistics/region-distribution", gameId)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json("{}"));
  }
}
