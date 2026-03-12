package com.fortnite.pronos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.fortnite.pronos.dto.SwapSoloRequest;
import com.fortnite.pronos.dto.SwapSoloResponse;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.UserResolver;
import com.fortnite.pronos.service.draft.SwapSoloService;

@ExtendWith(MockitoExtension.class)
class SwapSoloControllerTest {

  @Mock private SwapSoloService swapSoloService;
  @Mock private UserResolver userResolver;
  @Mock private HttpServletRequest httpRequest;

  private SwapSoloController controller;

  private final UUID gameId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final UUID playerOutId = UUID.randomUUID();
  private final UUID playerInId = UUID.randomUUID();
  private final UUID draftId = UUID.randomUUID();
  private final UUID participantId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    controller = new SwapSoloController(swapSoloService, userResolver);
  }

  private User stubUser() {
    User user = new User();
    user.setId(userId);
    user.setUsername("player1");
    return user;
  }

  @Nested
  @DisplayName("Swap Solo")
  class SwapSolo {

    @Test
    void whenValid_returns200WithResponse() {
      when(userResolver.resolve(null, httpRequest)).thenReturn(stubUser());
      SwapSoloResponse expected =
          new SwapSoloResponse(draftId, participantId, playerOutId, playerInId);
      when(swapSoloService.executeSoloSwap(gameId, userId, playerOutId, playerInId))
          .thenReturn(expected);

      SwapSoloRequest request = new SwapSoloRequest(playerOutId, playerInId);
      var response = controller.swapSolo(gameId, request, null, httpRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isEqualTo(expected);
      verify(swapSoloService).executeSoloSwap(gameId, userId, playerOutId, playerInId);
    }

    @Test
    void whenNotAuthenticated_returns401() {
      when(userResolver.resolve(null, httpRequest)).thenReturn(null);

      SwapSoloRequest request = new SwapSoloRequest(playerOutId, playerInId);
      var response = controller.swapSolo(gameId, request, null, httpRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
      verifyNoInteractions(swapSoloService);
    }
  }
}
