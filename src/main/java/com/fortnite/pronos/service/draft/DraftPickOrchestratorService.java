package com.fortnite.pronos.service.draft;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.domain.draft.model.DraftRegionCursor;
import com.fortnite.pronos.domain.draft.model.SnakeTurn;
import com.fortnite.pronos.domain.port.out.DraftRegionCursorRepositoryPort;

/**
 * Orchestrates per-region snake-draft pick progression.
 *
 * <p>Each region maintains an independent cursor that advances with each pick. The snake direction
 * reverses on even rounds (0-based parity of {@code currentRound - 1}).
 */
@Service
@Transactional
public class DraftPickOrchestratorService {

  private final DraftRegionCursorRepositoryPort cursorRepository;

  public DraftPickOrchestratorService(DraftRegionCursorRepositoryPort cursorRepository) {
    this.cursorRepository = cursorRepository;
  }

  /**
   * Initialises a fresh cursor for a region if none exists, then returns the current turn.
   *
   * @param draftId the draft identifier
   * @param region the region label (e.g. "NAE", "EU")
   * @param snakeOrder ordered list of participant IDs for this region
   * @return the current {@link SnakeTurn}
   */
  public SnakeTurn getOrInitTurn(UUID draftId, String region, List<UUID> snakeOrder) {
    DraftRegionCursor cursor =
        cursorRepository
            .findByDraftIdAndRegion(draftId, region)
            .orElseGet(
                () -> cursorRepository.save(new DraftRegionCursor(draftId, region, snakeOrder)));
    return toSnakeTurn(cursor);
  }

  /**
   * Records that the current participant has picked and advances the cursor to the next turn.
   *
   * @param draftId the draft identifier
   * @param region the region label
   * @return the <em>next</em> {@link SnakeTurn} after advancing, or empty if no cursor exists
   */
  public Optional<SnakeTurn> advance(UUID draftId, String region) {
    return cursorRepository
        .findByDraftIdAndRegion(draftId, region)
        .map(
            cursor -> {
              DraftRegionCursor advanced = cursor.advance();
              cursorRepository.save(advanced);
              return toSnakeTurn(advanced);
            });
  }

  /**
   * Returns the current turn for a region without modifying any state.
   *
   * @param draftId the draft identifier
   * @param region the region label
   * @return the current {@link SnakeTurn}, or empty if the cursor has not been initialised
   */
  @Transactional(readOnly = true)
  public Optional<SnakeTurn> getCurrentTurn(UUID draftId, String region) {
    return cursorRepository.findByDraftIdAndRegion(draftId, region).map(this::toSnakeTurn);
  }

  // ===== PRIVATE HELPERS =====

  private SnakeTurn toSnakeTurn(DraftRegionCursor cursor) {
    List<UUID> order = cursor.getSnakeOrder();
    int round = cursor.getCurrentRound();
    int pick = cursor.getCurrentPick();

    boolean reversed = (round % 2 == 0);
    UUID participantId = resolveParticipantId(order, pick, reversed);
    return new SnakeTurn(participantId, round, pick, reversed);
  }

  private UUID resolveParticipantId(List<UUID> order, int pick, boolean reversed) {
    int index = pick - 1; // 1-based -> 0-based
    if (reversed) {
      index = order.size() - 1 - index;
    }
    return order.get(index);
  }
}
