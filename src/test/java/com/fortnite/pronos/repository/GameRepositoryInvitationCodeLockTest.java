package com.fortnite.pronos.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.fortnite.pronos.adapter.out.persistence.game.GameEntityMapper;
import com.fortnite.pronos.adapter.out.persistence.game.GameRepositoryAdapter;
import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.dto.JoinGameRequest;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.game.GameParticipantService;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("test")
@Import({GameParticipantService.class, GameRepositoryAdapter.class, GameEntityMapper.class})
@DisplayName("GameRepository - invitation code locking")
class GameRepositoryInvitationCodeLockTest {

  private static final String LOCKED_CODE = "LOCK1234";
  private static final String JOIN_LOCKED_CODE = "JOIN1234";

  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private GameRepository gameRepository;
  @Autowired private GameParticipantRepository gameParticipantRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private GameParticipantService gameParticipantService;

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void findByInvitationCodeForUpdateSerializesConcurrentCodeConsumption() throws Exception {
    CreatedGame createdGame = createGameWithInvitationCode(LOCKED_CODE);
    CountDownLatch firstTransactionLockedCode = new CountDownLatch(1);
    CountDownLatch releaseFirstTransaction = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      Future<Boolean> firstConsumer =
          executor.submit(
              () ->
                  consumeCodeInTransaction(
                      LOCKED_CODE, firstTransactionLockedCode, releaseFirstTransaction));
      assertThat(firstTransactionLockedCode.await(5, TimeUnit.SECONDS)).isTrue();

      Future<Optional<Game>> secondConsumer =
          executor.submit(() -> findByInvitationCodeForUpdateInTransaction(LOCKED_CODE));
      assertSecondConsumerWaitsForFirstTransaction(secondConsumer);

      releaseFirstTransaction.countDown();
      assertThat(firstConsumer.get(5, TimeUnit.SECONDS)).isTrue();
      assertThat(secondConsumer.get(5, TimeUnit.SECONDS)).isEmpty();
    } finally {
      releaseFirstTransaction.countDown();
      executor.shutdownNow();
      try {
        executor.awaitTermination(5, TimeUnit.SECONDS);
      } finally {
        deleteCreatedGame(createdGame);
      }
    }
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void joinGameWithInvitationCodeAllowsOnlyOneConcurrentConsumer() throws Exception {
    CreatedGame createdGame = createGameWithInvitationCode(JOIN_LOCKED_CODE);
    UUID firstJoinerId = createUser("join-lock-a");
    UUID secondJoinerId = createUser("join-lock-b");
    CountDownLatch consumersReady = new CountDownLatch(2);
    CountDownLatch startConsumers = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      Future<JoinAttempt> firstJoin =
          executor.submit(
              () ->
                  joinWithCodeAfterStart(
                      firstJoinerId, JOIN_LOCKED_CODE, consumersReady, startConsumers));
      Future<JoinAttempt> secondJoin =
          executor.submit(
              () ->
                  joinWithCodeAfterStart(
                      secondJoinerId, JOIN_LOCKED_CODE, consumersReady, startConsumers));

      assertThat(consumersReady.await(5, TimeUnit.SECONDS)).isTrue();
      startConsumers.countDown();

      JoinAttempt firstResult = firstJoin.get(10, TimeUnit.SECONDS);
      JoinAttempt secondResult = secondJoin.get(10, TimeUnit.SECONDS);

      assertThat(firstResult.succeeded()).isNotEqualTo(secondResult.succeeded());
      assertThat(firstResult.failureType() + " " + secondResult.failureType())
          .contains("GameNotFoundException");
      assertThat(gameParticipantRepository.countByGameId(createdGame.gameId())).isEqualTo(1);
      assertThat(findInvitationCode(createdGame.gameId())).isNull();
    } finally {
      startConsumers.countDown();
      executor.shutdownNow();
      try {
        executor.awaitTermination(5, TimeUnit.SECONDS);
      } finally {
        deleteCreatedGame(createdGame, firstJoinerId, secondJoinerId);
      }
    }
  }

  private CreatedGame createGameWithInvitationCode(String invitationCode) {
    return transactionTemplate()
        .execute(
            status -> {
              User savedCreator = createUserEntity("lock-test");

              Game game = new Game();
              game.setName("Invitation Lock Test");
              game.setCreator(savedCreator);
              game.setMaxParticipants(4);
              game.setStatus(GameStatus.CREATING);
              game.setCreatedAt(LocalDateTime.now());
              game.setInvitationCode(invitationCode);
              game.setInvitationCodeExpiresAt(LocalDateTime.now().plusHours(1));
              Game savedGame = ((GameRepositoryPort) gameRepository).saveAndFlush(game);

              return new CreatedGame(savedGame.getId(), savedCreator.getId());
            });
  }

  private UUID createUser(String prefix) {
    return transactionTemplate().execute(status -> createUserEntity(prefix).getId());
  }

  private User createUserEntity(String prefix) {
    User user = new User();
    user.setUsername(prefix + "-" + UUID.randomUUID());
    user.setEmail(user.getUsername() + "@test.com");
    user.setPassword("password123");
    user.setRole(User.UserRole.USER);
    user.setCurrentSeason(2026);
    return userRepository.saveAndFlush(user);
  }

  private boolean consumeCodeInTransaction(
      String invitationCode, CountDownLatch lockedCode, CountDownLatch releaseTransaction) {
    transactionTemplate()
        .executeWithoutResult(
            status -> {
              Game lockedGame =
                  gameRepository.findByInvitationCodeForUpdate(invitationCode).orElseThrow();
              lockedCode.countDown();
              await(releaseTransaction);
              lockedGame.setInvitationCode(null);
              lockedGame.setInvitationCodeExpiresAt(null);
              ((GameRepositoryPort) gameRepository).saveAndFlush(lockedGame);
            });
    return true;
  }

  private Optional<Game> findByInvitationCodeForUpdateInTransaction(String invitationCode) {
    return transactionTemplate()
        .execute(status -> gameRepository.findByInvitationCodeForUpdate(invitationCode));
  }

  private JoinAttempt joinWithCodeAfterStart(
      UUID userId, String invitationCode, CountDownLatch ready, CountDownLatch start) {
    ready.countDown();
    await(start);
    try {
      boolean joined =
          transactionTemplate()
              .execute(
                  status -> gameParticipantService.joinGame(userId, joinRequest(invitationCode)));
      return new JoinAttempt(Boolean.TRUE.equals(joined), null);
    } catch (RuntimeException exception) {
      return new JoinAttempt(false, exception.getClass().getSimpleName());
    }
  }

  private JoinGameRequest joinRequest(String invitationCode) {
    JoinGameRequest request = new JoinGameRequest();
    request.setInvitationCode(invitationCode);
    return request;
  }

  private void assertSecondConsumerWaitsForFirstTransaction(Future<Optional<Game>> secondConsumer)
      throws Exception {
    try {
      Optional<Game> prematureResult = secondConsumer.get(200, TimeUnit.MILLISECONDS);
      fail("Second transaction completed before the first consumed the code: " + prematureResult);
    } catch (TimeoutException expected) {
      assertThat(secondConsumer.isDone()).isFalse();
    }
  }

  private String findInvitationCode(UUID gameId) {
    return transactionTemplate()
        .execute(status -> gameRepository.findById(gameId).orElseThrow().getInvitationCode());
  }

  private void deleteCreatedGame(CreatedGame createdGame, UUID... additionalUserIds) {
    transactionTemplate()
        .executeWithoutResult(
            status -> {
              gameRepository.deleteById(createdGame.gameId());
              userRepository.deleteById(createdGame.creatorId());
              for (UUID userId : additionalUserIds) {
                userRepository.deleteById(userId);
              }
            });
  }

  private TransactionTemplate transactionTemplate() {
    return new TransactionTemplate(transactionManager);
  }

  private static void await(CountDownLatch latch) {
    try {
      assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Interrupted while waiting for concurrent transaction", exception);
    }
  }

  private record CreatedGame(UUID gameId, UUID creatorId) {}

  private record JoinAttempt(boolean succeeded, String failureType) {}
}
