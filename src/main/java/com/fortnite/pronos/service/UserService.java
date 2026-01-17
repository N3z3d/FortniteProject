package com.fortnite.pronos.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public List<User> getAllUsers() {
    List<User> users = userRepository.findAll();
    log.debug("Returning {} users", users.size());
    return users;
  }

  @Transactional(readOnly = true)
  public Optional<User> findUserById(UUID id) {
    return userRepository.findById(id);
  }

  @Transactional(readOnly = true)
  public Optional<User> findUserByEmailOrUsername(String identifier) {
    return userRepository
        .findByEmail(identifier)
        .or(() -> userRepository.findByUsernameIgnoreCase(identifier));
  }

  @Transactional(readOnly = true)
  public Optional<User> findUserByUsername(String username) {
    return userRepository.findByUsernameIgnoreCase(username);
  }

  @Transactional
  public User saveUser(User user) {
    return userRepository.save(user);
  }
}
