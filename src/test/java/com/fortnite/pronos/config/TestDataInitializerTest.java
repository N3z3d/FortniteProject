package com.fortnite.pronos.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.model.*;
import com.fortnite.pronos.repository.*;
import com.fortnite.pronos.util.TestDataBuilderTest;

import lombok.RequiredArgsConstructor;

/** Initialise les données de test pour les tests d'intégration */
@Configuration
@Profile("test")
@RequiredArgsConstructor
public class TestDataInitializerTest {

  private static final Logger log = LoggerFactory.getLogger(TestDataInitializerTest.class);

  private final UserRepository userRepository;
  private final PlayerRepository playerRepository;
  private final TeamRepository teamRepository;
  private final GameRepository gameRepository;

  @Bean
  @Transactional
  public CommandLineRunner initTestData() {
    return args -> {
      log.info("Initialisation des données de test...");

      // Créer les utilisateurs
      User thibaut = userRepository.save(TestDataBuilderTest.createThibaut());
      User teddy = userRepository.save(TestDataBuilderTest.createTeddy());
      User marcel = userRepository.save(TestDataBuilderTest.createMarcel());
      User sarah = userRepository.save(TestDataBuilderTest.createSarah());

      // Créer quelques joueurs Fortnite
      Player aqua =
          playerRepository.save(TestDataBuilderTest.createValidPlayer("Aqua", Player.Region.EU));
      Player th0masHD =
          playerRepository.save(
              TestDataBuilderTest.createValidPlayer("Th0masHD", Player.Region.EU));
      Player rezon =
          playerRepository.save(TestDataBuilderTest.createValidPlayer("Rezon", Player.Region.EU));
      Player malibuca =
          playerRepository.save(
              TestDataBuilderTest.createValidPlayer("Malibuca", Player.Region.EU));

      // Créer les équipes pour les utilisateurs existants
      Team teamThibaut =
          teamRepository.save(TestDataBuilderTest.createValidTeam(thibaut, "Team Thibaut"));
      Team teamTeddy =
          teamRepository.save(TestDataBuilderTest.createValidTeam(teddy, "Team Teddy"));
      Team teamMarcel =
          teamRepository.save(TestDataBuilderTest.createValidTeam(marcel, "Team Marcel"));

      // Créer la game "Première Saison"
      Game premiereSaison = TestDataBuilderTest.createValidGame(marcel, "Première Saison");
      premiereSaison.setStatus(GameStatus.ACTIVE);
      gameRepository.save(premiereSaison);

      log.info("Données de test initialisées avec succès");
      log.info("- 4 utilisateurs créés");
      log.info("- 4 joueurs Fortnite créés");
      log.info("- 3 équipes créées");
      log.info("- 1 game active créée");
    };
  }
}
