package com.fortnite.pronos.service.draft;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.port.out.DraftRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameParticipantRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.dto.DraftActionResponse;
import com.fortnite.pronos.dto.DraftAdvanceResponse;
import com.fortnite.pronos.dto.DraftAvailablePlayerResponse;
import com.fortnite.pronos.dto.DraftCompleteResponse;
import com.fortnite.pronos.dto.DraftNextParticipantResponse;
import com.fortnite.pronos.dto.DraftOrderEntryResponse;
import com.fortnite.pronos.dto.DraftTimeoutResponse;
import com.fortnite.pronos.model.Draft;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.GameRegionRule;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.DraftRepository;
import com.fortnite.pronos.repository.GameParticipantRepository;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.PlayerRepository;

/**
 * TDD Tests for DraftService - Business Critical Component
 *
 * <p>This test suite validates complex draft workflows using RED-GREEN-REFACTOR TDD methodology.
 * DraftService manages snake drafts, round calculations, and player selection workflows that are
 * essential for the fantasy league functionality.
 *
 * <p>Business Logic Areas: - Snake draft algorithm implementation and position calculation - Round
 * progression and completion detection - Player selection workflow and timing constraints - Draft
 * state management (ACTIVE, PAUSED, FINISHED) - Regional rules integration for total rounds
 * calculation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DraftService - Complex Workflow TDD Tests")
class DraftServiceTddTest {

  @Mock private DraftRepository draftRepository;
  @Mock private GameParticipantRepository gameParticipantRepository;
  @Mock private GameRepository gameRepository;
  @Mock private PlayerRepository playerRepository;

  @InjectMocks private DraftService draftService;

  private Game testGame;
  private Draft testDraft;
  private List<GameParticipant> testParticipants;
  private Player testPlayer;

  @BeforeEach
  void setUp() {
    // Game setup with regional rules
    testGame = new Game();
    testGame.setId(UUID.randomUUID());
    testGame.setName("Test Fantasy League");
    testGame.setMaxParticipants(4);

    // Regional rules for total rounds calculation
    GameRegionRule naRule = GameRegionRule.builder().region(Player.Region.EU).maxPlayers(3).build();

    GameRegionRule euRule =
        GameRegionRule.builder().region(Player.Region.NAW).maxPlayers(2).build();

    testGame.setRegionRules(Arrays.asList(naRule, euRule));

    // Draft setup
    testDraft = new Draft(testGame);
    testDraft.setId(UUID.randomUUID());
    testDraft.setStatus(Draft.Status.ACTIVE);
    testDraft.setCurrentRound(1);
    testDraft.setCurrentPick(1);
    testDraft.setTotalRounds(5); // 3 + 2 from regional rules

    // Participants setup for draft order testing
    testParticipants =
        Arrays.asList(
            createParticipant(UUID.randomUUID(), 1),
            createParticipant(UUID.randomUUID(), 2),
            createParticipant(UUID.randomUUID(), 3),
            createParticipant(UUID.randomUUID(), 4));
    testGame.setParticipants(new ArrayList<>(testParticipants));

    // Player setup
    testPlayer = new Player();
    testPlayer.setId(UUID.randomUUID());
    testPlayer.setNickname("TestPlayer");
    testPlayer.setFortniteId("test_epic_123");
    testPlayer.setUsername("testplayer");
    testPlayer.setRegion(Player.Region.EU);
    testPlayer.setTranche("1-7");
  }

  private GameParticipant createParticipant(UUID userId, int draftOrder) {
    User user = new User();
    user.setId(userId);

    GameParticipant participant = new GameParticipant();
    participant.setId(UUID.randomUUID());
    participant.setUser(user);
    participant.setDraftOrder(draftOrder);
    return participant;
  }

  @Nested
  @DisplayName("Draft Creation and Initialization")
  class DraftCreationTests {

    @Test
    @DisplayName("Should create draft with proper initial state")
    void shouldCreateDraftWithProperInitialState() {
      // RED: Define expected behavior for draft creation
      when(((DraftRepositoryPort) draftRepository).save(any(Draft.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      Draft result = draftService.createDraft(testGame, testParticipants);

      // Verify draft initialization
      assertThat(result.getStatus()).isEqualTo(Draft.Status.ACTIVE);
      assertThat(result.getCurrentRound()).isEqualTo(1);
      assertThat(result.getCurrentPick()).isEqualTo(1);
      assertThat(result.getTotalRounds()).isEqualTo(5); // Sum of region rules: 3 + 2
      assertThat(result.getStartedAt()).isNotNull();
      assertThat(result.getId()).isNotNull();

      verify(((DraftRepositoryPort) draftRepository)).save(any(Draft.class));
    }

    @Test
    @DisplayName("Should calculate total rounds from regional rules")
    void shouldCalculateTotalRoundsFromRegionalRules() {
      // RED: Test regional rules integration for rounds calculation
      when(((DraftRepositoryPort) draftRepository).save(any(Draft.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      Draft result = draftService.createDraft(testGame, testParticipants);

      // Verify total rounds calculated from region rules (3 + 2 = 5)
      assertThat(result.getTotalRounds()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should use default rounds when no regional rules exist")
    void shouldUseDefaultRoundsWhenNoRegionalRulesExist() {
      // RED: Test fallback behavior for games without regional rules
      testGame.setRegionRules(null);
      when(((DraftRepositoryPort) draftRepository).save(any(Draft.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      Draft result = draftService.createDraft(testGame, testParticipants);

      // Should default to 10 rounds when no rules
      assertThat(result.getTotalRounds()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should assign draft order to participants")
    void shouldAssignDraftOrderToParticipants() {
      // RED: Test draft order assignment functionality
      draftService.updateDraftOrder(testDraft, testParticipants);

      // Verify each participant gets correct draft order
      for (int i = 0; i < testParticipants.size(); i++) {
        assertThat(testParticipants.get(i).getDraftOrder()).isEqualTo(i + 1);
      }
    }
  }

  @Nested
  @DisplayName("Snake Draft Algorithm Implementation")
  class SnakeDraftAlgorithmTests {

    @Test
    @DisplayName("Should calculate correct current picker for normal round")
    void shouldCalculateCorrectCurrentPickerForNormalRound() {
      // RED: Test snake draft position calculation for odd rounds
      testDraft.setCurrentRound(1); // Odd round = normal order
      testDraft.setCurrentPick(2);

      GameParticipant currentPicker = draftService.getCurrentPicker(testDraft, testParticipants);

      // Round 1, Pick 2 should be participant with draft order 2
      assertThat(currentPicker.getDraftOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should calculate correct current picker for reverse round")
    void shouldCalculateCorrectCurrentPickerForReverseRound() {
      // RED: Test snake draft position calculation for even rounds
      testDraft.setCurrentRound(2); // Even round = reverse order
      testDraft.setCurrentPick(1);

      GameParticipant currentPicker = draftService.getCurrentPicker(testDraft, testParticipants);

      // Round 2, Pick 1 should be participant with draft order 4 (reverse of 1)
      assertThat(currentPicker.getDraftOrder()).isEqualTo(4);
    }

    @Test
    @DisplayName("Should handle snake draft reverse order correctly")
    void shouldHandleSnakeDraftReverseOrderCorrectly() {
      // RED: Test complete snake draft reverse calculation
      testDraft.setCurrentRound(2); // Even round
      testDraft.setCurrentPick(3);

      GameParticipant currentPicker = draftService.getCurrentPicker(testDraft, testParticipants);

      // Round 2, Pick 3 should be participant with draft order 2 (reverse of 3 in 4-player)
      assertThat(currentPicker.getDraftOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should throw exception when no participant found for position")
    void shouldThrowExceptionWhenNoParticipantFoundForPosition() {
      // RED: Test error handling for invalid draft positions
      testDraft.setCurrentPick(99); // Invalid position

      assertThatThrownBy(() -> draftService.getCurrentPicker(testDraft, testParticipants))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Aucun participant trouvÃƒÆ’Ã‚Â© pour l'ordre");
    }
  }

  @Nested
  @DisplayName("Round Progression and Pick Management")
  class RoundProgressionTests {

    @Test
    @DisplayName("Should advance to next pick within same round")
    void shouldAdvanceToNextPickWithinSameRound() {
      // RED: Test pick advancement within a round
      testDraft.setCurrentPick(2);
      testDraft.setCurrentRound(1);
      when(((DraftRepositoryPort) draftRepository).save(any(Draft.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      Draft result = draftService.nextPick(testDraft, testGame.getMaxParticipants());

      assertThat(result.getCurrentPick()).isEqualTo(3);
      assertThat(result.getCurrentRound()).isEqualTo(1);
      assertThat(result.getStatus()).isEqualTo(Draft.Status.ACTIVE);
    }

    @Test
    @DisplayName("Should advance to next round when pick exceeds participant count")
    void shouldAdvanceToNextRoundWhenPickExceedsParticipantCount() {
      // RED: Test round progression when picks are exhausted
      testDraft.setCurrentPick(4); // Last pick
      testDraft.setCurrentRound(1);
      when(((DraftRepositoryPort) draftRepository).save(any(Draft.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      Draft result = draftService.nextPick(testDraft, testGame.getMaxParticipants());

      assertThat(result.getCurrentPick()).isEqualTo(1);
      assertThat(result.getCurrentRound()).isEqualTo(2);
      assertThat(result.getStatus()).isEqualTo(Draft.Status.ACTIVE);
    }

    @Test
    @DisplayName("Should finish draft when all rounds are completed")
    void shouldFinishDraftWhenAllRoundsAreCompleted() {
      // RED: Test draft completion logic
      testDraft.setCurrentPick(4); // Last pick
      testDraft.setCurrentRound(5); // Last round
      testDraft.setTotalRounds(5);
      when(((DraftRepositoryPort) draftRepository).save(any(Draft.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      Draft result = draftService.nextPick(testDraft, testGame.getMaxParticipants());

      assertThat(result.getStatus()).isEqualTo(Draft.Status.FINISHED);
      assertThat(result.getFinishedAt()).isNotNull();
      assertThat(result.getCurrentRound()).isEqualTo(6); // Exceeds total rounds
    }
  }

  @Nested
  @DisplayName("Draft State Management")
  class DraftStateManagementTests {

    @Test
    @DisplayName("Should start draft and set active status")
    void shouldStartDraftAndSetActiveStatus() {
      // RED: Test draft starting functionality
      when(((DraftRepositoryPort) draftRepository).save(any(Draft.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      Draft result = draftService.startDraft(testGame);

      assertThat(result.getStatus()).isEqualTo(Draft.Status.ACTIVE);
      assertThat(result.getStartedAt()).isNotNull();
      verify(((DraftRepositoryPort) draftRepository)).save(any(Draft.class));
    }

    @Test
    @DisplayName("Should pause active draft")
    void shouldPauseActiveDraft() {
      // RED: Test draft pausing functionality
      when(((DraftRepositoryPort) draftRepository).save(any(Draft.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      Draft result = draftService.pauseDraft(testDraft);

      assertThat(result.getStatus()).isEqualTo(Draft.Status.PAUSED);
      assertThat(result.getUpdatedAt()).isNotNull();
      verify(((DraftRepositoryPort) draftRepository)).save(testDraft);
    }

    @Test
    @DisplayName("Should resume paused draft")
    void shouldResumePausedDraft() {
      // RED: Test draft resuming functionality
      testDraft.setStatus(Draft.Status.PAUSED);
      when(((DraftRepositoryPort) draftRepository).save(any(Draft.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      Draft result = draftService.resumeDraft(testDraft);

      assertThat(result.getStatus()).isEqualTo(Draft.Status.ACTIVE);
      assertThat(result.getUpdatedAt()).isNotNull();
      verify(((DraftRepositoryPort) draftRepository)).save(testDraft);
    }

    @Test
    @DisplayName("Should finish draft and set completion time")
    void shouldFinishDraftAndSetCompletionTime() {
      // RED: Test draft finishing functionality
      when(((DraftRepositoryPort) draftRepository).save(any(Draft.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      Draft result = draftService.finishDraft(testDraft);

      assertThat(result.getStatus()).isEqualTo(Draft.Status.FINISHED);
      assertThat(result.getFinishedAt()).isNotNull();
      verify(((DraftRepositoryPort) draftRepository)).save(testDraft);
    }
  }

  @Nested
  @DisplayName("Player Selection Workflow")
  class PlayerSelectionWorkflowTests {

    @Test
    @DisplayName("Should advance draft after player selection")
    void shouldAdvanceDraftAfterPlayerSelection() {
      // RED: Test draft advancement after player pick
      testDraft.setCurrentPick(1);
      testDraft.setCurrentRound(1);
      when(((DraftRepositoryPort) draftRepository).save(any(Draft.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      UUID userId = UUID.randomUUID();
      draftService.selectPlayer(testDraft, userId, testPlayer);

      // Should advance to next pick
      assertThat(testDraft.getCurrentPick()).isEqualTo(2);
      assertThat(testDraft.getCurrentRound()).isEqualTo(1);
      verify(((DraftRepositoryPort) draftRepository)).save(testDraft);
    }

    @Test
    @DisplayName("Should detect draft completion correctly")
    void shouldDetectDraftCompletionCorrectly() {
      // RED: Test draft completion detection
      testDraft.setStatus(Draft.Status.FINISHED);

      boolean isComplete = draftService.isDraftComplete(testDraft);

      assertThat(isComplete).isTrue();
    }

    @Test
    @DisplayName("Should detect incomplete draft correctly")
    void shouldDetectIncompleteDraftCorrectly() {
      // RED: Test active draft detection
      testDraft.setStatus(Draft.Status.ACTIVE);
      testDraft.setCurrentRound(3);
      testDraft.setTotalRounds(5);

      boolean isComplete = draftService.isDraftComplete(testDraft);

      assertThat(isComplete).isFalse();
    }

    @Test
    @DisplayName("Should validate user turn for active draft")
    void shouldValidateUserTurnForActiveDraft() {
      // RED: Test user turn validation for active drafts
      testDraft.setStatus(Draft.Status.ACTIVE);
      UUID userId = UUID.randomUUID();

      boolean isUserTurn = draftService.isUserTurn(testDraft, userId);

      // Current implementation returns true for active drafts
      assertThat(isUserTurn).isTrue();
    }

    @Test
    @DisplayName("Should reject user turn for non-active draft")
    void shouldRejectUserTurnForNonActiveDraft() {
      // RED: Test user turn validation for non-active drafts
      testDraft.setStatus(Draft.Status.PAUSED);
      UUID userId = UUID.randomUUID();

      boolean isUserTurn = draftService.isUserTurn(testDraft, userId);

      assertThat(isUserTurn).isFalse();
    }
  }

  @Nested
  @DisplayName("Business Logic Integration Tests")
  class BusinessLogicIntegrationTests {

    @Test
    @DisplayName("Should handle complete draft workflow from start to finish")
    void shouldHandleCompleteDraftWorkflowFromStartToFinish() {
      // RED: Integration test for complete draft workflow
      when(((DraftRepositoryPort) draftRepository).save(any(Draft.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // Create draft
      Draft draft = draftService.createDraft(testGame, testParticipants);
      assertThat(draft.getStatus()).isEqualTo(Draft.Status.ACTIVE);

      // Simulate multiple picks through rounds
      for (int round = 1; round <= 2; round++) {
        for (int pick = 1; pick <= 4; pick++) {
          if (!draftService.isDraftComplete(draft)) {
            GameParticipant currentPicker = draftService.getCurrentPicker(draft, testParticipants);
            assertThat(currentPicker).isNotNull();

            draft = draftService.nextPick(draft, testGame.getMaxParticipants());
          }
        }
      }

      // Verify draft progression
      assertThat(draft.getCurrentRound()).isEqualTo(3);
      assertThat(draft.getCurrentPick()).isEqualTo(1);
      assertThat(draft.getStatus()).isEqualTo(Draft.Status.ACTIVE);
    }

    @Test
    @DisplayName("Should handle draft pause and resume workflow")
    void shouldHandleDraftPauseAndResumeWorkflow() {
      // RED: Integration test for pause/resume workflow
      when(((DraftRepositoryPort) draftRepository).save(any(Draft.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // Start with active draft
      testDraft.setStatus(Draft.Status.ACTIVE);

      // Pause draft
      Draft pausedDraft = draftService.pauseDraft(testDraft);
      assertThat(pausedDraft.getStatus()).isEqualTo(Draft.Status.PAUSED);
      assertThat(draftService.isUserTurn(pausedDraft, UUID.randomUUID())).isFalse();

      // Resume draft
      Draft resumedDraft = draftService.resumeDraft(pausedDraft);
      assertThat(resumedDraft.getStatus()).isEqualTo(Draft.Status.ACTIVE);
      assertThat(draftService.isUserTurn(resumedDraft, UUID.randomUUID())).isTrue();

      verify(((DraftRepositoryPort) draftRepository), times(2)).save(any(Draft.class));
    }

    @Test
    @DisplayName("Should maintain data consistency across state transitions")
    void shouldMaintainDataConsistencyAcrossStateTransitions() {
      // RED: Test data consistency during state changes
      ArgumentCaptor<Draft> draftCaptor = ArgumentCaptor.forClass(Draft.class);
      when(((DraftRepositoryPort) draftRepository).save(any(Draft.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // Multiple state transitions
      Draft draft1 = draftService.startDraft(testGame);
      Draft draft2 = draftService.pauseDraft(draft1);
      Draft draft3 = draftService.resumeDraft(draft2);
      Draft draft4 = draftService.finishDraft(draft3);

      // Verify all transitions saved to repository
      verify(((DraftRepositoryPort) draftRepository), times(4)).save(draftCaptor.capture());

      List<Draft> savedDrafts = draftCaptor.getAllValues();
      assertThat(savedDrafts).hasSize(4);
      assertThat(savedDrafts.get(3).getStatus()).isEqualTo(Draft.Status.FINISHED);
    }
  }

  @Nested
  @DisplayName("Next Participant Response")
  class NextParticipantResponseTests {

    @Test
    @DisplayName("Should build response when draft exists")
    void shouldBuildResponseWhenDraftExists() {
      testDraft.setCurrentPick(2);
      testDraft.setCurrentRound(3);
      when(draftRepository.findByGame(testGame)).thenReturn(Optional.of(testDraft));

      DraftNextParticipantResponse response =
          draftService.buildNextParticipantResponse(testGame, testParticipants);

      GameParticipant expected = testParticipants.get(1);
      assertThat(response.id()).isEqualTo(expected.getId());
      assertThat(response.userId()).isEqualTo(expected.getUser().getId());
      assertThat(response.draftOrder()).isEqualTo(expected.getDraftOrder());
      assertThat(response.currentPick()).isEqualTo(2);
      assertThat(response.currentRound()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should default pick and round when no draft exists")
    void shouldDefaultPickAndRoundWhenNoDraftExists() {
      when(draftRepository.findByGame(testGame)).thenReturn(Optional.empty());

      DraftNextParticipantResponse response =
          draftService.buildNextParticipantResponse(testGame, testParticipants);

      GameParticipant expected = testParticipants.get(0);
      assertThat(response.id()).isEqualTo(expected.getId());
      assertThat(response.currentPick()).isEqualTo(1);
      assertThat(response.currentRound()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should fallback to first participant when snake position is out of bounds")
    void shouldFallbackToFirstParticipantWhenSnakePositionOutOfBounds() {
      testDraft.setCurrentPick(99); // Invalid pick value
      testDraft.setCurrentRound(2);
      when(draftRepository.findByGame(testGame)).thenReturn(Optional.of(testDraft));

      DraftNextParticipantResponse response =
          draftService.buildNextParticipantResponse(testGame, testParticipants);

      // Fallback to first participant when snake position calculation yields invalid result
      GameParticipant expected = testParticipants.get(0);
      assertThat(response.id()).isEqualTo(expected.getId());
      assertThat(response.draftOrder()).isEqualTo(expected.getDraftOrder());
      assertThat(response.currentPick()).isEqualTo(99);
      assertThat(response.currentRound()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should use first participant when pick is zero")
    void shouldUseFirstParticipantWhenPickIsZero() {
      testDraft.setCurrentPick(0);
      testDraft.setCurrentRound(2);
      when(draftRepository.findByGame(testGame)).thenReturn(Optional.of(testDraft));

      DraftNextParticipantResponse response =
          draftService.buildNextParticipantResponse(testGame, testParticipants);

      GameParticipant expected = testParticipants.get(0);
      assertThat(response.id()).isEqualTo(expected.getId());
      assertThat(response.draftOrder()).isEqualTo(expected.getDraftOrder());
      assertThat(response.currentPick()).isEqualTo(0);
      assertThat(response.currentRound()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should throw when participants list is empty")
    void shouldThrowWhenParticipantsListIsEmpty() {
      assertThatThrownBy(() -> draftService.buildNextParticipantResponse(testGame, List.of()))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("No participants");
    }
  }

  @Nested
  @DisplayName("Draft Order Response")
  class DraftOrderResponseTests {

    @Test
    @DisplayName("Should build draft order response from participants")
    void shouldBuildDraftOrderResponseFromParticipants() {
      when(((GameParticipantRepositoryPort) gameParticipantRepository)
              .findByGameIdOrderByDraftOrderAsc(testGame.getId()))
          .thenReturn(testParticipants);

      List<DraftOrderEntryResponse> response =
          draftService.buildDraftOrderResponse(testGame.getId());

      assertThat(response).hasSize(testParticipants.size());
      DraftOrderEntryResponse first = response.get(0);
      GameParticipant expected = testParticipants.get(0);
      assertThat(first.id()).isEqualTo(expected.getId());
      assertThat(first.draftOrder()).isEqualTo(expected.getDraftOrder());
      assertThat(first.userId()).isEqualTo(expected.getUser().getId());
    }

    @Test
    @DisplayName("Should return empty order when no participants exist")
    void shouldReturnEmptyOrderWhenNoParticipantsExist() {
      when(((GameParticipantRepositoryPort) gameParticipantRepository)
              .findByGameIdOrderByDraftOrderAsc(testGame.getId()))
          .thenReturn(List.of());

      List<DraftOrderEntryResponse> response =
          draftService.buildDraftOrderResponse(testGame.getId());

      assertThat(response).isEmpty();
    }
  }

  @Nested
  @DisplayName("User Turn For Game")
  class UserTurnForGameTests {

    @Test
    @DisplayName("Should return true when first participant matches user")
    void shouldReturnTrueWhenFirstParticipantMatchesUser() {
      UUID userId = testParticipants.get(0).getUser().getId();
      when(((GameParticipantRepositoryPort) gameParticipantRepository)
              .findByGameIdOrderByDraftOrderAsc(testGame.getId()))
          .thenReturn(testParticipants);
      when(((GameRepositoryPort) gameRepository).findById(testGame.getId()))
          .thenReturn(Optional.of(testGame));
      when(draftRepository.findByGame(testGame)).thenReturn(Optional.empty());

      boolean result = draftService.isUserTurnForGame(testGame.getId(), userId);

      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when no participants exist")
    void shouldReturnFalseWhenNoParticipantsExist() {
      when(((GameParticipantRepositoryPort) gameParticipantRepository)
              .findByGameIdOrderByDraftOrderAsc(testGame.getId()))
          .thenReturn(List.of());

      boolean result = draftService.isUserTurnForGame(testGame.getId(), UUID.randomUUID());

      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when first participant has no user id")
    void shouldReturnFalseWhenFirstParticipantHasNoUserId() {
      GameParticipant participant = new GameParticipant();
      participant.setId(UUID.randomUUID());
      participant.setUser(new User());
      when(((GameParticipantRepositoryPort) gameParticipantRepository)
              .findByGameIdOrderByDraftOrderAsc(testGame.getId()))
          .thenReturn(List.of(participant));

      boolean result = draftService.isUserTurnForGame(testGame.getId(), UUID.randomUUID());

      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when user does not match first participant")
    void shouldReturnFalseWhenUserDoesNotMatchFirstParticipant() {
      UUID otherUser = UUID.randomUUID();
      when(((GameParticipantRepositoryPort) gameParticipantRepository)
              .findByGameIdOrderByDraftOrderAsc(testGame.getId()))
          .thenReturn(testParticipants);

      boolean result = draftService.isUserTurnForGame(testGame.getId(), otherUser);

      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("Advance Draft")
  class AdvanceDraftTests {

    @Test
    @DisplayName("Should advance draft and include next participant when draft exists")
    void shouldAdvanceDraftAndIncludeNextParticipantWhenDraftExists() {
      testDraft.setCurrentPick(1);
      testDraft.setCurrentRound(1);
      when(draftRepository.findByGame(testGame)).thenReturn(Optional.of(testDraft));
      when(((DraftRepositoryPort) draftRepository).save(any(Draft.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(((GameParticipantRepositoryPort) gameParticipantRepository)
              .findByGameIdOrderByDraftOrderAsc(testGame.getId()))
          .thenReturn(testParticipants);

      DraftAdvanceResponse response = draftService.advanceDraftToNextParticipant(testGame);

      assertThat(response.success()).isTrue();
      assertThat(response.currentPick()).isEqualTo(2);
      assertThat(response.currentRound()).isEqualTo(1);
      assertThat(response.isComplete()).isFalse();
      assertThat(response.nextParticipantId()).isEqualTo(testParticipants.get(1).getId());
      assertThat(response.nextDraftOrder()).isEqualTo(testParticipants.get(1).getDraftOrder());
    }

    @Test
    @DisplayName("Should create draft when none exists")
    void shouldCreateDraftWhenNoneExists() {
      when(draftRepository.findByGame(testGame)).thenReturn(Optional.empty());
      when(((DraftRepositoryPort) draftRepository).save(any(Draft.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(((GameParticipantRepositoryPort) gameParticipantRepository)
              .findByGameIdOrderByDraftOrderAsc(testGame.getId()))
          .thenReturn(testParticipants);

      DraftAdvanceResponse response = draftService.advanceDraftToNextParticipant(testGame);

      assertThat(response.success()).isTrue();
      assertThat(response.currentPick()).isEqualTo(2);
      assertThat(response.nextParticipantId()).isEqualTo(testParticipants.get(1).getId());
    }

    @Test
    @DisplayName("Should wrap to snake draft position when advancing to next round")
    void shouldWrapToSnakeDraftPositionWhenAdvancingToNextRound() {
      // Snake draft: Round 1 goes 1-2-3, Round 2 goes 3-2-1
      testDraft.setCurrentPick(testParticipants.size()); // Pick 3 (last of round 1)
      testDraft.setCurrentRound(1);
      when(draftRepository.findByGame(testGame)).thenReturn(Optional.of(testDraft));
      when(((DraftRepositoryPort) draftRepository).save(any(Draft.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(((GameParticipantRepositoryPort) gameParticipantRepository)
              .findByGameIdOrderByDraftOrderAsc(testGame.getId()))
          .thenReturn(testParticipants);

      DraftAdvanceResponse response = draftService.advanceDraftToNextParticipant(testGame);

      // After advancing: round becomes 2, pick becomes 1
      // In snake draft round 2 (even), pick 1 → snakePosition = 3 - 1 + 1 = 3
      assertThat(response.currentPick()).isEqualTo(1);
      assertThat(response.currentRound()).isEqualTo(2);
      // Snake draft: round 2 starts with last participant (position 3)
      GameParticipant lastParticipant = testParticipants.get(testParticipants.size() - 1);
      assertThat(response.nextParticipantId()).isEqualTo(lastParticipant.getId());
      assertThat(response.nextDraftOrder()).isEqualTo(lastParticipant.getDraftOrder());
    }
  }

  @Nested
  @DisplayName("Available Players Response")
  class AvailablePlayersResponseTests {

    @Test
    @DisplayName("Should build available players response")
    void shouldBuildAvailablePlayersResponse() {
      Player player = new Player();
      player.setId(UUID.randomUUID());
      player.setNickname("PlayerOne");
      player.setRegion(Player.Region.EU);
      when(playerRepository.findByRegion(Player.Region.EU)).thenReturn(List.of(player));

      List<DraftAvailablePlayerResponse> response =
          draftService.buildAvailablePlayersResponse(Player.Region.EU);

      assertThat(response).hasSize(1);
      assertThat(response.get(0).id()).isEqualTo(player.getId());
      assertThat(response.get(0).nickname()).isEqualTo(player.getNickname());
      assertThat(response.get(0).region()).isEqualTo(player.getRegion().name());
    }

    @Test
    @DisplayName("Should return empty list when no players available")
    void shouldReturnEmptyListWhenNoPlayersAvailable() {
      when(playerRepository.findByRegion(Player.Region.NAW)).thenReturn(List.of());

      List<DraftAvailablePlayerResponse> response =
          draftService.buildAvailablePlayersResponse(Player.Region.NAW);

      assertThat(response).isEmpty();
    }
  }

  @Nested
  @DisplayName("Draft Complete Response")
  class DraftCompleteResponseTests {

    @Test
    @DisplayName("Should report complete when game is active")
    void shouldReportCompleteWhenGameIsActive() {
      testGame.setStatus(GameStatus.ACTIVE);

      DraftCompleteResponse response = draftService.buildDraftCompleteResponse(testGame);

      assertThat(response.isComplete()).isTrue();
    }

    @Test
    @DisplayName("Should report incomplete when game is not active")
    void shouldReportIncompleteWhenGameIsNotActive() {
      testGame.setStatus(GameStatus.CREATING);

      DraftCompleteResponse response = draftService.buildDraftCompleteResponse(testGame);

      assertThat(response.isComplete()).isFalse();
    }
  }

  @Nested
  @DisplayName("Timeout Response")
  class TimeoutResponseTests {

    @Test
    @DisplayName("Should build default timeout response")
    void shouldBuildDefaultTimeoutResponse() {
      DraftTimeoutResponse response = draftService.buildTimeoutResponse();

      assertThat(response.timeoutCount()).isEqualTo(0);
      assertThat(response.message()).isEqualTo("Timeouts geres avec succes");
    }
  }

  @Nested
  @DisplayName("Finish Draft Response")
  class FinishDraftResponseTests {

    @Test
    @DisplayName("Should finish draft and return response")
    void shouldFinishDraftAndReturnResponse() {
      testGame.setStatus(GameStatus.DRAFTING);
      when(((GameRepositoryPort) gameRepository).save(any(Game.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      DraftActionResponse response = draftService.finishDraft(testGame);

      assertThat(response.success()).isTrue();
      assertThat(response.message()).isEqualTo("Draft termine avec succes");
      assertThat(testGame.getStatus()).isEqualTo(GameStatus.ACTIVE);
      verify(((GameRepositoryPort) gameRepository)).save(testGame);
    }
  }
}
