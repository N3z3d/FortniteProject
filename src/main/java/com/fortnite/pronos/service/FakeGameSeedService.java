package com.fortnite.pronos.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.config.SeedProperties;
import com.fortnite.pronos.domain.port.out.GameRepositoryPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * BE-P1-01: Disabled by default - only enabled with 'fake-data' profile. This service creates
 * additional test games that can cause data inconsistency. Use only for specific multi-game testing
 * scenarios.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@org.springframework.context.annotation.Profile("fake-data")
public class FakeGameSeedService {
  private static final String FAKE_GAME_NAME = "Fake League - 4 Players";
  private static final String FAKE_GAME_DESCRIPTION =
      "Mini dataset for multi-game navigation tests";
  private static final int FAKE_MAX_PARTICIPANTS = 4;
  private static final int FAKE_PLAYER_COUNT = 4;
  private static final int REGION_MAX_PLAYERS = 2;
  private static final int DEFAULT_SEASON = 2025;

  private final GameRepositoryPort gameRepository;
  private final com.fortnite.pronos.repository.UserRepository userRepository;
  private final com.fortnite.pronos.repository.PlayerRepository playerRepository;
  private final com.fortnite.pronos.repository.TeamRepository teamRepository;
  private final Environment environment;
  private final SeedProperties seedProperties;

  @EventListener(ApplicationReadyEvent.class)
  @Order(Ordered.LOWEST_PRECEDENCE)
  @Transactional
  public void seedFakeGame() {
    if (!seedProperties.isFakeGameEnabled()) {
      log.info("Fake game seed disabled (fortnite.seed.fake-game-enabled=false)");
      return;
    }
    if (!isDevProfile()) {
      return;
    }

    Optional<com.fortnite.pronos.model.User> creatorOption =
        userRepository.findByUsername("Thibaut");
    if (creatorOption.isEmpty()) {
      log.info("Fake game seed skipped: creator user not found");
      return;
    }

    seedForCreator(creatorOption.get());
  }

  private void seedForCreator(com.fortnite.pronos.model.User creator) {
    if (gameRepository.existsByNameAndCreator(FAKE_GAME_NAME, creator)) {
      log.info("Fake game already seeded: {}", FAKE_GAME_NAME);
      return;
    }

    List<com.fortnite.pronos.model.Player> players = pickPlayers();
    if (players.size() < FAKE_PLAYER_COUNT) {
      log.warn(
          "Fake game seed skipped: need {} players, found {}", FAKE_PLAYER_COUNT, players.size());
      return;
    }

    List<com.fortnite.pronos.model.User> participants = buildParticipants(creator);
    com.fortnite.pronos.model.Game game = buildGame(creator);
    attachParticipants(game, participants);
    com.fortnite.pronos.model.Game savedGame = gameRepository.save(game);

    List<com.fortnite.pronos.model.Team> teams = buildTeams(savedGame, participants, players);
    teamRepository.saveAll(teams);

    log.info(
        "Fake game seeded: {} (participants={}, players={})",
        FAKE_GAME_NAME,
        participants.size(),
        players.size());
  }

  private boolean isDevProfile() {
    for (String profile : environment.getActiveProfiles()) {
      if ("dev".equals(profile)) {
        return true;
      }
    }
    return false;
  }

  private List<com.fortnite.pronos.model.User> buildParticipants(
      com.fortnite.pronos.model.User creator) {
    List<com.fortnite.pronos.model.User> participants = new ArrayList<>();
    participants.add(creator);
    userRepository.findByUsername("Teddy").ifPresent(participants::add);
    userRepository.findByUsername("Marcel").ifPresent(participants::add);
    return participants;
  }

  private List<com.fortnite.pronos.model.Player> pickPlayers() {
    return playerRepository
        .findAll(PageRequest.of(0, FAKE_PLAYER_COUNT, Sort.by("nickname").ascending()))
        .getContent();
  }

  private com.fortnite.pronos.model.Game buildGame(com.fortnite.pronos.model.User creator) {
    com.fortnite.pronos.model.Game game =
        com.fortnite.pronos.model.Game.builder()
            .name(FAKE_GAME_NAME)
            .description(FAKE_GAME_DESCRIPTION)
            .creator(creator)
            .maxParticipants(FAKE_MAX_PARTICIPANTS)
            .status(com.fortnite.pronos.model.GameStatus.CREATING)
            .participants(new ArrayList<>())
            .regionRules(new ArrayList<>())
            .build();

    addRegionRules(game);
    return game;
  }

  private void addRegionRules(com.fortnite.pronos.model.Game game) {
    game.addRegionRule(
        com.fortnite.pronos.model.GameRegionRule.builder()
            .game(game)
            .region(com.fortnite.pronos.model.Player.Region.EU)
            .maxPlayers(REGION_MAX_PLAYERS)
            .build());
    game.addRegionRule(
        com.fortnite.pronos.model.GameRegionRule.builder()
            .game(game)
            .region(com.fortnite.pronos.model.Player.Region.NAC)
            .maxPlayers(REGION_MAX_PLAYERS)
            .build());
  }

  private void attachParticipants(
      com.fortnite.pronos.model.Game game, List<com.fortnite.pronos.model.User> participants) {
    for (int i = 0; i < participants.size(); i++) {
      com.fortnite.pronos.model.User user = participants.get(i);
      com.fortnite.pronos.model.GameParticipant participant =
          new com.fortnite.pronos.model.GameParticipant();
      participant.setGame(game);
      participant.setUser(user);
      participant.setDraftOrder(i + 1);
      participant.setJoinedAt(java.time.LocalDateTime.now());
      participant.setCreator(user.getId().equals(game.getCreator().getId()));
      game.addParticipant(participant);
    }
  }

  private List<com.fortnite.pronos.model.Team> buildTeams(
      com.fortnite.pronos.model.Game game,
      List<com.fortnite.pronos.model.User> participants,
      List<com.fortnite.pronos.model.Player> players) {
    List<com.fortnite.pronos.model.Team> teams = new ArrayList<>();
    int index = 0;
    int baseCount = players.size() / participants.size();
    int remainder = players.size() % participants.size();

    for (int i = 0; i < participants.size(); i++) {
      int count = baseCount + (i < remainder ? 1 : 0);
      List<com.fortnite.pronos.model.Player> assigned =
          players.subList(index, Math.min(index + count, players.size()));
      com.fortnite.pronos.model.Team team = buildTeam(game, participants.get(i), assigned, i + 1);
      teams.add(team);
      index += count;
    }

    return teams;
  }

  private com.fortnite.pronos.model.Team buildTeam(
      com.fortnite.pronos.model.Game game,
      com.fortnite.pronos.model.User owner,
      List<com.fortnite.pronos.model.Player> players,
      int order) {
    com.fortnite.pronos.model.Team team = new com.fortnite.pronos.model.Team();
    team.setName("Fake Team " + order + " - " + owner.getUsername());
    team.setOwner(owner);
    team.setSeason(owner.getCurrentSeason() != null ? owner.getCurrentSeason() : DEFAULT_SEASON);
    team.setGame(game);

    for (int i = 0; i < players.size(); i++) {
      team.addPlayer(players.get(i), i + 1);
    }

    return team;
  }
}
