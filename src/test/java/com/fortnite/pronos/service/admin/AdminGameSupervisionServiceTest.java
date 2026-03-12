package com.fortnite.pronos.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.dto.admin.GameSupervisionDto;
import com.fortnite.pronos.model.DraftMode;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.User;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminGameSupervisionService")
class AdminGameSupervisionServiceTest {

  @Mock private GameRepositoryPort gameRepository;

  private AdminGameSupervisionService service;

  @BeforeEach
  void setUp() {
    service = new AdminGameSupervisionService(gameRepository);
  }

  private User buildUser(String username) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername(username);
    return user;
  }

  private Game buildGame(String name, GameStatus status, User creator, int participantCount) {
    Game game = new Game();
    game.setId(UUID.randomUUID());
    game.setName(name);
    game.setStatus(status);
    game.setDraftMode(DraftMode.SNAKE);
    game.setMaxParticipants(10);
    game.setCreator(creator);
    game.setCreatedAt(LocalDateTime.now().minusHours(1));
    List<GameParticipant> participants = new java.util.ArrayList<>();
    for (int i = 0; i < participantCount; i++) {
      GameParticipant p = new GameParticipant();
      p.setUser(buildUser("user" + i));
      p.setGame(game);
      participants.add(p);
    }
    game.setParticipants(participants);
    return game;
  }

  @Nested
  @DisplayName("getAllActiveGames")
  class GetAllActiveGames {

    @Test
    @DisplayName("returns list from repository mapped to DTOs")
    void returnsMappedList() {
      User creator = buildUser("adminUser");
      Game game = buildGame("MyGame", GameStatus.ACTIVE, creator, 3);
      when(gameRepository.findByStatusInWithFetch(anyCollection())).thenReturn(List.of(game));

      List<GameSupervisionDto> result = service.getAllActiveGames();

      assertThat(result).hasSize(1);
      assertThat(result.get(0).gameName()).isEqualTo("MyGame");
    }

    @Test
    @DisplayName("passes all three active statuses to repository")
    void passesAllThreeStatuses() {
      when(gameRepository.findByStatusInWithFetch(anyCollection())).thenReturn(List.of());
      ArgumentCaptor<java.util.Collection<GameStatus>> captor =
          ArgumentCaptor.forClass(java.util.Collection.class);

      service.getAllActiveGames();

      verify(gameRepository).findByStatusInWithFetch(captor.capture());
      assertThat(captor.getValue())
          .containsExactlyInAnyOrder(GameStatus.CREATING, GameStatus.DRAFTING, GameStatus.ACTIVE);
    }

    @Test
    @DisplayName("maps all DTO fields correctly")
    void mapsAllFields() {
      User creator = buildUser("thibaut");
      Game game = buildGame("Epic Game", GameStatus.DRAFTING, creator, 4);
      when(gameRepository.findByStatusInWithFetch(anyCollection())).thenReturn(List.of(game));

      GameSupervisionDto dto = service.getAllActiveGames().get(0);

      assertThat(dto.gameId()).isEqualTo(game.getId());
      assertThat(dto.gameName()).isEqualTo("Epic Game");
      assertThat(dto.status()).isEqualTo("DRAFTING");
      assertThat(dto.draftMode()).isEqualTo("SNAKE");
      assertThat(dto.participantCount()).isEqualTo(4);
      assertThat(dto.maxParticipants()).isEqualTo(10);
      assertThat(dto.creatorUsername()).isEqualTo("thibaut");
      assertThat(dto.createdAt()).isInstanceOf(OffsetDateTime.class);
      assertThat(dto.createdAt().toLocalDateTime()).isEqualTo(game.getCreatedAt());
    }

    @Test
    @DisplayName("returns empty list when repository returns nothing")
    void emptyRepository() {
      when(gameRepository.findByStatusInWithFetch(anyCollection())).thenReturn(List.of());

      List<GameSupervisionDto> result = service.getAllActiveGames();

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("getActiveGamesByStatus")
  class GetActiveGamesByStatus {

    @Test
    @DisplayName("passes single status to repository")
    void passesSingleStatus() {
      when(gameRepository.findByStatusInWithFetch(anyCollection())).thenReturn(List.of());
      ArgumentCaptor<java.util.Collection<GameStatus>> captor =
          ArgumentCaptor.forClass(java.util.Collection.class);

      service.getActiveGamesByStatus(GameStatus.DRAFTING);

      verify(gameRepository).findByStatusInWithFetch(captor.capture());
      assertThat(captor.getValue()).containsExactly(GameStatus.DRAFTING);
    }

    @Test
    @DisplayName("participantCount equals size of participants list")
    void participantCountMatchesListSize() {
      User creator = buildUser("host");
      Game game = buildGame("Counting Game", GameStatus.ACTIVE, creator, 5);
      when(gameRepository.findByStatusInWithFetch(anyCollection())).thenReturn(List.of(game));

      GameSupervisionDto dto = service.getActiveGamesByStatus(GameStatus.ACTIVE).get(0);

      assertThat(dto.participantCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("throws IllegalArgumentException for non-active status FINISHED")
    void rejectsFinishedStatus() {
      assertThatThrownBy(() -> service.getActiveGamesByStatus(GameStatus.FINISHED))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("FINISHED");
    }

    @Test
    @DisplayName("throws IllegalArgumentException for non-active status CANCELLED")
    void rejectsCancelledStatus() {
      assertThatThrownBy(() -> service.getActiveGamesByStatus(GameStatus.CANCELLED))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("CANCELLED");
    }
  }
}
