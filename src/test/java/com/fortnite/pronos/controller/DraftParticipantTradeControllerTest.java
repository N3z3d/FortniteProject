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

import com.fortnite.pronos.dto.DraftTradeProposalRequest;
import com.fortnite.pronos.dto.DraftTradeProposalResponse;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.UserResolver;
import com.fortnite.pronos.service.draft.DraftParticipantTradeService;

@ExtendWith(MockitoExtension.class)
@DisplayName("DraftParticipantTradeController")
class DraftParticipantTradeControllerTest {

  @Mock private DraftParticipantTradeService tradeService;
  @Mock private UserResolver userResolver;
  @Mock private HttpServletRequest httpRequest;

  private DraftParticipantTradeController controller;

  private final UUID gameId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final UUID tradeId = UUID.randomUUID();
  private final UUID draftId = UUID.randomUUID();
  private final UUID proposerParticipantId = UUID.randomUUID();
  private final UUID targetParticipantId = UUID.randomUUID();
  private final UUID playerFromProposerId = UUID.randomUUID();
  private final UUID playerFromTargetId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    controller = new DraftParticipantTradeController(tradeService, userResolver);
  }

  private User stubUser() {
    User user = new User();
    user.setId(userId);
    user.setUsername("player1");
    return user;
  }

  private DraftTradeProposalResponse buildResponse(String status) {
    return new DraftTradeProposalResponse(
        tradeId,
        draftId,
        proposerParticipantId,
        targetParticipantId,
        playerFromProposerId,
        playerFromTargetId,
        status);
  }

  @Nested
  @DisplayName("Propose Trade")
  class ProposeTrade {

    @Test
    void whenAuthenticated_returns201WithResponse() {
      when(userResolver.resolve(null, httpRequest)).thenReturn(stubUser());
      DraftTradeProposalResponse expected = buildResponse("PENDING");
      when(tradeService.proposeTrade(
              gameId, userId, targetParticipantId, playerFromProposerId, playerFromTargetId))
          .thenReturn(expected);

      DraftTradeProposalRequest request =
          new DraftTradeProposalRequest(
              targetParticipantId, playerFromProposerId, playerFromTargetId);
      var response = controller.proposeTrade(gameId, request, null, httpRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(response.getBody()).isEqualTo(expected);
      verify(tradeService)
          .proposeTrade(
              gameId, userId, targetParticipantId, playerFromProposerId, playerFromTargetId);
    }

    @Test
    void whenNotAuthenticated_returns401() {
      when(userResolver.resolve(null, httpRequest)).thenReturn(null);

      DraftTradeProposalRequest request =
          new DraftTradeProposalRequest(
              targetParticipantId, playerFromProposerId, playerFromTargetId);
      var response = controller.proposeTrade(gameId, request, null, httpRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
      verifyNoInteractions(tradeService);
    }
  }

  @Nested
  @DisplayName("Accept Trade")
  class AcceptTrade {

    @Test
    void whenAuthenticated_returns200WithAcceptedResponse() {
      when(userResolver.resolve(null, httpRequest)).thenReturn(stubUser());
      DraftTradeProposalResponse expected = buildResponse("ACCEPTED");
      when(tradeService.acceptTrade(gameId, userId, tradeId)).thenReturn(expected);

      var response = controller.acceptTrade(gameId, tradeId, null, httpRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isEqualTo(expected);
      verify(tradeService).acceptTrade(gameId, userId, tradeId);
    }

    @Test
    void whenNotAuthenticated_returns401() {
      when(userResolver.resolve(null, httpRequest)).thenReturn(null);

      var response = controller.acceptTrade(gameId, tradeId, null, httpRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
      verifyNoInteractions(tradeService);
    }
  }

  @Nested
  @DisplayName("Reject Trade")
  class RejectTrade {

    @Test
    void whenAuthenticated_returns200WithRejectedResponse() {
      when(userResolver.resolve(null, httpRequest)).thenReturn(stubUser());
      DraftTradeProposalResponse expected = buildResponse("REJECTED");
      when(tradeService.rejectTrade(gameId, userId, tradeId)).thenReturn(expected);

      var response = controller.rejectTrade(gameId, tradeId, null, httpRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isEqualTo(expected);
      verify(tradeService).rejectTrade(gameId, userId, tradeId);
    }

    @Test
    void whenNotAuthenticated_returns401() {
      when(userResolver.resolve(null, httpRequest)).thenReturn(null);

      var response = controller.rejectTrade(gameId, tradeId, null, httpRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
      verifyNoInteractions(tradeService);
    }
  }
}
