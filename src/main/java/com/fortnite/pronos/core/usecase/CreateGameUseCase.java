package com.fortnite.pronos.core.usecase;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.domain.game.model.DraftMode;
import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameParticipant;
import com.fortnite.pronos.domain.game.model.GameRegionRule;
import com.fortnite.pronos.domain.game.model.GameStatus;
import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.dto.CreateGameRequest;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.dto.mapper.GameDtoMapper;
import com.fortnite.pronos.exception.InvalidGameRequestException;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.ValidationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Use Case for creating games. Uses domain models via GameDomainRepositoryPort. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateGameUseCase {

  private static final int ADMIN_MAX_ACTIVE_GAMES = 20;
  private static final int USER_MAX_ACTIVE_GAMES = 5;
  private static final int AUTO_USERNAME_PREFIX_LENGTH = 8;
  private static final int DEFAULT_FALLBACK_SEASON = 2025;
  private static final int DEFAULT_PLAYERS_PER_REGION = 7;

  private final GameDomainRepositoryPort gameDomainRepositoryPort;
  private final UserRepositoryPort userRepositoryPort;
  private final ValidationService validationService;

  @Transactional
  public GameDto execute(UUID userId, CreateGameRequest request) {
    log.info(
        "Executing CreateGameUseCase for user {} with game name '{}'", userId, request.getName());

    validateGameRequest(request);

    User creator = userRepositoryPort.findById(userId).orElseGet(() -> createFallbackUser(userId));
    request.setCreatorId(creator.getId());

    validateUserCanCreateGame(creator);

    DraftMode draftMode = request.getDraftMode() != null ? request.getDraftMode() : DraftMode.SNAKE;
    int teamSize = request.getTeamSize() != null ? request.getTeamSize() : 5;
    int trancheSize = request.getTrancheSize() != null ? request.getTrancheSize() : 10;
    boolean tranchesEnabled = !Boolean.FALSE.equals(request.getTranchesEnabled());
    Game game =
        new Game(
            request.getName(),
            creator.getId(),
            request.getMaxParticipants(),
            draftMode,
            teamSize,
            trancheSize,
            tranchesEnabled);
    if (request.getDescription() != null) {
      game.setDescription(request.getDescription());
    }
    if (request.getCompetitionStart() != null) {
      game.setCompetitionStart(request.getCompetitionStart());
    }
    if (request.getCompetitionEnd() != null) {
      game.setCompetitionEnd(request.getCompetitionEnd());
    }
    addRegionRules(game, request.getRegionRules());

    GameParticipant creatorParticipant =
        new GameParticipant(creator.getId(), creator.getUsername(), true);
    creatorParticipant.setDraftOrder(1);
    game.addParticipant(creatorParticipant);

    Game savedGame = gameDomainRepositoryPort.save(game);

    log.info("Successfully created game '{}' with ID {}", savedGame.getName(), savedGame.getId());

    GameDto dto = GameDtoMapper.fromDomainGame(savedGame);
    dto.setCreatorUsername(creator.getUsername());
    dto.setCreatorName(creator.getUsername());
    return dto;
  }

  private void validateUserCanCreateGame(User user) {
    if (user.getRole() == null) {
      throw new InvalidGameRequestException("User must have a valid role to create games");
    }

    long activeGamesCount =
        gameDomainRepositoryPort.countByCreatorAndStatusIn(
            user.getId(), List.of(GameStatus.CREATING, GameStatus.DRAFTING, GameStatus.ACTIVE));

    int maxActiveGames =
        user.getRole().toString().equals("ADMIN") ? ADMIN_MAX_ACTIVE_GAMES : USER_MAX_ACTIVE_GAMES;

    if (activeGamesCount >= maxActiveGames) {
      throw new InvalidGameRequestException(
          String.format(
              "User cannot have more than %d active games. Current: %d",
              maxActiveGames, activeGamesCount));
    }
  }

  private void validateGameRequest(CreateGameRequest request) {
    validationService.validateCreateGameRequest(request);

    if (request.getRegionRules() != null && !request.getRegionRules().isEmpty()) {
      try {
        validationService.validateRegionRules(request.getRegionRules());
      } catch (IllegalArgumentException e) {
        throw new InvalidGameRequestException("Invalid region rules: " + e.getMessage());
      }
    }
  }

  private void addRegionRules(
      Game game, Map<com.fortnite.pronos.model.Player.Region, Integer> regionRules) {
    if (regionRules == null || regionRules.isEmpty()) {
      PlayerRegion.ACTIVE_REGIONS.forEach(
          region -> game.addRegionRule(new GameRegionRule(region, DEFAULT_PLAYERS_PER_REGION)));
      return;
    }

    regionRules.forEach(
        (region, maxPlayers) -> {
          PlayerRegion domainRegion = PlayerRegion.valueOf(region.name());
          game.addRegionRule(new GameRegionRule(domainRegion, maxPlayers));
        });
  }

  private synchronized User createFallbackUser(UUID userId) {
    String fallbackUsername = "auto-" + userId.toString().substring(0, AUTO_USERNAME_PREFIX_LENGTH);
    return userRepositoryPort
        .findByUsername(fallbackUsername)
        .orElseGet(
            () -> {
              User fallbackUser = new User();
              fallbackUser.setId(userId);
              fallbackUser.setUsername(fallbackUsername);
              fallbackUser.setEmail("auto+" + userId + "@fortnite-pronos.test");
              fallbackUser.setPassword("placeholder");
              fallbackUser.setRole(User.UserRole.ADMIN);
              fallbackUser.setCurrentSeason(DEFAULT_FALLBACK_SEASON);
              return userRepositoryPort.save(fallbackUser);
            });
  }
}
