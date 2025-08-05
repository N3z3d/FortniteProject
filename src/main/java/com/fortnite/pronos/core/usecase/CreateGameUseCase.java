package com.fortnite.pronos.core.usecase;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.dto.CreateGameRequest;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.exception.InvalidGameRequestException;
import com.fortnite.pronos.exception.UserNotFoundException;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.UserRepository;
import com.fortnite.pronos.service.ValidationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PHASE 2A: CLEAN ARCHITECTURE - Use Case for creating games
 *
 * <p>This represents the business logic layer in Clean Architecture: - Contains
 * application-specific business rules - Independent of external frameworks and databases - Defines
 * the use case for creating a fantasy league game
 *
 * <p>Clean Architecture Layers: 1. Controllers (Presentation) → 2. Use Cases (Application) → 3.
 * Entities (Domain) → 4. Repositories (Infrastructure)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateGameUseCase {

  private final GameRepository gameRepository;
  private final UserRepository userRepository;
  private final ValidationService validationService;

  /**
   * Execute the create game use case
   *
   * @param userId The ID of the user creating the game
   * @param request The game creation request
   * @return The created game DTO
   * @throws UserNotFoundException if user doesn't exist
   * @throws InvalidGameRequestException if request is invalid
   */
  @Transactional
  public GameDto execute(UUID userId, CreateGameRequest request) {
    log.info(
        "Executing CreateGameUseCase for user {} with game name '{}'", userId, request.getName());

    // 1. Validate business rules
    validationService.validateCreateGameRequest(request);

    // 2. Verify user exists and can create games
    User creator =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

    // 3. Apply business logic - check user permissions, game limits, etc.
    validateUserCanCreateGame(creator);

    // 4. Create the domain entity
    Game game =
        Game.builder()
            .name(request.getName())
            .description(request.getDescription())
            .creator(creator)
            .maxParticipants(request.getMaxParticipants())
            .status(GameStatus.CREATING)
            .build();

    // 5. Persist the entity
    Game savedGame = gameRepository.save(game);

    log.info("Successfully created game '{}' with ID {}", savedGame.getName(), savedGame.getId());

    // 6. Return the result as DTO (crossing architectural boundaries)
    return GameDto.fromGame(savedGame);
  }

  /** Business rule: Validate user can create games */
  private void validateUserCanCreateGame(User user) {
    // Example business rules:
    // - User must be verified
    // - User can't have more than X active games
    // - Premium users might have higher limits

    if (user.getRole() == null) {
      throw new InvalidGameRequestException("User must have a valid role to create games");
    }

    // Check existing active games count
    long activeGamesCount =
        gameRepository.countByCreatorAndStatusIn(
            user,
            java.util.List.of(
                GameStatus.CREATING,
                GameStatus.DRAFT_PENDING,
                GameStatus.DRAFTING,
                GameStatus.ACTIVE));

    int maxActiveGames =
        user.getRole().toString().equals("ADMIN") ? 10 : 3; // Example business rule

    if (activeGamesCount >= maxActiveGames) {
      throw new InvalidGameRequestException(
          String.format(
              "User cannot have more than %d active games. Current: %d",
              maxActiveGames, activeGamesCount));
    }
  }
}
