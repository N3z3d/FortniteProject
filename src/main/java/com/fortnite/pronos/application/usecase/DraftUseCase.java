package com.fortnite.pronos.application.usecase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fortnite.pronos.dto.DraftActionResponse;
import com.fortnite.pronos.dto.DraftAdvanceResponse;
import com.fortnite.pronos.dto.DraftAvailablePlayerResponse;
import com.fortnite.pronos.dto.DraftCompleteResponse;
import com.fortnite.pronos.dto.DraftNextParticipantResponse;
import com.fortnite.pronos.dto.DraftOrderEntryResponse;
import com.fortnite.pronos.dto.DraftTimeoutResponse;
import com.fortnite.pronos.model.Draft;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.Player;

/** Application use case for Draft operations. Defines the public API for managing drafts. */
public interface DraftUseCase {

  Draft createDraft(Game game, List<GameParticipant> participants);

  Draft startDraft(Game game);

  Draft pauseDraft(Draft draft);

  Draft resumeDraft(Draft draft);

  Draft finishDraft(Draft draft);

  DraftActionResponse finishDraft(Game game);

  Draft saveDraft(Draft draft);

  Optional<Draft> findDraftByGame(Game game);

  boolean isDraftComplete(Draft draft);

  boolean isUserTurn(Draft draft, UUID userId);

  boolean isUserTurnForGame(UUID gameId, UUID userId);

  DraftNextParticipantResponse buildNextParticipantResponse(
      Game game, List<GameParticipant> participants);

  List<DraftOrderEntryResponse> buildDraftOrderResponse(UUID gameId);

  DraftAdvanceResponse advanceDraftToNextParticipant(Game game);

  List<DraftAvailablePlayerResponse> buildAvailablePlayersResponse(Player.Region region);

  DraftCompleteResponse buildDraftCompleteResponse(Game game);

  DraftTimeoutResponse buildTimeoutResponse();

  // Query helper methods for controller
  Optional<Game> findGameByIdOptional(UUID gameId);

  Optional<Player> findPlayerByIdOptional(UUID playerId);

  List<GameParticipant> getParticipantsOrderedByDraftOrder(UUID gameId);
}
