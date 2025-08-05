package com.fortnite.pronos.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.service.GameService;
import com.fortnite.pronos.service.UserContextService;
import com.fortnite.pronos.service.ValidationService;

@ExtendWith(MockitoExtension.class)
class GameControllerSimpleTest {

  @Mock private GameService gameService;

  @Mock private ValidationService validationService;

  @Mock private UserContextService userContextService;

  @InjectMocks private GameController gameController;

  @Test
  void shouldCreateController() {
    assertNotNull(gameController);
  }
}
