package com.fortnite.pronos.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

/** Request body for submitting a snake draft pick. */
@Data
public class SnakePickRequest {

  @NotNull(message = "playerId is required") private UUID playerId;

  @NotBlank(message = "region is required")
  private String region;
}
