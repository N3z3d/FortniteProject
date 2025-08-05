package com.fortnite.pronos.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.*;

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

import com.fortnite.pronos.dto.GameDetailDto;
import com.fortnite.pronos.dto.GameDetailDto.*;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.service.GameDetailService;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameDetailController - TDD")
class GameDetailControllerTest {

  @Mock private GameDetailService gameDetailService;

  @InjectMocks private GameDetailController gameDetailController;

  private MockMvc mockMvc;
  private UUID testGameId;
  private GameDetailDto testGameDetails;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(gameDetailController).build();

    testGameId = UUID.randomUUID();

    // Créer les données de test
    List<PlayerInfo> thibautPlayers =
        Arrays.asList(
            PlayerInfo.builder()
                .playerId(UUID.randomUUID())
                .nickname("Bugha")
                .region("NAW")
                .tranche("1-10")
                .currentScore(450)
                .build(),
            PlayerInfo.builder()
                .playerId(UUID.randomUUID())
                .nickname("Aqua")
                .region("EU")
                .tranche("1-10")
                .currentScore(380)
                .build());

    List<ParticipantInfo> participants =
        Arrays.asList(
            ParticipantInfo.builder()
                .participantId(UUID.randomUUID())
                .username("Thibaut")
                .email("thibaut@test.com")
                .joinedAt(LocalDateTime.now().minusDays(5))
                .joinOrder(1)
                .isCreator(true)
                .totalPlayers(2)
                .selectedPlayers(thibautPlayers)
                .build(),
            ParticipantInfo.builder()
                .participantId(UUID.randomUUID())
                .username("Teddy")
                .email("teddy@test.com")
                .joinedAt(LocalDateTime.now().minusDays(4))
                .joinOrder(2)
                .isCreator(false)
                .totalPlayers(3)
                .selectedPlayers(new ArrayList<>())
                .build(),
            ParticipantInfo.builder()
                .participantId(UUID.randomUUID())
                .username("Marcel")
                .email("marcel@test.com")
                .joinedAt(LocalDateTime.now().minusDays(4))
                .joinOrder(3)
                .isCreator(false)
                .totalPlayers(3)
                .selectedPlayers(new ArrayList<>())
                .build());

    GameStatistics statistics =
        GameStatistics.builder()
            .totalParticipants(3)
            .totalPlayers(8)
            .regionDistribution(Map.of("EU", 4, "NAW", 3, "BR", 1))
            .averagePlayersPerParticipant(2.67)
            .remainingSlots(7)
            .build();

    testGameDetails =
        GameDetailDto.builder()
            .gameId(testGameId)
            .gameName("Game Thibaut-Teddy-Marcel")
            .description("Game entre amis")
            .creatorName("Thibaut")
            .status("ACTIVE")
            .invitationCode("TTM2025")
            .maxParticipants(10)
            .createdAt(LocalDateTime.now().minusDays(5))
            .updatedAt(LocalDateTime.now())
            .participants(participants)
            .draftInfo(null)
            .statistics(statistics)
            .build();
  }

  @Test
  @DisplayName("devrait récupérer les détails d'une game avec succès")
  void shouldGetGameDetailsSuccessfully() throws Exception {
    // Given
    when(gameDetailService.getGameDetails(testGameId)).thenReturn(testGameDetails);

    // When & Then
    mockMvc
        .perform(get("/api/games/{gameId}/details", testGameId).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.gameId").value(testGameId.toString()))
        .andExpect(jsonPath("$.gameName").value("Game Thibaut-Teddy-Marcel"))
        .andExpect(jsonPath("$.creatorName").value("Thibaut"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.invitationCode").value("TTM2025"))
        .andExpect(jsonPath("$.participants").isArray())
        .andExpect(jsonPath("$.participants.length()").value(3))
        .andExpect(jsonPath("$.participants[0].username").value("Thibaut"))
        .andExpect(jsonPath("$.participants[0].isCreator").value(true))
        .andExpect(jsonPath("$.statistics.totalParticipants").value(3))
        .andExpect(jsonPath("$.statistics.totalPlayers").value(8));
  }

  @Test
  @DisplayName("devrait retourner 404 si la game n'existe pas")
  void shouldReturn404IfGameNotFound() throws Exception {
    // Given
    UUID unknownGameId = UUID.randomUUID();
    when(gameDetailService.getGameDetails(unknownGameId))
        .thenThrow(new GameNotFoundException("Game non trouvée"));

    // When & Then
    mockMvc
        .perform(
            get("/api/games/{gameId}/details", unknownGameId).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("GAME_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("Game non trouvée"));
  }

  @Test
  @DisplayName("devrait gérer les UUID invalides")
  void shouldHandleInvalidUuid() throws Exception {
    // When & Then
    mockMvc
        .perform(
            get("/api/games/{gameId}/details", "invalid-uuid").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
  }
}
