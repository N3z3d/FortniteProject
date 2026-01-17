package com.fortnite.pronos.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.TeamRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamQueryService {

  private final TeamRepository teamRepository;

  @Transactional(readOnly = true)
  public List<Team> findTeamsBySeasonWithFetch(int season) {
    return teamRepository.findBySeasonWithFetch(season);
  }

  @Transactional(readOnly = true)
  public Optional<Team> findTeamByIdWithFetch(UUID teamId) {
    return teamRepository.findByIdWithFetch(teamId);
  }

  @Transactional(readOnly = true)
  public Optional<Team> findTeamByOwnerAndSeason(User owner, int season) {
    return teamRepository.findByOwnerAndSeason(owner, season);
  }
}
