package com.fortnite.pronos.service;

import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.domain.player.model.Player;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.TeamDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.domain.team.model.Team;
import com.fortnite.pronos.dto.SwapPlayersRequest;
import com.fortnite.pronos.dto.SwapPlayersResponse;
import com.fortnite.pronos.dto.team.TeamDto;
import com.fortnite.pronos.exception.InvalidSwapException;
import com.fortnite.pronos.exception.TeamNotFoundException;
import com.fortnite.pronos.exception.UnauthorizedAccessException;
import com.fortnite.pronos.model.TeamPlayer;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.TeamPlayerRepository;
import com.fortnite.pronos.repository.TeamRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service de gestion des equipes */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {

  private static final String USER_NOT_FOUND_MESSAGE = "Utilisateur non trouve";
  private static final String TEAM_NOT_FOUND_MESSAGE = "Equipe non trouvee";
  private static final String PLAYER_NOT_FOUND_MESSAGE = "Joueur non trouve";

  private final TeamDomainRepositoryPort teamDomainRepository;
  private final PlayerDomainRepositoryPort playerDomainRepository;
  private final TeamRepository teamRepository;
  private final UserRepositoryPort userRepository;
  private final TeamPlayerRepository teamPlayerRepository;

  /** Cree une nouvelle equipe pour un utilisateur */
  @Transactional
  public TeamDto createTeam(UUID userId, String name, int season) {
    log.info(
        "Creation d'une equipe '{}' pour l'utilisateur {} et la saison {}", name, userId, season);

    User user = findUserById(userId);
    validateTeamCreation(user, season);

    Team savedTeam = teamDomainRepository.save(buildNewTeam(name, user, season));

    log.info("Equipe {} creee avec succes pour l'utilisateur {}", savedTeam.getId(), userId);
    return toTeamDto(savedTeam.getId());
  }

  /** Ajoute un joueur a une equipe */
  @Transactional
  public TeamDto addPlayerToTeam(UUID userId, UUID playerId, int position, int season) {
    log.info(
        "Ajout du joueur {} a l'equipe de l'utilisateur {} (position: {})",
        playerId,
        userId,
        position);

    User user = findUserById(userId);
    Player player = findPlayerById(playerId);
    Team team = findTeamByUserAndSeason(user, season);

    validatePlayerAddition(team, player);
    team.addMember(player.getId(), position);

    Team savedTeam = teamDomainRepository.save(team);
    log.info("Joueur {} ajoute avec succes a l'equipe {}", playerId, team.getId());
    return toTeamDto(savedTeam.getId());
  }

  /** Retire un joueur d'une equipe */
  @Transactional
  public TeamDto removePlayerFromTeam(UUID userId, UUID playerId, int season) {
    log.info("Suppression du joueur {} de l'equipe de l'utilisateur {}", playerId, userId);

    User user = findUserById(userId);
    Player player = findPlayerById(playerId);
    Team team = findTeamByUserAndSeason(user, season);

    validatePlayerRemoval(team, player);
    team.removeMember(player.getId());

    Team savedTeam = teamDomainRepository.save(team);
    log.info("Joueur {} supprime avec succes de l'equipe {}", playerId, team.getId());
    return toTeamDto(savedTeam.getId());
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

    Team savedTeam = teamDomainRepository.save(team);
    log.info("Changements de joueurs effectues avec succes pour l'equipe {}", team.getId());
    return toTeamDto(savedTeam.getId());
  }

  /** Echange deux joueurs entre deux equipes */
  @Transactional
  public SwapPlayersResponse swapPlayers(UUID userId, SwapPlayersRequest request) {
    log.debug("Demande d'echange de joueurs par l'utilisateur {}", userId);

    User user = validateUserExists(userId);
    Team team1 = findDomainTeamOrThrow(request.getTeamId1());
    Team team2 = findDomainTeamOrThrow(request.getTeamId2());

    validateUserCanSwap(user, team1, team2);
    validateTeamsCanSwap(team1, team2);

    Player player1 = findPlayerById(request.getPlayerId1());
    Player player2 = findPlayerById(request.getPlayerId2());

    performSwap(team1, player1, team2, player2);

    return createSuccessResponse(team1, player1, team2, player2, user);
  }

  /** Met a jour une equipe existante */
  @Transactional
  public TeamDto updateTeam(UUID teamId, TeamDto teamDto) {
    log.debug("Mise a jour de l'equipe: {}", teamId);

    Team team =
        teamDomainRepository
            .findById(teamId)
            .orElseThrow(() -> new EntityNotFoundException("Equipe non trouvee"));
    User owner = findUserById(team.getOwnerId());

    team.rename("Equipe " + owner.getUsername());
    Team updatedTeam = teamDomainRepository.save(team);

    log.info("Equipe mise a jour: {}", teamId);
    return toTeamDto(updatedTeam.getId());
  }

  /** Supprime une equipe */
  @Transactional
  public void deleteTeam(UUID teamId) {
    log.info("Suppression de l'equipe {}", teamId);

    if (teamDomainRepository.findById(teamId).isEmpty()) {
      throw new EntityNotFoundException(TEAM_NOT_FOUND_MESSAGE);
    }

    teamRepository.deleteById(teamId);
    log.info("Equipe {} supprimee avec succes", teamId);
  }

  private User validateUserExists(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND_MESSAGE));
  }

  private Team findDomainTeamOrThrow(UUID teamId) {
    return teamDomainRepository
        .findById(teamId)
        .orElseThrow(() -> new TeamNotFoundException("Equipe non trouvee : " + teamId));
  }

  private void validateUserCanSwap(User user, Team team1, Team team2) {
    boolean isOwnerTeam1 = team1.getOwnerId().equals(user.getId());
    boolean isOwnerTeam2 = team2.getOwnerId().equals(user.getId());

    if (!isOwnerTeam1 && !isOwnerTeam2) {
      throw new UnauthorizedAccessException(
          "Vous devez etre proprietaire d'au moins une des equipes pour effectuer un echange");
    }
  }

  private void validateTeamsCanSwap(Team team1, Team team2) {
    if (team1.getSeason() != team2.getSeason()) {
      throw new InvalidSwapException(
          "Les equipes doivent etre dans la meme saison pour echanger des joueurs");
    }
  }

  private void performSwap(Team team1, Player player1, Team team2, Player player2) {
    com.fortnite.pronos.model.Team team1Ref = toEntityTeamReference(team1);
    com.fortnite.pronos.model.Team team2Ref = toEntityTeamReference(team2);
    com.fortnite.pronos.model.Player player1Ref = toEntityPlayerReference(player1);
    com.fortnite.pronos.model.Player player2Ref = toEntityPlayerReference(player2);

    TeamPlayer tp1 =
        findTeamPlayerOrThrow(team1Ref, player1Ref, team1.getName(), player1.getNickname());
    TeamPlayer tp2 =
        findTeamPlayerOrThrow(team2Ref, player2Ref, team2.getName(), player2.getNickname());

    tp1.setTeam(team2Ref);
    tp2.setTeam(team1Ref);

    teamPlayerRepository.save(tp1);
    teamPlayerRepository.save(tp2);

    log.info(
        "Echange effectue : {} ({}) <-> {} ({})",
        player1.getNickname(),
        team1.getName(),
        player2.getNickname(),
        team2.getName());
  }

  private TeamPlayer findTeamPlayerOrThrow(
      com.fortnite.pronos.model.Team team,
      com.fortnite.pronos.model.Player player,
      String teamName,
      String playerNickname) {
    return teamPlayerRepository
        .findByTeamAndPlayer(team, player)
        .orElseThrow(
            () ->
                new InvalidSwapException(
                    String.format(
                        "Le joueur %s n'est pas dans l'equipe %s", playerNickname, teamName)));
  }

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
            "Echange effectue avec succes entre " + team1.getName() + " et " + team2.getName());
    response.setSwapDetails(details);
    return response;
  }

  private User findUserById(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(
            () -> {
              log.warn("Utilisateur non trouve avec l'ID: {}", userId);
              return new EntityNotFoundException(USER_NOT_FOUND_MESSAGE);
            });
  }

  private Team findTeamByUserAndSeason(User user, int season) {
    return teamDomainRepository
        .findByOwnerIdAndSeason(user.getId(), season)
        .orElseThrow(
            () -> {
              log.warn(
                  "Equipe non trouvee pour l'utilisateur {} et la saison {}", user.getId(), season);
              return new EntityNotFoundException(TEAM_NOT_FOUND_MESSAGE);
            });
  }

  private Player findPlayerById(UUID playerId) {
    return playerDomainRepository
        .findById(playerId)
        .orElseThrow(
            () -> {
              log.warn("Joueur non trouve avec l'ID: {}", playerId);
              return new EntityNotFoundException(PLAYER_NOT_FOUND_MESSAGE);
            });
  }

  private void validateTeamCreation(User user, int season) {
    if (teamDomainRepository.findByOwnerIdAndSeason(user.getId(), season).isPresent()) {
      log.warn("L'utilisateur {} a deja une equipe pour la saison {}", user.getId(), season);
      throw new IllegalStateException("L'utilisateur a deja une equipe pour cette saison");
    }
  }

  private Team buildNewTeam(String name, User user, int season) {
    return new Team(name, user.getId(), season);
  }

  private void validatePlayerAddition(Team team, Player player) {
    if (team.hasActiveMember(player.getId())) {
      log.warn("Le joueur {} est deja dans l'equipe {}", player.getId(), team.getId());
      throw new IllegalStateException("Le joueur est deja dans l'equipe");
    }
  }

  private void validatePlayerRemoval(Team team, Player player) {
    if (!team.hasActiveMember(player.getId())) {
      log.warn("Le joueur {} n'est pas dans l'equipe {}", player.getId(), team.getId());
      throw new IllegalStateException("Le joueur n'est pas dans l'equipe");
    }
  }

  private void validateUserPermissions(User user) {
    log.debug("Validation des permissions pour l'utilisateur {}", user.getId());
  }

  private void processPlayerChanges(Team team, Map<UUID, UUID> playerChanges, int season) {
    playerChanges.forEach(
        (playerOutId, playerInId) -> {
          try {
            Player playerOut = findPlayerById(playerOutId);
            Player playerIn = findPlayerById(playerInId);

            validatePlayerChange(team, playerOut, playerIn, season);
            executePlayerChange(team, playerOut, playerIn);
            log.debug("Changement effectue: {} -> {}", playerOutId, playerInId);
          } catch (RuntimeException e) {
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
    if (!team.hasActiveMember(playerOut.getId())) {
      throw new IllegalStateException("Le joueur sortant n'est pas dans l'equipe");
    }

    if (teamDomainRepository.findTeamByPlayerAndSeason(playerIn.getId(), season).isPresent()) {
      throw new IllegalStateException("Le joueur entrant n'est pas disponible");
    }

    validateChangeRules(playerOut, playerIn);
  }

  private void executePlayerChange(Team team, Player playerOut, Player playerIn) {
    int position = team.getMemberPosition(playerOut.getId());
    team.removeMember(playerOut.getId());
    team.addMember(playerIn.getId(), position);
  }

  private void validateChangeRules(Player playerOut, Player playerIn) {
    log.debug(
        "Validation des regles de changement pour {} -> {}", playerOut.getId(), playerIn.getId());
  }

  private TeamDto toTeamDto(UUID teamId) {
    com.fortnite.pronos.model.Team entity =
        teamRepository
            .findByIdWithFetch(teamId)
            .orElseThrow(() -> new EntityNotFoundException(TEAM_NOT_FOUND_MESSAGE));
    return TeamDto.from(entity);
  }

  private com.fortnite.pronos.model.Team toEntityTeamReference(Team team) {
    com.fortnite.pronos.model.Team entity = new com.fortnite.pronos.model.Team();
    entity.setId(team.getId());
    entity.setName(team.getName());
    entity.setSeason(team.getSeason());
    return entity;
  }

  private com.fortnite.pronos.model.Player toEntityPlayerReference(Player player) {
    return com.fortnite.pronos.model.Player.builder()
        .id(player.getId())
        .nickname(player.getNickname())
        .build();
  }
}
