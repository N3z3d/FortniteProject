package com.fortnite.pronos.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fortnite.pronos.application.usecase.GameQueryUseCase;
import com.fortnite.pronos.config.GameExceptionHandler;
import com.fortnite.pronos.config.GlobalExceptionHandler;
import com.fortnite.pronos.core.error.ErrorCode;
import com.fortnite.pronos.core.error.FortnitePronosException;
import com.fortnite.pronos.core.usecase.CreateGameUseCase;
import com.fortnite.pronos.dto.CreateGameRequest;
import com.fortnite.pronos.dto.DraftDto;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.dto.JoinGameRequest;
import com.fortnite.pronos.dto.JoinGameWithCodeRequest;
import com.fortnite.pronos.dto.RenameGameRequest;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.GameService;
import com.fortnite.pronos.service.InvitationCodeAttemptGuard;
import com.fortnite.pronos.service.UserResolver;
import com.fortnite.pronos.service.ValidationService;
import com.fortnite.pronos.service.admin.ErrorJournalService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings({"java:S5778"})
class GameControllerSimpleTest {

  @Mock private GameService gameService;

  @Mock private GameQueryUseCase gameQueryUseCase;

  @Mock private ValidationService validationService;

  @Mock private UserResolver userResolver;

  @Mock private CreateGameUseCase createGameUseCase;

  @Mock private InvitationCodeAttemptGuard invitationCodeAttemptGuard;

  @Mock private ErrorJournalService errorJournalService;

  private GameController gameController;

  private User user;
  private HttpServletRequest request;
  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername("tester");
    request = new MockHttpServletRequest();
    objectMapper = new ObjectMapper();
    GameInvitationCodeRequestHandler invitationCodeRequestHandler =
        new GameInvitationCodeRequestHandler(
            gameService,
            gameQueryUseCase,
            validationService,
            userResolver,
            invitationCodeAttemptGuard);
    gameController =
        new GameController(
            gameService,
            gameQueryUseCase,
            validationService,
            userResolver,
            createGameUseCase,
            invitationCodeRequestHandler);
    mockMvc =
        MockMvcBuilders.standaloneSetup(gameController)
            .setControllerAdvice(
                new GameExceptionHandler(errorJournalService),
                new GlobalExceptionHandler(errorJournalService))
            .build();
  }

  @Test
  void shouldCreateController() {
    assertNotNull(gameController);
  }

  @Test
  void shouldIgnoreClientCreatorIdAndUseResolvedUserInCreateGame() {
    UUID spoofedCreatorId = UUID.randomUUID();
    CreateGameRequest createRequest = new CreateGameRequest();
    createRequest.setName("Game test");
    createRequest.setMaxParticipants(8);
    createRequest.setRegionRules(Map.of(Player.Region.EU, 8));
    createRequest.setCreatorId(spoofedCreatorId);

    GameDto createdGame = new GameDto();
    createdGame.setId(UUID.randomUUID());
    createdGame.setCreatorId(user.getId());

    when(userResolver.resolve(null, request)).thenReturn(user);
    when(createGameUseCase.execute(eq(user.getId()), any(CreateGameRequest.class)))
        .thenReturn(createdGame);

    ResponseEntity<GameDto> response = gameController.createGame(createRequest, null, request);

    org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
    org.junit.jupiter.api.Assertions.assertEquals(user.getId(), createRequest.getCreatorId());
    verify(createGameUseCase).execute(eq(user.getId()), any(CreateGameRequest.class));
  }

  @Test
  void shouldReturnBadRequestWhenCreateGamePayloadOmitsRegionRules() throws Exception {
    when(userResolver.resolve(any(), any())).thenReturn(user);

    mockMvc
        .perform(
            post("/api/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("name", "Game test", "maxParticipants", 5))))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.regionRules")
                .value(Matchers.containsString("regionRules")));

    verify(createGameUseCase, never()).execute(any(), any(CreateGameRequest.class));
  }

  @Test
  void shouldReturnBadRequestWhenCreateGamePayloadContainsEmptyRegionRules() throws Exception {
    when(userResolver.resolve(any(), any())).thenReturn(user);

    mockMvc
        .perform(
            post("/api/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "name", "Game test", "maxParticipants", 5, "regionRules", Map.of()))))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.regionRules")
                .value(Matchers.containsString("regionRules")));

    verify(createGameUseCase, never()).execute(any(), any(CreateGameRequest.class));
  }

  @Test
  void shouldIgnoreClientUserIdAndUseResolvedUserInJoinGame() {
    UUID gameId = UUID.randomUUID();
    UUID spoofedUserId = UUID.randomUUID();
    JoinGameRequest joinRequest = new JoinGameRequest();
    joinRequest.setGameId(gameId);
    joinRequest.setUserId(spoofedUserId);

    when(userResolver.resolve(null, request)).thenReturn(user);
    when(gameService.joinGame(eq(user.getId()), any(JoinGameRequest.class))).thenReturn(true);

    ResponseEntity<Map<String, Object>> response =
        gameController.joinGame(joinRequest, null, request);

    org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    org.junit.jupiter.api.Assertions.assertEquals(user.getId(), joinRequest.getUserId());
    verify(validationService)
        .validateJoinGameRequest(
            argThat(
                requestArg -> requestArg != null && user.getId().equals(requestArg.getUserId())));
    verify(gameService).joinGame(eq(user.getId()), any(JoinGameRequest.class));
  }

  @Test
  void shouldReturnUnauthorizedInJoinGameEvenWhenClientUserIdProvidedIfResolverFails() {
    JoinGameRequest joinRequest = new JoinGameRequest();
    joinRequest.setGameId(UUID.randomUUID());
    joinRequest.setUserId(UUID.randomUUID());

    when(userResolver.resolve(null, request)).thenReturn(null);

    ResponseEntity<Map<String, Object>> response =
        gameController.joinGame(joinRequest, null, request);

    org.junit.jupiter.api.Assertions.assertEquals(
        HttpStatus.UNAUTHORIZED, response.getStatusCode());
    org.junit.jupiter.api.Assertions.assertEquals(
        "Utilisateur requis", response.getBody().get("error"));
  }

  @Test
  void shouldJoinGameWithCodeWhenPayloadIsValid() {
    UUID gameId = UUID.randomUUID();
    GameDto gameDto = new GameDto();
    gameDto.setId(gameId);
    gameDto.setName("Test game");
    gameDto.setInvitationCode("CODE123");
    gameDto.setInvitationCodeExpiresAt(java.time.LocalDateTime.now().plusHours(1));
    GameDto updatedGameDto = new GameDto();
    updatedGameDto.setId(gameId);
    updatedGameDto.setName("Test game");

    when(userResolver.resolve(null, request)).thenReturn(user);
    when(gameService.getGameByInvitationCode("CODE123")).thenReturn(Optional.of(gameDto));
    when(gameService.joinGame(eq(user.getId()), any(JoinGameRequest.class))).thenReturn(true);
    when(gameService.getGameByIdOrThrow(gameId)).thenReturn(updatedGameDto);

    ResponseEntity<GameDto> response =
        gameController.joinGameWithCode(
            new JoinGameWithCodeRequest(" code123 ", null), null, request);

    assertNotNull(response.getBody());
    org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    org.junit.jupiter.api.Assertions.assertEquals(gameId, response.getBody().getId());
    assertNull(response.getBody().getInvitationCode());
    assertNull(response.getBody().getInvitationCodeExpiresAt());

    verify(gameService).getGameByInvitationCode("CODE123");
    verify(invitationCodeAttemptGuard)
        .registerAttemptOrThrow(user.getId(), request.getRemoteAddr());
    verify(validationService).validateJoinGameRequest(any(JoinGameRequest.class));
    verify(gameService).joinGame(eq(user.getId()), any(JoinGameRequest.class));
    verify(gameService).getGameByIdOrThrow(gameId);
  }

  @Test
  void shouldNormalizeInvitationCodeWithLocaleRoot() {
    UUID gameId = UUID.randomUUID();
    GameDto gameDto = new GameDto();
    gameDto.setId(gameId);

    Locale initialLocale = Locale.getDefault();
    try {
      Locale.setDefault(Locale.forLanguageTag("tr-TR"));
      when(userResolver.resolve(null, request)).thenReturn(user);
      when(gameService.getGameByInvitationCode("I-TEST")).thenReturn(Optional.of(gameDto));
      when(gameService.joinGame(eq(user.getId()), any(JoinGameRequest.class))).thenReturn(true);

      ResponseEntity<GameDto> response =
          gameController.joinGameWithCode(
              new JoinGameWithCodeRequest("i-test", null), null, request);

      org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
      verify(gameService).getGameByInvitationCode("I-TEST");
    } finally {
      Locale.setDefault(initialLocale);
    }
  }

  @Test
  void shouldReturnBadRequestWhenInvitationCodeMissing() {
    when(userResolver.resolve(null, request)).thenReturn(user);

    ResponseEntity<GameDto> response =
        gameController.joinGameWithCode(new JoinGameWithCodeRequest(null, null), null, request);

    org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNull(response.getBody());
  }

  @Test
  void shouldReturnUnauthorizedWhenUserMissing() {
    when(userResolver.resolve(null, request)).thenReturn(null);

    ResponseEntity<GameDto> response =
        gameController.joinGameWithCode(
            new JoinGameWithCodeRequest("CODE123", null), null, request);

    org.junit.jupiter.api.Assertions.assertEquals(
        HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertNull(response.getBody());
  }

  @Test
  void shouldReturnBadRequestWhenJoinGameWithCodePayloadIsMissing() {
    when(userResolver.resolve(null, request)).thenReturn(user);

    ResponseEntity<GameDto> response = gameController.joinGameWithCode(null, null, request);

    org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNull(response.getBody());
  }

  @Test
  void shouldThrowRateLimitExceptionWhenTooManyJoinCodeAttempts() {
    when(userResolver.resolve(null, request)).thenReturn(user);
    doThrow(new FortnitePronosException(ErrorCode.SYS_004, "Rate limit exceeded"))
        .when(invitationCodeAttemptGuard)
        .registerAttemptOrThrow(user.getId(), request.getRemoteAddr());

    FortnitePronosException exception =
        assertThrows(
            FortnitePronosException.class,
            () ->
                gameController.joinGameWithCode(
                    new JoinGameWithCodeRequest("CODE123", null), null, request));

    org.junit.jupiter.api.Assertions.assertEquals(ErrorCode.SYS_004, exception.getErrorCode());
  }

  @Test
  void shouldRenameGameWhenUserIsCreatorAndPayloadIsValid() {
    UUID gameId = UUID.randomUUID();
    GameDto gameDto = new GameDto();
    gameDto.setId(gameId);
    gameDto.setCreatorId(user.getId());

    GameDto updatedGame = new GameDto();
    updatedGame.setId(gameId);
    updatedGame.setName("Renamed game");

    when(userResolver.resolve(null, request)).thenReturn(user);
    when(gameQueryUseCase.getGameByIdOrThrow(gameId)).thenReturn(gameDto);
    when(gameService.renameGame(gameId, "Renamed game")).thenReturn(updatedGame);

    ResponseEntity<GameDto> response =
        gameController.renameGame(gameId, new RenameGameRequest("  Renamed game "), null, request);

    org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    org.junit.jupiter.api.Assertions.assertEquals("Renamed game", response.getBody().getName());
    verify(gameService).renameGame(gameId, "Renamed game");
  }

  @Test
  void shouldReturnBadRequestWhenRenamePayloadIsBlank() {
    UUID gameId = UUID.randomUUID();
    GameDto gameDto = new GameDto();
    gameDto.setId(gameId);
    gameDto.setCreatorId(user.getId());

    when(userResolver.resolve(null, request)).thenReturn(user);
    when(gameQueryUseCase.getGameByIdOrThrow(gameId)).thenReturn(gameDto);

    ResponseEntity<GameDto> response =
        gameController.renameGame(gameId, new RenameGameRequest("   "), null, request);

    org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNull(response.getBody());
  }

  @Test
  void shouldLeaveGameWhenUserResolved() {
    UUID gameId = UUID.randomUUID();
    when(userResolver.resolve(null, request)).thenReturn(user);

    ResponseEntity<Map<String, Object>> response = gameController.leaveGame(gameId, null, request);

    org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    org.junit.jupiter.api.Assertions.assertEquals(true, response.getBody().get("success"));
    verify(gameService).leaveGame(user.getId(), gameId);
  }

  @Test
  void shouldReturnUnauthorizedOnLeaveWhenUserMissing() {
    UUID gameId = UUID.randomUUID();
    when(userResolver.resolve(null, request)).thenReturn(null);

    ResponseEntity<Map<String, Object>> response = gameController.leaveGame(gameId, null, request);

    org.junit.jupiter.api.Assertions.assertEquals(
        HttpStatus.UNAUTHORIZED, response.getStatusCode());
    org.junit.jupiter.api.Assertions.assertEquals(
        "Utilisateur requis", response.getBody().get("error"));
  }

  @Test
  void shouldStartDraftWhenCreatorMatchesResolvedUser() {
    UUID gameId = UUID.randomUUID();
    GameDto gameDto = new GameDto();
    gameDto.setId(gameId);
    gameDto.setCreatorId(user.getId());

    DraftDto draftDto = new DraftDto();
    draftDto.setId(UUID.randomUUID());

    when(userResolver.resolve(null, request)).thenReturn(user);
    when(gameQueryUseCase.getGameByIdOrThrow(gameId)).thenReturn(gameDto);
    when(gameService.startDraft(gameId, user.getId())).thenReturn(draftDto);

    ResponseEntity<Map<String, Object>> response = gameController.startDraft(gameId, null, request);

    org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    org.junit.jupiter.api.Assertions.assertEquals(true, response.getBody().get("success"));
    verify(gameService).startDraft(gameId, user.getId());
  }

  @Test
  void shouldReturnConflictWhenStartDraftGameCreatorIsMissing() {
    UUID gameId = UUID.randomUUID();
    GameDto gameDto = new GameDto();
    gameDto.setId(gameId);
    gameDto.setCreatorId(null);

    when(userResolver.resolve(null, request)).thenReturn(user);
    when(gameQueryUseCase.getGameByIdOrThrow(gameId)).thenReturn(gameDto);

    ResponseEntity<Map<String, Object>> response = gameController.startDraft(gameId, null, request);

    org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    org.junit.jupiter.api.Assertions.assertEquals(
        "Createur de la partie introuvable", response.getBody().get("error"));
  }

  @Test
  void shouldRegenerateInvitationCodeWhenCreatorIsResolved() {
    UUID gameId = UUID.randomUUID();
    GameDto gameDto = new GameDto();
    gameDto.setId(gameId);
    gameDto.setCreatorId(user.getId());

    GameDto updatedGame = new GameDto();
    updatedGame.setId(gameId);
    updatedGame.setInvitationCode("NEWCODE1");

    when(userResolver.resolve(null, request)).thenReturn(user);
    when(gameQueryUseCase.getGameByIdOrThrow(gameId)).thenReturn(gameDto);
    when(gameService.regenerateInvitationCode(gameId, "24h")).thenReturn(updatedGame);

    ResponseEntity<GameDto> response =
        gameController.regenerateInvitationCode(gameId, null, request, "24h");

    org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    org.junit.jupiter.api.Assertions.assertEquals(
        "NEWCODE1", response.getBody().getInvitationCode());
  }

  @Test
  void shouldReturnUnauthorizedOnRegenerateWhenUserMissing() {
    UUID gameId = UUID.randomUUID();
    when(userResolver.resolve(null, request)).thenReturn(null);

    ResponseEntity<GameDto> response =
        gameController.regenerateInvitationCode(gameId, null, request, "permanent");

    org.junit.jupiter.api.Assertions.assertEquals(
        HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertNull(response.getBody());
  }

  @Test
  void shouldDeleteInvitationCodeWhenCreatorIsResolved() {
    UUID gameId = UUID.randomUUID();
    GameDto gameDto = new GameDto();
    gameDto.setId(gameId);
    gameDto.setCreatorId(user.getId());

    GameDto updatedGame = new GameDto();
    updatedGame.setId(gameId);
    updatedGame.setInvitationCode(null);

    when(userResolver.resolve(null, request)).thenReturn(user);
    when(gameQueryUseCase.getGameByIdOrThrow(gameId)).thenReturn(gameDto);
    when(gameService.deleteInvitationCode(gameId)).thenReturn(updatedGame);

    ResponseEntity<GameDto> response = gameController.deleteInvitationCode(gameId, null, request);

    org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    org.junit.jupiter.api.Assertions.assertNull(response.getBody().getInvitationCode());
    verify(gameService).deleteInvitationCode(gameId);
  }

  @Test
  void shouldReturnUnauthorizedOnDeleteInvitationCodeWhenUserMissing() {
    UUID gameId = UUID.randomUUID();
    when(userResolver.resolve(null, request)).thenReturn(null);

    ResponseEntity<GameDto> response = gameController.deleteInvitationCode(gameId, null, request);

    org.junit.jupiter.api.Assertions.assertEquals(
        HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertNull(response.getBody());
  }

  @Test
  void shouldReturnForbiddenOnDeleteInvitationCodeWhenUserIsNotCreator() {
    UUID gameId = UUID.randomUUID();
    User anotherUser = new User();
    anotherUser.setId(UUID.randomUUID());
    anotherUser.setUsername("other");

    GameDto gameDto = new GameDto();
    gameDto.setId(gameId);
    gameDto.setCreatorId(user.getId());

    when(userResolver.resolve(null, request)).thenReturn(anotherUser);
    when(gameQueryUseCase.getGameByIdOrThrow(gameId)).thenReturn(gameDto);

    ResponseEntity<GameDto> response = gameController.deleteInvitationCode(gameId, null, request);

    org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertNull(response.getBody());
  }

  @Test
  void archiveGame_whenCreator_returns200() {
    UUID gameId = UUID.randomUUID();
    GameDto gameDto = new GameDto();
    gameDto.setId(gameId);
    gameDto.setCreatorId(user.getId());

    when(userResolver.resolve(null, request)).thenReturn(user);
    when(gameQueryUseCase.getGameByIdOrThrow(gameId)).thenReturn(gameDto);

    ResponseEntity<Map<String, Object>> response =
        gameController.archiveGame(gameId, null, request);

    org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(gameService).archiveGame(gameId);
  }

  @Test
  void archiveGame_whenNotAuthenticated_returns401() {
    UUID gameId = UUID.randomUUID();
    when(userResolver.resolve(null, request)).thenReturn(null);

    ResponseEntity<Map<String, Object>> response =
        gameController.archiveGame(gameId, null, request);

    org.junit.jupiter.api.Assertions.assertEquals(
        HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void archiveGame_whenNotCreator_returns403() {
    UUID gameId = UUID.randomUUID();
    User otherUser = new User();
    otherUser.setId(UUID.randomUUID());
    GameDto gameDto = new GameDto();
    gameDto.setId(gameId);
    gameDto.setCreatorId(user.getId());

    when(userResolver.resolve(null, request)).thenReturn(otherUser);
    when(gameQueryUseCase.getGameByIdOrThrow(gameId)).thenReturn(gameDto);

    ResponseEntity<Map<String, Object>> response =
        gameController.archiveGame(gameId, null, request);

    org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void archiveGame_whenGameNotFound_throwsGameNotFoundException() {
    UUID gameId = UUID.randomUUID();
    when(userResolver.resolve(null, request)).thenReturn(user);
    when(gameQueryUseCase.getGameByIdOrThrow(gameId))
        .thenThrow(new GameNotFoundException("Game not found: " + gameId));

    assertThrows(
        GameNotFoundException.class, () -> gameController.archiveGame(gameId, null, request));
  }

  @Test
  void shouldReturnForbiddenOnRegenerateWhenUserIsNotCreator() {
    UUID gameId = UUID.randomUUID();
    User anotherUser = new User();
    anotherUser.setId(UUID.randomUUID());
    anotherUser.setUsername("other");

    GameDto gameDto = new GameDto();
    gameDto.setId(gameId);
    gameDto.setCreatorId(user.getId());

    when(userResolver.resolve(null, request)).thenReturn(anotherUser);
    when(gameQueryUseCase.getGameByIdOrThrow(gameId)).thenReturn(gameDto);

    ResponseEntity<GameDto> response =
        gameController.regenerateInvitationCode(gameId, null, request, "permanent");

    org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertNull(response.getBody());
  }
}
