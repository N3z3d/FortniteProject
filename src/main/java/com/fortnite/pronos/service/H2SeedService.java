package com.fortnite.pronos.service;

import java.util.Arrays;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service de seed minimal pour le profil H2. Crée des utilisateurs de test pour permettre
 * l'authentification.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class H2SeedService {

  private final UserRepository userRepository;
  private final Environment environment;

  @EventListener(ApplicationReadyEvent.class)
  @Order(1)
  @Transactional
  public void seedTestUsers() {
    if (!isH2Profile()) {
      return;
    }

    log.info("H2 Seed: creating test users for H2 profile");

    createUserIfNotExists("thibaut", "thibaut@test.com", User.UserRole.ADMIN);
    createUserIfNotExists("teddy", "teddy@test.com", User.UserRole.USER);
    createUserIfNotExists("marcel", "marcel@test.com", User.UserRole.USER);
    createUserIfNotExists("sarah", "sarah@test.com", User.UserRole.USER);

    log.info("H2 Seed: completed - {} users in database", userRepository.count());
  }

  private boolean isH2Profile() {
    return Arrays.asList(environment.getActiveProfiles()).contains("h2");
  }

  private void createUserIfNotExists(String username, String email, User.UserRole role) {
    if (userRepository.findByUsername(username).isPresent()) {
      log.debug("H2 Seed: user {} already exists", username);
      return;
    }

    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setPassword("$2a$10$DummyHashForTestUsers");
    user.setRole(role);

    userRepository.save(user);
    log.info("H2 Seed: created user {} ({})", username, role);
  }
}
