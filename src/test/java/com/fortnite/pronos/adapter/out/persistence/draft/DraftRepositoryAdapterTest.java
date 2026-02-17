package com.fortnite.pronos.adapter.out.persistence.draft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.repository.CrudRepository;

import com.fortnite.pronos.domain.draft.model.Draft;
import com.fortnite.pronos.domain.draft.model.DraftStatus;
import com.fortnite.pronos.domain.port.out.DraftRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.model.Game;

@ExtendWith(MockitoExtension.class)
class DraftRepositoryAdapterTest {

  private DraftRepositoryPort draftRepository;
  private GameRepositoryPort gameRepository;

  private DraftRepositoryAdapter adapter;
  private CrudRepository<com.fortnite.pronos.model.Draft, UUID> draftCrudRepository;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    draftRepository =
        mock(DraftRepositoryPort.class, withSettings().extraInterfaces(CrudRepository.class));
    gameRepository = mock(GameRepositoryPort.class);
    adapter = new DraftRepositoryAdapter(draftRepository, gameRepository, new DraftEntityMapper());
    draftCrudRepository = (CrudRepository<com.fortnite.pronos.model.Draft, UUID>) draftRepository;
  }

  @Test
  void findByIdReturnsEmptyWhenNotFound() {
    UUID draftId = UUID.randomUUID();
    when(draftCrudRepository.findById(draftId)).thenReturn(Optional.empty());

    Optional<Draft> result = adapter.findById(draftId);

    assertThat(result).isEmpty();
  }

  @Test
  void findByIdReturnsMappedDomain() {
    UUID draftId = UUID.randomUUID();
    UUID gameId = UUID.randomUUID();
    com.fortnite.pronos.model.Draft entity = buildEntityDraft(draftId, gameId);
    when(draftCrudRepository.findById(draftId)).thenReturn(Optional.of(entity));

    Optional<Draft> result = adapter.findById(draftId);

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().getId()).isEqualTo(draftId);
    assertThat(result.orElseThrow().getGameId()).isEqualTo(gameId);
  }

  @Test
  void findByGameIdReturnsEmptyWhenGameMissing() {
    UUID gameId = UUID.randomUUID();
    when(gameRepository.findById(gameId)).thenReturn(Optional.empty());

    Optional<Draft> result = adapter.findByGameId(gameId);

    assertThat(result).isEmpty();
    verifyNoInteractions(draftRepository);
  }

  @Test
  void findByGameIdReturnsMappedDraft() {
    UUID gameId = UUID.randomUUID();
    UUID draftId = UUID.randomUUID();
    Game game = new Game();
    game.setId(gameId);
    com.fortnite.pronos.model.Draft entity = buildEntityDraft(draftId, gameId);
    when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
    when(draftRepository.findByGame(game)).thenReturn(Optional.of(entity));

    Optional<Draft> result = adapter.findByGameId(gameId);

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().getId()).isEqualTo(draftId);
  }

  @Test
  void findActiveByGameIdUsesActiveStatus() {
    UUID gameId = UUID.randomUUID();
    Game game = new Game();
    game.setId(gameId);
    com.fortnite.pronos.model.Draft entity = buildEntityDraft(UUID.randomUUID(), gameId);
    when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
    when(draftRepository.findByGameAndStatus(game, com.fortnite.pronos.model.Draft.Status.ACTIVE))
        .thenReturn(Optional.of(entity));

    Optional<Draft> result = adapter.findActiveByGameId(gameId);

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().getStatus()).isEqualTo(DraftStatus.ACTIVE);
  }

  @Test
  void existsByGameIdReturnsFalseWhenGameMissing() {
    UUID gameId = UUID.randomUUID();
    when(gameRepository.findById(gameId)).thenReturn(Optional.empty());

    boolean exists = adapter.existsByGameId(gameId);

    assertThat(exists).isFalse();
  }

  @Test
  void existsByGameIdDelegatesToRepository() {
    UUID gameId = UUID.randomUUID();
    Game game = new Game();
    game.setId(gameId);
    when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
    when(draftRepository.existsByGame(game)).thenReturn(true);

    boolean exists = adapter.existsByGameId(gameId);

    assertThat(exists).isTrue();
  }

  @Test
  void saveThrowsWhenGameMissing() {
    UUID gameId = UUID.randomUUID();
    Draft draft = buildDomainDraft(UUID.randomUUID(), gameId);
    when(gameRepository.findById(gameId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> adapter.save(draft))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Game not found");
  }

  @Test
  void savePersistsAndReturnsMappedDomain() {
    UUID draftId = UUID.randomUUID();
    UUID gameId = UUID.randomUUID();
    Game game = new Game();
    game.setId(gameId);
    Draft domain = buildDomainDraft(draftId, gameId);
    com.fortnite.pronos.model.Draft entity = buildEntityDraft(draftId, gameId);

    when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
    when(draftCrudRepository.save(any(com.fortnite.pronos.model.Draft.class))).thenReturn(entity);

    Draft saved = adapter.save(domain);

    assertThat(saved.getId()).isEqualTo(draftId);
    assertThat(saved.getGameId()).isEqualTo(gameId);
    verify(draftCrudRepository).save(any(com.fortnite.pronos.model.Draft.class));
  }

  private Draft buildDomainDraft(UUID draftId, UUID gameId) {
    LocalDateTime now = LocalDateTime.now();
    return Draft.restore(
        draftId, gameId, DraftStatus.ACTIVE, 1, 1, 4, now.minusHours(1), now, now, null);
  }

  private com.fortnite.pronos.model.Draft buildEntityDraft(UUID draftId, UUID gameId) {
    Game game = new Game();
    game.setId(gameId);
    com.fortnite.pronos.model.Draft draft = new com.fortnite.pronos.model.Draft();
    draft.setId(draftId);
    draft.setGame(game);
    draft.setStatus(com.fortnite.pronos.model.Draft.Status.ACTIVE);
    draft.setCurrentRound(1);
    draft.setCurrentPick(1);
    draft.setTotalRounds(4);
    draft.setCreatedAt(LocalDateTime.now().minusHours(1));
    draft.setUpdatedAt(LocalDateTime.now());
    return draft;
  }
}
