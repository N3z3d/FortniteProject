package com.fortnite.pronos.dto.team;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fortnite.pronos.model.Team;

import lombok.Data;

@Data
public class TeamDto {
  private UUID id;
  private UUID userId;
  private String userEmail;
  private List<TeamPlayerDto> players;
  private String name;
  private Integer season;
  private String ownerUsername;
  private Integer totalScore;

  @Data
  public static class TeamPlayerDto {
    private UUID playerId;
    private String nickname;
    private String region;
    private String tranche;
  }

  public static TeamDto from(Team team) {
    TeamDto dto = new TeamDto();
    dto.id = team.getId();
    dto.name = team.getName();
    dto.season = team.getSeason();
    dto.ownerUsername = team.getOwner().getUsername();

    dto.totalScore =
        team.getPlayers().stream()
            .filter(tp -> tp.getUntil() == null)
            .mapToInt(
                tp ->
                    tp.getPlayer().getScores().stream()
                        .filter(s -> s.getSeason() == team.getSeason())
                        .mapToInt(s -> s.getPoints())
                        .sum())
            .sum();

    dto.players =
        team.getPlayers().stream()
            .filter(tp -> tp.getUntil() == null)
            .map(
                tp -> {
                  TeamPlayerDto playerDto = new TeamPlayerDto();
                  playerDto.playerId = tp.getPlayer().getId();
                  playerDto.nickname = tp.getPlayer().getNickname();
                  playerDto.region = tp.getPlayer().region.name();
                  playerDto.tranche = tp.getPlayer().getTranche();
                  return playerDto;
                })
            .collect(Collectors.toList());

    return dto;
  }
}
