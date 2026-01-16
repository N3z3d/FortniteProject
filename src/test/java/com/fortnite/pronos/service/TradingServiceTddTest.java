package com.fortnite.pronos.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.fortnite.pronos.exception.BusinessException;
import com.fortnite.pronos.model.*;
import com.fortnite.pronos.repository.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("TDD - TradingService")
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class TradingServiceTddTest {

  @Mock private TradeRepository tradeRepository;

  @Mock private TeamRepository teamRepository;

  @Mock private PlayerRepository playerRepository;

  @Mock private GameRepository gameRepository;

  @Mock private ValidationService validationService;

  @Mock private TradeNotificationService tradeNotificationService;

  @InjectMocks private TradingService tradingService;

  private Game game;
  private User user1;
  private User user2;
  private Team team1;
  private Team team2;
  private Player player1;
  private Player player2;
  private Player player3;
  private Player player4;

  @BeforeEach
  void setUp() {
    // Setup test data
    game = new Game();
    game.setId(UUID.randomUUID());
    game.setName("Test Game");
    game.setStatus(GameStatus.ACTIVE);
    game.setTradingEnabled(true);
    game.setMaxTradesPerTeam(5);

    user1 = new User();
    user1.setId(UUID.randomUUID());
    user1.setUsername("user1");

    user2 = new User();
    user2.setId(UUID.randomUUID());
    user2.setUsername("user2");

    team1 = new Team();
    team1.setId(UUID.randomUUID());
    team1.setName("Team 1");
    team1.setUser(user1);
    team1.setGame(game);
    team1.setPlayers(new ArrayList<>());

    team2 = new Team();
    team2.setId(UUID.randomUUID());
    team2.setName("Team 2");
    team2.setUser(user2);
    team2.setGame(game);
    team2.setPlayers(new ArrayList<>());

    player1 = new Player();
    player1.setId(UUID.randomUUID());
    player1.setName("Player 1");
    player1.setRegion("EU");

    player2 = new Player();
    player2.setId(UUID.randomUUID());
    player2.setName("Player 2");
    player2.setRegion("NA");

    player3 = new Player();
    player3.setId(UUID.randomUUID());
    player3.setName("Player 3");
    player3.setRegion(Player.Region.EU);

    player4 = new Player();
    player4.setId(UUID.randomUUID());
    player4.setName("Player 4");
    player4.setRegion(Player.Region.NAW);
  }

  private void addPlayer(Team team, Player player) {
    TeamPlayer tp = new TeamPlayer();
    tp.setTeam(team);
    tp.setPlayer(player);
    tp.setPosition(team.getPlayers().size() + 1);
    team.getPlayers().add(tp);
  }

  private void addPlayers(Team team, Player... players) {
    for (Player p : players) {
      addPlayer(team, p);
    }
  }

  @Nested
  @DisplayName("Proposition de Trade")
  class ProposeTrade {

    @Test
    @DisplayName("Devrait créer une proposition de trade valide")
    void shouldCreateValidTradeProposal() {
      // Given
      addPlayer(team1, player1);
      addPlayer(team2, player2);

      List<Player> offeredPlayers = List.of(player1);
      List<Player> requestedPlayers = List.of(player2);

      when(teamRepository.findById(team1.getId())).thenReturn(Optional.of(team1));
      when(teamRepository.findById(team2.getId())).thenReturn(Optional.of(team2));
      when(tradeRepository.save(any(Trade.class))).thenAnswer(i -> i.getArgument(0));

      // When
      Trade trade =
          tradingService.proposeTrade(
              team1.getId(), team2.getId(), offeredPlayers, requestedPlayers);

      // Then
      assertNotNull(trade);
      assertEquals(Trade.Status.PENDING, trade.getStatus());
      assertEquals(team1, trade.getFromTeam());
      assertEquals(team2, trade.getToTeam());
      assertEquals(offeredPlayers, trade.getOfferedPlayers());
      assertEquals(requestedPlayers, trade.getRequestedPlayers());
      assertNotNull(trade.getProposedAt());
      verify(tradeRepository).save(any(Trade.class));
    }

    @Test
    @DisplayName("Devrait rejeter un trade si le trading est désactivé pour la game")
    void shouldRejectTradeIfTradingDisabled() {
      // Given
      game.setTradingEnabled(false);
      when(teamRepository.findById(team1.getId())).thenReturn(Optional.of(team1));
      when(teamRepository.findById(team2.getId())).thenReturn(Optional.of(team2));

      // When & Then
      assertThrows(
          BusinessException.class,
          () ->
              tradingService.proposeTrade(
                  team1.getId(), team2.getId(), List.of(player1), List.of(player2)));
    }

    @Test
    @DisplayName("Devrait rejeter un trade entre équipes de games différentes")
    void shouldRejectTradeBetweenDifferentGames() {
      // Given
      Game otherGame = new Game();
      otherGame.setId(UUID.randomUUID());
      team2.setGame(otherGame);

      when(teamRepository.findById(team1.getId())).thenReturn(Optional.of(team1));
      when(teamRepository.findById(team2.getId())).thenReturn(Optional.of(team2));

      // When & Then
      assertThrows(
          BusinessException.class,
          () ->
              tradingService.proposeTrade(
                  team1.getId(), team2.getId(), List.of(player1), List.of(player2)));
    }

    @Test
    @DisplayName("Devrait rejeter un trade si une équipe a atteint la limite de trades")
    void shouldRejectTradeIfMaxTradesReached() {
      // Given
      team1.setCompletedTradesCount(5);

      when(teamRepository.findById(team1.getId())).thenReturn(Optional.of(team1));
      when(teamRepository.findById(team2.getId())).thenReturn(Optional.of(team2));

      // When & Then
      assertThrows(
          BusinessException.class,
          () ->
              tradingService.proposeTrade(
                  team1.getId(), team2.getId(), List.of(player1), List.of(player2)));
    }

    @Test
    @DisplayName("Devrait rejeter un trade avec un joueur non possédé")
    void shouldRejectTradeWithUnownedPlayer() {
      // Given
      addPlayer(team2, player2);
      // player1 n'est pas dans team1

      when(teamRepository.findById(team1.getId())).thenReturn(Optional.of(team1));
      when(teamRepository.findById(team2.getId())).thenReturn(Optional.of(team2));

      // When & Then
      assertThrows(
          BusinessException.class,
          () ->
              tradingService.proposeTrade(
                  team1.getId(),
                  team2.getId(),
                  List.of(player1), // team1 ne possède pas player1
                  List.of(player2)));
    }
  }

  @Nested
  @DisplayName("Acceptation de Trade")
  class AcceptTrade {

    private Trade pendingTrade;

    @BeforeEach
    void setUpTrade() {
      addPlayer(team1, player1);
      addPlayer(team2, player2);

      pendingTrade = new Trade();
      pendingTrade.setId(UUID.randomUUID());
      pendingTrade.setFromTeam(team1);
      pendingTrade.setToTeam(team2);
      pendingTrade.setOfferedPlayers(List.of(player1));
      pendingTrade.setRequestedPlayers(List.of(player2));
      pendingTrade.setStatus(Trade.Status.PENDING);
      pendingTrade.setProposedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Devrait accepter un trade valide et échanger les joueurs")
    void shouldAcceptValidTradeAndSwapPlayers() {
      // Given
      when(tradeRepository.findById(pendingTrade.getId())).thenReturn(Optional.of(pendingTrade));
      when(teamRepository.save(any(Team.class))).thenAnswer(i -> i.getArgument(0));
      when(tradeRepository.save(any(Trade.class))).thenAnswer(i -> i.getArgument(0));

      // When
      Trade acceptedTrade = tradingService.acceptTrade(pendingTrade.getId(), user2.getId());

      // Then
      assertEquals(Trade.Status.ACCEPTED, acceptedTrade.getStatus());
      assertNotNull(acceptedTrade.getAcceptedAt());

      // Vérifier l'échange des joueurs
      assertFalse(team1.getPlayers().stream().anyMatch(tp -> tp.getPlayer().equals(player1)));
      assertTrue(team1.getPlayers().stream().anyMatch(tp -> tp.getPlayer().equals(player2)));
      assertFalse(team2.getPlayers().stream().anyMatch(tp -> tp.getPlayer().equals(player2)));
      assertTrue(team2.getPlayers().stream().anyMatch(tp -> tp.getPlayer().equals(player1)));

      verify(teamRepository, times(2)).save(any(Team.class));
      verify(tradeRepository).save(acceptedTrade);
    }

    @Test
    @DisplayName("Devrait rejeter l'acceptation par un utilisateur non autorisé")
    void shouldRejectAcceptanceByUnauthorizedUser() {
      // Given
      UUID wrongUserId = UUID.randomUUID();
      when(tradeRepository.findById(pendingTrade.getId())).thenReturn(Optional.of(pendingTrade));

      // When & Then
      assertThrows(
          BusinessException.class,
          () -> tradingService.acceptTrade(pendingTrade.getId(), wrongUserId));
    }

    @Test
    @DisplayName("Devrait rejeter l'acceptation d'un trade déjà traité")
    void shouldRejectAcceptanceOfProcessedTrade() {
      // Given
      pendingTrade.setStatus(Trade.Status.ACCEPTED);
      when(tradeRepository.findById(pendingTrade.getId())).thenReturn(Optional.of(pendingTrade));

      // When & Then
      assertThrows(
          BusinessException.class,
          () -> tradingService.acceptTrade(pendingTrade.getId(), user2.getId()));
    }

    @Test
    @DisplayName("Devrait incrémenter le compteur de trades complétés")
    void shouldIncrementCompletedTradesCounter() {
      // Given
      team1.setCompletedTradesCount(2);
      team2.setCompletedTradesCount(1);

      when(tradeRepository.findById(pendingTrade.getId())).thenReturn(Optional.of(pendingTrade));
      when(teamRepository.save(any(Team.class))).thenAnswer(i -> i.getArgument(0));
      when(tradeRepository.save(any(Trade.class))).thenAnswer(i -> i.getArgument(0));

      // When
      tradingService.acceptTrade(pendingTrade.getId(), user2.getId());

      // Then
      assertEquals(3, team1.getCompletedTradesCount());
      assertEquals(2, team2.getCompletedTradesCount());
    }
  }

  @Nested
  @DisplayName("Rejet de Trade")
  class RejectTrade {

    private Trade pendingTrade;

    @BeforeEach
    void setUpTrade() {
      pendingTrade = new Trade();
      pendingTrade.setId(UUID.randomUUID());
      pendingTrade.setFromTeam(team1);
      pendingTrade.setToTeam(team2);
      pendingTrade.setStatus(Trade.Status.PENDING);
    }

    @Test
    @DisplayName("Devrait rejeter un trade par le destinataire")
    void shouldRejectTradeByRecipient() {
      // Given
      when(tradeRepository.findById(pendingTrade.getId())).thenReturn(Optional.of(pendingTrade));
      when(tradeRepository.save(any(Trade.class))).thenAnswer(i -> i.getArgument(0));

      // When
      Trade rejectedTrade = tradingService.rejectTrade(pendingTrade.getId(), user2.getId());

      // Then
      assertEquals(Trade.Status.REJECTED, rejectedTrade.getStatus());
      assertNotNull(rejectedTrade.getRejectedAt());
      verify(tradeRepository).save(rejectedTrade);
    }

    @Test
    @DisplayName("Devrait permettre l'annulation par l'initiateur")
    void shouldAllowCancellationByInitiator() {
      // Given
      when(tradeRepository.findById(pendingTrade.getId())).thenReturn(Optional.of(pendingTrade));
      when(tradeRepository.save(any(Trade.class))).thenAnswer(i -> i.getArgument(0));

      // When
      Trade cancelledTrade = tradingService.cancelTrade(pendingTrade.getId(), user1.getId());

      // Then
      assertEquals(Trade.Status.CANCELLED, cancelledTrade.getStatus());
      assertNotNull(cancelledTrade.getCancelledAt());
      verify(tradeRepository).save(cancelledTrade);
    }
  }

  @Nested
  @DisplayName("Contre-proposition de Trade")
  class CounterTrade {

    private Trade originalTrade;

    @BeforeEach
    void setUpTrade() {
      addPlayer(team1, player1);
      addPlayers(team2, player2, player3);

      originalTrade = new Trade();
      originalTrade.setId(UUID.randomUUID());
      originalTrade.setFromTeam(team1);
      originalTrade.setToTeam(team2);
      originalTrade.setOfferedPlayers(List.of(player1));
      originalTrade.setRequestedPlayers(List.of(player2));
      originalTrade.setStatus(Trade.Status.PENDING);
    }

    @Test
    @DisplayName("Devrait créer une contre-proposition valide")
    void shouldCreateValidCounterOffer() {
      // Given
      List<Player> counterOffered = List.of(player3);
      List<Player> counterRequested = List.of(player1);

      when(tradeRepository.findById(originalTrade.getId())).thenReturn(Optional.of(originalTrade));
      when(teamRepository.findById(team2.getId())).thenReturn(Optional.of(team2));
      when(teamRepository.findById(team1.getId())).thenReturn(Optional.of(team1));
      when(tradeRepository.save(any(Trade.class))).thenAnswer(i -> i.getArgument(0));

      // When
      Trade counterTrade =
          tradingService.counterTrade(
              originalTrade.getId(), user2.getId(), counterOffered, counterRequested);

      // Then
      assertNotNull(counterTrade);
      assertEquals(Trade.Status.PENDING, counterTrade.getStatus());
      assertEquals(team2, counterTrade.getFromTeam());
      assertEquals(team1, counterTrade.getToTeam());
      assertEquals(counterOffered, counterTrade.getOfferedPlayers());
      assertEquals(counterRequested, counterTrade.getRequestedPlayers());
      assertEquals(originalTrade.getId(), counterTrade.getOriginalTradeId());

      // Vérifier que le trade original est marqué comme contré
      assertEquals(Trade.Status.COUNTERED, originalTrade.getStatus());
    }
  }

  @Nested
  @DisplayName("Historique et Requêtes")
  class TradeHistory {

    @Test
    @DisplayName("Devrait récupérer l'historique des trades d'une équipe")
    void shouldGetTeamTradeHistory() {
      // Given
      List<Trade> trades =
          List.of(
              createTrade(team1, team2, Trade.Status.ACCEPTED),
              createTrade(team2, team1, Trade.Status.REJECTED),
              createTrade(team1, team2, Trade.Status.PENDING));

      when(tradeRepository.findByTeamId(team1.getId())).thenReturn(trades);

      // When
      List<Trade> history = tradingService.getTeamTradeHistory(team1.getId());

      // Then
      assertEquals(3, history.size());
      verify(tradeRepository).findByTeamId(team1.getId());
    }

    @Test
    @DisplayName("Devrait récupérer les trades en attente pour une équipe")
    void shouldGetPendingTradesForTeam() {
      // Given
      Trade pendingTrade1 = createTrade(team2, team1, Trade.Status.PENDING);
      Trade pendingTrade2 = createTrade(team2, team1, Trade.Status.PENDING);

      when(tradeRepository.findPendingTradesForTeam(team1.getId()))
          .thenReturn(List.of(pendingTrade1, pendingTrade2));

      // When
      List<Trade> pendingTrades = tradingService.getPendingTradesForTeam(team1.getId());

      // Then
      assertEquals(2, pendingTrades.size());
      assertTrue(pendingTrades.stream().allMatch(t -> t.getStatus() == Trade.Status.PENDING));
    }

    @Test
    @DisplayName("Devrait récupérer les statistiques de trade d'une game")
    void shouldGetGameTradeStatistics() {
      // Given
      when(tradeRepository.countByGameIdAndStatus(game.getId(), Trade.Status.ACCEPTED))
          .thenReturn(10L);
      when(tradeRepository.countByGameIdAndStatus(game.getId(), Trade.Status.PENDING))
          .thenReturn(3L);
      when(tradeRepository.countByGameIdAndStatus(game.getId(), Trade.Status.REJECTED))
          .thenReturn(5L);

      // When
      Map<String, Long> stats = tradingService.getGameTradeStatistics(game.getId());

      // Then
      assertEquals(10L, stats.get("accepted"));
      assertEquals(3L, stats.get("pending"));
      assertEquals(5L, stats.get("rejected"));
      assertEquals(18L, stats.get("total"));
    }
  }

  @Nested
  @DisplayName("Validation de Trade")
  class TradeValidation {

    @Test
    @DisplayName("Devrait valider le respect des règles régionales après trade")
    void shouldValidateRegionalRulesAfterTrade() {
      // Given
      GameRegionRule euRule =
          GameRegionRule.builder().region(Player.Region.EU).maxPlayers(2).game(game).build();

      GameRegionRule naRule =
          GameRegionRule.builder().region(Player.Region.NA).maxPlayers(1).game(game).build();

      game.setRegionRules(List.of(euRule, naRule));

      addPlayers(team1, player1, player3); // 2 EU
      addPlayer(team2, player2); // 1 NA

      // Tentative de trade qui violerait les règles (team1 aurait 2 EU + 1 NA)
      Trade invalidTrade = new Trade();
      invalidTrade.setFromTeam(team1);
      invalidTrade.setToTeam(team2);
      invalidTrade.setOfferedPlayers(List.of(player1)); // EU
      invalidTrade.setRequestedPlayers(List.of(player2)); // NA

      when(tradeRepository.findById(any())).thenReturn(Optional.of(invalidTrade));
      doThrow(new BusinessException("Regional rules violated"))
          .when(validationService)
          .validateTeamComposition(any(), any());

      // When & Then
      assertThrows(
          BusinessException.class,
          () -> tradingService.acceptTrade(invalidTrade.getId(), user2.getId()));
    }

    @Test
    @DisplayName("Devrait empêcher le trade de joueurs verrouillés")
    void shouldPreventTradingLockedPlayers() {
      // Given
      player1.setLocked(true); // Joueur verrouillé (ex: pendant un match)
      addPlayer(team1, player1);
      addPlayer(team2, player2);

      when(teamRepository.findById(team1.getId())).thenReturn(Optional.of(team1));
      when(teamRepository.findById(team2.getId())).thenReturn(Optional.of(team2));

      // When & Then
      assertThrows(
          BusinessException.class,
          () ->
              tradingService.proposeTrade(
                  team1.getId(), team2.getId(), List.of(player1), List.of(player2)));
    }

    @Test
    @DisplayName("Devrait vérifier la deadline de trade")
    void shouldCheckTradeDeadline() {
      // Given
      game.setTradeDeadline(LocalDateTime.now().minusDays(1));

      when(teamRepository.findById(team1.getId())).thenReturn(Optional.of(team1));
      when(teamRepository.findById(team2.getId())).thenReturn(Optional.of(team2));

      // When & Then
      assertThrows(
          BusinessException.class,
          () ->
              tradingService.proposeTrade(
                  team1.getId(), team2.getId(), List.of(player1), List.of(player2)));
    }
  }

  @Nested
  @DisplayName("Trades Multi-joueurs")
  class MultiPlayerTrades {

    @Test
    @DisplayName("Devrait gérer un trade 2-pour-1")
    void shouldHandleTwoForOneTrade() {
      // Given
      Player player4 = new Player();
      player4.setId(UUID.randomUUID());
      player4.setName("Player 4");

      addPlayers(team1, player1, player3);
      addPlayers(team2, player2, player4);

      List<Player> offered = List.of(player1, player3);
      List<Player> requested = List.of(player2);

      when(teamRepository.findById(team1.getId())).thenReturn(Optional.of(team1));
      when(teamRepository.findById(team2.getId())).thenReturn(Optional.of(team2));
      when(tradeRepository.save(any(Trade.class))).thenAnswer(i -> i.getArgument(0));

      // When
      Trade trade = tradingService.proposeTrade(team1.getId(), team2.getId(), offered, requested);

      // Then
      assertEquals(2, trade.getOfferedPlayers().size());
      assertEquals(1, trade.getRequestedPlayers().size());
    }

    @Test
    @DisplayName("Devrait limiter le nombre de joueurs par trade")
    void shouldLimitPlayersPerTrade() {
      // Given
      List<Player> tooManyPlayers = new ArrayList<>();
      for (int i = 0; i < 6; i++) {
        Player p = new Player();
        p.setId(UUID.randomUUID());
        tooManyPlayers.add(p);
        addPlayer(team1, p);
      }

      when(teamRepository.findById(team1.getId())).thenReturn(Optional.of(team1));
      when(teamRepository.findById(team2.getId())).thenReturn(Optional.of(team2));

      // When & Then
      assertThrows(
          BusinessException.class,
          () ->
              tradingService.proposeTrade(
                  team1.getId(), team2.getId(), tooManyPlayers, List.of(player2)));
    }
  }

  // Helper method
  private Trade createTrade(Team from, Team to, Trade.Status status) {
    Trade trade = new Trade();
    trade.setId(UUID.randomUUID());
    trade.setFromTeam(from);
    trade.setToTeam(to);
    trade.setStatus(status);
    trade.setProposedAt(LocalDateTime.now());
    return trade;
  }
}
