package com.fortnite.pronos.service.game;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameParticipantRepository;
import com.fortnite.pronos.repository.GameRepository;

@Service
@Transactional
public class GameCreatorParticipantBackfillService {

  private final GameRepository gameRepository;
  private final GameParticipantRepository participantRepository;
  private final JpaRepository<GameParticipant, UUID> participantJpaRepository;
  private final Clock clock;

  @Autowired
  public GameCreatorParticipantBackfillService(
      GameRepository gameRepository, GameParticipantRepository participantRepository) {
    this(gameRepository, participantRepository, Clock.systemUTC());
  }

  GameCreatorParticipantBackfillService(
      GameRepository gameRepository, GameParticipantRepository participantRepository, Clock clock) {
    this.gameRepository = Objects.requireNonNull(gameRepository);
    this.participantRepository = Objects.requireNonNull(participantRepository);
    this.participantJpaRepository = participantRepository;
    this.clock = Objects.requireNonNull(clock);
  }

  @Transactional(readOnly = true)
  public List<UUID> findGameIdsMissingCreatorParticipant() {
    return gameRepository.findNonDeletedGamesMissingCreatorParticipant().stream()
        .map(Game::getId)
        .toList();
  }

  public BackfillResult backfillMissingCreatorParticipants() {
    int updatedCreatorFlagCount = markExistingCreatorParticipants();
    int insertedCount = insertMissingCreatorParticipants();
    return new BackfillResult(insertedCount, updatedCreatorFlagCount);
  }

  private int markExistingCreatorParticipants() {
    List<GameParticipant> participants =
        participantRepository.findNonDeletedCreatorParticipantsWithoutCreatorFlag();
    for (GameParticipant participant : participants) {
      participant.setCreator(true);
      if (participant.getDraftOrder() == null && participant.getGame() != null) {
        participant.setDraftOrder(nextDraftOrderFor(participant.getGame().getId()));
      }
      if (participant.getJoinedAt() == null) {
        participant.setJoinedAt(joinedAtFor(participant.getGame()));
      }
    }
    if (!participants.isEmpty()) {
      participantJpaRepository.saveAll(participants);
    }
    return participants.size();
  }

  private int insertMissingCreatorParticipants() {
    int insertedCount = 0;
    for (Game game : gameRepository.findNonDeletedGamesMissingCreatorParticipant()) {
      if (canInsertCreatorParticipant(game)) {
        participantJpaRepository.save(createCreatorParticipant(game));
        insertedCount++;
      }
    }
    return insertedCount;
  }

  private boolean canInsertCreatorParticipant(Game game) {
    if (game == null || game.getId() == null || game.getCreator() == null) {
      return false;
    }
    User creator = game.getCreator();
    return creator.getId() != null
        && !participantRepository.existsByGameIdAndUserId(game.getId(), creator.getId());
  }

  private GameParticipant createCreatorParticipant(Game game) {
    return GameParticipant.builder()
        .game(game)
        .user(game.getCreator())
        .draftOrder(nextDraftOrderFor(game.getId()))
        .joinedAt(joinedAtFor(game))
        .creator(true)
        .build();
  }

  private int nextDraftOrderFor(UUID gameId) {
    return participantRepository.findByGameIdOrderByJoinedAt(gameId).stream()
            .map(GameParticipant::getDraftOrder)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .max()
            .orElse(0)
        + 1;
  }

  private LocalDateTime joinedAtFor(Game game) {
    if (game != null && game.getCreatedAt() != null) {
      return game.getCreatedAt();
    }
    return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
  }

  public record BackfillResult(int insertedCount, int updatedCreatorFlagCount) {}
}
