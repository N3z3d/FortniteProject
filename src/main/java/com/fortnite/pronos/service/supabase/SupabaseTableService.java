package com.fortnite.pronos.service.supabase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.service.supabase.dto.SupabaseGameParticipantRowDto;
import com.fortnite.pronos.service.supabase.dto.SupabaseGameRowDto;
import com.fortnite.pronos.service.supabase.dto.SupabasePlayerAssignmentRowDto;
import com.fortnite.pronos.service.supabase.dto.SupabasePlayerRowDto;
import com.fortnite.pronos.service.supabase.dto.SupabaseScoreRowDto;
import com.fortnite.pronos.service.supabase.dto.SupabaseTeamPlayerRowDto;
import com.fortnite.pronos.service.supabase.dto.SupabaseTeamRowDto;
import com.fortnite.pronos.service.supabase.dto.SupabaseUserRowDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupabaseTableService {

  private static final String ASSIGNMENTS_VIEW = "player_assignments";
  private static final String PLAYERS_TABLE = "players";
  private static final String SCORES_TABLE = "scores";
  private static final String USERS_TABLE = "users";
  private static final String TEAMS_TABLE = "teams";
  private static final String TEAM_PLAYERS_TABLE = "team_players";
  private static final String GAMES_TABLE = "games";
  private static final String GAME_PARTICIPANTS_TABLE = "game_participants";

  private final SupabaseRestService restClient;

  public List<SupabasePlayerAssignmentRowDto> fetchPlayerAssignments() {
    return restClient.fetchAll(ASSIGNMENTS_VIEW, SupabasePlayerAssignmentRowDto[].class);
  }

  public List<SupabasePlayerRowDto> fetchPlayers() {
    return restClient.fetchAll(PLAYERS_TABLE, SupabasePlayerRowDto[].class);
  }

  public List<SupabaseScoreRowDto> fetchScores() {
    return restClient.fetchAll(SCORES_TABLE, SupabaseScoreRowDto[].class);
  }

  public List<SupabaseUserRowDto> fetchUsers() {
    return restClient.fetchAll(USERS_TABLE, SupabaseUserRowDto[].class);
  }

  public List<SupabaseTeamRowDto> fetchTeams() {
    return restClient.fetchAll(TEAMS_TABLE, SupabaseTeamRowDto[].class);
  }

  public List<SupabaseTeamPlayerRowDto> fetchTeamPlayers() {
    return restClient.fetchAll(TEAM_PLAYERS_TABLE, SupabaseTeamPlayerRowDto[].class);
  }

  public List<SupabaseGameRowDto> fetchGames() {
    return restClient.fetchAll(GAMES_TABLE, SupabaseGameRowDto[].class);
  }

  public List<SupabaseGameParticipantRowDto> fetchGameParticipants() {
    return restClient.fetchAll(GAME_PARTICIPANTS_TABLE, SupabaseGameParticipantRowDto[].class);
  }
}
