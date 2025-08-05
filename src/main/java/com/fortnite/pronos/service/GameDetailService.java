package com.fortnite.pronos.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
        .participants(buildParticipantInfos(participants, game.getCreator()))
        .draftInfo(draft.map(this::buildDraftInfo).orElse(null))
        .statistics(buildStatistics(participants, game))
        .build();
  }

  /** Construit la liste des informations des participants Clean Code : méthode focalisée */
  private List<ParticipantInfo> buildParticipantInfos(
      List<GameParticipant> participants, User creator) {
    return participants.stream()
        .map(participant -> buildParticipantInfo(participant, creator))
        .collect(Collectors.toList());
  }

  /** Construit l'information d'un participant Clean Code : transformation simple et claire */
  private ParticipantInfo buildParticipantInfo(GameParticipant participant, User creator) {
    User user = participant.getUser();
    List<Player> selectedPlayers = participant.getSelectedPlayers();

    return ParticipantInfo.builder()
        .participantId(participant.getId())
        .username(user.getUsername())
        .email(user.getEmail())
        .joinedAt(LocalDateTime.now()) // Valeur par défaut
        .joinOrder(
            participant.getDraftOrder() != null
                ? participant.getDraftOrder()
                : 0) // Utiliser draftOrder
        .isCreator(user.getId().equals(creator.getId()))
        .totalPlayers(selectedPlayers.size())
        .selectedPlayers(buildPlayerInfos(selectedPlayers))
        .build();
  }

  /** Construit la liste des informations des joueurs Clean Code : transformation de collection */
  private List<PlayerInfo> buildPlayerInfos(List<Player> players) {
    return players.stream().map(this::buildPlayerInfo).collect(Collectors.toList());
  }

  /** Construit l'information d'un joueur Clean Code : méthode simple et pure */
  private PlayerInfo buildPlayerInfo(Player player) {
    return PlayerInfo.builder()
        .playerId(player.getId())
        .nickname(player.getNickname())
        .region(player.getRegion().name())
        .tranche(player.getTranche())
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
  private GameStatistics buildStatistics(List<GameParticipant> participants, Game game) {
    int totalParticipants = participants.size();
    int totalPlayers = calculateTotalPlayers(participants);
    Map<String, Integer> regionDistribution = calculateRegionDistribution(participants);
    double averagePlayersPerParticipant =
        totalParticipants > 0 ? (double) totalPlayers / totalParticipants : 0.0;
    int remainingSlots = game.getMaxParticipants() - totalParticipants;

    return GameStatistics.builder()
        .totalParticipants(totalParticipants)
        .totalPlayers(totalPlayers)
        .regionDistribution(regionDistribution)
        .averagePlayersPerParticipant(averagePlayersPerParticipant)
        .remainingSlots(remainingSlots)
        .build();
  }

  /** Calcule le nombre total de joueurs Clean Code : méthode focalisée */
  private int calculateTotalPlayers(List<GameParticipant> participants) {
    return participants.stream().mapToInt(p -> p.getSelectedPlayers().size()).sum();
  }

  /** Calcule la distribution par région Clean Code : calcul isolé avec stream */
  private Map<String, Integer> calculateRegionDistribution(List<GameParticipant> participants) {
    Map<String, Integer> distribution = new HashMap<>();

    participants.stream()
        .flatMap(p -> p.getSelectedPlayers().stream())
        .forEach(
            player -> {
              String region = player.getRegion().name();
              distribution.merge(region, 1, Integer::sum);
            });

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
