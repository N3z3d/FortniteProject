package com.fortnite.pronos.config;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuration automatique de la base de données avec détection intelligente
 *
 * <p>PRINCIPE UX : ZERO-CONFIGURATION PAR DÉFAUT - Détecte automatiquement si PostgreSQL est
 * disponible - Fallback automatique vers H2 si PostgreSQL indisponible - Messages UX clairs pour
 * informer l'utilisateur - Aucune intervention manuelle requise
 */
@Slf4j
@Configuration
public class DatabaseAutoConfiguration implements ApplicationListener<ApplicationReadyEvent> {

  private final DataSource dataSource;

  public DatabaseAutoConfiguration(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    detectAndReportDatabaseStatus();
  }

  /** Détecte le type de base de données utilisé et affiche des messages UX appropriés */
  private void detectAndReportDatabaseStatus() {
    try (Connection connection = dataSource.getConnection()) {
      String databaseType = connection.getMetaData().getDatabaseProductName();
      String jdbcUrl = connection.getMetaData().getURL();

      displayDatabaseStatus(databaseType, jdbcUrl);
      displayUserGuidance(databaseType);

    } catch (SQLException e) {
      log.error("❌ Erreur lors de la détection de la base de données: {}", e.getMessage());
      displayDatabaseError(e);
    }
  }

  /** Affiche le statut de la base de données de manière UX-friendly */
  private void displayDatabaseStatus(String databaseType, String jdbcUrl) {
    log.info("🔥 ════════════════════════════════════════════════════════════");
    log.info("🚀   FORTNITE PRONOS - STATUT BASE DE DONNÉES");
    log.info("🔥 ════════════════════════════════════════════════════════════");

    if (databaseType.contains("H2")) {
      log.info("BASE DE DONNEES: H2 (profil test)");
      log.info("Donnees en memoire: reinitialisees a chaque demarrage");
      log.info("Utilisez le profil dev pour PostgreSQL persistante");

    } else if (databaseType.contains("PostgreSQL")) {
      log.info("🐘 BASE DE DONNÉES: PostgreSQL (Mode Production)");
      log.info("🔗 URL: {}", jdbcUrl);
      log.info("✅ Configuration PostgreSQL détectée et fonctionnelle");

    } else {
      log.info("📊 BASE DE DONNÉES: {} (Mode personnalisé)", databaseType);
      log.info("🔗 URL: {}", jdbcUrl);
    }

    log.info("🔥 ════════════════════════════════════════════════════════════");
  }

  /** Affiche des conseils UX selon le type de base de données */
  private void displayUserGuidance(String databaseType) {
    if (databaseType.contains("H2")) {
      log.info("");
      log.info("MODE TEST H2");
      log.info("  Donnees temporaires en memoire");
      log.info("  Pour PostgreSQL: spring.profiles.active=dev");
      log.info("");

    } else if (databaseType.contains("PostgreSQL")) {
      log.info("");
      log.info("MODE DEV/PROD PostgreSQL");
      log.info("  Donnees persistantes entre redemarrages");
      log.info("  Migrations Flyway actives");
      log.info("");
    }
  }

  /** Affiche des messages d'erreur UX-friendly avec solutions */
  private void displayDatabaseError(SQLException e) {
    log.error("DATABASE CONNECTION ERROR");
    log.error("Error: {}", e.getMessage());
    log.error("");
    log.error("RECOMMENDED STEPS:");
    log.error("  1) Ensure PostgreSQL is running");
    log.error("  2) Verify database name, user, and password");
    log.error("  3) For tests only: use spring.profiles.active=test");
  }
}
