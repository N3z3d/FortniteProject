package com.fortnite.pronos.service.team;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.dto.team.TeamDto;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.TeamRepository;
import com.fortnite.pronos.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service for read-only team queries. Extracted from TeamService to respect SRP. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamQueryService {

  private static final String USER_NOT_FOUND_MESSAGE = "Utilisateur non trouve";
  private static final String TEAM_NOT_FOUND_MESSAGE = "Equipe non trouvee";

  private final TeamRepository teamRepository;
  private final PlayerRepository playerRepository;
  private final UserRepository userRepository;

  /** Recupere l'equipe d'un utilisateur pour une saison donnee */
  public TeamDto getTeam(UUID userId, int season) {
    log.debug("Recuperation de l'equipe pour l'utilisateur {} et la saison {}", userId, season);

    User user = findUserById(userId);
    Team team = findTeamByUserAndSeason(user, season);

    log.info("Equipe {} recuperee pour l'utilisateur {}", team.getId(), userId);
    return TeamDto.from(team);
  }

  /** Recupere une equipe par son ID */
  public TeamDto getTeamById(UUID teamId) {
    log.debug("Recuperation de l'equipe avec l'ID {} - VERSION OPTIMISEE", teamId);

    Team team =
        teamRepository
            .findByIdWithFetch(teamId)
            .orElseThrow(() -> new EntityNotFoundException(TEAM_NOT_FOUND_MESSAGE));

    log.info("Equipe {} recuperee (avec optimisation)", team.getId());
    return TeamDto.from(team);
  }

  /** Recupere toutes les equipes d'une saison */
  public List<TeamDto> getAllTeams(int season) {
    log.debug("Recuperation de toutes les equipes pour la saison {} - VERSION OPTIMISEE", season);

    List<Team> teams = teamRepository.findBySeasonWithFetch(season);
    log.info("{} equipes trouvees pour la saison {} (avec optimisation)", teams.size(), season);

    return teams.stream().map(TeamDto::from).collect(Collectors.toList());
  }

  /** Recupere les equipes par username et annee */
  public List<TeamDto> getTeamsByUsernameAndYear(String username, int year) {
    log.debug("Recuperation des equipes pour l'utilisateur '{}' et l'annee {}", username, year);

    User user = findUserByUsername(username);
    Team team = findTeamByUserAndSeason(user, year);

    return List.of(TeamDto.from(team));
  }

  /** Recupere l'equipe d'un joueur pour une saison */
  public TeamDto getTeamByPlayer(UUID playerId, int season) {
    playerRepository
        .findById(playerId)
        .orElseThrow(() -> new EntityNotFoundException("Joueur non trouve"));

    Team team =
        teamRepository
            .findTeamByPlayerAndSeason(playerId, season)
            .orElseThrow(() -> new EntityNotFoundException(TEAM_NOT_FOUND_MESSAGE));

    return TeamDto.from(team);
  }

  /** Recupere les equipes pour une liste de joueurs */
  public Map<UUID, TeamDto> getTeamsByPlayers(List<UUID> playerIds, int season) {
    return playerIds.stream()
        .collect(
            Collectors.toMap(playerId -> playerId, playerId -> getTeamByPlayer(playerId, season)));
  }

  /** Recupere les equipes par saison */
  public List<TeamDto> getTeamsBySeason(int season) {
    return teamRepository.findBySeasonWithFetch(season).stream()
        .map(TeamDto::from)
        .collect(Collectors.toList());
  }

  /** Recupere les equipes participantes pour une saison */
  public List<TeamDto> getParticipantTeams(int season) {
    return teamRepository.findParticipantTeamsWithFetch(season).stream()
        .map(TeamDto::from)
        .collect(Collectors.toList());
  }

  /** Recupere les equipes de Marcel (obsolete - retourne liste vide) */
  public List<TeamDto> getMarcelTeams(int season) {
    log.debug("Recuperation des equipes de Marcel pour la saison {} - role obsolete", season);
    return java.util.Collections.emptyList();
  }

  /** Recupere l'equipe d'un utilisateur pour une saison donnee (alias) */
  public TeamDto getTeamByUserAndSeason(UUID userId, int season) {
    log.debug("Recuperation de l'equipe pour l'utilisateur {} et la saison {}", userId, season);

    User user = findUserById(userId);
    Team team = findTeamByUserAndSeason(user, season);

    log.info("Equipe {} recuperee pour l'utilisateur {}", team.getId(), userId);
    return TeamDto.from(team);
  }

  /** Recupere toutes les equipes d'une game specifique */
  public List<TeamDto> getTeamsByGame(UUID gameId) {
    log.debug("Recuperation des equipes pour la game {}", gameId);

    List<Team> teams = teamRepository.findByGameIdWithFetch(gameId);
    log.info("{} equipes trouvees pour la game {}", teams.size(), gameId);

    return teams.stream().map(TeamDto::from).toList();
  }

  // Entity-returning methods for legacy compatibility (ApiController)

  /** Returns Team entities for a season (legacy API) */
  public List<Team> findTeamsBySeasonWithFetch(int season) {
    return teamRepository.findBySeasonWithFetch(season);
  }

  /** Returns Team entity by ID (legacy API) */
  public Optional<Team> findTeamByIdWithFetch(UUID teamId) {
    return teamRepository.findByIdWithFetch(teamId);
  }

  /** Returns Team entity by owner and season (legacy API) */
  public Optional<Team> findTeamByOwnerAndSeason(User owner, int season) {
    return teamRepository.findByOwnerAndSeason(owner, season);
  }

  // Private helpers

  private User findUserById(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(
            () -> {
              log.warn("Utilisateur non trouve avec l'ID: {}", userId);
              return new EntityNotFoundException(USER_NOT_FOUND_MESSAGE);
            });
  }

  private User findUserByUsername(String username) {
    return userRepository
        .findByUsernameIgnoreCase(username)
        .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouve: " + username));
  }

  private Team findTeamByUserAndSeason(User user, int season) {
    return teamRepository
        .findByOwnerAndSeason(user, season)
        .orElseThrow(
            () -> {
              log.warn(
                  "Equipe non trouvee pour l'utilisateur {} et la saison {}", user.getId(), season);
              return new EntityNotFoundException(TEAM_NOT_FOUND_MESSAGE);
            });
  }
}
