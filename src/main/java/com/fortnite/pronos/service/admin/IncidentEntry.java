package com.fortnite.pronos.service.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fortnite.pronos.dto.IncidentReportRequest.IncidentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Immutable snapshot of a participant-submitted game incident for admin monitoring. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class IncidentEntry {

  private UUID id;
  private UUID gameId;
  private String gameName;
  private UUID reporterId;
  private String reporterUsername;
  private IncidentType incidentType;
  private String description;
  private OffsetDateTime timestamp;
}
