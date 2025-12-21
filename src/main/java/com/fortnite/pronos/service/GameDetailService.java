package com.fortnite.pronos.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;

import org.hibernate.ObjectNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.dto.GameDetailDto;
import com.fortnite.pronos.dto.GameDetailDto.*;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.model.*;
import com.fortnite.pronos.repository.DraftRepository;
import com.fortnite.pronos.repository.GameParticipantRepository;
import com.fortnite.pronos.repository.GameRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service pour récupérer les détails complets d'une game Clean Code : Service focalisé sur la
 * lecture des détails de game
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameDetailService {

  private static final String MISSING_PLAYER_NICKNAME = "Joueur indisponible";
  private static final String MISSING_PLAYER_REGION = "UNKNOWN";
  private static final String MISSING_PLAYER_TRANCHE = "N/A";
  private static final String MISSING_USER_NAME = "Utilisateur indisponible";

  private final GameRepository gameRepository;
  private final GameParticipantRepository gameParticipantRepository;
  private final DraftRepository draftRepository;

  /**
   * Récupère les détails complets d'une game Clean Code : méthode principale qui orchestre la
   * récupération
   */
  public GameDetailDto getGameDetails(UUID gameId) {
    log.debug("Récupération des détails de la game {}", gameId);

    // Récupérer la game
    Game game = findGameOrThrow(gameId);

    // Récupérer les participants
    List<GameParticipant> participants = gameParticipantRepository.findByGame(game);

    // Récupérer le draft si présent
    Optional<Draft> draft = draftRepository.findByGame(game);

    // Construire le DTO
    return buildGameDetailDto(game, participants, draft);
  }

  /** Trouve une game ou lance une exception Clean Code : responsabilité unique */
  private Game findGameOrThrow(UUID gameId) {
    return gameRepository
        .findById(gameId)
        .orElseThrow(() -> new GameNotFoundException("Game non trouvée : " + gameId));
  }

  /** Construit le DTO complet Clean Code : méthode de construction isolée */
  private GameDetailDto buildGameDetailDto(
      Game game, List<GameParticipant> participants, Optional<Draft> draft) {
    return GameDetailDto.builder()
        .gameId(game.getId())
        .gameName(game.getName())
        .description(game.getDescription())
        .creatorName(game.getCreator().getUsername())
        .status(game.getStatus().name())
        .invitationCode(game.getInvitationCode())
        .maxParticipants(game.getMaxParticipants())
        .createdAt(game.getCreatedAt())
        .updatedAt(game.getCreatedAt()) // Utiliser createdAt à défaut d'updatedAt
        .participants(buildParticipantInfos(participants, game.getCreator(), game.getId()))
        .draftInfo(draft.map(this::buildDraftInfo).orElse(null))
        .statistics(buildStatistics(participants, game, game.getId()))
        .build();
  }

  /** Construit la liste des informations des participants Clean Code : méthode focalisée */
  private List<ParticipantInfo> buildParticipantInfos(
      List<GameParticipant> participants, User creator, UUID gameId) {
    return participants.stream()
        .map(participant -> buildParticipantInfo(participant, creator, gameId))
        .collect(Collectors.toList());
  }

  /** Construit l'information d'un participant Clean Code : transformation simple et claire */
  private ParticipantInfo buildParticipantInfo(
      GameParticipant participant, User creator, UUID gameId) {
    User user = getParticipantUserSafe(participant, gameId);

    List<PlayerInfo> selectedPlayers = buildPlayerInfosSafe(participant, gameId);

    return ParticipantInfo.builder()
        .participantId(participant.getId())
        .username(user != null ? user.getUsername() : MISSING_USER_NAME)
        .email(user != null ? user.getEmail() : null)
        .joinedAt(resolveJoinedAt(participant))
        .joinOrder(participant.getDraftOrder() != null ? participant.getDraftOrder() : 0)
        .isCreator(isCreator(user, creator))
        .totalPlayers(selectedPlayers.size())
        .selectedPlayers(selectedPlayers)
        .build();
  }

  private LocalDateTime resolveJoinedAt(GameParticipant participant) {
    return participant.getJoinedAt() != null ? participant.getJoinedAt() : LocalDateTime.now();
  }

  private boolean isCreator(User user, User creator) {
    return user != null
        && creator != null
        && user.getId() != null
        && user.getId().equals(creator.getId());
  }

  private User getParticipantUserSafe(GameParticipant participant, UUID gameId) {
    try {
      User user = participant.getUser();
      if (user == null) {
        log.warn(
            "Participant sans utilisateur detecte (participantId={}, gameId={})",
            participant.getId(),
            gameId);
      }
      return user;
    } catch (EntityNotFoundException | ObjectNotFoundException e) {
      log.warn(
          "Utilisateur introuvable (participantId={}, gameId={})", participant.getId(), gameId, e);
      return null;
    }
  }

  private List<PlayerInfo> buildPlayerInfosSafe(GameParticipant participant, UUID gameId) {
    List<Player> players = getSelectedPlayersSafe(participant, gameId);
    if (players.isEmpty()) {
      return Collections.emptyList();
    }

    List<PlayerInfo> infos = new ArrayList<>();
    for (Player player : players) {
      infos.add(buildPlayerInfoOrFallback(player, gameId, participant.getId()));
    }
    return infos;
  }

  private List<Player> getSelectedPlayersSafe(GameParticipant participant, UUID gameId) {
    try {
      List<Player> players = participant.getSelectedPlayers();
      return players != null ? players : Collections.emptyList();
    } catch (EntityNotFoundException | ObjectNotFoundException e) {
      log.warn(
          "Joueurs selectionnes introuvables (participantId={}, gameId={})",
          participant.getId(),
          gameId,
          e);
      return Collections.<Player>singletonList(null);
    }
  }

  private PlayerInfo buildPlayerInfoOrFallback(Player player, UUID gameId, UUID participantId) {
    if (player == null) {
      log.warn(
          "Joueur manquant (participantId={}, gameId={}) - fallback applique",
          participantId,
          gameId);
      return buildMissingPlayerInfo();
    }

    try {
      return buildPlayerInfo(player);
    } catch (EntityNotFoundException | ObjectNotFoundException e) {
      log.warn(
          "Joueur introuvable en base (participantId={}, gameId={}) - fallback applique",
          participantId,
          gameId,
          e);
      return buildMissingPlayerInfo();
    } catch (RuntimeException e) {
      log.error(
          "Erreur lors du mapping du joueur (participantId={}, gameId={})",
          participantId,
          gameId,
          e);
      return buildMissingPlayerInfo();
    }
  }

  private PlayerInfo buildMissingPlayerInfo() {
    return PlayerInfo.builder()
        .playerId(null)
        .nickname(MISSING_PLAYER_NICKNAME)
        .region(MISSING_PLAYER_REGION)
        .tranche(MISSING_PLAYER_TRANCHE)
        .currentScore(0)
        .build();
  }

  /** Construit l'information d'un joueur Clean Code : methode simple et pure */
  private PlayerInfo buildPlayerInfo(Player player) {
    String region = player.getRegion() != null ? player.getRegion().name() : MISSING_PLAYER_REGION;
    String tranche = player.getTranche() != null ? player.getTranche() : MISSING_PLAYER_TRANCHE;

    return PlayerInfo.builder()
        .playerId(player.getId())
        .nickname(player.getNickname())
        .region(region)
        .tranche(tranche)
        .currentScore(calculateCurrentScore(player)) // Calcul du score actuel
        .build();
  }

  /** Construit l'information du draft Clean Code : transformation isolée */
  private DraftInfo buildDraftInfo(Draft draft) {
    return DraftInfo.builder()
        .draftId(draft.getId())
        .status(draft.getStatus().name())
        .startedAt(draft.getStartedAt())
        .finishedAt(draft.getFinishedAt())
        .pausedAt(null) // Champ non disponible
        .currentRound(draft.getCurrentRound())
        .currentPick(draft.getCurrentPick())
        .currentPickerUsername(getCurrentPickerUsername(draft)) // Récupération du picker actuel
        .build();
  }

  /** Calcule les statistiques de la game Clean Code : calculs regroupés */
  private GameStatistics buildStatistics(
      List<GameParticipant> participants, Game game, UUID gameId) {
    int totalParticipants = participants.size();
    int totalPlayers = calculateTotalPlayers(participants, gameId);
    Map<String, Integer> regionDistribution = calculateRegionDistribution(participants, gameId);
    double averagePlayersPerParticipant =
        totalParticipants > 0 ? (double) totalPlayers / totalParticipants : 0.0;
    int maxParticipants = game.getMaxParticipants() != null ? game.getMaxParticipants() : 0;
    int remainingSlots = Math.max(0, maxParticipants - totalParticipants);

    return GameStatistics.builder()
        .totalParticipants(totalParticipants)
        .totalPlayers(totalPlayers)
        .regionDistribution(regionDistribution)
        .averagePlayersPerParticipant(averagePlayersPerParticipant)
        .remainingSlots(remainingSlots)
        .build();
  }

  /** Calcule le nombre total de joueurs Clean Code : méthode focalisée */
  private int calculateTotalPlayers(List<GameParticipant> participants, UUID gameId) {
    return participants.stream()
        .mapToInt(participant -> getSelectedPlayersSafe(participant, gameId).size())
        .sum();
  }

  /** Calcule la distribution par région Clean Code : calcul isolé avec stream */
  private Map<String, Integer> calculateRegionDistribution(
      List<GameParticipant> participants, UUID gameId) {
    Map<String, Integer> distribution = new HashMap<>();

    for (GameParticipant participant : participants) {
      List<Player> players = getSelectedPlayersSafe(participant, gameId);
      for (Player player : players) {
        if (player == null) {
          log.warn(
              "Joueur null dans les statistiques (participantId={}, gameId={})",
              participant.getId(),
              gameId);
          distribution.merge(MISSING_PLAYER_REGION, 1, Integer::sum);
          continue;
        }
        String region =
            player.getRegion() != null ? player.getRegion().name() : MISSING_PLAYER_REGION;
        distribution.merge(region, 1, Integer::sum);
      }
    }

    return distribution;
  }

  /** Calcule le score actuel d'un joueur Clean Code : méthode d'aide focalisée */
  private int calculateCurrentScore(Player player) {
    // Note: Pour l'instant, on retourne 0 car les scores sont dans une table séparée
    // TODO: Intégrer avec ScoreService quand il sera disponible
    return 0;
  }

  /**
   * Récupère le nom d'utilisateur du picker actuel dans un draft Clean Code : méthode d'aide
   * focalisée
   */
  private String getCurrentPickerUsername(Draft draft) {
    if (draft.getStatus() != Draft.Status.IN_PROGRESS) {
      return null;
    }

    // Note: La logique du draft devrait être dans DraftService
    // Pour l'instant, on retourne null car cette info n'est pas facilement accessible
    return null;
  }
}
