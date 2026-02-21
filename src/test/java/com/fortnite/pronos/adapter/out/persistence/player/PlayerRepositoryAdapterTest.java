package com.fortnite.pronos.adapter.out.persistence.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.repository.CrudRepository;

import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.repository.PlayerRepository;

@ExtendWith(MockitoExtension.class)
class PlayerRepositoryAdapterTest {

  @Mock private PlayerRepository playerRepository;

  private PlayerRepositoryAdapter adapter;
  private CrudRepository<Player, UUID> playerCrudRepository;

  @BeforeEach
  void setUp() {
    adapter = new PlayerRepositoryAdapter(playerRepository, new PlayerEntityMapper());
    playerCrudRepository = playerRepository;
  }

  @Test
  void findByIdReturnsEmptyWhenMissing() {
    UUID id = UUID.randomUUID();
    when(playerCrudRepository.findById(id)).thenReturn(Optional.empty());

    Optional<com.fortnite.pronos.domain.player.model.Player> result = adapter.findById(id);

    assertThat(result).isEmpty();
  }

  @Test
  void findByIdReturnsMappedDomainPlayer() {
    UUID id = UUID.randomUUID();
    Player entity = buildEntity(id, "user1", "nick1", Player.Region.EU, "1-7");
    when(playerCrudRepository.findById(id)).thenReturn(Optional.of(entity));

    Optional<com.fortnite.pronos.domain.player.model.Player> result = adapter.findById(id);

    assertThat(result).isPresent();
    com.fortnite.pronos.domain.player.model.Player player = result.orElseThrow();
    assertThat(player.getId()).isEqualTo(id);
    assertThat(player.getUsername()).isEqualTo("user1");
  }

  @Test
  void saveMapAndReturnsRoundTrippedPlayer() {
    UUID id = UUID.randomUUID();
    com.fortnite.pronos.domain.player.model.Player domainPlayer =
        com.fortnite.pronos.domain.player.model.Player.restore(
            id, "FN1", "user1", "nick1", PlayerRegion.EU, "1-7", 2025, false);
    Player savedEntity = buildEntity(id, "user1", "nick1", Player.Region.EU, "1-7");
    when(playerCrudRepository.save(any(Player.class))).thenReturn(savedEntity);

    com.fortnite.pronos.domain.player.model.Player result = adapter.save(domainPlayer);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(id);
    verify(playerCrudRepository).save(any(Player.class));
  }

  @Test
  void saveRejectsNullPlayer() {
    assertThatNullPointerException().isThrownBy(() -> adapter.save(null));
  }

  @Test
  void findByRegionReturnsMappedList() {
    UUID id = UUID.randomUUID();
    Player entity = buildEntity(id, "user1", "nick1", Player.Region.EU, "1-7");
    when(playerRepository.findByRegion(Player.Region.EU)).thenReturn(List.of(entity));

    List<com.fortnite.pronos.domain.player.model.Player> result =
        adapter.findByRegion(PlayerRegion.EU);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo(id);
  }

  @Test
  void findByRegionReturnsEmptyForNull() {
    assertThat(adapter.findByRegion(null)).isEmpty();
  }

  @Test
  void findByTrancheReturnsMappedList() {
    UUID id = UUID.randomUUID();
    Player entity = buildEntity(id, "user1", "nick1", Player.Region.NAW, "NOUVEAU");
    when(playerRepository.findByTranche("NOUVEAU")).thenReturn(List.of(entity));

    List<com.fortnite.pronos.domain.player.model.Player> result = adapter.findByTranche("NOUVEAU");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTranche()).isEqualTo("NOUVEAU");
  }

  @Test
  void findByTrancheReturnsEmptyForNull() {
    assertThat(adapter.findByTranche(null)).isEmpty();
  }

  @Test
  void findActivePlayersReturnsMappedList() {
    UUID id = UUID.randomUUID();
    Player entity = buildEntity(id, "user1", "nick1", Player.Region.EU, "1-7");
    when(playerRepository.findActivePlayers()).thenReturn(List.of(entity));

    List<com.fortnite.pronos.domain.player.model.Player> result = adapter.findActivePlayers();

    assertThat(result).hasSize(1);
  }

  @Test
  void findByNicknameReturnsMappedPlayer() {
    UUID id = UUID.randomUUID();
    Player entity = buildEntity(id, "user1", "nick1", Player.Region.EU, "1-7");
    when(playerRepository.findByNickname("nick1")).thenReturn(Optional.of(entity));

    Optional<com.fortnite.pronos.domain.player.model.Player> result =
        adapter.findByNickname("nick1");

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().getNickname()).isEqualTo("nick1");
  }

  @Test
  void findByNicknameReturnsEmptyForNull() {
    assertThat(adapter.findByNickname(null)).isEmpty();
  }

  @Test
  void findByUsernameReturnsMappedPlayer() {
    UUID id = UUID.randomUUID();
    Player entity = buildEntity(id, "user1", "nick1", Player.Region.EU, "1-7");
    when(playerRepository.findByUsername("user1")).thenReturn(Optional.of(entity));

    Optional<com.fortnite.pronos.domain.player.model.Player> result =
        adapter.findByUsername("user1");

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().getUsername()).isEqualTo("user1");
  }

  @Test
  void findByUsernameReturnsEmptyForNull() {
    assertThat(adapter.findByUsername(null)).isEmpty();
  }

  @Test
  void existsByNicknameReturnsTrue() {
    when(playerRepository.existsByNickname("nick1")).thenReturn(true);

    assertThat(adapter.existsByNickname("nick1")).isTrue();
  }

  @Test
  void existsByNicknameReturnsFalseForNull() {
    assertThat(adapter.existsByNickname(null)).isFalse();
  }

  @Test
  void countByRegionDelegatesToRepository() {
    when(playerRepository.countByRegion(Player.Region.EU)).thenReturn(5L);

    assertThat(adapter.countByRegion(PlayerRegion.EU)).isEqualTo(5L);
  }

  @Test
  void countByRegionReturnsZeroForNull() {
    assertThat(adapter.countByRegion(null)).isZero();
  }

  @Test
  void findAllReturnsMappedList() {
    UUID id = UUID.randomUUID();
    Player entity = buildEntity(id, "user1", "nick1", Player.Region.EU, "1-7");
    when(playerRepository.findAll()).thenReturn(List.of(entity));

    List<com.fortnite.pronos.domain.player.model.Player> result = adapter.findAll();

    assertThat(result).hasSize(1);
  }

  @Test
  void countDelegatesToRepository() {
    when(playerRepository.count()).thenReturn(42L);

    assertThat(adapter.count()).isEqualTo(42L);
  }

  // ===============================
  // HELPERS
  // ===============================

  private Player buildEntity(
      UUID id, String username, String nickname, Player.Region region, String tranche) {
    Player entity = new Player();
    entity.setId(id);
    entity.setUsername(username);
    entity.setNickname(nickname);
    entity.setRegion(region);
    entity.setTranche(tranche);
    entity.setCurrentSeason(2025);
    entity.setLocked(false);
    return entity;
  }
}
