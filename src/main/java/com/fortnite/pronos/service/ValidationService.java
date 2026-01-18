package com.fortnite.pronos.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.dto.CreateGameRequest;
import com.fortnite.pronos.dto.JoinGameRequest;
import com.fortnite.pronos.exception.BusinessException;
import com.fortnite.pronos.model.GameRegionRule;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.RegionRule;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.TeamPlayer;

import lombok.extern.slf4j.Slf4j;

/** Service de validation pour le MVP - Basique pour éviter les erreurs de compilation */
@Service
@Slf4j
public class ValidationService {

  /** Valide une requête générique */
  public boolean validateRequest(Object request) {
    if (request == null) {
      log.warn("Requête null reçue");
      return false;
    }
    return true;
  }

  /** Valide qu'une chaîne n'est pas vide */
  public boolean isNotEmpty(String value) {
    return value != null && !value.trim().isEmpty();
  }

  /** Valide un email */
  public boolean isValidEmail(String email) {
    if (email == null) {
      return false;
    }
    String trimmed = email.trim();
    // Basic pattern: local part + @ + domain with at least one dot, no spaces
    return trimmed.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{1,}$");
  }

  /** Valide une requête de création de game */
  public void validateCreateGameRequest(CreateGameRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("La requête de création de game ne peut pas être null");
    }

    log.info("Validation de la requête de création de game: {}", request.getName());

    if (!request.isValid()) {
      String errors = String.join(", ", request.getValidationErrors());
      log.warn("Erreurs de validation pour la création de game: {}", errors);
      throw new IllegalArgumentException("Erreurs de validation: " + errors);
    }

    log.info("Requête de création de game validée avec succès");
  }

  /** Valide une requête de rejoindre une game */
  public void validateJoinGameRequest(JoinGameRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("La requête de rejoindre une game ne peut pas être null");
    }

    log.info("Validation de la requête de rejoindre une game: {}", request.getGameId());

    if (!request.isValid()) {
      String errors = String.join(", ", request.getValidationErrors());
      log.warn("Erreurs de validation pour rejoindre la game: {}", errors);
      throw new IllegalArgumentException("Erreurs de validation: " + errors);
    }

    log.info("Requête de rejoindre une game validée avec succès");
  }

  /** Valide les règles de région */
  public void validateRegionRules(Map<Player.Region, Integer> regionRules) {
    log.info("Validation des règles de région");

    if (regionRules == null) {
      log.info("Aucune règle de région définie - validation réussie");
      return;
    }

    if (regionRules.isEmpty()) {
      log.info("Règles de région vides - validation réussie");
      return;
    }

    // Validation de base
    int totalPlayers = 0;
    for (Map.Entry<Player.Region, Integer> entry : regionRules.entrySet()) {
      Player.Region region = entry.getKey();
      Integer maxPlayers = entry.getValue();

      if (region == null) {
        throw new IllegalArgumentException("La région ne peut pas être null");
      }

      if (maxPlayers == null || maxPlayers <= 0) {
        throw new IllegalArgumentException(
            "Le nombre de joueurs par région doit être positif pour la région " + region);
      }

      totalPlayers += maxPlayers;
    }

    if (totalPlayers > 20) { // Max 20 joueurs au total
      throw new IllegalArgumentException("Le nombre total de joueurs ne peut pas dépasser 20");
    }

    for (Map.Entry<Player.Region, Integer> entry : regionRules.entrySet()) {
      Player.Region region = entry.getKey();
      Integer maxPlayers = entry.getValue();

      if (maxPlayers > 10) { // Max 10 joueurs par région
        throw new IllegalArgumentException(
            "Le nombre de joueurs par région ne peut pas dépasser 10 pour la région " + region);
      }
    }

    log.info("Règles de région validées avec succès - Total joueurs: {}", totalPlayers);
  }

  /**
   * Validate team composition against region rules
   *
   * @param team The team to validate
   * @param regionRules The region rules to validate against
   * @throws BusinessException if validation fails
   */
  public void validateTeamComposition(Team team, List<?> regionRules) {
    if (team == null) {
      throw new IllegalArgumentException("Team is required");
    }

    log.info("Validating team composition for team: {}", team.getName());

    if (regionRules == null || regionRules.isEmpty()) {
      log.info("No region rules to validate against");
      return;
    }

    // Count active players by region using TeamPlayer mapping
    Map<Player.Region, Long> playersByRegion = new java.util.HashMap<>();

    List<TeamPlayer> teamPlayers =
        team.getPlayers() != null ? team.getPlayers() : Collections.emptyList();

    teamPlayers.stream()
        .filter(tp -> tp != null && tp.getUntil() == null)
        .map(TeamPlayer::getPlayer)
        .filter(p -> p != null && p.getRegion() != null)
        .forEach(regionPlayer -> playersByRegion.merge(regionPlayer.getRegion(), 1L, Long::sum));

    // Check against region rules
    for (Object ruleObj : regionRules) {
      Player.Region region = null;
      Integer maxPlayers = null;

      if (ruleObj == null) {
        throw new BusinessException("Region rule cannot be null");
      }

      if (ruleObj instanceof GameRegionRule grr) {
        region = grr.getRegion();
        maxPlayers = grr.getMaxPlayers();
      } else if (ruleObj instanceof RegionRule rr) {
        if (rr.getRegion() == null || rr.getRegion().trim().isEmpty()) {
          throw new BusinessException("Region rule requires a region");
        }
        try {
          region = Player.Region.valueOf(rr.getRegion().trim());
        } catch (IllegalArgumentException ex) {
          throw new BusinessException("Unknown region in rule: " + rr.getRegion());
        }
        maxPlayers = rr.getMaxPlayers();
      } else {
        throw new BusinessException("Unsupported region rule type: " + ruleObj);
      }

      if (region == null) {
        throw new BusinessException("Region rule requires a region");
      }

      if (maxPlayers == null || maxPlayers <= 0) {
        throw new BusinessException("Region rule maxPlayers must be positive");
      }

      Long currentCount = playersByRegion.getOrDefault(region, 0L);

      if (currentCount > maxPlayers) {
        throw new BusinessException(
            String.format(
                "Team %s exceeds regional limit for %s: %d > %d",
                team.getName(), region, currentCount, maxPlayers));
      }
    }

    log.info("Team composition validation successful for team: {}", team.getName());
  }
}
