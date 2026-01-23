package com.fortnite.pronos.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.domain.InvitationCodeGenerator;
import com.fortnite.pronos.repository.GameRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsable de la génération des codes d'invitation uniques Principe de responsabilité
 * unique : ne s'occupe que des codes d'invitation. Delegates code generation to domain layer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationCodeService {

  private final GameRepository gameRepository;

  private static final int MAX_GENERATION_ATTEMPTS = 100;

  private final InvitationCodeGenerator codeGenerator = new InvitationCodeGenerator();

  /**
   * Génère un code d'invitation unique pour une game
   *
   * @return Le code d'invitation unique
   * @throws InvitationCodeGenerationException si impossible de générer un code unique
   */
  public String generateUniqueCode() {
    log.debug("Génération d'un nouveau code d'invitation");

    for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
      String code = codeGenerator.generate();

      if (!gameRepository.existsByInvitationCode(code)) {
        log.info("Code d'invitation généré avec succès : {} (tentative {})", code, attempt);
        return code;
      }

      log.debug("Code {} déjà existant, nouvelle tentative", code);
    }

    throw new InvitationCodeGenerationException(
        "Impossible de générer un code unique après " + MAX_GENERATION_ATTEMPTS + " tentatives");
  }

  /**
   * Valide le format d'un code d'invitation. Delegates to domain layer.
   *
   * @param code Le code à valider
   * @return true si le code est valide
   */
  public boolean isValidCodeFormat(String code) {
    return InvitationCodeGenerator.isValidFormat(code);
  }

  /**
   * Calcule la date d'expiration en fonction de la durée choisie
   *
   * @param duration La durée du code
   * @return La date d'expiration, ou null pour un code permanent
   */
  public LocalDateTime calculateExpirationDate(CodeDuration duration) {
    if (duration == null || duration == CodeDuration.PERMANENT) {
      return null;
    }
    return LocalDateTime.now().plusHours(duration.getHours());
  }

  /** Enum pour les durées de validité du code d'invitation */
  public enum CodeDuration {
    HOURS_24(24),
    HOURS_48(48),
    DAYS_7(168), // 7 * 24
    PERMANENT(0);

    private final int hours;

    CodeDuration(int hours) {
      this.hours = hours;
    }

    public int getHours() {
      return hours;
    }

    public static CodeDuration fromString(String value) {
      if (value == null) {
        return PERMANENT;
      }
      return switch (value.toLowerCase()) {
        case "24h" -> HOURS_24;
        case "48h" -> HOURS_48;
        case "7d", "7days" -> DAYS_7;
        default -> PERMANENT;
      };
    }
  }

  /** Exception spécifique à la génération de codes */
  public static class InvitationCodeGenerationException extends RuntimeException {
    public InvitationCodeGenerationException(String message) {
      super(message);
    }
  }
}
