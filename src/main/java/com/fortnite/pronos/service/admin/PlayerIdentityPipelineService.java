package com.fortnite.pronos.service.admin;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.fortnite.pronos.domain.player.identity.model.IdentityStatus;
import com.fortnite.pronos.domain.player.identity.model.PlayerIdentityEntry;
import com.fortnite.pronos.domain.player.identity.model.RegionalStatRow;
import com.fortnite.pronos.domain.player.model.FortnitePlayerData;
import com.fortnite.pronos.domain.port.out.EpicIdValidatorPort;
import com.fortnite.pronos.domain.port.out.PlayerIdentityRepositoryPort;
import com.fortnite.pronos.domain.port.out.ResolutionPort;
import com.fortnite.pronos.dto.admin.AdapterInfoResponse;
import com.fortnite.pronos.dto.admin.EpicIdSuggestionResponse;
import com.fortnite.pronos.dto.admin.PipelineCountResponse;
import com.fortnite.pronos.dto.admin.PipelineRegionalStatsDto;
import com.fortnite.pronos.dto.admin.PlayerIdentityEntryResponse;
import com.fortnite.pronos.exception.InvalidEpicIdException;
import com.fortnite.pronos.exception.PlayerIdentityNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerIdentityPipelineService {

  static final String WS_TOPIC = "/topic/admin/pipeline";
  private static final int MAX_PAGE_SIZE = 200;
  private static final int DEFAULT_PAGE_SIZE = 50;

  private final PlayerIdentityRepositoryPort identityRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final ConfidenceScoreService confidenceScoreService;
  private final EpicIdValidatorPort epicIdValidator;
  private final ResolutionPort resolutionPort;

  public List<PlayerIdentityEntryResponse> getUnresolved() {
    return getUnresolved(0, DEFAULT_PAGE_SIZE);
  }

  public List<PlayerIdentityEntryResponse> getUnresolved(int page, int size) {
    int safePage = Math.max(page, 0);
    int safeSize = sanitizePageSize(size);
    return identityRepository
        .findByStatusPaged(IdentityStatus.UNRESOLVED, safePage, safeSize)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  public List<PlayerIdentityEntryResponse> getResolved() {
    return getResolved(0, DEFAULT_PAGE_SIZE);
  }

  public List<PlayerIdentityEntryResponse> getResolved(int page, int size) {
    int safePage = Math.max(page, 0);
    int safeSize = sanitizePageSize(size);
    return identityRepository
        .findByStatusPaged(IdentityStatus.RESOLVED, safePage, safeSize)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  public PipelineCountResponse getCount() {
    long unresolved = identityRepository.countByStatus(IdentityStatus.UNRESOLVED);
    long resolved = identityRepository.countByStatus(IdentityStatus.RESOLVED);
    return new PipelineCountResponse(unresolved, resolved);
  }

  public AdapterInfoResponse getAdapterInfo() {
    return new AdapterInfoResponse(resolutionPort.adapterName());
  }

  public PlayerIdentityEntryResponse resolve(UUID playerId, String epicId, String resolvedBy) {
    if (!epicIdValidator.validate(epicId)) {
      throw new InvalidEpicIdException(epicId);
    }

    PlayerIdentityEntry entry = findEntry(playerId);
    int score = confidenceScoreService.compute(entry, epicId);
    entry.resolve(epicId, score, resolvedBy);
    PlayerIdentityEntry saved = identityRepository.save(entry);

    broadcastCount();
    log.info("Player {} resolved with Epic ID {} (score {})", playerId, epicId, score);
    return toResponse(saved);
  }

  public PlayerIdentityEntryResponse reject(UUID playerId, String reason, String rejectedBy) {
    PlayerIdentityEntry entry = findEntry(playerId);
    entry.reject(reason, rejectedBy);
    PlayerIdentityEntry saved = identityRepository.save(entry);

    broadcastCount();
    log.info("Player {} rejected (reason: {})", playerId, reason);
    return toResponse(saved);
  }

  public PlayerIdentityEntryResponse correctMetadata(
      UUID playerId, String newUsername, String newRegion, String correctedBy) {
    PlayerIdentityEntry entry = findEntry(playerId);
    entry.correctMetadata(newUsername, newRegion, correctedBy);
    PlayerIdentityEntry saved = identityRepository.save(entry);
    log.info("Player {} metadata corrected by {}", playerId, correctedBy);
    return toResponse(saved);
  }

  public EpicIdSuggestionResponse suggestEpicId(UUID playerId) {
    PlayerIdentityEntry entry = findEntry(playerId);
    try {
      return resolutionPort
          .resolvePlayer(entry.getPlayerUsername(), entry.getPlayerRegion())
          .map(data -> toSuggestion(entry, data))
          .orElse(EpicIdSuggestionResponse.notFound());
    } catch (RuntimeException e) {
      log.warn(
          "suggestEpicId: resolution unavailable for player {} ({}): {}",
          playerId,
          entry.getPlayerUsername(),
          e.getMessage());
      return EpicIdSuggestionResponse.notFound();
    }
  }

  private EpicIdSuggestionResponse toSuggestion(
      PlayerIdentityEntry entry, FortnitePlayerData data) {
    String epicAccountId = data.epicAccountId();
    if (epicAccountId == null || epicAccountId.isBlank()) {
      return EpicIdSuggestionResponse.notFound();
    }
    int score = confidenceScoreService.compute(entry, epicAccountId, data.displayName());
    return new EpicIdSuggestionResponse(epicAccountId, data.displayName(), score, true);
  }

  public List<PipelineRegionalStatsDto> getRegionalStats() {
    List<RegionalStatRow> rows = identityRepository.countByRegionAndStatus();
    Map<String, LocalDateTime> lastDates = identityRepository.findLastIngestedAtByRegion();
    return assembleRegionalStats(rows, lastDates);
  }

  private List<PipelineRegionalStatsDto> assembleRegionalStats(
      List<RegionalStatRow> rows, Map<String, LocalDateTime> lastDates) {
    Map<String, Map<IdentityStatus, Long>> grouped =
        rows.stream()
            .collect(
                Collectors.groupingBy(
                    RegionalStatRow::region,
                    Collectors.groupingBy(
                        RegionalStatRow::status, Collectors.summingLong(RegionalStatRow::count))));

    return grouped.entrySet().stream()
        .map(
            e -> {
              String region = e.getKey();
              Map<IdentityStatus, Long> counts = e.getValue();
              long unresolved = counts.getOrDefault(IdentityStatus.UNRESOLVED, 0L);
              long resolved = counts.getOrDefault(IdentityStatus.RESOLVED, 0L);
              long rejected = counts.getOrDefault(IdentityStatus.REJECTED, 0L);
              long total = unresolved + resolved + rejected;
              LocalDateTime lastAt = lastDates.get(region);
              return new PipelineRegionalStatsDto(
                  region, unresolved, resolved, rejected, total, lastAt);
            })
        .sorted(Comparator.comparing(PipelineRegionalStatsDto::region))
        .toList();
  }

  private PlayerIdentityEntry findEntry(UUID playerId) {
    return identityRepository
        .findByPlayerId(playerId)
        .orElseThrow(() -> new PlayerIdentityNotFoundException(playerId));
  }

  private void broadcastCount() {
    PipelineCountResponse count = getCount();
    messagingTemplate.convertAndSend(WS_TOPIC, count);
  }

  private int sanitizePageSize(int requestedSize) {
    if (requestedSize <= 0) {
      return DEFAULT_PAGE_SIZE;
    }
    return Math.min(requestedSize, MAX_PAGE_SIZE);
  }

  private PlayerIdentityEntryResponse toResponse(PlayerIdentityEntry e) {
    return new PlayerIdentityEntryResponse(
        e.getId(),
        e.getPlayerId(),
        e.getPlayerUsername(),
        e.getPlayerRegion(),
        e.getEpicId(),
        e.getStatus().name(),
        e.getConfidenceScore(),
        e.getResolvedBy(),
        e.getResolvedAt(),
        e.getRejectedAt(),
        e.getRejectionReason(),
        e.getCreatedAt(),
        e.getCorrectedUsername(),
        e.getCorrectedRegion(),
        e.getCorrectedBy(),
        e.getCorrectedAt());
  }
}
