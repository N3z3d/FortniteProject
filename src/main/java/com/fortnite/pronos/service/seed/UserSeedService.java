package com.fortnite.pronos.service.seed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.UserRepository;

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

  private static final String DEFAULT_PASSWORD = "password";
  private static final int CURRENT_SEASON = 2025;

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  /**
   * Creates default users if they don't exist.
   *
   * @return list of newly created users
   */
  public List<User> createDefaultUsers() {
    List<User> users = new ArrayList<>();

    if (!userRepository.existsByUsername("admin")) {
      users.add(createUser("admin", "admin@fortnite-pronos.com", User.UserRole.ADMIN));
    }
    if (!userRepository.existsByUsername("Thibaut")) {
      users.add(createUser("Thibaut", "thibaut@test.com", User.UserRole.USER));
    }
    if (!userRepository.existsByUsername("Teddy")) {
      users.add(createUser("Teddy", "teddy@test.com", User.UserRole.USER));
    }
    if (!userRepository.existsByUsername("Marcel")) {
      users.add(createUser("Marcel", "marcel@test.com", User.UserRole.USER));
    }

    return users;
  }

  /** Creates a user with specified parameters. */
  public User createUser(String username, String email, User.UserRole role) {
    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
    user.setRole(role);
    user.setCurrentSeason(CURRENT_SEASON);
    return user;
  }

  /**
   * Validates a user before saving.
   *
   * @param user the user to validate
   * @throws IllegalArgumentException if validation fails
   */
  public void validateUser(User user) {
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
  public void validateUsers(List<User> users) {
    for (User user : users) {
      validateUser(user);
    }
  }

  /**
   * Saves users after validation.
   *
   * @param users the users to save
   * @return list of saved users
   */
  public List<User> saveUsers(List<User> users) {
    if (users.isEmpty()) {
      return new ArrayList<>();
    }

    validateUsers(users);
    List<User> savedUsers = userRepository.saveAll(users);
    log.info("{} new users created and validated", savedUsers.size());
    return savedUsers;
  }

  /** Creates minimal test data as fallback. */
  public void createMinimalTestData() {
    log.info("Initializing minimal fallback data...");

    if (userRepository.count() == 0) {
      User admin = createUser("admin", "admin@test.com", User.UserRole.ADMIN);
      User testUser = createUser("testuser", "test@test.com", User.UserRole.USER);

      validateUser(admin);
      validateUser(testUser);

      userRepository.saveAll(Arrays.asList(admin, testUser));
      log.info("Minimal data created (2 users)");
    }
  }

  /** Returns all users in the repository. */
  public List<User> getAllUsers() {
    return userRepository.findAll();
  }
}
