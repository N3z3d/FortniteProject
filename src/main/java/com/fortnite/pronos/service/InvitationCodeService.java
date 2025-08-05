package com.fortnite.pronos.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.repository.GameRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsable de la génération des codes d'invitation uniques Principe de responsabilité
 * unique : ne s'occupe que des codes d'invitation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationCodeService {

  private final GameRepository gameRepository;

  private static final String ALLOWED_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  private static final int CODE_LENGTH = 6;
  private static final int MAX_GENERATION_ATTEMPTS = 100;

  private final SecureRandom secureRandom = new SecureRandom();

  /**
   * Génère un code d'invitation unique pour une game
   *
   * @return Le code d'invitation unique
   * @throws InvitationCodeGenerationException si impossible de générer un code unique
   */
  public String generateUniqueCode() {
    log.debug("Génération d'un nouveau code d'invitation");

    for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
      String code = generateRandomCode();

      if (!gameRepository.existsByInvitationCode(code)) {
        log.info("Code d'invitation généré avec succès : {} (tentative {})", code, attempt);
        return code;
      }

      log.debug("Code {} déjà existant, nouvelle tentative", code);
    }

    throw new InvitationCodeGenerationException(
        "Impossible de générer un code unique après " + MAX_GENERATION_ATTEMPTS + " tentatives");
  }

  /** Génère un code aléatoire Utilise SecureRandom pour une meilleure sécurité */
  private String generateRandomCode() {
    StringBuilder code = new StringBuilder(CODE_LENGTH);

    for (int i = 0; i < CODE_LENGTH; i++) {
      int randomIndex = secureRandom.nextInt(ALLOWED_CHARACTERS.length());
      code.append(ALLOWED_CHARACTERS.charAt(randomIndex));
    }

    return code.toString();
  }

  /**
   * Valide le format d'un code d'invitation
   *
   * @param code Le code à valider
   * @return true si le code est valide
   */
  public boolean isValidCodeFormat(String code) {
    if (code == null || code.length() != CODE_LENGTH) {
      return false;
    }

    return code.matches("^[A-Z0-9]{" + CODE_LENGTH + "}$");
  }

  /** Exception spécifique à la génération de codes */
  public static class InvitationCodeGenerationException extends RuntimeException {
    public InvitationCodeGenerationException(String message) {
      super(message);
    }
  }
}
