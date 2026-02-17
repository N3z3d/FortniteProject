package com.fortnite.pronos.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fortnite.pronos.model.Player;

/**
 * Output port for Player persistence operations. Implemented by the persistence adapter
 * (PlayerRepository).
 *
 * <p>Note: Methods using Spring-specific types (Page, Pageable) are not included here to maintain
 * domain purity. These methods are accessed directly through the repository interface.
 */
public interface PlayerRepositoryPort {

  List<Player> findByRegion(Player.Region region);

  List<Player> findByTranche(String tranche);

  List<Player> findActivePlayers();

  Optional<Player> findById(UUID id);
}
