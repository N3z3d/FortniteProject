package com.fortnite.pronos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fortnite.pronos.service.PlayerService;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerController - Stats")
class PlayerControllerTest {

  @Mock private PlayerService playerService;

  @InjectMocks private PlayerController playerController;

  @Test
  @DisplayName("Should return player stats from service")
  void shouldReturnPlayerStatsFromService() {
    Map<String, Object> payload = Map.of("totalPlayers", 147);
    when(playerService.getPlayersStats()).thenReturn(payload);

    ResponseEntity<?> response = playerController.getPlayersStats();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(payload);
    verify(playerService).getPlayersStats();
  }
}
