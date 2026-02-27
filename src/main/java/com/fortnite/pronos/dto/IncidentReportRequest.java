package com.fortnite.pronos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

/** Request body for submitting a game incident report. */
@Data
public class IncidentReportRequest {

  /** Type of incident that a participant can report in a game. */
  public enum IncidentType {
    CHEATING,
    ABUSE,
    BUG,
    DISPUTE,
    OTHER
  }

  @NotNull(message = "Incident type is required") private IncidentType incidentType;

  @NotBlank(message = "Description is required")
  @Size(max = 1000, message = "Description must not exceed 1000 characters")
  private String description;
}
