package com.fortnite.pronos.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.dto.admin.AdminUserDto;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.User;

@ExtendWith(MockitoExtension.class)
class AdminGameCatalogServiceTest {

  @Mock private UserRepositoryPort userRepository;
  @Mock private GameRepositoryPort gameRepository;

  private AdminGameCatalogService service;

  @BeforeEach
  void setUp() {
    service = new AdminGameCatalogService(userRepository, gameRepository);
  }

  @Test
  void mapsAllUsersFromPortsToAdminDtos() {
    User activeUser = buildUser("thibaut", false);
    User deletedUser = buildUser("marcel", true);
    when(userRepository.findAll()).thenReturn(List.of(activeUser, deletedUser));

    List<AdminUserDto> result = service.getAllUsers();

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getUsername()).isEqualTo("thibaut");
    assertThat(result.get(0).isDeleted()).isFalse();
    assertThat(result.get(1).getUsername()).isEqualTo("marcel");
    assertThat(result.get(1).isDeleted()).isTrue();
  }

  @Test
  void trimsAndNormalizesStatusBeforeFilteringGames() {
    Game activeGame = new Game();
    activeGame.setName("Active game");
    activeGame.setStatus(GameStatus.ACTIVE);
    when(gameRepository.findByStatus(GameStatus.ACTIVE)).thenReturn(List.of(activeGame));

    List<Game> result = service.getAllGames(" active ");

    assertThat(result).containsExactly(activeGame);
    verify(gameRepository).findByStatus(GameStatus.ACTIVE);
    verify(gameRepository, never()).findAll();
  }

  @Test
  void fallsBackToAllGamesWhenStatusIsInvalid() {
    Game game = new Game();
    game.setName("Fallback game");
    when(gameRepository.findAll()).thenReturn(List.of(game));

    List<Game> result = service.getAllGames("not-a-status");

    assertThat(result).containsExactly(game);
    verify(gameRepository).findAll();
  }

  private User buildUser(String username, boolean deleted) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername(username);
    user.setEmail(username + "@fortnite-pronos.com");
    user.setRole(User.UserRole.USER);
    user.setCurrentSeason(2025);
    if (deleted) {
      user.setDeletedAt(java.time.LocalDateTime.now());
    }
    return user;
  }
}
