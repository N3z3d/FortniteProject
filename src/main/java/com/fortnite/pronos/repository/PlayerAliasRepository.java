package com.fortnite.pronos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fortnite.pronos.model.PlayerAlias;
import com.fortnite.pronos.model.enums.AliasStatus;

@Repository
public interface PlayerAliasRepository extends JpaRepository<PlayerAlias, UUID> {
  List<PlayerAlias> findByStatus(AliasStatus status);

  Optional<PlayerAlias> findFirstByPlayer_IdAndCurrentTrue(UUID playerId);

  boolean existsByNicknameAndCurrentTrue(String nickname);
}
