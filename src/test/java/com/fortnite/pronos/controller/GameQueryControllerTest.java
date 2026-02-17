package com.fortnite.pronos.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.fortnite.pronos.application.usecase.GameQueryUseCase;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.UserResolver;

@ExtendWith(MockitoExtension.class)
class GameQueryControllerTest {

  @Mock private GameQueryUseCase gameQueryUseCase;

  @Mock private UserResolver userResolver;

  @InjectMocks private GameQueryController gameQueryController;

  private User user;
  private HttpServletRequest request;

  @BeforeEach
  void setUp() {
    user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername("tester");
    request = new MockHttpServletRequest();
  }

  @Test
  void shouldReturnUnauthorizedWhenGettingMyGamesWithUnresolvedUser() {
    when(userResolver.resolve(null, request)).thenReturn(null);

    ResponseEntity<List<GameDto>> response = gameQueryController.getUserGames(null, request);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void shouldReturnGamesByUserWhenUsernameFilterIsProvided() {
    when(userResolver.resolve("tester", request)).thenReturn(user);
    when(gameQueryUseCase.getGamesByUser(user.getId())).thenReturn(List.of(new GameDto()));

    ResponseEntity<List<GameDto>> response = gameQueryController.getAllGames("tester", request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
  }

  @Test
  void shouldReturnAllGamesWhenUsernameFilterIsBlank() {
    when(userResolver.resolve("", request)).thenReturn(user);
    when(gameQueryUseCase.getAllGames()).thenReturn(List.of(new GameDto(), new GameDto()));

    ResponseEntity<List<GameDto>> response = gameQueryController.getAllGames("", request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(2, response.getBody().size());
  }

  @Test
  void shouldReturnAvailableGamesWhenUserResolved() {
    when(userResolver.resolve(null, request)).thenReturn(user);
    when(gameQueryUseCase.getAvailableGames()).thenReturn(List.of(new GameDto()));

    ResponseEntity<List<GameDto>> response = gameQueryController.getAvailableGames(null, request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
  }

  @Test
  void shouldMapParticipantsFromGameDto() {
    UUID gameId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();
    GameDto gameDto = new GameDto();
    gameDto.setParticipants(Map.of(participantId, "thibaut"));
    when(gameQueryUseCase.getGameByIdOrThrow(gameId)).thenReturn(gameDto);

    ResponseEntity<List<Map<String, Object>>> response =
        gameQueryController.getGameParticipants(gameId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().size());
    assertEquals(participantId, response.getBody().getFirst().get("userId"));
    assertEquals("thibaut", response.getBody().getFirst().get("username"));
  }
}
