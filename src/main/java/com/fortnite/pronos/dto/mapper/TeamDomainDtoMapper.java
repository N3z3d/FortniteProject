package com.fortnite.pronos.dto.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import com.fortnite.pronos.domain.player.model.Player;
import com.fortnite.pronos.domain.team.model.Team;
import com.fortnite.pronos.domain.team.model.TeamMember;
import com.fortnite.pronos.dto.team.TeamDto;
import com.fortnite.pronos.model.User;

/**
 * Maps domain team aggregates to TeamDto without relying on legacy Team JPA entities.
 *
 * <p>Owner and player details are resolved through resolver functions to keep the mapper reusable
 * and infrastructure-agnostic.
 */
@SuppressWarnings({"java:S2789"})
public final class TeamDomainDtoMapper {

  public TeamDto fromDomainTeam(
      Team team,
      Function<UUID, Optional<User>> userResolver,
      Function<UUID, Optional<Player>> playerResolver) {
    TeamDto dto = new TeamDto();
    dto.setId(team.getId());
    dto.setName(team.getName());
    dto.setSeason(team.getSeason());
    dto.setUserId(team.getOwnerId());
    applyOwner(dto, team.getOwnerId(), userResolver);
    dto.setPlayers(mapActivePlayers(team.getActiveMembers(), playerResolver));
    dto.setTotalScore(0);
    return dto;
  }

  private void applyOwner(TeamDto dto, UUID ownerId, Function<UUID, Optional<User>> userResolver) {
    Optional<User> owner = resolve(ownerId, userResolver);
    owner.ifPresent(
        user -> {
          dto.setOwnerUsername(user.getUsername());
          dto.setUserEmail(user.getEmail());
        });
  }

  private List<TeamDto.TeamPlayerDto> mapActivePlayers(
      List<TeamMember> members, Function<UUID, Optional<Player>> playerResolver) {
    return members.stream()
        .filter(TeamMember::isActive)
        .sorted(Comparator.comparingInt(TeamMember::getPosition))
        .map(member -> mapPlayer(member.getPlayerId(), playerResolver))
        .flatMap(Optional::stream)
        .toList();
  }

  private Optional<TeamDto.TeamPlayerDto> mapPlayer(
      UUID playerId, Function<UUID, Optional<Player>> playerResolver) {
    Optional<Player> player = resolve(playerId, playerResolver);
    if (player.isEmpty()) {
      return Optional.empty();
    }
    Player resolvedPlayer = player.orElseThrow();
    TeamDto.TeamPlayerDto dto = new TeamDto.TeamPlayerDto();
    dto.setPlayerId(resolvedPlayer.getId());
    dto.setNickname(resolvedPlayer.getNickname());
    dto.setRegion(resolvedPlayer.getRegionName());
    dto.setTranche(resolvedPlayer.getTranche());
    return Optional.of(dto);
  }

  private <T> Optional<T> resolve(UUID id, Function<UUID, Optional<T>> resolver) {
    if (id == null || resolver == null) {
      return Optional.empty();
    }
    Optional<T> resolved = resolver.apply(id);
    return resolved == null ? Optional.empty() : resolved;
  }
}
