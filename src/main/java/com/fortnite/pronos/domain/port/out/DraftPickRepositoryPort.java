package com.fortnite.pronos.domain.port.out;

import java.util.List;

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
}
