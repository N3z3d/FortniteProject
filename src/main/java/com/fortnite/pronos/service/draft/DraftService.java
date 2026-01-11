package com.fortnite.pronos.service.draft;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.model.Draft;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.repository.DraftRepository;
import com.fortnite.pronos.repository.GameParticipantRepository;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.PlayerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service pour la gestion des drafts Clean Code : séparation des responsabilités */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DraftService {

  private final DraftRepository draftRepository;
  private final GameRepository gameRepository;
  private final GameParticipantRepository gameParticipantRepository;
  private final PlayerRepository playerRepository;

  /** Crée un nouveau draft pour une game */
  public Draft createDraft(Game game, List<GameParticipant> participants) {
    log.debug("Création d'un draft pour la game {}", game.getName());

    Draft draft = new Draft(game);
    draft.setId(UUID.randomUUID());
    draft.setStatus(Draft.Status.ACTIVE);
    draft.setStartedAt(LocalDateTime.now());
    draft.setCurrentRound(1);
    draft.setCurrentPick(1);
    draft.setTotalRounds(calculateTotalRounds(game));

    return draftRepository.save(draft);
  }

  /** Calcule le nombre total de rounds basé sur les règles régionales */
  private int calculateTotalRounds(Game game) {
    if (game.getRegionRules() == null || game.getRegionRules().isEmpty()) {
      // Par défaut : 10 rounds si pas de règles
      return 10;
    }

    // Somme des joueurs max par région
    return game.getRegionRules().stream().mapToInt(rule -> rule.getMaxPlayers()).sum();
  }

  /** Met à jour l'ordre de draft */
  public void updateDraftOrder(Draft draft, List<GameParticipant> orderedParticipants) {
    log.debug("Mise à jour de l'ordre de draft");

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

      // Vérifier si le draft est terminé
      if (draft.getCurrentRound() > draft.getTotalRounds()) {
        draft.setStatus(Draft.Status.FINISHED);
        draft.setFinishedAt(LocalDateTime.now());
      }
    }

    return draftRepository.save(draft);
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
                    "Aucun participant trouvé pour l'ordre " + currentPosition));
  }

  /** Calcule la position actuelle dans l'ordre de draft (snake draft) */
  private int calculateCurrentPosition(Draft draft, int participantCount) {
    boolean isReverseRound = draft.getCurrentRound() % 2 == 0;

    if (isReverseRound) {
      // Round pair : ordre inverse (snake draft)
      return participantCount - draft.getCurrentPick() + 1;
    } else {
      // Round impair : ordre normal
      return draft.getCurrentPick();
    }
  }

  /** Démarre un draft pour une game */
  public Draft startDraft(Game game) {
    log.debug("Démarrage du draft pour la game {}", game.getName());

    Draft draft = new Draft(game);
    draft.setStatus(Draft.Status.ACTIVE);
    draft.setStartedAt(LocalDateTime.now());

    return draftRepository.save(draft);
  }

  /** Met en pause un draft */
  public Draft pauseDraft(Draft draft) {
    log.debug("Mise en pause du draft {}", draft.getId());
    draft.setStatus(Draft.Status.PAUSED);
    draft.setUpdatedAt(LocalDateTime.now());
    return draftRepository.save(draft);
  }

  /** Reprend un draft en pause */
  public Draft resumeDraft(Draft draft) {
    log.debug("Reprise du draft {}", draft.getId());
    draft.setStatus(Draft.Status.ACTIVE);
    draft.setUpdatedAt(LocalDateTime.now());
    return draftRepository.save(draft);
  }

  /** Termine un draft */
  public Draft finishDraft(Draft draft) {
    log.debug("Fin du draft {}", draft.getId());
    draft.setStatus(Draft.Status.FINISHED);
    draft.setFinishedAt(LocalDateTime.now());
    return draftRepository.save(draft);
  }

  /** Sélectionne un joueur dans le draft */
  public void selectPlayer(Draft draft, UUID userId, com.fortnite.pronos.model.Player player) {
    log.debug(
        "Sélection du joueur {} par l'utilisateur {} dans le draft {}",
        player.getName(),
        userId,
        draft.getId());

    // Créer le pick
    com.fortnite.pronos.model.DraftPick pick = new com.fortnite.pronos.model.DraftPick();
    pick.setDraft(draft);
    pick.setRound(draft.getCurrentRound());
    pick.setPickNumber(calculatePickOrder(draft));
    pick.setPlayer(player);
    pick.setSelectionTime(LocalDateTime.now());

    // La logique de sauvegarde du pick sera gérée par le service appelant

    // Passer au pick suivant
    nextPick(draft, draft.getGame().getMaxParticipants());
  }

  private int calculatePickOrder(Draft draft) {
    return (draft.getCurrentRound() - 1) * draft.getGame().getMaxParticipants()
        + draft.getCurrentPick();
  }

  /** Vérifie si le draft est terminé */
  public boolean isDraftComplete(Draft draft) {
    return Draft.Status.FINISHED.equals(draft.getStatus())
        || draft.getCurrentRound() > draft.getTotalRounds();
  }

  /** Vérifie si c'est le tour d'un utilisateur */
  public boolean isUserTurn(Draft draft, UUID userId) {
    if (!Draft.Status.ACTIVE.equals(draft.getStatus())) {
      return false;
    }

    // Cette méthode nécessiterait l'accès aux participants
    // Pour l'instant, on retourne true pour éviter l'erreur de compilation
    return true;
  }

  // ============== MÉTHODES POUR DÉCOUPLER LE CONTROLLER ==============

  /** Trouve une game par ID ou lève une exception */
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

  /** Trouve un joueur par ID ou lève une exception */
  @Transactional(readOnly = true)
  public Player findPlayerById(UUID playerId) {
    return playerRepository
        .findById(playerId)
        .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));
  }

  /** Trouve un joueur par ID (optionnel) */
  @Transactional(readOnly = true)
  public Optional<Player> findPlayerByIdOptional(UUID playerId) {
    return playerRepository.findById(playerId);
  }

  /** Récupère les participants d'une game ordonnés par draft order */
  @Transactional(readOnly = true)
  public List<GameParticipant> getParticipantsOrderedByDraftOrder(UUID gameId) {
    return gameParticipantRepository.findByGameIdOrderByDraftOrderAsc(gameId);
  }

  /** Trouve le draft associé à une game */
  @Transactional(readOnly = true)
  public Optional<Draft> findDraftByGame(Game game) {
    return draftRepository.findByGame(game);
  }

  /** Sauvegarde un draft */
  public Draft saveDraft(Draft draft) {
    return draftRepository.save(draft);
  }

  /** Récupère les joueurs disponibles par région */
  @Transactional(readOnly = true)
  public List<Player> getAvailablePlayersByRegion(Player.Region region) {
    return playerRepository.findByRegion(region);
  }

  /** Met à jour le statut d'une game */
  public Game updateGameStatus(Game game, GameStatus status) {
    game.setStatus(status);
    return gameRepository.save(game);
  }

  /** Avance le draft au pick suivant et retourne le draft sauvegardé */
  public Draft advanceToNextPick(Draft draft) {
    draft.nextPick();
    return draftRepository.save(draft);
  }
}
