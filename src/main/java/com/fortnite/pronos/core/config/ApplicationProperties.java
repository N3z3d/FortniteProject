package com.fortnite.pronos.core.config;

import java.time.Duration;

import jakarta.validation.constraints.*;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration centralisée de l'application. Suit les principes de configuration externalisée
 * selon les 12-factor apps.
 *
 * @author Fortnite Pronos Team
 * @version 1.0
 * @since 2025-06-09
 */
@ConfigurationProperties(prefix = "fortnite.pronos")
@Validated
public class ApplicationProperties {

  /** Configuration de sécurité. */
  private Security security = new Security();

  /** Configuration métier. */
  private Business business = new Business();

  /** Configuration de scraping. */
  private Scraping scraping = new Scraping();

  /** Configuration d'audit. */
  private Audit audit = new Audit();

  /** Configuration des limites système. */
  private Limits limits = new Limits();

  public static class Security {

    /** Secret pour la signature JWT. */
    @NotBlank(message = "Le secret JWT ne peut pas être vide")
    @Size(min = 32, message = "Le secret JWT doit faire au minimum 32 caractères")
    private String jwtSecret =
        "changeme-this-is-not-secure-in-production-please-use-environment-variable";

    /** Durée de validité du token JWT. */
    @NotNull(message = "La durée du token JWT ne peut pas être nulle") private Duration jwtExpiration = Duration.ofHours(24);

    /** Nombre maximum de tentatives de connexion. */
    @Min(value = 1, message = "Le nombre de tentatives doit être au minimum 1")
    @Max(value = 10, message = "Le nombre de tentatives doit être au maximum 10")
    private int maxLoginAttempts = 5;

    /** Durée de blocage après échec de connexion. */
    @NotNull(message = "La durée de blocage ne peut pas être nulle") private Duration lockoutDuration = Duration.ofMinutes(15);

    // Getters et Setters
    public String getJwtSecret() {
      return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
      this.jwtSecret = jwtSecret;
    }

    public Duration getJwtExpiration() {
      return jwtExpiration;
    }

    public void setJwtExpiration(Duration jwtExpiration) {
      this.jwtExpiration = jwtExpiration;
    }

    public int getMaxLoginAttempts() {
      return maxLoginAttempts;
    }

    public void setMaxLoginAttempts(int maxLoginAttempts) {
      this.maxLoginAttempts = maxLoginAttempts;
    }

    public Duration getLockoutDuration() {
      return lockoutDuration;
    }

    public void setLockoutDuration(Duration lockoutDuration) {
      this.lockoutDuration = lockoutDuration;
    }
  }

  public static class Business {

    /** Saison courante. */
    @Min(value = 1, message = "La saison doit être supérieure à 0")
    private int currentSeason = 5;

    /** Nombre maximum de joueurs par équipe. */
    @Min(value = 1, message = "Le nombre de joueurs par équipe doit être au minimum 1")
    @Max(value = 10, message = "Le nombre de joueurs par équipe doit être au maximum 10")
    private int maxPlayersPerTeam = 5;

    /** Nombre maximum d'échanges par saison. */
    @Min(value = 0, message = "Le nombre d'échanges ne peut pas être négatif")
    private int maxTradesPerSeason = 3;

    /** Délai pour accepter un échange. */
    @NotNull(message = "Le délai d'échange ne peut pas être nul") private Duration tradeAcceptanceTimeout = Duration.ofDays(7);

    // Getters et Setters
    public int getCurrentSeason() {
      return currentSeason;
    }

    public void setCurrentSeason(int currentSeason) {
      this.currentSeason = currentSeason;
    }

    public int getMaxPlayersPerTeam() {
      return maxPlayersPerTeam;
    }

    public void setMaxPlayersPerTeam(int maxPlayersPerTeam) {
      this.maxPlayersPerTeam = maxPlayersPerTeam;
    }

    public int getMaxTradesPerSeason() {
      return maxTradesPerSeason;
    }

    public void setMaxTradesPerSeason(int maxTradesPerSeason) {
      this.maxTradesPerSeason = maxTradesPerSeason;
    }

    public Duration getTradeAcceptanceTimeout() {
      return tradeAcceptanceTimeout;
    }

    public void setTradeAcceptanceTimeout(Duration tradeAcceptanceTimeout) {
      this.tradeAcceptanceTimeout = tradeAcceptanceTimeout;
    }
  }

  public static class Scraping {

    /** URL de base pour le scraping. */
    @NotBlank(message = "L'URL de scraping ne peut pas être vide")
    @Pattern(regexp = "^https?://.*", message = "L'URL doit commencer par http:// ou https://")
    private String baseUrl = "https://fortnitetracker.com";

    /** Fréquence de scraping. */
    @NotNull(message = "La fréquence de scraping ne peut pas être nulle") private Duration frequency = Duration.ofHours(1);

    /** Timeout pour les requêtes de scraping. */
    @NotNull(message = "Le timeout de scraping ne peut pas être nul") private Duration timeout = Duration.ofSeconds(30);

    /** Nombre de tentatives en cas d'échec. */
    @Min(value = 1, message = "Le nombre de tentatives doit être au minimum 1")
    @Max(value = 5, message = "Le nombre de tentatives doit être au maximum 5")
    private int retryAttempts = 3;

    // Getters et Setters
    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public Duration getFrequency() {
      return frequency;
    }

    public void setFrequency(Duration frequency) {
      this.frequency = frequency;
    }

    public Duration getTimeout() {
      return timeout;
    }

    public void setTimeout(Duration timeout) {
      this.timeout = timeout;
    }

    public int getRetryAttempts() {
      return retryAttempts;
    }

    public void setRetryAttempts(int retryAttempts) {
      this.retryAttempts = retryAttempts;
    }
  }

  public static class Audit {

    /** Activer l'audit des actions utilisateur. */
    private boolean enabled = true;

    /** Durée de rétention des logs d'audit. */
    @NotNull(message = "La durée de rétention ne peut pas être nulle") private Duration retentionPeriod = Duration.ofDays(90);

    /** Niveau de détail de l'audit. */
    @NotNull(message = "Le niveau d'audit ne peut pas être nul") private AuditLevel level = AuditLevel.STANDARD;

    public enum AuditLevel {
      MINIMAL,
      STANDARD,
      DETAILED,
      FULL
    }

    // Getters et Setters
    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public Duration getRetentionPeriod() {
      return retentionPeriod;
    }

    public void setRetentionPeriod(Duration retentionPeriod) {
      this.retentionPeriod = retentionPeriod;
    }

    public AuditLevel getLevel() {
      return level;
    }

    public void setLevel(AuditLevel level) {
      this.level = level;
    }
  }

  public static class Limits {

    /** Nombre maximum de requêtes par minute et par utilisateur. */
    @Min(value = 1, message = "La limite de requêtes doit être au minimum 1")
    private int requestsPerMinute = 60;

    /** Taille maximum des uploads en bytes. */
    @Min(value = 1024, message = "La taille d'upload doit être au minimum 1KB")
    private long maxUploadSize = 10 * 1024 * 1024; // 10MB

    /** Nombre maximum de résultats par page. */
    @Min(value = 1, message = "La taille de page doit être au minimum 1")
    @Max(value = 1000, message = "La taille de page doit être au maximum 1000")
    private int maxPageSize = 100;

    // Getters et Setters
    public int getRequestsPerMinute() {
      return requestsPerMinute;
    }

    public void setRequestsPerMinute(int requestsPerMinute) {
      this.requestsPerMinute = requestsPerMinute;
    }

    public long getMaxUploadSize() {
      return maxUploadSize;
    }

    public void setMaxUploadSize(long maxUploadSize) {
      this.maxUploadSize = maxUploadSize;
    }

    public int getMaxPageSize() {
      return maxPageSize;
    }

    public void setMaxPageSize(int maxPageSize) {
      this.maxPageSize = maxPageSize;
    }
  }

  // Getters pour les sections principales
  public Security getSecurity() {
    return security;
  }

  public void setSecurity(Security security) {
    this.security = security;
  }

  public Business getBusiness() {
    return business;
  }

  public void setBusiness(Business business) {
    this.business = business;
  }

  public Scraping getScraping() {
    return scraping;
  }

  public void setScraping(Scraping scraping) {
    this.scraping = scraping;
  }

  public Audit getAudit() {
    return audit;
  }

  public void setAudit(Audit audit) {
    this.audit = audit;
  }

  public Limits getLimits() {
    return limits;
  }

  public void setLimits(Limits limits) {
    this.limits = limits;
  }
}
