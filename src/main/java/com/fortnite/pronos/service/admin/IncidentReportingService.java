package com.fortnite.pronos.service.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.dto.IncidentReportRequest;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.exception.UnauthorizedAccessException;
import com.fortnite.pronos.service.game.GameParticipantService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Orchestrates incident report validation and recording. */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentReportingService {

  private final GameDomainRepositoryPort gameDomainRepository;
  private final GameParticipantService gameParticipantService;
  private final GameIncidentService gameIncidentService;

  /**
   * Validates and records an incident report submitted by a participant.
   *
   * @param gameId the game where the incident occurred
   * @param reporterId the reporting user's ID
   * @param reporterUsername the reporting user's username
   * @param request the incident details
   * @return the recorded IncidentEntry
   * @throws GameNotFoundException if the game does not exist
   * @throws UnauthorizedAccessException if the reporter is not a participant of the game
   */
  public IncidentEntry reportIncident(
      UUID gameId, UUID reporterId, String reporterUsername, IncidentReportRequest request) {

    Game game =
        gameDomainRepository
            .findById(gameId)
            .orElseThrow(() -> new GameNotFoundException("Game not found: " + gameId));

    if (!gameParticipantService.isUserParticipant(reporterId, gameId)) {
      throw new UnauthorizedAccessException("You are not a participant of this game: " + gameId);
    }

    IncidentEntry entry =
        IncidentEntry.builder()
            .id(UUID.randomUUID())
            .gameId(gameId)
            .gameName(game.getName())
            .reporterId(reporterId)
            .reporterUsername(reporterUsername)
            .incidentType(request.getIncidentType())
            .description(request.getDescription())
            .timestamp(OffsetDateTime.now())
            .build();

    gameIncidentService.recordIncident(entry);
    log.info(
        "Incident {} reported by {} in game {}",
        entry.getIncidentType(),
        reporterUsername,
        game.getName());
    return entry;
  }
}
