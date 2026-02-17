package com.fortnite.pronos.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.domain.player.model.Player;

/**
 * Output port for Player domain persistence operations. Implemented by the persistence adapter.
 *
 * <p>This port coexists with the legacy {@link PlayerRepositoryPort} during migration.
 */
public interface PlayerDomainRepositoryPort {

  Optional<Player> findById(UUID id);

  Player save(Player player);

  List<Player> findByRegion(PlayerRegion region);

  List<Player> findByTranche(String tranche);

  List<Player> findActivePlayers();

  Optional<Player> findByNickname(String nickname);

  Optional<Player> findByUsername(String username);

  boolean existsByNickname(String nickname);

  long countByRegion(PlayerRegion region);

  List<Player> findAll();

  long count();
}
