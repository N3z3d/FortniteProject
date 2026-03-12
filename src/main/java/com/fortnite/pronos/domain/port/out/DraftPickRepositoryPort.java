package com.fortnite.pronos.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fortnite.pronos.model.Draft;
import com.fortnite.pronos.model.DraftPick;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.Player;

/**
 * Output port for DraftPick persistence operations. Implemented by the persistence adapter
 * (DraftPickRepository).
 */
public interface DraftPickRepositoryPort {

  List<DraftPick> findByDraft(Draft draft);

  List<DraftPick> findByParticipant(GameParticipant participant);

  boolean existsByDraftAndPlayer(Draft draft, Player player);

  long countByDraftAndPlayerRegion(Draft draft, Player.Region region);

  List<DraftPick> findByDraftAndRound(Draft draft, Integer round);

  DraftPick findByDraftAndRoundAndPickNumber(Draft draft, Integer round, Integer pickNumber);

  long countByDraftAndParticipant(Draft draft, GameParticipant participant);

  List<DraftPick> findByDraftOrderByPickNumber(Draft draft);

  DraftPick save(DraftPick draftPick);

  /** Returns the UUIDs of all players already picked in a given draft, identified by draft ID. */
  List<UUID> findPickedPlayerIdsByDraftId(UUID draftId);

  /**
   * Returns the UUIDs of players picked by a specific participant in a given draft. Used for team
   * delta computation.
   */
  List<UUID> findPlayerIdsByDraftIdAndParticipantId(UUID draftId, UUID participantId);

  /** Removes the pick for the given player from the given draft. No-op if absent. */
  void deleteByDraftIdAndPlayerId(UUID draftId, UUID playerId);

  /** Returns true if the given player is picked by the given participant in the given draft. */
  boolean existsByDraftIdAndParticipantIdAndPlayerId(
      UUID draftId, UUID participantId, UUID playerId);

  /** Returns the DraftPick entity for the given player/participant/draft, if present. */
  Optional<DraftPick> findByDraftIdAndParticipantIdAndPlayerId(
      UUID draftId, UUID participantId, UUID playerId);
}
