package com.fortnite.pronos.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.fortnite.pronos.model.DraftParticipantTradeEntity;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("test")
class DraftParticipantTradeJpaRepositoryTest {

  @Autowired private DraftParticipantTradeJpaRepository repository;

  @Test
  void savesTradeWithAssignedDomainId() {
    UUID tradeId = UUID.randomUUID();
    DraftParticipantTradeEntity entity = buildEntity(tradeId);

    DraftParticipantTradeEntity saved = repository.saveAndFlush(entity);

    assertThat(saved.getId()).isEqualTo(tradeId);
    assertThat(repository.findById(tradeId)).isPresent();
  }

  @Test
  void updatesExistingTradeWithAssignedDomainId() {
    UUID tradeId = UUID.randomUUID();
    repository.saveAndFlush(buildEntity(tradeId));

    DraftParticipantTradeEntity existing = repository.findById(tradeId).orElseThrow();
    existing.setStatus("ACCEPTED");
    existing.setResolvedAt(LocalDateTime.now());

    repository.saveAndFlush(existing);

    DraftParticipantTradeEntity reloaded = repository.findById(tradeId).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo("ACCEPTED");
    assertThat(reloaded.getResolvedAt()).isNotNull();
  }

  private DraftParticipantTradeEntity buildEntity(UUID tradeId) {
    DraftParticipantTradeEntity entity = new DraftParticipantTradeEntity();
    entity.setId(tradeId);
    entity.setDraftId(UUID.randomUUID());
    entity.setProposerParticipantId(UUID.randomUUID());
    entity.setTargetParticipantId(UUID.randomUUID());
    entity.setPlayerFromProposerId(UUID.randomUUID());
    entity.setPlayerFromTargetId(UUID.randomUUID());
    entity.setStatus("PENDING");
    entity.setProposedAt(LocalDateTime.now());
    return entity;
  }
}
