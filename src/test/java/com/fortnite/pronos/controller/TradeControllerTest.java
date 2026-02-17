package com.fortnite.pronos.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import com.fortnite.pronos.application.usecase.TradeQueryUseCase;
import com.fortnite.pronos.domain.trade.model.Trade;
import com.fortnite.pronos.domain.trade.model.TradeStatus;
import com.fortnite.pronos.dto.CounterTradeRequestDto;
import com.fortnite.pronos.dto.TradeRequestDto;
import com.fortnite.pronos.dto.TradeResponseDto;
import com.fortnite.pronos.service.TradingService;
import com.fortnite.pronos.service.UserContextService;
import com.fortnite.pronos.service.trade.TradeResponseMapper;

@ExtendWith(MockitoExtension.class)
class TradeControllerTest {

  @Mock private TradingService tradingService;
  @Mock private TradeQueryUseCase tradeQueryUseCase;
  @Mock private UserContextService userContextService;
  @Mock private TradeResponseMapper tradeResponseMapper;
  @InjectMocks private TradeController controller;

  private final UUID userId = UUID.randomUUID();
  private final UUID tradeId = UUID.randomUUID();
  private final UUID teamId = UUID.randomUUID();
  private final UUID gameId = UUID.randomUUID();

  private UserDetails mockUser() {
    UserDetails user = mock(UserDetails.class);
    when(user.getUsername()).thenReturn("testuser");
    return user;
  }

  private com.fortnite.pronos.model.Trade mockModelTrade() {
    com.fortnite.pronos.model.Trade trade = mock(com.fortnite.pronos.model.Trade.class);
    when(trade.getId()).thenReturn(tradeId);
    when(trade.getStatus()).thenReturn(com.fortnite.pronos.model.Trade.Status.PENDING);
    return trade;
  }

  // --- proposeTrade ---

  @Test
  void proposeTrade_returnsCreated() {
    UserDetails user = mockUser();
    when(userContextService.getUserIdFromUsername("testuser")).thenReturn(userId);

    UUID fromTeam = UUID.randomUUID();
    UUID toTeam = UUID.randomUUID();
    UUID playerId1 = UUID.randomUUID();
    UUID playerId2 = UUID.randomUUID();

    TradeRequestDto request =
        TradeRequestDto.builder()
            .fromTeamId(fromTeam)
            .toTeamId(toTeam)
            .offeredPlayerIds(List.of(playerId1))
            .requestedPlayerIds(List.of(playerId2))
            .build();

    com.fortnite.pronos.model.Trade modelTrade = mockModelTrade();
    when(tradingService.proposeTradeWithPlayerIds(
            eq(fromTeam), eq(toTeam), eq(List.of(playerId1)), eq(List.of(playerId2))))
        .thenReturn(modelTrade);

    ResponseEntity<TradeResponseDto> response = controller.proposeTrade(request, user);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  // --- acceptTrade ---

  @Test
  void acceptTrade_returnsOk() {
    UserDetails user = mockUser();
    when(userContextService.getUserIdFromUsername("testuser")).thenReturn(userId);

    com.fortnite.pronos.model.Trade modelTrade = mockModelTrade();
    when(tradingService.acceptTrade(tradeId, userId)).thenReturn(modelTrade);

    ResponseEntity<TradeResponseDto> response = controller.acceptTrade(tradeId, user);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(tradingService).acceptTrade(tradeId, userId);
  }

  // --- rejectTrade ---

  @Test
  void rejectTrade_returnsOk() {
    UserDetails user = mockUser();
    when(userContextService.getUserIdFromUsername("testuser")).thenReturn(userId);

    com.fortnite.pronos.model.Trade modelTrade = mockModelTrade();
    when(tradingService.rejectTrade(tradeId, userId)).thenReturn(modelTrade);

    ResponseEntity<TradeResponseDto> response = controller.rejectTrade(tradeId, user);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(tradingService).rejectTrade(tradeId, userId);
  }

  // --- cancelTrade ---

  @Test
  void cancelTrade_returnsOk() {
    UserDetails user = mockUser();
    when(userContextService.getUserIdFromUsername("testuser")).thenReturn(userId);

    com.fortnite.pronos.model.Trade modelTrade = mockModelTrade();
    when(tradingService.cancelTrade(tradeId, userId)).thenReturn(modelTrade);

    ResponseEntity<TradeResponseDto> response = controller.cancelTrade(tradeId, user);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(tradingService).cancelTrade(tradeId, userId);
  }

  // --- counterTrade ---

  @Test
  void counterTrade_returnsCreated() {
    UserDetails user = mockUser();
    when(userContextService.getUserIdFromUsername("testuser")).thenReturn(userId);

    UUID p1 = UUID.randomUUID();
    UUID p2 = UUID.randomUUID();
    CounterTradeRequestDto request =
        CounterTradeRequestDto.builder()
            .offeredPlayerIds(List.of(p1))
            .requestedPlayerIds(List.of(p2))
            .build();

    com.fortnite.pronos.model.Trade modelTrade = mockModelTrade();
    when(tradingService.counterTradeWithPlayerIds(tradeId, userId, List.of(p1), List.of(p2)))
        .thenReturn(modelTrade);

    ResponseEntity<TradeResponseDto> response = controller.counterTrade(tradeId, request, user);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
  }

  // --- Query endpoints ---

  @Test
  void getTeamTradeHistory_returnsOk() {
    UserDetails user = mockUser();
    List<Trade> trades = List.of();
    when(tradeQueryUseCase.getTeamTradeHistory(teamId)).thenReturn(trades);
    when(tradeResponseMapper.toDtos(trades)).thenReturn(List.of());

    ResponseEntity<List<TradeResponseDto>> response = controller.getTeamTradeHistory(teamId, user);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(0, response.getBody().size());
  }

  @Test
  void getTeamTradeHistory_handlesNullUserDetails() {
    List<Trade> trades = List.of();
    when(tradeQueryUseCase.getTeamTradeHistory(teamId)).thenReturn(trades);
    when(tradeResponseMapper.toDtos(trades)).thenReturn(List.of());

    ResponseEntity<List<TradeResponseDto>> response = controller.getTeamTradeHistory(teamId, null);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void getPendingTradesForTeam_returnsOk() {
    UserDetails user = mockUser();
    List<Trade> trades = List.of();
    when(tradeQueryUseCase.getPendingTradesForTeam(teamId)).thenReturn(trades);
    when(tradeResponseMapper.toDtos(trades)).thenReturn(List.of());

    ResponseEntity<List<TradeResponseDto>> response =
        controller.getPendingTradesForTeam(teamId, user);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void getGameTradeStatistics_returnsOk() {
    UserDetails user = mockUser();
    Map<String, Object> stats = Map.of("total", 5);
    when(tradeQueryUseCase.getGameTradeStatistics(gameId)).thenReturn(stats);

    ResponseEntity<Map<String, Object>> response = controller.getGameTradeStatistics(gameId, user);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(5, response.getBody().get("total"));
  }

  @Test
  void getTrade_returnsOk() {
    UserDetails user = mockUser();
    Trade domainTrade = mock(Trade.class);
    TradeResponseDto dto = new TradeResponseDto();
    when(tradeQueryUseCase.getTrade(tradeId)).thenReturn(domainTrade);
    when(tradeResponseMapper.toDto(domainTrade)).thenReturn(dto);

    ResponseEntity<TradeResponseDto> response = controller.getTrade(tradeId, user);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertSame(dto, response.getBody());
  }

  @Test
  void getGameTrades_withoutStatus_returnsAllTrades() {
    UserDetails user = mockUser();
    List<Trade> trades = List.of();
    when(tradeQueryUseCase.getAllGameTrades(gameId)).thenReturn(trades);
    when(tradeResponseMapper.toDtos(trades)).thenReturn(List.of());

    ResponseEntity<List<TradeResponseDto>> response = controller.getGameTrades(gameId, null, user);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(tradeQueryUseCase).getAllGameTrades(gameId);
    verify(tradeQueryUseCase, never()).getGameTradesByStatus(any(), any());
  }

  @Test
  void getGameTrades_withStatus_filtersbyStatus() {
    UserDetails user = mockUser();
    List<Trade> trades = List.of();
    when(tradeQueryUseCase.getGameTradesByStatus(gameId, TradeStatus.PENDING)).thenReturn(trades);
    when(tradeResponseMapper.toDtos(trades)).thenReturn(List.of());

    ResponseEntity<List<TradeResponseDto>> response =
        controller.getGameTrades(gameId, TradeStatus.PENDING, user);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(tradeQueryUseCase).getGameTradesByStatus(gameId, TradeStatus.PENDING);
    verify(tradeQueryUseCase, never()).getAllGameTrades(any());
  }
}
