package com.fortnite.pronos.application.usecase;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fortnite.pronos.dto.team.TeamDto;

/** Application use case for Team query operations. Defines the public API for querying teams. */
public interface TeamQueryUseCase {

  TeamDto getTeam(UUID userId, int season);

  TeamDto getTeamById(UUID teamId);

  List<TeamDto> getAllTeams(int season);

  List<TeamDto> getTeamsByUsernameAndYear(String username, int year);

  TeamDto getTeamByPlayer(UUID playerId, int season);

  Map<UUID, TeamDto> getTeamsByPlayers(List<UUID> playerIds, int season);

  List<TeamDto> getTeamsBySeason(int season);

  List<TeamDto> getParticipantTeams(int season);

  TeamDto getTeamByUserAndSeason(UUID userId, int season);

  List<TeamDto> getTeamsByGame(UUID gameId);
}
