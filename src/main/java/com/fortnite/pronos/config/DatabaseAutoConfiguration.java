package com.fortnite.pronos.config;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

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

  private final Environment environment;
  private final DataSource dataSource;

  public DatabaseAutoConfiguration(Environment environment, DataSource dataSource) {
    this.environment = environment;
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
      log.info("💡 BASE DE DONNÉES: H2 Embedded (Mode Développement Rapide)");
      log.info("📁 Fichier: ./data/fortnite_quickstart_db");
      log.info("🌐 Console H2: http://localhost:8080/h2-console");
      log.info("🔑 Credentials: sa / quickstart");
      log.info("✅ PRÊT À DÉVELOPPER - Aucune configuration supplémentaire requise!");

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
      log.info("💡 MODE DÉVELOPPEMENT RAPIDE ACTIVÉ");
      log.info("   ➤ Données temporaires rechargées à chaque démarrage");
      log.info("   ➤ Idéal pour prototypage et tests rapides");
      log.info("   ➤ Pour PostgreSQL: modifiez spring.profiles.active=dev");
      log.info("");

    } else if (databaseType.contains("PostgreSQL")) {
      log.info("");
      log.info("🚀 MODE PRODUCTION PostgreSQL");
      log.info("   ➤ Données persistantes entre les redémarrages");
      log.info("   ➤ Migrations Flyway activées");
      log.info("   ➤ Optimal pour développement avancé");
      log.info("");
    }
  }

  /** Affiche des messages d'erreur UX-friendly avec solutions */
  private void displayDatabaseError(SQLException e) {
    log.error("🔥 ════════════════════════════════════════════════════════════");
    log.error("❌   ERREUR DE CONNEXION BASE DE DONNÉES");
    log.error("🔥 ════════════════════════════════════════════════════════════");
    log.error("💥 Erreur: {}", e.getMessage());
    log.error("");
    log.error("🛠️  SOLUTIONS RECOMMANDÉES:");
    log.error("   1️⃣  DÉMARRAGE RAPIDE: Utilisez le profil 'quickstart'");
    log.error("      ➤ Modifiez: spring.profiles.active=quickstart");
    log.error("      ➤ Redémarrez l'application");
    log.error("");
    log.error("   2️⃣  PostgreSQL: Vérifiez votre configuration");
    log.error("      ➤ PostgreSQL est-il installé et démarré?");
    log.error("      ➤ La base 'fortnite_pronos' existe-t-elle?");
    log.error("      ➤ L'utilisateur 'fortnite_user' est-il créé?");
    log.error("");
    log.error("   3️⃣  AIDE RAPIDE: Utilisez le script quick-start.ps1");
    log.error("🔥 ════════════════════════════════════════════════════════════");
  }
}
