package com.fortnite.pronos.service.seed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for seeding users during initialization. Extracted from DataInitializationService for SRP
 * compliance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserSeedService {

  private static final String ADMIN_USERNAME = "admin";
  private static final int CURRENT_SEASON = 2025;

  private final com.fortnite.pronos.repository.UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  /**
   * Creates default users if they don't exist.
   *
   * @return list of newly created users
   */
  public List<com.fortnite.pronos.model.User> createDefaultUsers() {
    List<com.fortnite.pronos.model.User> users = new ArrayList<>();

    if (!userRepository.existsByUsername(ADMIN_USERNAME)) {
      users.add(
          createUser(
              ADMIN_USERNAME,
              "admin@fortnite-pronos.com",
              com.fortnite.pronos.model.User.UserRole.ADMIN));
    }
    if (!userRepository.existsByUsername("Thibaut")) {
      users.add(
          createUser("Thibaut", "thibaut@test.com", com.fortnite.pronos.model.User.UserRole.USER));
    }
    if (!userRepository.existsByUsername("Teddy")) {
      users.add(
          createUser("Teddy", "teddy@test.com", com.fortnite.pronos.model.User.UserRole.USER));
    }
    if (!userRepository.existsByUsername("Marcel")) {
      users.add(
          createUser("Marcel", "marcel@test.com", com.fortnite.pronos.model.User.UserRole.USER));
    }

    return users;
  }

  /** Creates a user with specified parameters. */
  public com.fortnite.pronos.model.User createUser(
      String username, String email, com.fortnite.pronos.model.User.UserRole role) {
    com.fortnite.pronos.model.User user = new com.fortnite.pronos.model.User();
    user.setUsername(username);
    user.setEmail(email);
    user.setPassword(passwordEncoder.encode(generateSeedPassword(username)));
    user.setRole(role);
    user.setCurrentSeason(CURRENT_SEASON);
    return user;
  }

  private String generateSeedPassword(String username) {
    return "seed-" + username + "-" + UUID.randomUUID();
  }

  /**
   * Validates a user before saving.
   *
   * @param user the user to validate
   * @throws IllegalArgumentException if validation fails
   */
  public void validateUser(com.fortnite.pronos.model.User user) {
    if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
      throw new IllegalArgumentException("Username cannot be empty for user: " + user.getEmail());
    }
    if (user.getEmail() == null || !user.getEmail().contains("@")) {
      throw new IllegalArgumentException("Invalid email for user: " + user.getUsername());
    }
    if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
      throw new IllegalArgumentException(
          "Password cannot be empty for user: " + user.getUsername());
    }
  }

  /**
   * Validates a list of users.
   *
   * @param users the users to validate
   */
  public void validateUsers(List<com.fortnite.pronos.model.User> users) {
    for (com.fortnite.pronos.model.User user : users) {
      validateUser(user);
    }
  }

  /**
   * Saves users after validation.
   *
   * @param users the users to save
   * @return list of saved users
   */
  public List<com.fortnite.pronos.model.User> saveUsers(
      List<com.fortnite.pronos.model.User> users) {
    if (users.isEmpty()) {
      return new ArrayList<>();
    }

    validateUsers(users);
    List<com.fortnite.pronos.model.User> savedUsers = userRepository.saveAll(users);
    log.info("{} new users created and validated", savedUsers.size());
    return savedUsers;
  }

  /** Creates minimal test data as fallback. */
  public void createMinimalTestData() {
    log.info("Initializing minimal fallback data...");

    if (userRepository.count() == 0) {
      com.fortnite.pronos.model.User admin =
          createUser(
              ADMIN_USERNAME, "admin@test.com", com.fortnite.pronos.model.User.UserRole.ADMIN);
      com.fortnite.pronos.model.User testUser =
          createUser("testuser", "test@test.com", com.fortnite.pronos.model.User.UserRole.USER);

      validateUser(admin);
      validateUser(testUser);

      userRepository.saveAll(Arrays.asList(admin, testUser));
      log.info("Minimal data created (2 users)");
    }
  }

  /** Returns all users in the repository. */
  public List<com.fortnite.pronos.model.User> getAllUsers() {
    return userRepository.findAll();
  }
}
