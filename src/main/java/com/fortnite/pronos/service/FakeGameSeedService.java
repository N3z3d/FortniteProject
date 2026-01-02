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

import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.GameRegionRule;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.TeamRepository;
import com.fortnite.pronos.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FakeGameSeedService {

  private static final String FAKE_GAME_NAME = "Fake League - 4 Players";
  private static final String FAKE_GAME_DESCRIPTION =
      "Mini dataset for multi-game navigation tests";
  private static final int FAKE_MAX_PARTICIPANTS = 4;
  private static final int FAKE_PLAYER_COUNT = 4;

  private final GameRepository gameRepository;
  private final UserRepository userRepository;
  private final PlayerRepository playerRepository;
  private final TeamRepository teamRepository;
  private final Environment environment;

  @EventListener(ApplicationReadyEvent.class)
  @Order(Ordered.LOWEST_PRECEDENCE)
  @Transactional
  public void seedFakeGame() {
    if (!isDevProfile()) {
      return;
    }

    Optional<User> creatorOption = userRepository.findByUsername("Thibaut");
    if (creatorOption.isEmpty()) {
      log.info("Fake game seed skipped: creator user not found");
      return;
    }

    User creator = creatorOption.get();
    if (gameRepository.existsByNameAndCreator(FAKE_GAME_NAME, creator)) {
      log.info("Fake game already seeded: {}", FAKE_GAME_NAME);
      return;
    }

    List<User> participants = buildParticipants(creator);
    List<Player> players = pickPlayers();
    if (players.size() < FAKE_PLAYER_COUNT) {
      log.warn("Fake game seed skipped: need {} players, found {}", FAKE_PLAYER_COUNT, players.size());
      return;
    }

    Game game = buildGame(creator);
    attachParticipants(game, participants);
    Game savedGame = gameRepository.save(game);

    List<Team> teams = buildTeams(savedGame, participants, players);
    teamRepository.saveAll(teams);

    log.info("Fake game seeded: {} (participants={}, players={})", FAKE_GAME_NAME, participants.size(), players.size());
  }

  private boolean isDevProfile() {
    String[] profiles = environment.getActiveProfiles();
    for (String profile : profiles) {
      if (profile.equals("dev")
          || profile.equals("local")
          || profile.equals("quickstart")
          || profile.equals("h2")
          || profile.equals("fast-startup")) {
        return true;
      }
    }
    return false;
  }

  private List<User> buildParticipants(User creator) {
    List<User> participants = new ArrayList<>();
    participants.add(creator);
    userRepository.findByUsername("Teddy").ifPresent(participants::add);
    userRepository.findByUsername("Marcel").ifPresent(participants::add);
    return participants;
  }

  private List<Player> pickPlayers() {
    return playerRepository
        .findAll(PageRequest.of(0, FAKE_PLAYER_COUNT, Sort.by("nickname").ascending()))
        .getContent();
  }

  private Game buildGame(User creator) {
    Game game =
        Game.builder()
            .name(FAKE_GAME_NAME)
            .description(FAKE_GAME_DESCRIPTION)
            .creator(creator)
            .maxParticipants(FAKE_MAX_PARTICIPANTS)
            .status(GameStatus.CREATING)
            .participants(new ArrayList<>())
            .regionRules(new ArrayList<>())
            .build();

    addRegionRules(game);
    return game;
  }

  private void addRegionRules(Game game) {
    game.addRegionRule(
        GameRegionRule.builder().game(game).region(Player.Region.EU).maxPlayers(2).build());
    game.addRegionRule(
        GameRegionRule.builder().game(game).region(Player.Region.NAC).maxPlayers(2).build());
  }

  private void attachParticipants(Game game, List<User> participants) {
    for (int i = 0; i < participants.size(); i++) {
      User user = participants.get(i);
      GameParticipant participant = new GameParticipant();
      participant.setGame(game);
      participant.setUser(user);
      participant.setDraftOrder(i + 1);
      participant.setJoinedAt(java.time.LocalDateTime.now());
      participant.setCreator(user.getId().equals(game.getCreator().getId()));
      game.addParticipant(participant);
    }
  }

  private List<Team> buildTeams(Game game, List<User> participants, List<Player> players) {
    List<Team> teams = new ArrayList<>();
    int index = 0;
    int baseCount = players.size() / participants.size();
    int remainder = players.size() % participants.size();

    for (int i = 0; i < participants.size(); i++) {
      int count = baseCount + (i < remainder ? 1 : 0);
      List<Player> assigned = players.subList(index, Math.min(index + count, players.size()));
      Team team = buildTeam(game, participants.get(i), assigned, i + 1);
      teams.add(team);
      index += count;
    }

    return teams;
  }

  private Team buildTeam(Game game, User owner, List<Player> players, int order) {
    Team team = new Team();
    team.setName("Fake Team " + order + " - " + owner.getUsername());
    team.setOwner(owner);
    team.setSeason(owner.getCurrentSeason() != null ? owner.getCurrentSeason() : 2025);
    team.setGame(game);

    for (int i = 0; i < players.size(); i++) {
      team.addPlayer(players.get(i), i + 1);
    }

    return team;
  }
}
