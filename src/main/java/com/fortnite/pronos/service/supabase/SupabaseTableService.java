package com.fortnite.pronos.service.supabase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.service.supabase.dto.SupabaseGameParticipantRow;
import com.fortnite.pronos.service.supabase.dto.SupabaseGameRow;
import com.fortnite.pronos.service.supabase.dto.SupabasePlayerAssignmentRow;
import com.fortnite.pronos.service.supabase.dto.SupabasePlayerRow;
import com.fortnite.pronos.service.supabase.dto.SupabaseScoreRow;
import com.fortnite.pronos.service.supabase.dto.SupabaseTeamPlayerRow;
import com.fortnite.pronos.service.supabase.dto.SupabaseTeamRow;
import com.fortnite.pronos.service.supabase.dto.SupabaseUserRow;

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

  private final SupabaseRestClient restClient;

  public List<SupabasePlayerAssignmentRow> fetchPlayerAssignments() {
    return restClient.fetchAll(ASSIGNMENTS_VIEW, SupabasePlayerAssignmentRow[].class);
  }

  public List<SupabasePlayerRow> fetchPlayers() {
    return restClient.fetchAll(PLAYERS_TABLE, SupabasePlayerRow[].class);
  }

  public List<SupabaseScoreRow> fetchScores() {
    return restClient.fetchAll(SCORES_TABLE, SupabaseScoreRow[].class);
  }

  public List<SupabaseUserRow> fetchUsers() {
    return restClient.fetchAll(USERS_TABLE, SupabaseUserRow[].class);
  }

  public List<SupabaseTeamRow> fetchTeams() {
    return restClient.fetchAll(TEAMS_TABLE, SupabaseTeamRow[].class);
  }

  public List<SupabaseTeamPlayerRow> fetchTeamPlayers() {
    return restClient.fetchAll(TEAM_PLAYERS_TABLE, SupabaseTeamPlayerRow[].class);
  }

  public List<SupabaseGameRow> fetchGames() {
    return restClient.fetchAll(GAMES_TABLE, SupabaseGameRow[].class);
  }

  public List<SupabaseGameParticipantRow> fetchGameParticipants() {
    return restClient.fetchAll(GAME_PARTICIPANTS_TABLE, SupabaseGameParticipantRow[].class);
  }
}
