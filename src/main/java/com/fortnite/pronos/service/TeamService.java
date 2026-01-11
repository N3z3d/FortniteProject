package com.fortnite.pronos.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.dto.SwapPlayersRequest;
import com.fortnite.pronos.dto.SwapPlayersResponse;
import com.fortnite.pronos.dto.team.TeamDto;
import com.fortnite.pronos.exception.InvalidSwapException;
import com.fortnite.pronos.exception.TeamNotFoundException;
import com.fortnite.pronos.exception.UnauthorizedAccessException;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.TeamPlayer;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.TeamPlayerRepository;
import com.fortnite.pronos.repository.TeamRepository;
import com.fortnite.pronos.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service de gestion des équipes */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {

  private static final String USER_NOT_FOUND_MESSAGE = "Utilisateur non trouvé";
  private static final String TEAM_NOT_FOUND_MESSAGE = "Équipe non trouvée";
  private static final String PLAYER_NOT_FOUND_MESSAGE = "Joueur non trouvé";

  private final TeamRepository teamRepository;
  private final PlayerRepository playerRepository;
  private final UserRepository userRepository;
  private final TeamPlayerRepository teamPlayerRepository;

  /** Récupère l'équipe d'un utilisateur pour une saison donnée */
  @Transactional(readOnly = true)
  public TeamDto getTeam(UUID userId, int season) {
    log.debug("Récupération de l'équipe pour l'utilisateur {} et la saison {}", userId, season);

    User user = findUserById(userId);
    Team team = findTeamByUserAndSeason(user, season);

    log.info("Équipe {} récupérée pour l'utilisateur {}", team.getId(), userId);
    return TeamDto.from(team);
  }

  /** Récupère une équipe par son ID */
  @Transactional(readOnly = true)
  // @Cacheable(value = "teamById", key = "#teamId")
  public TeamDto getTeamById(UUID teamId) {
    log.debug("Récupération de l'équipe avec l'ID {} - VERSION OPTIMISÉE", teamId);

    // OPTIMISATION: Utiliser la requête avec FETCH JOIN
    Team team =
        teamRepository
            .findByIdWithFetch(teamId)
            .orElseThrow(() -> new EntityNotFoundException(TEAM_NOT_FOUND_MESSAGE));

    log.info("Équipe {} récupérée (avec optimisation)", team.getId());
    return TeamDto.from(team);
  }

  /** Récupère toutes les équipes d'une saison */
  @Transactional(readOnly = true)
  // @Cacheable(value = "teamsBySeason", key = "#season")
  public List<TeamDto> getAllTeams(int season) {
    log.debug("Récupération de toutes les équipes pour la saison {} - VERSION OPTIMISÉE", season);

    // OPTIMISATION: Utiliser la requête avec FETCH JOIN pour éviter N+1
    List<Team> teams = teamRepository.findBySeasonWithFetch(season);
    log.info("{} équipes trouvées pour la saison {} (avec optimisation)", teams.size(), season);

    return teams.stream().map(TeamDto::from).collect(Collectors.toList());
  }

  /** Crée une nouvelle équipe pour un utilisateur */
  @Transactional
  public TeamDto createTeam(UUID userId, String name, int season) {
    log.info(
        "Création d'une équipe '{}' pour l'utilisateur {} et la saison {}", name, userId, season);

    User user = findUserById(userId);
    validateTeamCreation(user, season);

    Team team = buildNewTeam(name, user, season);
    Team savedTeam = teamRepository.save(team);

    log.info("Équipe {} créée avec succès pour l'utilisateur {}", savedTeam.getId(), userId);
    return TeamDto.from(savedTeam);
  }

  /** Ajoute un joueur à une équipe */
  @Transactional
  public TeamDto addPlayerToTeam(UUID userId, UUID playerId, int position, int season) {
    log.info(
        "Ajout du joueur {} à l'équipe de l'utilisateur {} (position: {})",
        playerId,
        userId,
        position);

    User user = findUserById(userId);
    Player player = findPlayerById(playerId);
    Team team = findTeamByUserAndSeason(user, season);

    validatePlayerAddition(team, player);
    team.addPlayer(player, position);

    Team savedTeam = teamRepository.save(team);
    log.info("Joueur {} ajouté avec succès à l'équipe {}", playerId, team.getId());

    return TeamDto.from(savedTeam);
  }

  /** Retire un joueur d'une équipe */
  @Transactional
  public TeamDto removePlayerFromTeam(UUID userId, UUID playerId, int season) {
    log.info("Suppression du joueur {} de l'équipe de l'utilisateur {}", playerId, userId);

    User user = findUserById(userId);
    Player player = findPlayerById(playerId);
    Team team = findTeamByUserAndSeason(user, season);

    validatePlayerRemoval(team, player);
    team.removePlayer(player);

    Team savedTeam = teamRepository.save(team);
    log.info("Joueur {} supprimé avec succès de l'équipe {}", playerId, team.getId());

    return TeamDto.from(savedTeam);
  }

  /** Effectue des changements de joueurs en lot */
  @Transactional
  public TeamDto makePlayerChanges(UUID userId, Map<UUID, UUID> playerChanges, int season) {
    log.info(
        "Changements de joueurs pour l'utilisateur {} : {} modifications",
        userId,
        playerChanges.size());

    User user = findUserById(userId);
    Team team = findTeamByUserAndSeason(user, season);

    validateUserPermissions(user);
    processPlayerChanges(team, playerChanges, season);

    Team savedTeam = teamRepository.save(team);
    log.info("Changements de joueurs effectués avec succès pour l'équipe {}", team.getId());

    return TeamDto.from(savedTeam);
  }

  /**
   * Échange deux joueurs entre deux équipes Clean Code : méthode principale qui orchestre le
   * processus
   */
  @Transactional
  public SwapPlayersResponse swapPlayers(UUID userId, SwapPlayersRequest request) {
    log.debug("Demande d'échange de joueurs par l'utilisateur {}", userId);

    // Valider l'utilisateur
    User user = validateUserExists(userId);

    // Récupérer les équipes
    Team team1 = findTeamOrThrow(request.getTeamId1());
    Team team2 = findTeamOrThrow(request.getTeamId2());

    // Valider les droits et conditions
    validateUserCanSwap(user, team1, team2);
    validateTeamsCanSwap(team1, team2);

    // Récupérer et valider les joueurs
    Player player1 = findPlayerOrThrow(request.getPlayerId1());
    Player player2 = findPlayerOrThrow(request.getPlayerId2());

    // Effectuer l'échange
    performSwap(team1, player1, team2, player2);

    // Créer la réponse
    return createSuccessResponse(team1, player1, team2, player2, user);
  }

  /** Valide qu'un utilisateur existe Clean Code : méthode avec responsabilité unique */
  private User validateUserExists(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND_MESSAGE));
  }

  /** Trouve une équipe ou lance une exception Clean Code : évite la duplication */
  private Team findTeamOrThrow(UUID teamId) {
    return teamRepository
        .findById(teamId)
        .orElseThrow(() -> new TeamNotFoundException("Équipe non trouvée : " + teamId));
  }

  /** Trouve un joueur ou lance une exception Clean Code : méthode focalisée */
  private Player findPlayerOrThrow(UUID playerId) {
    return playerRepository
        .findById(playerId)
        .orElseThrow(
            () -> new EntityNotFoundException(PLAYER_NOT_FOUND_MESSAGE + " : " + playerId));
  }

  /**
   * Valide que l'utilisateur peut effectuer l'échange Clean Code : séparation des préoccupations
   */
  private void validateUserCanSwap(User user, Team team1, Team team2) {
    boolean isOwnerTeam1 = team1.getOwner().getId().equals(user.getId());
    boolean isOwnerTeam2 = team2.getOwner().getId().equals(user.getId());

    if (!isOwnerTeam1 && !isOwnerTeam2) {
      throw new UnauthorizedAccessException(
          "Vous devez être propriétaire d'au moins une des équipes pour effectuer un échange");
    }
  }

  /** Valide que les équipes peuvent échanger Clean Code : validation métier isolée */
  private void validateTeamsCanSwap(Team team1, Team team2) {
    if (!team1.getSeason().equals(team2.getSeason())) {
      throw new InvalidSwapException(
          "Les équipes doivent être dans la même saison pour échanger des joueurs");
    }
  }

  /** Effectue l'échange physique des joueurs Clean Code : méthode qui fait une seule chose */
  private void performSwap(Team team1, Player player1, Team team2, Player player2) {
    // Récupérer les associations actuelles
    TeamPlayer tp1 = findTeamPlayerOrThrow(team1, player1, team1.getName());
    TeamPlayer tp2 = findTeamPlayerOrThrow(team2, player2, team2.getName());

    // Échanger les équipes
    tp1.setTeam(team2);
    tp2.setTeam(team1);

    // Sauvegarder les modifications
    teamPlayerRepository.save(tp1);
    teamPlayerRepository.save(tp2);

    log.info(
        "Échange effectué : {} ({}) ↔ {} ({})",
        player1.getNickname(),
        team1.getName(),
        player2.getNickname(),
        team2.getName());
  }

  /**
   * Trouve l'association TeamPlayer ou lance une exception Clean Code : méthode utilitaire
   * réutilisable
   */
  private TeamPlayer findTeamPlayerOrThrow(Team team, Player player, String teamName) {
    return teamPlayerRepository
        .findByTeamAndPlayer(team, player)
        .orElseThrow(
            () ->
                new InvalidSwapException(
                    String.format(
                        "Le joueur %s n'est pas dans l'équipe %s",
                        player.getNickname(), teamName)));
  }

  /** Crée la réponse de succès Clean Code : construction de la réponse isolée */
  private SwapPlayersResponse createSuccessResponse(
      Team team1, Player player1, Team team2, Player player2, User initiator) {

    SwapPlayersResponse.SwapDetails details =
        SwapPlayersResponse.SwapDetails.builder()
            .fromTeamId(team1.getId())
            .toTeamId(team2.getId())
            .fromPlayerName(player1.getNickname())
            .toPlayerName(player2.getNickname())
            .swapReason("Manual swap")
            .build();

    SwapPlayersResponse response =
        SwapPlayersResponse.success(
            team1.getId(),
            player1.getId(),
            player2.getId(),
            "Échange effectué avec succès entre " + team1.getName() + " et " + team2.getName());
    response.setSwapDetails(details);

    return response;
  }

  // Méthodes utilitaires privées

  private User findUserById(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(
            () -> {
              log.warn("Utilisateur non trouvé avec l'ID: {}", userId);
              return new EntityNotFoundException(USER_NOT_FOUND_MESSAGE);
            });
  }

  private Team findTeamByUserAndSeason(User user, int season) {
    return teamRepository
        .findByOwnerAndSeason(user, season)
        .orElseThrow(
            () -> {
              log.warn(
                  "Équipe non trouvée pour l'utilisateur {} et la saison {}", user.getId(), season);
              return new EntityNotFoundException(TEAM_NOT_FOUND_MESSAGE);
            });
  }

  private Player findPlayerById(UUID playerId) {
    return playerRepository
        .findById(playerId)
        .orElseThrow(
            () -> {
              log.warn("Joueur non trouvé avec l'ID: {}", playerId);
              return new EntityNotFoundException(PLAYER_NOT_FOUND_MESSAGE);
            });
  }

  private void validateTeamCreation(User user, int season) {
    if (teamRepository.findByOwnerAndSeason(user, season).isPresent()) {
      log.warn("L'utilisateur {} a déjà une équipe pour la saison {}", user.getId(), season);
      throw new IllegalStateException("L'utilisateur a déjà une équipe pour cette saison");
    }
  }

  private Team buildNewTeam(String name, User user, int season) {
    Team team = new Team();
    team.setName(name);
    team.setOwner(user);
    team.setSeason(season);
    return team;
  }

  private void validatePlayerAddition(Team team, Player player) {
    if (team.hasPlayer(player)) {
      log.warn("Le joueur {} est déjà dans l'équipe {}", player.getId(), team.getId());
      throw new IllegalStateException("Le joueur est déjà dans l'équipe");
    }
  }

  private void validatePlayerRemoval(Team team, Player player) {
    if (!team.hasPlayer(player)) {
      log.warn("Le joueur {} n'est pas dans l'équipe {}", player.getId(), team.getId());
      throw new IllegalStateException("Le joueur n'est pas dans l'équipe");
    }
  }

  private void validateUserPermissions(User user) {
    // Tous les utilisateurs peuvent modifier leur équipe maintenant
    // Plus de restriction spéciale pour Marcel
    log.debug("Validation des permissions pour l'utilisateur {}", user.getId());
  }

  private void processPlayerChanges(Team team, Map<UUID, UUID> playerChanges, int season) {
    playerChanges.forEach(
        (playerOutId, playerInId) -> {
          try {
            Player playerOut = findPlayerById(playerOutId);
            Player playerIn = findPlayerById(playerInId);

            validatePlayerChange(team, playerOut, playerIn, season);
            executePlayerChange(team, playerOut, playerIn, season);

            log.debug("Changement effectué: {} -> {}", playerOutId, playerInId);
          } catch (Exception e) {
            log.error(
                "Erreur lors du changement de joueur {} -> {}: {}",
                playerOutId,
                playerInId,
                e.getMessage());
            throw e;
          }
        });
  }

  private void validatePlayerChange(Team team, Player playerOut, Player playerIn, int season) {
    // Vérifier que le joueur sortant est dans l'équipe
    if (!team.hasPlayer(playerOut)) {
      throw new IllegalStateException("Le joueur sortant n'est pas dans l'équipe");
    }

    // Vérifier que le joueur entrant est disponible
    if (teamRepository.findTeamByPlayerAndSeason(playerIn.getId(), season).isPresent()) {
      throw new IllegalStateException("Le joueur entrant n'est pas disponible");
    }

    // Vérifier les règles de changement
    validateChangeRules(playerOut, playerIn, season);
  }

  private void executePlayerChange(Team team, Player playerOut, Player playerIn, int season) {
    // Récupérer la position du joueur sortant
    int position = team.findPlayerPositionInTeam(playerOut);

    // Effectuer le changement
    team.removePlayer(playerOut);
    team.addPlayer(playerIn, position);
  }

  private void validateChangeRules(Player playerOut, Player playerIn, int season) {
    // Règles métier spécifiques aux changements
    // À implémenter selon les règles du jeu
    log.debug(
        "Validation des règles de changement pour {} -> {}", playerOut.getId(), playerIn.getId());
  }

  @Transactional(readOnly = true)
  public TeamDto getTeamByPlayer(UUID playerId, int season) {
    Player player =
        playerRepository
            .findById(playerId)
            .orElseThrow(() -> new EntityNotFoundException("Joueur non trouvé"));

    Team team =
        teamRepository
            .findTeamByPlayerAndSeason(playerId, season)
            .orElseThrow(() -> new EntityNotFoundException("Équipe non trouvée"));

    return TeamDto.from(team);
  }

  @Transactional(readOnly = true)
  public Map<UUID, TeamDto> getTeamsByPlayers(List<UUID> playerIds, int season) {
    return playerIds.stream()
        .collect(
            Collectors.toMap(playerId -> playerId, playerId -> getTeamByPlayer(playerId, season)));
  }

  @Transactional(readOnly = true)
  // @Cacheable(value = "teamsBySeason", key = "#season + '_all'")
  public List<TeamDto> getTeamsBySeason(int season) {
    // OPTIMISATION: Utiliser la requête optimisée
    return teamRepository.findBySeasonWithFetch(season).stream()
        .map(TeamDto::from)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  // @Cacheable(value = "participantTeams", key = "#season")
  public List<TeamDto> getParticipantTeams(int season) {
    // OPTIMISATION: Utiliser une requête optimisée si disponible
    return teamRepository.findParticipantTeamsWithFetch(season).stream()
        .map(TeamDto::from)
        .collect(Collectors.toList());
  }

  /**
   * Récupère les équipes de Marcel pour une saison donnée Maintenant retourne une liste vide car
   * Marcel n'est plus un rôle spécial
   */
  @Transactional(readOnly = true)
  public List<TeamDto> getMarcelTeams(int season) {
    log.debug("Récupération des équipes de Marcel pour la saison {} - rôle obsolète", season);

    // Marcel n'est plus un rôle spécial, retourne liste vide
    return java.util.Collections.emptyList();
  }

  /** Récupère l'équipe d'un utilisateur pour une saison donnée (nouvelle signature) */
  @Transactional(readOnly = true)
  public TeamDto getTeamByUserAndSeason(UUID userId, int season) {
    log.debug("Récupération de l'équipe pour l'utilisateur {} et la saison {}", userId, season);

    User user = findUserById(userId);
    Team team = findTeamByUserAndSeason(user, season);

    log.info("Équipe {} récupérée pour l'utilisateur {}", team.getId(), userId);
    return TeamDto.from(team);
  }

  /** Crée une nouvelle équipe */
  @Transactional
  public TeamDto createTeam(TeamDto teamDto) {
    log.debug("Création d'une nouvelle équipe");

    // Simplification : créer une équipe avec des valeurs par défaut
    User owner =
        userRepository
            .findById(teamDto.getUserId())
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));

    Team team = new Team();
    team.setName("Équipe " + owner.getUsername()); // Nom par défaut
    team.setOwner(owner);
    team.setSeason(owner.getCurrentSeason()); // Saison de l'utilisateur

    Team savedTeam = teamRepository.save(team);
    log.info("Équipe créée: {}", savedTeam.getId());

    return TeamDto.from(savedTeam);
  }

  /** Met à jour une équipe existante */
  @Transactional
  public TeamDto updateTeam(UUID teamId, TeamDto teamDto) {
    log.debug("Mise à jour de l'équipe: {}", teamId);

    Team team =
        teamRepository
            .findById(teamId)
            .orElseThrow(() -> new EntityNotFoundException("Équipe non trouvée"));

    // Mise à jour simple du nom uniquement
    team.setName("Équipe " + team.getOwner().getUsername());

    Team updatedTeam = teamRepository.save(team);
    log.info("Équipe mise à jour: {}", teamId);

    return TeamDto.from(updatedTeam);
  }

  /** Supprime une équipe */
  @Transactional
  public void deleteTeam(UUID teamId) {
    log.info("Suppression de l'équipe {}", teamId);

    if (!teamRepository.existsById(teamId)) {
      throw new EntityNotFoundException(TEAM_NOT_FOUND_MESSAGE);
    }

    teamRepository.deleteById(teamId);
    log.info("Équipe {} supprimée avec succès", teamId);
  }

  /** Récupère toutes les équipes d'une game spécifique */
  @Transactional(readOnly = true)
  public List<TeamDto> getTeamsByGame(UUID gameId) {
    log.debug("Récupération des équipes pour la game {}", gameId);

    List<Team> teams = teamRepository.findByGameIdWithFetch(gameId);
    log.info("{} équipes trouvées pour la game {}", teams.size(), gameId);

    return teams.stream().map(TeamDto::from).toList();
  }
}
