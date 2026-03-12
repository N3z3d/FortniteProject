package com.fortnite.pronos.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.application.usecase.GameDetailUseCase;
import com.fortnite.pronos.domain.draft.model.Draft;
import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameParticipant;
import com.fortnite.pronos.domain.port.out.DraftDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.DraftPickRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.ScoreRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.dto.GameDetailDto;
import com.fortnite.pronos.dto.GameDetailDto.DraftInfo;
import com.fortnite.pronos.dto.GameDetailDto.GameStatistics;
import com.fortnite.pronos.dto.GameDetailDto.ParticipantInfo;
import com.fortnite.pronos.dto.GameDetailDto.PlayerInfo;
import com.fortnite.pronos.exception.GameNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service pour recuperer les details complets d'une game. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@SuppressWarnings({"java:S1612"})
public class GameDetailService implements GameDetailUseCase {

  private static final String MISSING_PLAYER_NICKNAME = "Joueur indisponible";
  private static final String MISSING_PLAYER_REGION = "UNKNOWN";
  private static final String MISSING_PLAYER_TRANCHE = "N/A";
  private static final String MISSING_USER_NAME = "Utilisateur indisponible";
  private static final int DEFAULT_SEASON = 2025;

  private final GameDomainRepositoryPort gameRepository;
  private final DraftDomainRepositoryPort draftRepository;
  private final DraftPickRepositoryPort draftPickRepository;
  private final PlayerDomainRepositoryPort playerRepository;
  private final ScoreRepositoryPort scoreRepository;
  private final UserRepositoryPort userRepository;

  @Override
  public GameDetailDto getGameDetails(UUID gameId) {
    log.debug("Recuperation des details de la game {}", gameId);
    Game game = findGameOrThrow(gameId);
    List<GameParticipant> participants = game.getParticipants();
    Optional<Draft> draft = draftRepository.findByGameId(gameId);
    Map<UUID, List<UUID>> selectedPlayerIdsByParticipant =
        resolveSelectedPlayerIdsByParticipant(participants, draft);
    return buildGameDetailDto(game, participants, draft, selectedPlayerIdsByParticipant);
  }

  private Game findGameOrThrow(UUID gameId) {
    return gameRepository
        .findById(gameId)
        .orElseThrow(() -> new GameNotFoundException("Game non trouvee : " + gameId));
  }

  private GameDetailDto buildGameDetailDto(
      Game game,
      List<GameParticipant> participants,
      Optional<Draft> draft,
      Map<UUID, List<UUID>> selectedPlayerIdsByParticipant) {
    return GameDetailDto.builder()
        .gameId(game.getId())
        .gameName(game.getName())
        .description(game.getDescription())
        .creatorName(resolveCreatorName(game, participants))
        .status(game.getStatus().name())
        .invitationCode(game.getInvitationCode())
        .maxParticipants(game.getMaxParticipants())
        .createdAt(game.getCreatedAt())
        .updatedAt(game.getCreatedAt())
        .participants(
            buildParticipantInfos(
                participants, game.getCreatorId(), selectedPlayerIdsByParticipant))
        .draftInfo(draft.map(this::buildDraftInfo).orElse(null))
        .statistics(buildStatistics(participants, game, selectedPlayerIdsByParticipant))
        .build();
  }

  private String resolveCreatorName(Game game, List<GameParticipant> participants) {
    if (game.getCreatorId() == null) {
      return MISSING_USER_NAME;
    }

    Optional<String> creatorFromParticipants =
        participants.stream()
            .filter(participant -> game.getCreatorId().equals(participant.getUserId()))
            .map(GameParticipant::getUsername)
            .filter(this::hasText)
            .findFirst();
    if (creatorFromParticipants.isPresent()) {
      return creatorFromParticipants.get();
    }

    return userRepository
        .findById(game.getCreatorId())
        .map(com.fortnite.pronos.model.User::getUsername)
        .filter(this::hasText)
        .orElse(MISSING_USER_NAME);
  }

  private List<ParticipantInfo> buildParticipantInfos(
      List<GameParticipant> participants,
      UUID creatorId,
      Map<UUID, List<UUID>> selectedPlayerIdsByParticipant) {
    return participants.stream()
        .map(
            participant ->
                buildParticipantInfo(participant, creatorId, selectedPlayerIdsByParticipant))
        .toList();
  }

  private ParticipantInfo buildParticipantInfo(
      GameParticipant participant,
      UUID creatorId,
      Map<UUID, List<UUID>> selectedPlayerIdsByParticipant) {
    Optional<com.fortnite.pronos.model.User> user = findUser(participant.getUserId());
    List<PlayerInfo> selectedPlayers =
        buildPlayerInfos(resolveSelectedPlayerIds(participant, selectedPlayerIdsByParticipant));

    return ParticipantInfo.builder()
        .participantId(participant.getId())
        .username(resolveParticipantUsername(participant, user.orElse(null)))
        .email(user.map(com.fortnite.pronos.model.User::getEmail).orElse(null))
        .joinedAt(resolveJoinedAt(participant))
        .joinOrder(participant.getDraftOrder() != null ? participant.getDraftOrder() : 0)
        .isCreator(isCreator(participant, creatorId))
        .totalPlayers(selectedPlayers.size())
        .selectedPlayers(selectedPlayers)
        .build();
  }

  private Optional<com.fortnite.pronos.model.User> findUser(UUID userId) {
    if (userId == null) {
      return Optional.empty();
    }
    return userRepository.findById(userId);
  }

  private String resolveParticipantUsername(
      GameParticipant participant, com.fortnite.pronos.model.User user) {
    if (hasText(participant.getUsername())) {
      return participant.getUsername();
    }
    if (user != null && hasText(user.getUsername())) {
      return user.getUsername();
    }
    return MISSING_USER_NAME;
  }

  private LocalDateTime resolveJoinedAt(GameParticipant participant) {
    return participant.getJoinedAt() != null ? participant.getJoinedAt() : LocalDateTime.now();
  }

  private boolean isCreator(GameParticipant participant, UUID creatorId) {
    return participant.isCreator()
        || (creatorId != null
            && participant.getUserId() != null
            && creatorId.equals(participant.getUserId()));
  }

  private List<PlayerInfo> buildPlayerInfos(List<UUID> selectedPlayerIds) {
    if (selectedPlayerIds == null || selectedPlayerIds.isEmpty()) {
      return Collections.emptyList();
    }

    return selectedPlayerIds.stream().map(this::buildPlayerInfoOrFallback).toList();
  }

  private PlayerInfo buildPlayerInfoOrFallback(UUID playerId) {
    if (playerId == null) {
      return buildMissingPlayerInfo();
    }

    return playerRepository
        .findById(playerId)
        .map(this::buildPlayerInfo)
        .orElseGet(this::buildMissingPlayerInfo);
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

  private PlayerInfo buildPlayerInfo(com.fortnite.pronos.domain.player.model.Player player) {
    String region = player.getRegion() != null ? player.getRegion().name() : MISSING_PLAYER_REGION;
    String tranche = hasText(player.getTranche()) ? player.getTranche() : MISSING_PLAYER_TRANCHE;

    return PlayerInfo.builder()
        .playerId(player.getId())
        .nickname(player.getNickname())
        .region(region)
        .tranche(tranche)
        .currentScore(calculateCurrentScore(player.getId(), player.getCurrentSeason()))
        .build();
  }

  private DraftInfo buildDraftInfo(Draft draft) {
    return DraftInfo.builder()
        .draftId(draft.getId())
        .status(draft.getStatus().name())
        .startedAt(draft.getStartedAt())
        .finishedAt(draft.getFinishedAt())
        .pausedAt(null)
        .currentRound(draft.getCurrentRound())
        .currentPick(draft.getCurrentPick())
        .totalRounds(draft.getTotalRounds())
        .currentPickerUsername(null)
        .build();
  }

  private GameStatistics buildStatistics(
      List<GameParticipant> participants,
      Game game,
      Map<UUID, List<UUID>> selectedPlayerIdsByParticipant) {
    int totalParticipants = calculateTotalParticipants(participants, game.getCreatorId());
    int totalPlayers = calculateTotalPlayers(participants, selectedPlayerIdsByParticipant);
    Map<String, Integer> regionDistribution =
        calculateRegionDistribution(participants, selectedPlayerIdsByParticipant);
    double averagePlayersPerParticipant =
        totalParticipants > 0 ? (double) totalPlayers / totalParticipants : 0.0;
    int remainingSlots = Math.max(0, game.getMaxParticipants() - totalParticipants);

    return GameStatistics.builder()
        .totalParticipants(totalParticipants)
        .totalPlayers(totalPlayers)
        .regionDistribution(regionDistribution)
        .averagePlayersPerParticipant(averagePlayersPerParticipant)
        .remainingSlots(remainingSlots)
        .build();
  }

  private int calculateTotalParticipants(List<GameParticipant> participants, UUID creatorId) {
    int participantsCount = participants.size();
    if (creatorId == null) {
      return participantsCount;
    }

    boolean creatorAlreadyCounted =
        participants.stream().anyMatch(participant -> creatorId.equals(participant.getUserId()));
    return creatorAlreadyCounted ? participantsCount : participantsCount + 1;
  }

  private int calculateTotalPlayers(
      List<GameParticipant> participants, Map<UUID, List<UUID>> selectedPlayerIdsByParticipant) {
    return participants.stream()
        .map(participant -> resolveSelectedPlayerIds(participant, selectedPlayerIdsByParticipant))
        .filter(ids -> ids != null)
        .mapToInt(List::size)
        .sum();
  }

  private Map<String, Integer> calculateRegionDistribution(
      List<GameParticipant> participants, Map<UUID, List<UUID>> selectedPlayerIdsByParticipant) {
    Map<String, Integer> distribution = new HashMap<>();

    for (GameParticipant participant : participants) {
      List<UUID> selectedPlayerIds =
          resolveSelectedPlayerIds(participant, selectedPlayerIdsByParticipant);
      if (selectedPlayerIds == null) {
        continue;
      }

      for (UUID playerId : selectedPlayerIds) {
        String region = resolvePlayerRegion(playerId);
        distribution.merge(region, 1, Integer::sum);
      }
    }

    return distribution;
  }

  private String resolvePlayerRegion(UUID playerId) {
    if (playerId == null) {
      return MISSING_PLAYER_REGION;
    }

    return playerRepository
        .findById(playerId)
        .map(
            player ->
                player.getRegion() != null ? player.getRegion().name() : MISSING_PLAYER_REGION)
        .orElse(MISSING_PLAYER_REGION);
  }

  private int calculateCurrentScore(UUID playerId, int season) {
    if (playerId == null) {
      return 0;
    }

    int effectiveSeason = season > 0 ? season : DEFAULT_SEASON;
    Integer points = scoreRepository.sumPointsByPlayerAndSeason(playerId, effectiveSeason);
    return points != null ? points : 0;
  }

  private boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }

  private Map<UUID, List<UUID>> resolveSelectedPlayerIdsByParticipant(
      List<GameParticipant> participants, Optional<Draft> draft) {
    Map<UUID, List<UUID>> selectedPlayerIdsByParticipant = new HashMap<>();
    UUID draftId = draft.map(Draft::getId).orElse(null);

    for (GameParticipant participant : participants) {
      if (participant.getId() == null) {
        continue;
      }
      selectedPlayerIdsByParticipant.put(
          participant.getId(), loadSelectedPlayerIds(participant, draftId));
    }

    return selectedPlayerIdsByParticipant;
  }

  private List<UUID> loadSelectedPlayerIds(GameParticipant participant, UUID draftId) {
    List<UUID> participantSelectedPlayerIds =
        normalizePlayerIds(participant.getSelectedPlayerIds());
    if (draftId == null || participant.getId() == null) {
      return participantSelectedPlayerIds;
    }

    List<UUID> draftPickPlayerIds =
        normalizePlayerIds(
            draftPickRepository.findPlayerIdsByDraftIdAndParticipantId(
                draftId, participant.getId()));
    if (draftPickPlayerIds.isEmpty() && !participantSelectedPlayerIds.isEmpty()) {
      return participantSelectedPlayerIds;
    }
    return draftPickPlayerIds;
  }

  private List<UUID> resolveSelectedPlayerIds(
      GameParticipant participant, Map<UUID, List<UUID>> selectedPlayerIdsByParticipant) {
    if (participant.getId() == null) {
      return normalizePlayerIds(participant.getSelectedPlayerIds());
    }
    return selectedPlayerIdsByParticipant.getOrDefault(
        participant.getId(), normalizePlayerIds(participant.getSelectedPlayerIds()));
  }

  private List<UUID> normalizePlayerIds(List<UUID> playerIds) {
    return playerIds != null ? playerIds : Collections.emptyList();
  }
}
