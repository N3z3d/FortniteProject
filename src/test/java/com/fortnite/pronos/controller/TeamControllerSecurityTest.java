package com.fortnite.pronos.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.fortnite.pronos.application.usecase.TeamQueryUseCase;
import com.fortnite.pronos.dto.team.TeamDto;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.TeamService;
import com.fortnite.pronos.service.UserResolver;

@ExtendWith(MockitoExtension.class)
class TeamControllerSecurityTest {

  @Mock private TeamService teamService;
  @Mock private TeamQueryUseCase teamQueryUseCase;
  @Mock private UserResolver userResolver;

  @InjectMocks private TeamController teamController;

  private User authenticatedUser;
  private HttpServletRequest httpRequest;

  @BeforeEach
  void setUp() {
    authenticatedUser = new User();
    authenticatedUser.setId(UUID.randomUUID());
    authenticatedUser.setUsername("owner");
    httpRequest = new MockHttpServletRequest();
  }

  @Test
  void shouldReturnUnauthorizedWhenUserCannotBeResolvedForTeamCreation() {
    TeamController.CreateTeamRequest request =
        new TeamController.CreateTeamRequest(authenticatedUser.getId(), "Team", 2026);
    when(userResolver.resolve(null, httpRequest)).thenReturn(null);

    ResponseEntity<TeamDto> response = teamController.createTeam(request, null, httpRequest);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verify(teamService, never()).createTeam(any(), any(), anyInt());
  }

  @Test
  void shouldReturnForbiddenWhenCreateTeamRequestUserIdDoesNotMatchAuthenticatedUser() {
    TeamController.CreateTeamRequest request =
        new TeamController.CreateTeamRequest(UUID.randomUUID(), "Team", 2026);
    when(userResolver.resolve(null, httpRequest)).thenReturn(authenticatedUser);

    ResponseEntity<TeamDto> response = teamController.createTeam(request, null, httpRequest);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    verify(teamService, never()).createTeam(any(), any(), anyInt());
  }

  @Test
  void shouldUseAuthenticatedUserForTeamCreationWhenPayloadUserIdIsMissing() {
    TeamController.CreateTeamRequest request =
        new TeamController.CreateTeamRequest(null, "Team", 2026);
    TeamDto createdTeam = new TeamDto();
    createdTeam.setId(UUID.randomUUID());

    when(userResolver.resolve(null, httpRequest)).thenReturn(authenticatedUser);
    when(teamService.createTeam(eq(authenticatedUser.getId()), eq("Team"), eq(2026)))
        .thenReturn(createdTeam);

    ResponseEntity<TeamDto> response = teamController.createTeam(request, null, httpRequest);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(createdTeam.getId(), response.getBody().getId());
    verify(teamService).createTeam(authenticatedUser.getId(), "Team", 2026);
  }

  @Test
  void shouldReturnForbiddenWhenAddPlayerUserIdDoesNotMatchAuthenticatedUser() {
    UUID targetUserId = UUID.randomUUID();
    TeamController.AddPlayerRequest addPlayerRequest =
        new TeamController.AddPlayerRequest(UUID.randomUUID(), 1);
    when(userResolver.resolve(null, httpRequest)).thenReturn(authenticatedUser);

    ResponseEntity<TeamDto> response =
        teamController.addPlayerToTeam(targetUserId, 2026, addPlayerRequest, null, httpRequest);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    verify(teamService, never()).addPlayerToTeam(any(), any(), anyInt(), anyInt());
  }

  @Test
  void shouldReturnForbiddenWhenRemovePlayerUserIdDoesNotMatchAuthenticatedUser() {
    UUID targetUserId = UUID.randomUUID();
    TeamController.RemovePlayerRequest removePlayerRequest =
        new TeamController.RemovePlayerRequest(UUID.randomUUID());
    when(userResolver.resolve(null, httpRequest)).thenReturn(authenticatedUser);

    ResponseEntity<TeamDto> response =
        teamController.removePlayerFromTeam(
            targetUserId, 2026, removePlayerRequest, null, httpRequest);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    verify(teamService, never()).removePlayerFromTeam(any(), any(), anyInt());
  }

  @Test
  void shouldReturnForbiddenWhenRemovePlayerIsRequestedEvenForOwner() {
    TeamController.RemovePlayerRequest removePlayerRequest =
        new TeamController.RemovePlayerRequest(UUID.randomUUID());
    when(userResolver.resolve(null, httpRequest)).thenReturn(authenticatedUser);

    ResponseEntity<TeamDto> response =
        teamController.removePlayerFromTeam(
            authenticatedUser.getId(), 2026, removePlayerRequest, null, httpRequest);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    verify(teamService, never()).removePlayerFromTeam(any(), any(), anyInt());
  }

  @Test
  void shouldReturnForbiddenWhenPlayerChangesUserIdDoesNotMatchAuthenticatedUser() {
    UUID targetUserId = UUID.randomUUID();
    Map<UUID, UUID> changes = Map.of(UUID.randomUUID(), UUID.randomUUID());
    when(userResolver.resolve(null, httpRequest)).thenReturn(authenticatedUser);

    ResponseEntity<TeamDto> response =
        teamController.makePlayerChanges(targetUserId, 2026, changes, null, httpRequest);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    verify(teamService, never()).makePlayerChanges(any(), any(), anyInt());
  }

  @Test
  void shouldReturnForbiddenWhenPlayerChangesAreRequestedEvenForOwner() {
    Map<UUID, UUID> changes = Map.of(UUID.randomUUID(), UUID.randomUUID());
    when(userResolver.resolve(null, httpRequest)).thenReturn(authenticatedUser);

    ResponseEntity<TeamDto> response =
        teamController.makePlayerChanges(
            authenticatedUser.getId(), 2026, changes, null, httpRequest);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    verify(teamService, never()).makePlayerChanges(any(), any(), anyInt());
  }
}
