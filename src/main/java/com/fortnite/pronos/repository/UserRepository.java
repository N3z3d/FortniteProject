package com.fortnite.pronos.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, UserRepositoryPort {
  Optional<User> findByEmail(String email);

  Optional<User> findByUsername(String username);

  Optional<User> findByUsernameIgnoreCase(String username);

  boolean existsByEmail(String email);

  boolean existsByUsername(String username);

  List<User> findByRole(User.UserRole role);

  @Query(
      "SELECT u FROM User u WHERE u.role = 'USER' AND NOT EXISTS "
          + "(SELECT t FROM Team t WHERE t.owner = u AND t.season = ?1)")
  List<User> findParticipantsWithoutTeam(int season);

  @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'USER'")
  long countParticipants();

  @Query(
      "SELECT u FROM User u WHERE u.role = 'USER' AND EXISTS "
          + "(SELECT t FROM Team t WHERE t.owner = u AND t.season = ?1)")
  List<User> findParticipantsWithTeam(int season);

  @Modifying
  @Transactional
  @Query("UPDATE User u SET u.deletedAt = :deletedAt WHERE u.id = :id")
  void softDeleteById(@Param("id") UUID id, @Param("deletedAt") LocalDateTime deletedAt);

  @Override
  default void softDelete(UUID userId, LocalDateTime deletedAt) {
    softDeleteById(userId, deletedAt);
  }
}
