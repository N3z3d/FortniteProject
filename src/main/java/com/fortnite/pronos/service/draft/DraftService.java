package com.fortnite.pronos.service.draft;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.application.usecase.DraftUseCase;
import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.domain.port.out.DraftDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.DraftRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameParticipantRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;
import com.fortnite.pronos.dto.DraftActionResponse;
import com.fortnite.pronos.dto.DraftAdvanceResponse;
import com.fortnite.pronos.dto.DraftAvailablePlayerResponse;
import com.fortnite.pronos.dto.DraftCompleteResponse;
import com.fortnite.pronos.dto.DraftNextParticipantResponse;
import com.fortnite.pronos.dto.DraftOrderEntryResponse;
import com.fortnite.pronos.dto.DraftTimeoutResponse;
import com.fortnite.pronos.exception.GameNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service pour la gestion des drafts Clean Code : sÃƒÆ’Ã†'Ãƒâ€šÃ‚Â©paration des
 * responsabilitÃƒÆ’Ã†'Ãƒâ€šÃ‚Â©s
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
@SuppressWarnings({"java:S1126"})
public class DraftService implements DraftUseCase {

  private final DraftRepositoryPort draftRepository;
  private final DraftDomainRepositoryPort draftDomainRepository;
  private final GameRepositoryPort gameRepository;
  private final GameParticipantRepositoryPort gameParticipantRepository;
  private final PlayerDomainRepositoryPort playerRepository;

  /** CrÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©e un nouveau draft pour une game */
  public com.fortnite.pronos.model.Draft createDraft(
      com.fortnite.pronos.model.Game game,
      List<com.fortnite.pronos.model.GameParticipant> participants) {
    log.debug("CrÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©ation d'un draft pour la game {}", game.getName());

    com.fortnite.pronos.model.Draft draft = new com.fortnite.pronos.model.Draft(game);
    draft.setId(UUID.randomUUID());
    draft.setStatus(com.fortnite.pronos.model.Draft.Status.ACTIVE);
    draft.setStartedAt(LocalDateTime.now());
    draft.setCurrentRound(1);
    draft.setCurrentPick(1);
    draft.setTotalRounds(calculateTotalRounds(game));

    return persistDraftEntity(draft);
  }

  /**
   * Calcule le nombre total de rounds basÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© sur les rÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¨gles
   * rÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©gionales
   */
  private int calculateTotalRounds(com.fortnite.pronos.model.Game game) {
    if (game.getRegionRules() == null || game.getRegionRules().isEmpty()) {
      // Par dÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©faut : 10 rounds si pas de rÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¨gles
      return 10;
    }

    // Somme des joueurs max par rÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©gion
    return game.getRegionRules().stream().mapToInt(rule -> rule.getMaxPlayers()).sum();
  }

  /** Passe au pick suivant */
  public com.fortnite.pronos.model.Draft nextPick(
      com.fortnite.pronos.model.Draft draft, int participantCount) {
    draft.setCurrentPick(draft.getCurrentPick() + 1);

    // Si on a fini le round
    if (draft.getCurrentPick() > participantCount) {
      draft.setCurrentRound(draft.getCurrentRound() + 1);
      draft.setCurrentPick(1);

      // VÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©rifier si le draft est terminÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©
      if (draft.getCurrentRound() > draft.getTotalRounds()) {
        draft.setStatus(com.fortnite.pronos.model.Draft.Status.FINISHED);
        draft.setFinishedAt(LocalDateTime.now());
      }
    }

    return persistDraftEntity(draft);
  }

  /** Obtient le participant actuel qui doit picker */
  public com.fortnite.pronos.model.GameParticipant getCurrentPicker(
      com.fortnite.pronos.model.Draft draft,
      List<com.fortnite.pronos.model.GameParticipant> participants) {
    int currentPosition = calculateCurrentPosition(draft, participants.size());

    return participants.stream()
        .filter(p -> p.getDraftOrder() == currentPosition)
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Aucun participant trouvÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© pour l'ordre " + currentPosition));
  }

  /**
   * Calcule la position actuelle dans l'ordre de draft (snake draft). Public pour permettre son
   * utilisation dans les methodes de construction de reponse.
   */
  public int calculateCurrentPosition(com.fortnite.pronos.model.Draft draft, int participantCount) {
    boolean isReverseRound = draft.getCurrentRound() % 2 == 0;

    if (isReverseRound) {
      // Round pair : ordre inverse (snake draft)
      return participantCount - draft.getCurrentPick() + 1;
    } else {
      // Round impair : ordre normal
      return draft.getCurrentPick();
    }
  }

  /** DÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©marre un draft pour une game */
  /** Starts a draft for a game identified by its ID */
  public com.fortnite.pronos.model.Draft startDraftForGame(UUID gameId) {
    com.fortnite.pronos.model.Game game = findGameById(gameId);
    return startDraft(game);
  }

  public com.fortnite.pronos.model.Draft startDraft(com.fortnite.pronos.model.Game game) {
    log.debug("DÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©marrage du draft pour la game {}", game.getName());

    com.fortnite.pronos.model.Draft draft = new com.fortnite.pronos.model.Draft(game);
    draft.setStatus(com.fortnite.pronos.model.Draft.Status.ACTIVE);
    draft.setStartedAt(LocalDateTime.now());

    return persistDraftEntity(draft);
  }

  /** Met en pause un draft */
  public com.fortnite.pronos.model.Draft pauseDraft(com.fortnite.pronos.model.Draft draft) {
    log.debug("Mise en pause du draft {}", draft.getId());
    draft.setStatus(com.fortnite.pronos.model.Draft.Status.PAUSED);
    draft.setUpdatedAt(LocalDateTime.now());
    return persistDraftEntity(draft);
  }

  /** Reprend un draft en pause */
  public com.fortnite.pronos.model.Draft resumeDraft(com.fortnite.pronos.model.Draft draft) {
    log.debug("Reprise du draft {}", draft.getId());
    draft.setStatus(com.fortnite.pronos.model.Draft.Status.ACTIVE);
    draft.setUpdatedAt(LocalDateTime.now());
    return persistDraftEntity(draft);
  }

  /** Termine un draft */
  public com.fortnite.pronos.model.Draft finishDraft(com.fortnite.pronos.model.Draft draft) {
    log.debug("Fin du draft {}", draft.getId());
    draft.setStatus(com.fortnite.pronos.model.Draft.Status.FINISHED);
    draft.setFinishedAt(LocalDateTime.now());
    return persistDraftEntity(draft);
  }

  /** VÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©rifie si le draft est terminÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© */
  public boolean isDraftComplete(com.fortnite.pronos.model.Draft draft) {
    return com.fortnite.pronos.model.Draft.Status.FINISHED.equals(draft.getStatus())
        || draft.getCurrentRound() > draft.getTotalRounds();
  }

  /** VÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©rifie si c'est le tour d'un utilisateur */
  public boolean isUserTurn(com.fortnite.pronos.model.Draft draft, UUID userId) {
    if (!com.fortnite.pronos.model.Draft.Status.ACTIVE.equals(draft.getStatus())) {
      return false;
    }

    // Cette mÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©thode nÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©cessiterait l'accÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¨s aux
    // participants
    // Pour l'instant, on retourne true pour ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©viter l'erreur de compilation
    return true;
  }

  // ============== MÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â°THODES POUR DÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â°COUPLER LE
  // CONTROLLER ==============

  /** Trouve une game par ID ou lÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¨ve une exception */
  @Transactional(readOnly = true)
  public com.fortnite.pronos.model.Game findGameById(UUID gameId) {
    return gameRepository
        .findById(gameId)
        .orElseThrow(() -> new GameNotFoundException("Game not found: " + gameId));
  }

  /** Trouve une game par ID (optionnel) */
  @Transactional(readOnly = true)
  public Optional<com.fortnite.pronos.model.Game> findGameByIdOptional(UUID gameId) {
    return gameRepository.findById(gameId);
  }

  /** Trouve un joueur par ID ou lÃƒÆ’Ã†'Ãƒâ€šÃ‚Â¨ve une exception */
  @Transactional(readOnly = true)
  public com.fortnite.pronos.model.Player findPlayerById(UUID playerId) {
    return playerRepository
        .findById(playerId)
        .map(this::toLegacyPlayer)
        .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));
  }

  /** Trouve un joueur par ID (optionnel) */
  @Transactional(readOnly = true)
  public Optional<com.fortnite.pronos.model.Player> findPlayerByIdOptional(UUID playerId) {
    return playerRepository.findById(playerId).map(this::toLegacyPlayer);
  }

  /**
   * RÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©cupÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¨re les participants d'une game
   * ordonnÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©s par draft order
   */
  @Transactional(readOnly = true)
  public List<com.fortnite.pronos.model.GameParticipant> getParticipantsOrderedByDraftOrder(
      UUID gameId) {
    return gameParticipantRepository.findByGameIdOrderByDraftOrderAsc(gameId);
  }

  /** Trouve le draft associÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â une game */
  @Transactional(readOnly = true)
  public Optional<com.fortnite.pronos.model.Draft> findDraftByGame(
      com.fortnite.pronos.model.Game game) {
    if (game == null || game.getId() == null) {
      return Optional.empty();
    }

    Optional<com.fortnite.pronos.domain.draft.model.Draft> domainDraft =
        Optional.ofNullable(draftDomainRepository.findByGameId(game.getId()))
            .orElse(Optional.empty());
    if (domainDraft.isPresent() && domainDraft.orElseThrow().getId() != null) {
      return draftRepository.findById(domainDraft.orElseThrow().getId());
    }
    return draftRepository.findByGame(game);
  }

  /** Construit la reponse du prochain participant a partir d'une game et de ses participants */
  @Transactional(readOnly = true)
  public DraftNextParticipantResponse buildNextParticipantResponse(
      com.fortnite.pronos.model.Game game,
      List<com.fortnite.pronos.model.GameParticipant> participants) {
    if (participants.isEmpty()) {
      throw new IllegalArgumentException("No participants available for draft");
    }

    Optional<com.fortnite.pronos.model.Draft> draftOpt = findDraftByGame(game);
    final int currentPick;
    final int currentRound;
    final int snakePosition;

    if (draftOpt.isPresent()) {
      com.fortnite.pronos.model.Draft draft = draftOpt.get();
      currentPick = draft.getCurrentPick();
      currentRound = draft.getCurrentRound();
      snakePosition = calculateCurrentPosition(draft, participants.size());
    } else {
      currentPick = 1;
      currentRound = 1;
      snakePosition = 1;
    }

    // Snake draft: find participant by their draftOrder matching snake position
    com.fortnite.pronos.model.GameParticipant next =
        participants.stream()
            .filter(p -> p.getDraftOrder() == snakePosition)
            .findFirst()
            .orElse(participants.get(0)); // fallback to first if not found

    return new DraftNextParticipantResponse(
        next.getId(), next.getDraftOrder(), participantUserId(next), currentPick, currentRound);
  }

  /** Construit l'ordre de draft pour une game */
  @Transactional(readOnly = true)
  public List<DraftOrderEntryResponse> buildDraftOrderResponse(UUID gameId) {
    List<com.fortnite.pronos.model.GameParticipant> participants =
        getParticipantsOrderedByDraftOrder(gameId);
    return participants.stream()
        .map(
            participant ->
                new DraftOrderEntryResponse(
                    participant.getId(),
                    participant.getDraftOrder(),
                    participantUserId(participant)))
        .toList();
  }

  /** Determine si c'est le tour d'un utilisateur pour une game */
  @Transactional(readOnly = true)
  public boolean isUserTurnForGame(UUID gameId, UUID userId) {
    List<com.fortnite.pronos.model.GameParticipant> participants =
        getParticipantsOrderedByDraftOrder(gameId);
    if (participants.isEmpty()) {
      return false;
    }

    // Get the game and its draft to determine current position
    Optional<com.fortnite.pronos.model.Game> gameOpt = gameRepository.findById(gameId);
    if (gameOpt.isEmpty()) {
      return false;
    }

    Optional<com.fortnite.pronos.model.Draft> draftOpt = findDraftByGame(gameOpt.get());
    final int snakePosition;
    if (draftOpt.isPresent()) {
      com.fortnite.pronos.model.Draft draft = draftOpt.get();
      snakePosition = calculateCurrentPosition(draft, participants.size());
    } else {
      snakePosition = 1; // Default to first participant
    }

    // Snake draft: find participant by their draftOrder matching snake position
    com.fortnite.pronos.model.GameParticipant currentParticipant =
        participants.stream()
            .filter(p -> p.getDraftOrder() == snakePosition)
            .findFirst()
            .orElse(participants.get(0));

    if (!participantHasUserId(currentParticipant)) {
      return false;
    }

    return participantUserId(currentParticipant).equals(userId);
  }

  /** Avance le draft et construit la reponse pour le participant suivant */
  public DraftAdvanceResponse advanceDraftToNextParticipant(com.fortnite.pronos.model.Game game) {
    // Get participants first to have the accurate count from DB
    List<com.fortnite.pronos.model.GameParticipant> participants =
        getParticipantsOrderedByDraftOrder(game.getId());
    int participantCount = participants.isEmpty() ? game.getMaxParticipants() : participants.size();

    Optional<com.fortnite.pronos.model.Draft> draftOpt = findDraftByGame(game);
    com.fortnite.pronos.model.Draft draft =
        draftOpt.orElseGet(() -> createDraft(game, game.getParticipants()));

    com.fortnite.pronos.model.Draft savedDraft = advanceToNextPick(draft, participantCount);

    UUID nextParticipantId = null;
    Integer nextDraftOrder = null;
    if (!participants.isEmpty() && !savedDraft.isDraftComplete()) {
      // Snake draft: calculate correct position based on round and pick
      int snakePosition = calculateCurrentPosition(savedDraft, participants.size());
      com.fortnite.pronos.model.GameParticipant nextParticipant =
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
  public List<DraftAvailablePlayerResponse> buildAvailablePlayersResponse(
      com.fortnite.pronos.model.Player.Region region) {
    List<com.fortnite.pronos.model.Player> players = getAvailablePlayersByRegion(region);
    return players.stream()
        .map(
            player ->
                new DraftAvailablePlayerResponse(
                    player.getId(), player.getNickname(), player.getRegion().name()))
        .toList();
  }

  /** Construit la reponse pour l'etat de completion du draft */
  @Transactional(readOnly = true)
  public DraftCompleteResponse buildDraftCompleteResponse(com.fortnite.pronos.model.Game game) {
    boolean isComplete = game.getStatus() == com.fortnite.pronos.model.GameStatus.ACTIVE;
    return new DraftCompleteResponse(isComplete);
  }

  /** Construit une reponse de timeouts par defaut */
  public DraftTimeoutResponse buildTimeoutResponse() {
    return new DraftTimeoutResponse(0, "Timeouts geres avec succes");
  }

  /** Termine le draft pour une game */
  public DraftActionResponse finishDraft(com.fortnite.pronos.model.Game game) {
    updateGameStatus(game, com.fortnite.pronos.model.GameStatus.ACTIVE);
    return new DraftActionResponse(true, "Draft termine avec succes");
  }

  /** Sauvegarde un draft */
  public com.fortnite.pronos.model.Draft saveDraft(com.fortnite.pronos.model.Draft draft) {
    return persistDraftEntity(draft);
  }

  /**
   * RÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©cupÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¨re les joueurs disponibles par rÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©gion
   */
  @Transactional(readOnly = true)
  public List<com.fortnite.pronos.model.Player> getAvailablePlayersByRegion(
      com.fortnite.pronos.model.Player.Region region) {
    PlayerRegion domainRegion = toDomainRegion(region);
    return playerRepository.findByRegion(domainRegion).stream().map(this::toLegacyPlayer).toList();
  }

  /** Met ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â jour le statut d'une game */
  public com.fortnite.pronos.model.Game updateGameStatus(
      com.fortnite.pronos.model.Game game, com.fortnite.pronos.model.GameStatus status) {
    game.setStatus(status);
    return gameRepository.save(game);
  }

  /** Avance le draft au pick suivant et retourne le draft sauvegarde */
  public com.fortnite.pronos.model.Draft advanceToNextPick(com.fortnite.pronos.model.Draft draft) {
    draft.nextPick();
    return persistDraftEntity(draft);
  }

  /** Avance le draft au pick suivant avec un nombre de participants explicite */
  public com.fortnite.pronos.model.Draft advanceToNextPick(
      com.fortnite.pronos.model.Draft draft, int participantCount) {
    draft.nextPick(participantCount);
    return persistDraftEntity(draft);
  }

  private com.fortnite.pronos.model.Draft persistDraftEntity(
      com.fortnite.pronos.model.Draft draft) {
    com.fortnite.pronos.model.Draft persistedDraft = draftRepository.save(draft);
    syncDomainDraft(persistedDraft);
    return persistedDraft;
  }

  private void syncDomainDraft(com.fortnite.pronos.model.Draft draft) {
    if (!draftHasGameId(draft)) {
      return;
    }
    draftDomainRepository.save(toDomainDraft(draft));
  }

  private com.fortnite.pronos.domain.draft.model.Draft toDomainDraft(
      com.fortnite.pronos.model.Draft draft) {
    return com.fortnite.pronos.domain.draft.model.Draft.restore(
        draft.getId(),
        gameIdOf(draft),
        toDomainStatus(draft.getStatus()),
        safeInt(draft.getCurrentRound()),
        safeInt(draft.getCurrentPick()),
        safeInt(draft.getTotalRounds()),
        draft.getCreatedAt(),
        draft.getUpdatedAt(),
        draft.getStartedAt(),
        draft.getFinishedAt());
  }

  private UUID participantUserId(com.fortnite.pronos.model.GameParticipant participant) {
    return participant.getUserId();
  }

  private boolean participantHasUserId(com.fortnite.pronos.model.GameParticipant participant) {
    return participantUserId(participant) != null;
  }

  private UUID gameIdOf(com.fortnite.pronos.model.Draft draft) {
    return draft.getGameId();
  }

  private boolean draftHasGameId(com.fortnite.pronos.model.Draft draft) {
    return draft != null && gameIdOf(draft) != null;
  }

  private com.fortnite.pronos.domain.draft.model.DraftStatus toDomainStatus(
      com.fortnite.pronos.model.Draft.Status status) {
    if (status == null) {
      return com.fortnite.pronos.domain.draft.model.DraftStatus.PENDING;
    }
    return com.fortnite.pronos.domain.draft.model.DraftStatus.valueOf(status.name());
  }

  private int safeInt(Integer value) {
    return value != null && value > 0 ? value : 1;
  }

  private com.fortnite.pronos.model.Player toLegacyPlayer(
      com.fortnite.pronos.domain.player.model.Player player) {
    com.fortnite.pronos.model.Player legacyPlayer = new com.fortnite.pronos.model.Player();
    legacyPlayer.setId(player.getId());
    legacyPlayer.setFortniteId(player.getFortniteId());
    legacyPlayer.setUsername(player.getUsername());
    legacyPlayer.setNickname(player.getNickname());
    legacyPlayer.setRegion(toLegacyRegion(player.getRegion()));
    legacyPlayer.setTranche(player.getTranche());
    legacyPlayer.setCurrentSeason(player.getCurrentSeason());
    legacyPlayer.setLocked(player.isLocked());
    return legacyPlayer;
  }

  private PlayerRegion toDomainRegion(com.fortnite.pronos.model.Player.Region region) {
    if (region == null) {
      return PlayerRegion.UNKNOWN;
    }
    try {
      return PlayerRegion.valueOf(region.name());
    } catch (IllegalArgumentException ex) {
      return PlayerRegion.UNKNOWN;
    }
  }

  private com.fortnite.pronos.model.Player.Region toLegacyRegion(PlayerRegion region) {
    if (region == null) {
      return com.fortnite.pronos.model.Player.Region.UNKNOWN;
    }
    try {
      return com.fortnite.pronos.model.Player.Region.valueOf(region.name());
    } catch (IllegalArgumentException ex) {
      return com.fortnite.pronos.model.Player.Region.UNKNOWN;
    }
  }
}
