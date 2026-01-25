package com.fortnite.pronos.service.seed;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing reference users during seeding. Extracted from ReferenceGameSeedService for
 * SRP compliance and reduced coupling.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReferenceUserSeedService {

  private static final String DEFAULT_PASSWORD = "password";
  private static final int CURRENT_SEASON = 2025;

  private final UserRepositoryPort userRepository;
  private final PasswordEncoder passwordEncoder;

  /**
   * Ensures reference users exist (Thibaut, Teddy, Marcel).
   *
   * @return map of username to User
   */
  public Map<String, User> ensureReferenceUsers() {
    Map<String, User> users = new LinkedHashMap<>();
    users.put("Thibaut", ensureUser("Thibaut", "thibaut@test.com"));
    users.put("Teddy", ensureUser("Teddy", "teddy@test.com"));
    users.put("Marcel", ensureUser("Marcel", "marcel@test.com"));
    log.info("Reference users ensured: Thibaut, Teddy, Marcel");
    return users;
  }

  /**
   * Ensures a user exists, creating if necessary.
   *
   * @param username the username
   * @param email the email
   * @return the user (existing or newly created)
   */
  public User ensureUser(String username, String email) {
    return userRepository
        .findByUsernameIgnoreCase(username)
        .orElseGet(() -> createUser(username, email));
  }

  private User createUser(String username, String email) {
    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
    user.setRole(User.UserRole.USER);
    user.setCurrentSeason(CURRENT_SEASON);
    return userRepository.save(user);
  }
}
