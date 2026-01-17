package com.fortnite.pronos.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.core.usecase.CreateGameUseCase;
import com.fortnite.pronos.core.usecase.JoinGameUseCase;
import com.fortnite.pronos.service.FlexibleAuthenticationService;
import com.fortnite.pronos.service.GameService;
import com.fortnite.pronos.service.JwtService;
import com.fortnite.pronos.service.UserService;
import com.fortnite.pronos.service.ValidationService;

@ExtendWith(MockitoExtension.class)
class GameControllerSimpleTest {

  @Mock private GameService gameService;

  @Mock private ValidationService validationService;

  @Mock private FlexibleAuthenticationService flexibleAuthenticationService;

  @Mock private UserService userService;

  @Mock private JwtService jwtService;

  @Mock private CreateGameUseCase createGameUseCase;

  @Mock private JoinGameUseCase joinGameUseCase;

  @InjectMocks private GameController gameController;

  @Test
  void shouldCreateController() {
    assertNotNull(gameController);
  }
}
