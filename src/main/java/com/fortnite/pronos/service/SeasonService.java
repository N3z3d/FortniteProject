package com.fortnite.pronos.service;

import java.time.Year;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Service de gestion des saisons de l'application Fortnite Fantasy
 *
 * <p>Responsabilités : - Gestion de la saison courante - Validation des saisons - Calcul des
 * périodes de saison - Utilitaires de saison
 */
@Slf4j
@Service
public class SeasonService {

  // Configuration des saisons
  private static final int FIRST_SEASON = 2024;
  private static final int DEFAULT_CURRENT_SEASON = 2025;

  @Value("${app.season.current:2025}")
  private Integer configuredCurrentSeason;

  @Value("${app.season.max-future-years:5}")
  private Integer maxFutureYears;

  /**
   * Obtient la saison courante
   *
   * @return la saison courante (année)
   */
  public int getCurrentSeason() {
    int currentSeason =
        configuredCurrentSeason != null ? configuredCurrentSeason : DEFAULT_CURRENT_SEASON;
    log.debug("Saison courante récupérée: {}", currentSeason);
    return currentSeason;
  }

  /**
   * Vérifie si une saison est valide
   *
   * @param season la saison à valider
   * @return true si la saison est valide, false sinon
   */
  public boolean isValidSeason(int season) {
    boolean isValid = season >= FIRST_SEASON && season <= getMaxValidSeason();
    log.debug("Validation de la saison {}: {}", season, isValid ? "VALIDE" : "INVALIDE");

    if (!isValid) {
      log.warn(
          "Tentative d'utilisation d'une saison invalide: {} (plage valide: {}-{})",
          season,
          FIRST_SEASON,
          getMaxValidSeason());
    }

    return isValid;
  }

  /**
   * Obtient la saison maximale autorisée
   *
   * @return la saison maximale (année courante + années futures configurées)
   */
  public int getMaxValidSeason() {
    int maxSeason = Year.now().getValue() + maxFutureYears;
    log.trace("Saison maximale calculée: {}", maxSeason);
    return maxSeason;
  }

  /**
   * Obtient la première saison disponible
   *
   * @return la première saison de l'application
   */
  public int getFirstSeason() {
    log.trace("Première saison: {}", FIRST_SEASON);
    return FIRST_SEASON;
  }

  /**
   * Vérifie si une saison est la saison courante
   *
   * @param season la saison à vérifier
   * @return true si c'est la saison courante
   */
  public boolean isCurrentSeason(int season) {
    boolean isCurrent = season == getCurrentSeason();
    log.debug("Vérification saison courante pour {}: {}", season, isCurrent);
    return isCurrent;
  }

  /**
   * Vérifie si une saison est dans le passé
   *
   * @param season la saison à vérifier
   * @return true si la saison est passée
   */
  public boolean isPastSeason(int season) {
    boolean isPast = season < getCurrentSeason();
    log.debug("Vérification saison passée pour {}: {}", season, isPast);
    return isPast;
  }

  /**
   * Vérifie si une saison est dans le futur
   *
   * @param season la saison à vérifier
   * @return true si la saison est future
   */
  public boolean isFutureSeason(int season) {
    boolean isFuture = season > getCurrentSeason();
    log.debug("Vérification saison future pour {}: {}", season, isFuture);
    return isFuture;
  }

  /**
   * Obtient la saison précédente
   *
   * @param season la saison de référence
   * @return la saison précédente ou null si pas de saison précédente valide
   */
  public Integer getPreviousSeason(int season) {
    if (season <= FIRST_SEASON) {
      log.debug("Pas de saison précédente pour {}", season);
      return null;
    }

    int previousSeason = season - 1;
    log.debug("Saison précédente de {}: {}", season, previousSeason);
    return previousSeason;
  }

  /**
   * Obtient la saison suivante
   *
   * @param season la saison de référence
   * @return la saison suivante ou null si dépasse la limite
   */
  public Integer getNextSeason(int season) {
    int nextSeason = season + 1;

    if (nextSeason > getMaxValidSeason()) {
      log.debug("Pas de saison suivante valide pour {}", season);
      return null;
    }

    log.debug("Saison suivante de {}: {}", season, nextSeason);
    return nextSeason;
  }

  /**
   * Obtient toutes les saisons valides
   *
   * @return liste des saisons valides
   */
  public List<Integer> getAllValidSeasons() {
    List<Integer> seasons =
        IntStream.rangeClosed(FIRST_SEASON, getMaxValidSeason()).boxed().toList();

    log.debug(
        "Saisons valides générées: {} saisons de {} à {}",
        seasons.size(),
        FIRST_SEASON,
        getMaxValidSeason());

    return seasons;
  }

  /**
   * Obtient les saisons disponibles (passées et courante)
   *
   * @return liste des saisons disponibles pour consultation
   */
  public List<Integer> getAvailableSeasons() {
    List<Integer> seasons =
        IntStream.rangeClosed(FIRST_SEASON, getCurrentSeason()).boxed().toList();

    log.debug(
        "Saisons disponibles: {} saisons de {} à {}",
        seasons.size(),
        FIRST_SEASON,
        getCurrentSeason());

    return seasons;
  }

  /**
   * Valide et normalise une saison
   *
   * @param season la saison à valider
   * @return la saison validée
   * @throws IllegalArgumentException si la saison est invalide
   */
  public int validateAndNormalizeSeason(Integer season) {
    if (season == null) {
      int currentSeason = getCurrentSeason();
      log.debug("Saison null normalisée vers la saison courante: {}", currentSeason);
      return currentSeason;
    }

    if (!isValidSeason(season)) {
      log.error(
          "Saison invalide fournie: {} (plage valide: {}-{})",
          season,
          FIRST_SEASON,
          getMaxValidSeason());
      throw new IllegalArgumentException(
          String.format(
              "Saison invalide: %d. Plage valide: %d-%d",
              season, FIRST_SEASON, getMaxValidSeason()));
    }

    log.debug("Saison validée: {}", season);
    return season;
  }

  /**
   * Calcule le nombre d'années entre deux saisons
   *
   * @param fromSeason saison de début
   * @param toSeason saison de fin
   * @return nombre d'années
   */
  public int getYearsBetweenSeasons(int fromSeason, int toSeason) {
    int years = Math.abs(toSeason - fromSeason);
    log.debug("Années entre saisons {} et {}: {}", fromSeason, toSeason, years);
    return years;
  }

  /**
   * Formate une saison pour l'affichage
   *
   * @param season la saison à formater
   * @return chaîne formatée
   */
  public String formatSeason(int season) {
    String formatted = "Saison " + season;
    log.trace("Saison formatée: {}", formatted);
    return formatted;
  }

  /**
   * Obtient des informations détaillées sur une saison
   *
   * @param season la saison
   * @return informations de la saison
   */
  public SeasonInfo getSeasonInfo(int season) {
    validateAndNormalizeSeason(season);

    SeasonInfo info =
        SeasonInfo.builder()
            .season(season)
            .isCurrent(isCurrentSeason(season))
            .isPast(isPastSeason(season))
            .isFuture(isFutureSeason(season))
            .previousSeason(getPreviousSeason(season))
            .nextSeason(getNextSeason(season))
            .formattedName(formatSeason(season))
            .build();

    log.debug("Informations de saison générées pour {}: {}", season, info);
    return info;
  }

  /** Classe d'information sur une saison */
  public static class SeasonInfo {
    private final int season;
    private final boolean isCurrent;
    private final boolean isPast;
    private final boolean isFuture;
    private final Integer previousSeason;
    private final Integer nextSeason;
    private final String formattedName;

    private SeasonInfo(Builder builder) {
      this.season = builder.season;
      this.isCurrent = builder.isCurrent;
      this.isPast = builder.isPast;
      this.isFuture = builder.isFuture;
      this.previousSeason = builder.previousSeason;
      this.nextSeason = builder.nextSeason;
      this.formattedName = builder.formattedName;
    }

    public static Builder builder() {
      return new Builder();
    }

    // Getters
    public int getSeason() {
      return season;
    }

    public boolean isCurrent() {
      return isCurrent;
    }

    public boolean isPast() {
      return isPast;
    }

    public boolean isFuture() {
      return isFuture;
    }

    public Integer getPreviousSeason() {
      return previousSeason;
    }

    public Integer getNextSeason() {
      return nextSeason;
    }

    public String getFormattedName() {
      return formattedName;
    }

    @Override
    public String toString() {
      return String.format(
          "SeasonInfo{season=%d, current=%s, past=%s, future=%s}",
          season, isCurrent, isPast, isFuture);
    }

    public static class Builder {
      private int season;
      private boolean isCurrent;
      private boolean isPast;
      private boolean isFuture;
      private Integer previousSeason;
      private Integer nextSeason;
      private String formattedName;

      public Builder season(int season) {
        this.season = season;
        return this;
      }

      public Builder isCurrent(boolean isCurrent) {
        this.isCurrent = isCurrent;
        return this;
      }

      public Builder isPast(boolean isPast) {
        this.isPast = isPast;
        return this;
      }

      public Builder isFuture(boolean isFuture) {
        this.isFuture = isFuture;
        return this;
      }

      public Builder previousSeason(Integer previousSeason) {
        this.previousSeason = previousSeason;
        return this;
      }

      public Builder nextSeason(Integer nextSeason) {
        this.nextSeason = nextSeason;
        return this;
      }

      public Builder formattedName(String formattedName) {
        this.formattedName = formattedName;
        return this;
      }

      public SeasonInfo build() {
        return new SeasonInfo(this);
      }
    }
  }
}
