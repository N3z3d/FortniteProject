package com.fortnite.pronos.dto.player;

import java.util.UUID;

import com.fortnite.pronos.model.Player;

import lombok.Data;

@Data
public class PlayerDto {
  private UUID id;
  private String username;
  private String nickname;
  private Player.Region region;
  private String tranche;
  private String fortniteId;
  private Integer totalPoints;
  private boolean isAvailable;
  private Integer currentSeason;

  public static PlayerDto fromEntity(Player player) {
    PlayerDto dto = new PlayerDto();
    dto.id = player.getId();
    dto.fortniteId = player.getFortniteId();
    dto.username = player.getUsername();
    dto.nickname = player.getNickname();
    // Use the enum field directly, not the getRegion() method which returns String
    dto.region = player.region;
    dto.tranche = player.getTranche();
    dto.currentSeason = player.getCurrentSeason();
    return dto;
  }

  public static PlayerDto from(Player player, Integer totalPoints, boolean includeDetails) {
    PlayerDto dto = new PlayerDto();
    dto.id = player.getId();
    dto.fortniteId = player.getFortniteId();
    dto.username = player.getUsername();
    dto.nickname = player.getNickname();
    // Use the enum field directly, not the getRegion() method which returns String
    dto.region = player.region;
    dto.tranche = player.getTranche();
    dto.totalPoints = totalPoints;
    dto.currentSeason = player.getCurrentSeason();
    dto.isAvailable = includeDetails;

    return dto;
  }
}
