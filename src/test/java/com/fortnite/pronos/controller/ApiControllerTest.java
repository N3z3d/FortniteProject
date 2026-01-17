package com.fortnite.pronos.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.PlayerService;
import com.fortnite.pronos.service.TeamQueryService;
import com.fortnite.pronos.service.UserService;

@ExtendWith(MockitoExtension.class)
public class ApiControllerTest {

  @Mock private UserService userService;

  @Mock private TeamQueryService teamQueryService;

  @Mock private PlayerService playerService;

  @Mock private Authentication authentication;

  @Mock private HttpServletRequest request;

  @InjectMocks private ApiController apiController;

  private User testUser;
  private Team testTeam;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(UUID.randomUUID());
    testUser.setUsername("Teddy");
    testUser.setEmail("teddy@example.com");
    testUser.setCurrentSeason(2025);

    testTeam = new Team();
    testTeam.setId(UUID.randomUUID());
    testTeam.setName("Team Teddy");
    testTeam.setOwner(testUser);
    testTeam.setSeason(2025);
    testTeam.setPlayers(new ArrayList<>());
  }

  @Test
  void testGetTradeFormData_WithUserParameter_Success() {
    // Given
    String username = "Teddy";
    when(userService.findUserByEmailOrUsername(username)).thenReturn(Optional.of(testUser));
    when(teamQueryService.findTeamByOwnerAndSeason(testUser, 2025))
        .thenReturn(Optional.of(testTeam));
    when(teamQueryService.findTeamsBySeasonWithFetch(2025)).thenReturn(Arrays.asList(testTeam));
    when(playerService.findAllPlayers()).thenReturn(new ArrayList<>());

    // When
    ResponseEntity<?> response = apiController.getTradeFormData(username, request, null);

    // Then
    assertEquals(200, response.getStatusCodeValue());
    assertTrue(response.getBody() instanceof Map);

    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertNotNull(body.get("myTeam"));
    assertNotNull(body.get("myPlayers"));
    assertNotNull(body.get("otherTeams"));
    assertNotNull(body.get("allPlayers"));

    verify(userService).findUserByEmailOrUsername(username);
    verify(teamQueryService).findTeamByOwnerAndSeason(testUser, 2025);
  }

  @Test
  void testGetTradeFormData_WithUserParameter_CaseInsensitive() {
    // Given
    String username = "TEDDY"; // Majuscules
    when(userService.findUserByEmailOrUsername(username)).thenReturn(Optional.of(testUser));
    when(teamQueryService.findTeamByOwnerAndSeason(testUser, 2025))
        .thenReturn(Optional.of(testTeam));
    when(teamQueryService.findTeamsBySeasonWithFetch(2025)).thenReturn(Arrays.asList(testTeam));
    when(playerService.findAllPlayers()).thenReturn(new ArrayList<>());

    // When
    ResponseEntity<?> response = apiController.getTradeFormData(username, request, null);

    // Then
    assertEquals(200, response.getStatusCodeValue());
    verify(userService).findUserByEmailOrUsername("TEDDY");
  }

  @Test
  void testGetTradeFormData_UserNotFound_Error() {
    // Given
    String username = "NonExistentUser";
    when(userService.findUserByEmailOrUsername(username)).thenReturn(Optional.empty());

    // When
    ResponseEntity<?> response = apiController.getTradeFormData(username, request, null);

    // Then
    assertEquals(400, response.getStatusCodeValue());
    assertTrue(response.getBody() instanceof Map);

    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertTrue(body.containsKey("error"));
  }

  @Test
  void testGetTradeFormData_NoTeamFound_SearchByOwner() {
    // Given
    String username = "Teddy";
    Team otherTeam = new Team();
    otherTeam.setId(UUID.randomUUID());
    otherTeam.setName("Team Other");
    otherTeam.setOwner(testUser);
    otherTeam.setSeason(2025);
    otherTeam.setPlayers(new ArrayList<>());

    when(userService.findUserByEmailOrUsername(username)).thenReturn(Optional.of(testUser));
    when(teamQueryService.findTeamByOwnerAndSeason(testUser, 2025))
        .thenReturn(Optional.empty()); // Pas d'équipe trouvée directement
    when(teamQueryService.findTeamsBySeasonWithFetch(2025))
        .thenReturn(Arrays.asList(otherTeam)); // Mais trouvée par propriétaire
    when(playerService.findAllPlayers()).thenReturn(new ArrayList<>());

    // When
    ResponseEntity<?> response = apiController.getTradeFormData(username, request, null);

    // Then
    assertEquals(200, response.getStatusCodeValue());
    verify(teamQueryService, atLeast(1)).findTeamsBySeasonWithFetch(2025);
  }

  @Test
  void testGetTradeFormData_WithAuthentication() {
    // Given
    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getName()).thenReturn("Teddy");
    when(userService.findUserByEmailOrUsername("Teddy")).thenReturn(Optional.of(testUser));
    when(teamQueryService.findTeamByOwnerAndSeason(testUser, 2025))
        .thenReturn(Optional.of(testTeam));
    when(teamQueryService.findTeamsBySeasonWithFetch(2025)).thenReturn(Arrays.asList(testTeam));
    when(playerService.findAllPlayers()).thenReturn(new ArrayList<>());

    // When
    ResponseEntity<?> response = apiController.getTradeFormData(null, request, authentication);

    // Then
    assertEquals(200, response.getStatusCodeValue());
    verify(authentication, atLeast(1)).getName();
    verify(userService).findUserByEmailOrUsername("Teddy");
  }

  @Test
  void testGetTradeFormData_DefaultFallback() {
    // Given
    User defaultUser = new User();
    defaultUser.setUsername("Thibaut");
    defaultUser.setCurrentSeason(2025);

    Team defaultTeam = new Team();
    defaultTeam.setName("Team Thibaut");
    defaultTeam.setOwner(defaultUser);
    defaultTeam.setSeason(2025);
    defaultTeam.setPlayers(new ArrayList<>());

    when(userService.findUserByEmailOrUsername("Thibaut")).thenReturn(Optional.of(defaultUser));
    when(teamQueryService.findTeamByOwnerAndSeason(defaultUser, 2025))
        .thenReturn(Optional.of(defaultTeam));
    when(teamQueryService.findTeamsBySeasonWithFetch(2025)).thenReturn(Arrays.asList(defaultTeam));
    when(playerService.findAllPlayers()).thenReturn(new ArrayList<>());

    // When (aucun paramètre user, aucune auth)
    ResponseEntity<?> response = apiController.getTradeFormData(null, request, null);

    // Then
    assertEquals(200, response.getStatusCodeValue());
    verify(userService).findUserByEmailOrUsername("Thibaut");
  }
}
