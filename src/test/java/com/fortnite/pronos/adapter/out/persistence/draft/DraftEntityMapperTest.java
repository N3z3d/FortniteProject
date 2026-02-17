package com.fortnite.pronos.adapter.out.persistence.draft;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fortnite.pronos.domain.draft.model.Draft;
import com.fortnite.pronos.domain.draft.model.DraftStatus;
import com.fortnite.pronos.model.Game;

class DraftEntityMapperTest {

  private DraftEntityMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new DraftEntityMapper();
  }

  @Test
  void toDomainMapsScalarFields() {
    UUID draftId = UUID.randomUUID();
    UUID gameId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();

    Game game = new Game();
    game.setId(gameId);

    com.fortnite.pronos.model.Draft entity = new com.fortnite.pronos.model.Draft();
    entity.setId(draftId);
    entity.setGame(game);
    entity.setStatus(com.fortnite.pronos.model.Draft.Status.PAUSED);
    entity.setCurrentRound(3);
    entity.setCurrentPick(2);
    entity.setTotalRounds(9);
    entity.setCreatedAt(now.minusHours(1));
    entity.setUpdatedAt(now);
    entity.setStartedAt(now.minusMinutes(30));
    entity.setFinishedAt(null);

    Draft domain = mapper.toDomain(entity);

    assertThat(domain).isNotNull();
    assertThat(domain.getId()).isEqualTo(draftId);
    assertThat(domain.getGameId()).isEqualTo(gameId);
    assertThat(domain.getStatus()).isEqualTo(DraftStatus.PAUSED);
    assertThat(domain.getCurrentRound()).isEqualTo(3);
    assertThat(domain.getCurrentPick()).isEqualTo(2);
    assertThat(domain.getTotalRounds()).isEqualTo(9);
  }

  @Test
  void toEntityMapsScalarFields() {
    UUID draftId = UUID.randomUUID();
    UUID gameId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();
    Draft domain =
        Draft.restore(
            draftId,
            gameId,
            DraftStatus.ACTIVE,
            2,
            1,
            6,
            now.minusHours(1),
            now,
            now.minusMinutes(15),
            null);

    Game game = new Game();
    game.setId(gameId);

    com.fortnite.pronos.model.Draft entity = mapper.toEntity(domain, game);

    assertThat(entity).isNotNull();
    assertThat(entity.getId()).isEqualTo(draftId);
    assertThat(entity.getGame()).isEqualTo(game);
    assertThat(entity.getStatus()).isEqualTo(com.fortnite.pronos.model.Draft.Status.ACTIVE);
    assertThat(entity.getCurrentRound()).isEqualTo(2);
    assertThat(entity.getCurrentPick()).isEqualTo(1);
    assertThat(entity.getTotalRounds()).isEqualTo(6);
  }

  @Test
  void toDomainHandlesNullGameReference() {
    com.fortnite.pronos.model.Draft entity = new com.fortnite.pronos.model.Draft();
    entity.setId(UUID.randomUUID());
    entity.setGame(null);
    entity.setStatus(com.fortnite.pronos.model.Draft.Status.PENDING);

    Draft domain = mapper.toDomain(entity);

    assertThat(domain.getGameId()).isNull();
    assertThat(domain.getStatus()).isEqualTo(DraftStatus.PENDING);
  }

  @Test
  void enumMappingCoversAllValues() {
    for (com.fortnite.pronos.model.Draft.Status entityStatus :
        com.fortnite.pronos.model.Draft.Status.values()) {
      DraftStatus domainStatus = DraftStatus.valueOf(entityStatus.name());
      assertThat(mapper.toDomainStatus(entityStatus)).isEqualTo(domainStatus);
      assertThat(mapper.toEntityStatus(domainStatus)).isEqualTo(entityStatus);
    }
  }

  @Test
  void nullInputsReturnNull() {
    assertThat(mapper.toDomain(null)).isNull();
    assertThat(mapper.toEntity(null, null)).isNull();
  }
}
