package com.fortnite.pronos.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.dto.CreateGameRequest;
import com.fortnite.pronos.dto.JoinGameRequest;
import com.fortnite.pronos.model.Player;

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
    return email != null && email.contains("@") && email.contains(".");
  }

  /** Valide une requête de création de game */
  public void validateCreateGameRequest(CreateGameRequest request) {
    log.info("Validation de la requête de création de game: {}", request.getName());

    if (request == null) {
      throw new IllegalArgumentException("La requête de création de game ne peut pas être null");
    }

    if (!request.isValid()) {
      String errors = String.join(", ", request.getValidationErrors());
      log.warn("Erreurs de validation pour la création de game: {}", errors);
      throw new IllegalArgumentException("Erreurs de validation: " + errors);
    }

    log.info("Requête de création de game validée avec succès");
  }

  /** Valide une requête de rejoindre une game */
  public void validateJoinGameRequest(JoinGameRequest request) {
    log.info("Validation de la requête de rejoindre une game: {}", request.getGameId());

    if (request == null) {
      throw new IllegalArgumentException("La requête de rejoindre une game ne peut pas être null");
    }

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

      if (maxPlayers > 10) { // Max 10 joueurs par région
        throw new IllegalArgumentException(
            "Le nombre de joueurs par région ne peut pas dépasser 10 pour la région " + region);
      }

      totalPlayers += maxPlayers;
    }

    if (totalPlayers > 20) { // Max 20 joueurs au total
      throw new IllegalArgumentException(
          "Le nombre total de joueurs ne peut pas dépasser 20 (actuel: " + totalPlayers + ")");
    }

    log.info("Règles de région validées avec succès - Total joueurs: {}", totalPlayers);
  }
}
