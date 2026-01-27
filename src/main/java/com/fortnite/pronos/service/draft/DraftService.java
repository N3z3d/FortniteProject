package com.fortnite.pronos.service.draft;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.application.usecase.DraftUseCase;
import com.fortnite.pronos.domain.port.out.DraftRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerRepositoryPort;
import com.fortnite.pronos.dto.DraftActionResponse;
import com.fortnite.pronos.dto.DraftAdvanceResponse;
import com.fortnite.pronos.dto.DraftAvailablePlayerResponse;
import com.fortnite.pronos.dto.DraftCompleteResponse;
import com.fortnite.pronos.dto.DraftNextParticipantResponse;
import com.fortnite.pronos.dto.DraftOrderEntryResponse;
import com.fortnite.pronos.dto.DraftTimeoutResponse;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.model.Draft;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.repository.DraftRepository;
import com.fortnite.pronos.repository.GameParticipantRepository;
import com.fortnite.pronos.repository.PlayerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service pour la gestion des drafts Clean Code : sÃƒÆ'Ã‚Â©paration des responsabilitÃƒÆ'Ã‚Â©s */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DraftService implements DraftUseCase {

  private final DraftRepository draftRepository;
  private final GameRepositoryPort gameRepository;
  private final GameParticipantRepository gameParticipantRepository;
  private final PlayerRepository playerRepository;

  /** CrÃƒÆ’Ã‚Â©e un nouveau draft pour une game */
  public Draft createDraft(Game game, List<GameParticipant> participants) {
    log.debug("CrÃƒÆ’Ã‚Â©ation d'un draft pour la game {}", game.getName());

    Draft draft = new Draft(game);
    draft.setId(UUID.randomUUID());
    draft.setStatus(Draft.Status.ACTIVE);
    draft.setStartedAt(LocalDateTime.now());
    draft.setCurrentRound(1);
    draft.setCurrentPick(1);
    draft.setTotalRounds(calculateTotalRounds(game));

    return ((DraftRepositoryPort) draftRepository).save(draft);
  }

  /** Calcule le nombre total de rounds basÃƒÆ’Ã‚Â© sur les rÃƒÆ’Ã‚Â¨gles rÃƒÆ’Ã‚Â©gionales */
  private int calculateTotalRounds(Game game) {
    if (game.getRegionRules() == null || game.getRegionRules().isEmpty()) {
      // Par dÃƒÆ’Ã‚Â©faut : 10 rounds si pas de rÃƒÆ’Ã‚Â¨gles
      return 10;
    }

    // Somme des joueurs max par rÃƒÆ’Ã‚Â©gion
    return game.getRegionRules().stream().mapToInt(rule -> rule.getMaxPlayers()).sum();
  }

  /** Met ÃƒÆ’Ã‚Â  jour l'ordre de draft */
  public void updateDraftOrder(Draft draft, List<GameParticipant> orderedParticipants) {
    log.debug("Mise ÃƒÆ’Ã‚Â  jour de l'ordre de draft");

    for (int i = 0; i < orderedParticipants.size(); i++) {
      orderedParticipants.get(i).setDraftOrder(i + 1);
    }
  }

  /** Passe au pick suivant */
  public Draft nextPick(Draft draft, int participantCount) {
    draft.setCurrentPick(draft.getCurrentPick() + 1);

    // Si on a fini le round
    if (draft.getCurrentPick() > participantCount) {
      draft.setCurrentRound(draft.getCurrentRound() + 1);
      draft.setCurrentPick(1);

      // VÃƒÆ’Ã‚Â©rifier si le draft est terminÃƒÆ’Ã‚Â©
      if (draft.getCurrentRound() > draft.getTotalRounds()) {
        draft.setStatus(Draft.Status.FINISHED);
        draft.setFinishedAt(LocalDateTime.now());
      }
    }

    return ((DraftRepositoryPort) draftRepository).save(draft);
  }

  /** Obtient le participant actuel qui doit picker */
  public GameParticipant getCurrentPicker(Draft draft, List<GameParticipant> participants) {
    int currentPosition = calculateCurrentPosition(draft, participants.size());

    return participants.stream()
        .filter(p -> p.getDraftOrder() == currentPosition)
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Aucun participant trouvÃƒÆ’Ã‚Â© pour l'ordre " + currentPosition));
  }

  /**
   * Calcule la position actuelle dans l'ordre de draft (snake draft). Public pour permettre son
   * utilisation dans les methodes de construction de reponse.
   */
  public int calculateCurrentPosition(Draft draft, int participantCount) {
    boolean isReverseRound = draft.getCurrentRound() % 2 == 0;

    if (isReverseRound) {
      // Round pair : ordre inverse (snake draft)
      return participantCount - draft.getCurrentPick() + 1;
    } else {
      // Round impair : ordre normal
      return draft.getCurrentPick();
    }
  }

  /** DÃƒÆ’Ã‚Â©marre un draft pour une game */
  public Draft startDraft(Game game) {
    log.debug("DÃƒÆ’Ã‚Â©marrage du draft pour la game {}", game.getName());

    Draft draft = new Draft(game);
    draft.setStatus(Draft.Status.ACTIVE);
    draft.setStartedAt(LocalDateTime.now());

    return ((DraftRepositoryPort) draftRepository).save(draft);
  }

  /** Met en pause un draft */
  public Draft pauseDraft(Draft draft) {
    log.debug("Mise en pause du draft {}", draft.getId());
    draft.setStatus(Draft.Status.PAUSED);
    draft.setUpdatedAt(LocalDateTime.now());
    return ((DraftRepositoryPort) draftRepository).save(draft);
  }

  /** Reprend un draft en pause */
  public Draft resumeDraft(Draft draft) {
    log.debug("Reprise du draft {}", draft.getId());
    draft.setStatus(Draft.Status.ACTIVE);
    draft.setUpdatedAt(LocalDateTime.now());
    return ((DraftRepositoryPort) draftRepository).save(draft);
  }

  /** Termine un draft */
  public Draft finishDraft(Draft draft) {
    log.debug("Fin du draft {}", draft.getId());
    draft.setStatus(Draft.Status.FINISHED);
    draft.setFinishedAt(LocalDateTime.now());
    return ((DraftRepositoryPort) draftRepository).save(draft);
  }

  /** SÃƒÆ’Ã‚Â©lectionne un joueur dans le draft */
  public void selectPlayer(Draft draft, UUID userId, com.fortnite.pronos.model.Player player) {
    log.debug(
        "SÃƒÆ’Ã‚Â©lection du joueur {} par l'utilisateur {} dans le draft {}",
        player.getName(),
        userId,
        draft.getId());

    // CrÃƒÆ’Ã‚Â©er le pick
    com.fortnite.pronos.model.DraftPick pick = new com.fortnite.pronos.model.DraftPick();
    pick.setDraft(draft);
    pick.setRound(draft.getCurrentRound());
    pick.setPickNumber(calculatePickOrder(draft));
    pick.setPlayer(player);
    pick.setSelectionTime(LocalDateTime.now());

    // La logique de sauvegarde du pick sera gÃƒÆ’Ã‚Â©rÃƒÆ’Ã‚Â©e par le service appelant

    // Passer au pick suivant
    nextPick(draft, draft.getGame().getMaxParticipants());
  }

  private int calculatePickOrder(Draft draft) {
    return (draft.getCurrentRound() - 1) * draft.getGame().getMaxParticipants()
        + draft.getCurrentPick();
  }

  /** VÃƒÆ’Ã‚Â©rifie si le draft est terminÃƒÆ’Ã‚Â© */
  public boolean isDraftComplete(Draft draft) {
    return Draft.Status.FINISHED.equals(draft.getStatus())
        || draft.getCurrentRound() > draft.getTotalRounds();
  }

  /** VÃƒÆ’Ã‚Â©rifie si c'est le tour d'un utilisateur */
  public boolean isUserTurn(Draft draft, UUID userId) {
    if (!Draft.Status.ACTIVE.equals(draft.getStatus())) {
      return false;
    }

    // Cette mÃƒÆ’Ã‚Â©thode nÃƒÆ’Ã‚Â©cessiterait l'accÃƒÆ’Ã‚Â¨s aux participants
    // Pour l'instant, on retourne true pour ÃƒÆ’Ã‚Â©viter l'erreur de compilation
    return true;
  }

  // ============== MÃƒÆ’Ã¢â‚¬Â°THODES POUR DÃƒÆ’Ã¢â‚¬Â°COUPLER LE CONTROLLER ==============

  /** Trouve une game par ID ou lÃƒÆ’Ã‚Â¨ve une exception */
  @Transactional(readOnly = true)
  public Game findGameById(UUID gameId) {
    return gameRepository
        .findById(gameId)
        .orElseThrow(() -> new GameNotFoundException("Game not found: " + gameId));
  }

  /** Trouve une game par ID (optionnel) */
  @Transactional(readOnly = true)
  public Optional<Game> findGameByIdOptional(UUID gameId) {
    return gameRepository.findById(gameId);
  }

  /** Trouve un joueur par ID ou lÃƒÆ'Ã‚Â¨ve une exception */
  @Transactional(readOnly = true)
  public Player findPlayerById(UUID playerId) {
    return ((PlayerRepositoryPort) playerRepository)
        .findById(playerId)
        .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));
  }

  /** Trouve un joueur par ID (optionnel) */
  @Transactional(readOnly = true)
  public Optional<Player> findPlayerByIdOptional(UUID playerId) {
    return ((PlayerRepositoryPort) playerRepository).findById(playerId);
  }

  /** RÃƒÆ’Ã‚Â©cupÃƒÆ’Ã‚Â¨re les participants d'une game ordonnÃƒÆ’Ã‚Â©s par draft order */
  @Transactional(readOnly = true)
  public List<GameParticipant> getParticipantsOrderedByDraftOrder(UUID gameId) {
    return gameParticipantRepository.findByGameIdOrderByDraftOrderAsc(gameId);
  }

  /** Trouve le draft associÃƒÆ’Ã‚Â© ÃƒÆ’Ã‚Â  une game */
  @Transactional(readOnly = true)
  public Optional<Draft> findDraftByGame(Game game) {
    return draftRepository.findByGame(game);
  }

  /** Construit la reponse du prochain participant a partir d'une game et de ses participants */
  @Transactional(readOnly = true)
  public DraftNextParticipantResponse buildNextParticipantResponse(
      Game game, List<GameParticipant> participants) {
    if (participants.isEmpty()) {
      throw new IllegalArgumentException("No participants available for draft");
    }

    Optional<Draft> draftOpt = findDraftByGame(game);
    final int currentPick;
    final int currentRound;
    final int snakePosition;

    if (draftOpt.isPresent()) {
      Draft draft = draftOpt.get();
      currentPick = draft.getCurrentPick();
      currentRound = draft.getCurrentRound();
      snakePosition = calculateCurrentPosition(draft, participants.size());
    } else {
      currentPick = 1;
      currentRound = 1;
      snakePosition = 1;
    }

    // Snake draft: find participant by their draftOrder matching snake position
    GameParticipant next =
        participants.stream()
            .filter(p -> p.getDraftOrder() == snakePosition)
            .findFirst()
            .orElse(participants.get(0)); // fallback to first if not found

    return new DraftNextParticipantResponse(
        next.getId(), next.getDraftOrder(), next.getUser().getId(), currentPick, currentRound);
  }

  /** Construit l'ordre de draft pour une game */
  @Transactional(readOnly = true)
  public List<DraftOrderEntryResponse> buildDraftOrderResponse(UUID gameId) {
    List<GameParticipant> participants = getParticipantsOrderedByDraftOrder(gameId);
    return participants.stream()
        .map(
            participant ->
                new DraftOrderEntryResponse(
                    participant.getId(),
                    participant.getDraftOrder(),
                    participant.getUser().getId()))
        .toList();
  }

  /** Determine si c'est le tour d'un utilisateur pour une game */
  @Transactional(readOnly = true)
  public boolean isUserTurnForGame(UUID gameId, UUID userId) {
    List<GameParticipant> participants = getParticipantsOrderedByDraftOrder(gameId);
    if (participants.isEmpty()) {
      return false;
    }

    // Get the game and its draft to determine current position
    Optional<Game> gameOpt = gameRepository.findById(gameId);
    if (gameOpt.isEmpty()) {
      return false;
    }

    Optional<Draft> draftOpt = findDraftByGame(gameOpt.get());
    final int snakePosition;
    if (draftOpt.isPresent()) {
      Draft draft = draftOpt.get();
      snakePosition = calculateCurrentPosition(draft, participants.size());
    } else {
      snakePosition = 1; // Default to first participant
    }

    // Snake draft: find participant by their draftOrder matching snake position
    GameParticipant currentParticipant =
        participants.stream()
            .filter(p -> p.getDraftOrder() == snakePosition)
            .findFirst()
            .orElse(participants.get(0));

    if (currentParticipant.getUser() == null || currentParticipant.getUser().getId() == null) {
      return false;
    }

    return currentParticipant.getUser().getId().equals(userId);
  }

  /** Avance le draft et construit la reponse pour le participant suivant */
  public DraftAdvanceResponse advanceDraftToNextParticipant(Game game) {
    // Get participants first to have the accurate count from DB
    List<GameParticipant> participants = getParticipantsOrderedByDraftOrder(game.getId());
    int participantCount = participants.isEmpty() ? game.getMaxParticipants() : participants.size();

    Optional<Draft> draftOpt = findDraftByGame(game);
    Draft draft = draftOpt.orElseGet(() -> createDraft(game, game.getParticipants()));

    Draft savedDraft = advanceToNextPick(draft, participantCount);

    UUID nextParticipantId = null;
    Integer nextDraftOrder = null;
    if (!participants.isEmpty() && !savedDraft.isDraftComplete()) {
      // Snake draft: calculate correct position based on round and pick
      int snakePosition = calculateCurrentPosition(savedDraft, participants.size());
      GameParticipant nextParticipant =
          participants.stream()
              .filter(p -> p.getDraftOrder() == snakePosition)
              .findFirst()
              .orElse(participants.get(0));
      nextParticipantId = nextParticipant.getId();
      nextDraftOrder = nextParticipant.getDraftOrder();
    }

    return new DraftAdvanceResponse(
        true,
        "Passage au participant suivant",
        savedDraft.getCurrentRound(),
        savedDraft.getCurrentPick(),
        savedDraft.isDraftComplete(),
        nextParticipantId,
        nextDraftOrder);
  }

  /** Construit la reponse d'ordre pour les joueurs disponibles par region */
  @Transactional(readOnly = true)
  public List<DraftAvailablePlayerResponse> buildAvailablePlayersResponse(Player.Region region) {
    List<Player> players = getAvailablePlayersByRegion(region);
    return players.stream()
        .map(
            player ->
                new DraftAvailablePlayerResponse(
                    player.getId(), player.getNickname(), player.getRegion().name()))
        .toList();
  }

  /** Construit la reponse pour l'etat de completion du draft */
  @Transactional(readOnly = true)
  public DraftCompleteResponse buildDraftCompleteResponse(Game game) {
    boolean isComplete = game.getStatus() == GameStatus.ACTIVE;
    return new DraftCompleteResponse(isComplete);
  }

  /** Construit une reponse de timeouts par defaut */
  public DraftTimeoutResponse buildTimeoutResponse() {
    return new DraftTimeoutResponse(0, "Timeouts geres avec succes");
  }

  /** Termine le draft pour une game */
  public DraftActionResponse finishDraft(Game game) {
    updateGameStatus(game, GameStatus.ACTIVE);
    return new DraftActionResponse(true, "Draft termine avec succes");
  }

  /** Sauvegarde un draft */
  public Draft saveDraft(Draft draft) {
    return ((DraftRepositoryPort) draftRepository).save(draft);
  }

  /** RÃƒÆ’Ã‚Â©cupÃƒÆ’Ã‚Â¨re les joueurs disponibles par rÃƒÆ’Ã‚Â©gion */
  @Transactional(readOnly = true)
  public List<Player> getAvailablePlayersByRegion(Player.Region region) {
    return playerRepository.findByRegion(region);
  }

  /** Met ÃƒÆ’Ã‚Â  jour le statut d'une game */
  public Game updateGameStatus(Game game, GameStatus status) {
    game.setStatus(status);
    return gameRepository.save(game);
  }

  /** Avance le draft au pick suivant et retourne le draft sauvegarde */
  public Draft advanceToNextPick(Draft draft) {
    draft.nextPick();
    return ((DraftRepositoryPort) draftRepository).save(draft);
  }

  /** Avance le draft au pick suivant avec un nombre de participants explicite */
  public Draft advanceToNextPick(Draft draft, int participantCount) {
    draft.nextPick(participantCount);
    return ((DraftRepositoryPort) draftRepository).save(draft);
  }
}
