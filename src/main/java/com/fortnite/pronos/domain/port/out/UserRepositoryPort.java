package com.fortnite.pronos.domain.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fortnite.pronos.model.User;

/**
 * Output port for User persistence operations. Implemented by the persistence adapter
 * (UserRepository).
 */
public interface UserRepositoryPort {

  Optional<User> findById(UUID id);

  Optional<User> findByEmail(String email);

  Optional<User> findByUsername(String username);

  Optional<User> findByUsernameIgnoreCase(String username);

  boolean existsByEmail(String email);

  boolean existsByUsername(String username);

  List<User> findByRole(User.UserRole role);

  List<User> findParticipantsWithoutTeam(int season);

  long countParticipants();

  List<User> findParticipantsWithTeam(int season);

  User save(User user);

  long count();

  List<User> findAll();

  void deleteAllInBatch();

  void softDelete(UUID userId, LocalDateTime deletedAt);
}
