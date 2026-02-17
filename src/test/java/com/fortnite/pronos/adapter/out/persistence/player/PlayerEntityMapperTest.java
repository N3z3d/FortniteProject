package com.fortnite.pronos.adapter.out.persistence.player;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.model.Player;

class PlayerEntityMapperTest {

  private PlayerEntityMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new PlayerEntityMapper();
  }

  @Nested
  class ToDomain {

    @Test
    void mapsAllFieldsCorrectly() {
      UUID id = UUID.randomUUID();
      Player entity =
          buildEntity(id, "FN123", "user1", "nick1", Player.Region.EU, "1-7", 2025, true);

      com.fortnite.pronos.domain.player.model.Player domain = mapper.toDomain(entity);

      assertThat(domain).isNotNull();
      assertThat(domain.getId()).isEqualTo(id);
      assertThat(domain.getFortniteId()).isEqualTo("FN123");
      assertThat(domain.getUsername()).isEqualTo("user1");
      assertThat(domain.getNickname()).isEqualTo("nick1");
      assertThat(domain.getRegion()).isEqualTo(PlayerRegion.EU);
      assertThat(domain.getTranche()).isEqualTo("1-7");
      assertThat(domain.getCurrentSeason()).isEqualTo(2025);
      assertThat(domain.isLocked()).isTrue();
    }

    @Test
    void returnsNullForNullEntity() {
      assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void handlesNullRegion() {
      UUID id = UUID.randomUUID();
      Player entity = buildEntity(id, null, "user1", "nick1", null, "1-7", 2025, false);

      com.fortnite.pronos.domain.player.model.Player domain = mapper.toDomain(entity);

      assertThat(domain).isNotNull();
      assertThat(domain.getRegion()).isNull();
    }

    @Test
    void handlesNullFortniteId() {
      UUID id = UUID.randomUUID();
      Player entity =
          buildEntity(id, null, "user1", "nick1", Player.Region.NAW, "1-7", 2025, false);

      com.fortnite.pronos.domain.player.model.Player domain = mapper.toDomain(entity);

      assertThat(domain.getFortniteId()).isNull();
    }

    @Test
    void defaultsNullCurrentSeasonTo2025() {
      UUID id = UUID.randomUUID();
      Player entity = buildEntity(id, null, "user1", "nick1", Player.Region.EU, "1-7", null, false);

      com.fortnite.pronos.domain.player.model.Player domain = mapper.toDomain(entity);

      assertThat(domain.getCurrentSeason()).isEqualTo(2025);
    }

    @Test
    void defaultsNullLockedToFalse() {
      UUID id = UUID.randomUUID();
      Player entity = buildEntity(id, null, "user1", "nick1", Player.Region.EU, "1-7", 2025, null);

      com.fortnite.pronos.domain.player.model.Player domain = mapper.toDomain(entity);

      assertThat(domain.isLocked()).isFalse();
    }
  }

  @Nested
  class ToEntity {

    @Test
    void mapsAllFieldsCorrectly() {
      UUID id = UUID.randomUUID();
      com.fortnite.pronos.domain.player.model.Player domain =
          com.fortnite.pronos.domain.player.model.Player.restore(
              id, "FN456", "user2", "nick2", PlayerRegion.NAW, "1-10", 2024, true);

      Player entity = mapper.toEntity(domain);

      assertThat(entity).isNotNull();
      assertThat(entity.getId()).isEqualTo(id);
      assertThat(entity.getFortniteId()).isEqualTo("FN456");
      assertThat(entity.getUsername()).isEqualTo("user2");
      assertThat(entity.getNickname()).isEqualTo("nick2");
      assertThat(entity.getRegion()).isEqualTo(Player.Region.NAW);
      assertThat(entity.getTranche()).isEqualTo("1-10");
      assertThat(entity.getCurrentSeason()).isEqualTo(2024);
      assertThat(entity.isLocked()).isTrue();
    }

    @Test
    void returnsNullForNullDomain() {
      assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void handlesNullRegionInDomain() {
      UUID id = UUID.randomUUID();
      com.fortnite.pronos.domain.player.model.Player domain =
          com.fortnite.pronos.domain.player.model.Player.restore(
              id, null, "user", "nick", null, "1-7", 2025, false);

      Player entity = mapper.toEntity(domain);

      assertThat(entity.getRegion()).isNull();
    }
  }

  @Nested
  class EnumMapping {

    @Test
    void mapsAllPlayerRegionValuesToDomain() {
      for (Player.Region entityRegion : Player.Region.values()) {
        PlayerRegion domainRegion = mapper.toDomainRegion(entityRegion);
        assertThat(domainRegion).isNotNull();
        assertThat(domainRegion.name()).isEqualTo(entityRegion.name());
      }
    }

    @Test
    void mapsAllPlayerRegionValuesToEntity() {
      for (PlayerRegion domainRegion : PlayerRegion.values()) {
        Player.Region entityRegion = mapper.toEntityRegion(domainRegion);
        assertThat(entityRegion).isNotNull();
        assertThat(entityRegion.name()).isEqualTo(domainRegion.name());
      }
    }

    @Test
    void toDomainRegionReturnsNullForNull() {
      assertThat(mapper.toDomainRegion(null)).isNull();
    }

    @Test
    void toEntityRegionReturnsNullForNull() {
      assertThat(mapper.toEntityRegion(null)).isNull();
    }
  }

  @Nested
  class ToDomainList {

    @Test
    void mapsMultipleEntities() {
      UUID id1 = UUID.randomUUID();
      UUID id2 = UUID.randomUUID();
      Player e1 = buildEntity(id1, null, "user1", "nick1", Player.Region.EU, "1-7", 2025, false);
      Player e2 = buildEntity(id2, "FN2", "user2", "nick2", Player.Region.NAW, "1-10", 2024, true);

      List<com.fortnite.pronos.domain.player.model.Player> result =
          mapper.toDomainList(List.of(e1, e2));

      assertThat(result).hasSize(2);
      assertThat(result.get(0).getId()).isEqualTo(id1);
      assertThat(result.get(1).getId()).isEqualTo(id2);
    }

    @Test
    void returnsEmptyForNullList() {
      assertThat(mapper.toDomainList(null)).isEmpty();
    }

    @Test
    void returnsEmptyForEmptyList() {
      assertThat(mapper.toDomainList(List.of())).isEmpty();
    }
  }

  @Nested
  class RoundTrip {

    @Test
    void toDomainOfToEntityPreservesAllFields() {
      UUID id = UUID.randomUUID();
      com.fortnite.pronos.domain.player.model.Player original =
          com.fortnite.pronos.domain.player.model.Player.restore(
              id, "FN789", "user3", "nick3", PlayerRegion.ASIA, "NOUVEAU", 2023, true);

      Player entity = mapper.toEntity(original);
      com.fortnite.pronos.domain.player.model.Player roundTripped = mapper.toDomain(entity);

      assertThat(roundTripped.getId()).isEqualTo(original.getId());
      assertThat(roundTripped.getFortniteId()).isEqualTo(original.getFortniteId());
      assertThat(roundTripped.getUsername()).isEqualTo(original.getUsername());
      assertThat(roundTripped.getNickname()).isEqualTo(original.getNickname());
      assertThat(roundTripped.getRegion()).isEqualTo(original.getRegion());
      assertThat(roundTripped.getTranche()).isEqualTo(original.getTranche());
      assertThat(roundTripped.getCurrentSeason()).isEqualTo(original.getCurrentSeason());
      assertThat(roundTripped.isLocked()).isEqualTo(original.isLocked());
    }

    @Test
    void toEntityOfToDomainPreservesAllFields() {
      UUID id = UUID.randomUUID();
      Player original =
          buildEntity(id, "FN000", "user4", "nick4", Player.Region.ME, "1-7", 2025, false);

      com.fortnite.pronos.domain.player.model.Player domain = mapper.toDomain(original);
      Player roundTripped = mapper.toEntity(domain);

      assertThat(roundTripped.getId()).isEqualTo(original.getId());
      assertThat(roundTripped.getFortniteId()).isEqualTo(original.getFortniteId());
      assertThat(roundTripped.getUsername()).isEqualTo(original.getUsername());
      assertThat(roundTripped.getNickname()).isEqualTo(original.getNickname());
      assertThat(roundTripped.getRegion()).isEqualTo(original.getRegion());
      assertThat(roundTripped.getTranche()).isEqualTo(original.getTranche());
      assertThat(roundTripped.getCurrentSeason()).isEqualTo(original.getCurrentSeason());
      assertThat(roundTripped.isLocked()).isEqualTo(original.isLocked());
    }
  }

  // ===============================
  // HELPERS
  // ===============================

  private Player buildEntity(
      UUID id,
      String fortniteId,
      String username,
      String nickname,
      Player.Region region,
      String tranche,
      Integer currentSeason,
      Boolean locked) {
    Player entity = new Player();
    entity.setId(id);
    entity.setFortniteId(fortniteId);
    entity.setUsername(username);
    entity.setNickname(nickname);
    if (region != null) {
      entity.setRegion(region);
    }
    entity.setTranche(tranche);
    entity.setCurrentSeason(currentSeason);
    entity.setLocked(locked != null && locked);
    return entity;
  }
}
