package com.fortnite.pronos.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.model.*;
import com.fortnite.pronos.repository.*;
import com.fortnite.pronos.util.TestDataBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Initialise les données de test pour les tests d'intégration */
@Slf4j
@Configuration
@Profile("test")
@RequiredArgsConstructor
public class TestDataInitializer {

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
      User thibaut = userRepository.save(TestDataBuilder.createThibaut());
      User teddy = userRepository.save(TestDataBuilder.createTeddy());
      User marcel = userRepository.save(TestDataBuilder.createMarcel());
      User sarah = userRepository.save(TestDataBuilder.createSarah());

      // Créer quelques joueurs Fortnite
      Player aqua =
          playerRepository.save(TestDataBuilder.createValidPlayer("Aqua", Player.Region.EU));
      Player th0masHD =
          playerRepository.save(TestDataBuilder.createValidPlayer("Th0masHD", Player.Region.EU));
      Player rezon =
          playerRepository.save(TestDataBuilder.createValidPlayer("Rezon", Player.Region.EU));
      Player malibuca =
          playerRepository.save(TestDataBuilder.createValidPlayer("Malibuca", Player.Region.EU));

      // Créer les équipes pour les utilisateurs existants
      Team teamThibaut =
          teamRepository.save(TestDataBuilder.createValidTeam(thibaut, "Team Thibaut"));
      Team teamTeddy = teamRepository.save(TestDataBuilder.createValidTeam(teddy, "Team Teddy"));
      Team teamMarcel = teamRepository.save(TestDataBuilder.createValidTeam(marcel, "Team Marcel"));

      // Créer la game "Première Saison"
      Game premiereSaison = TestDataBuilder.createValidGame(marcel, "Première Saison");
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
