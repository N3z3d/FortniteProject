package com.fortnite.pronos.core.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameParticipant;
import com.fortnite.pronos.domain.game.model.GameStatus;
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

  private final GameDomainRepositoryPort gameDomainRepositoryPort;
  private final UserRepositoryPort userRepositoryPort;
  private final ValidationService validationService;

  @Transactional
  public GameDto execute(UUID userId, CreateGameRequest request) {
    log.info(
        "Executing CreateGameUseCase for user {} with game name '{}'", userId, request.getName());

    validationService.validateCreateGameRequest(request);

    User creator = userRepositoryPort.findById(userId).orElseGet(() -> createFallbackUser(userId));
    request.setCreatorId(creator.getId());

    validateUserCanCreateGame(creator);

    Game game = new Game(request.getName(), creator.getId(), request.getMaxParticipants());
    if (request.getDescription() != null) {
      game.setDescription(request.getDescription());
    }

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

    int maxActiveGames = user.getRole().toString().equals("ADMIN") ? 20 : 5;

    if (activeGamesCount >= maxActiveGames) {
      throw new InvalidGameRequestException(
          String.format(
              "User cannot have more than %d active games. Current: %d",
              maxActiveGames, activeGamesCount));
    }
  }

  private synchronized User createFallbackUser(UUID userId) {
    String fallbackUsername = "auto-" + userId.toString().substring(0, 8);
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
              fallbackUser.setCurrentSeason(2025);
              return userRepositoryPort.save(fallbackUser);
            });
  }
}
