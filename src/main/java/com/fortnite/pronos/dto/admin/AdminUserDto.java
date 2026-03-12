package com.fortnite.pronos.dto.admin;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {

  private UUID id;
  private String username;
  private String email;
  private String role;
  private Integer currentSeason;
  private boolean deleted;
}
