package com.fortnite.pronos.service.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.game.model.DraftMode;
import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameStatus;
import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.domain.player.model.Player;
import com.fortnite.pronos.domain.player.model.RankSnapshot;
import com.fortnite.pronos.domain.port.out.DraftPickRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameParticipantRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.RankSnapshotRepositoryPort;
import com.fortnite.pronos.domain.port.out.TeamScoreDeltaRepositoryPort;
import com.fortnite.pronos.domain.team.model.TeamScoreDelta;
import com.fortnite.pronos.model.GameParticipant;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamScoreDeltaBatchService")
class TeamScoreDeltaBatchServiceTest {

  @Mock private GameDomainRepositoryPort gameDomainRepository;
  @Mock private GameParticipantRepositoryPort participantRepository;
  @Mock private DraftPickRepositoryPort draftPickRepository;
  @Mock private PlayerDomainRepositoryPort playerRepository;
  @Mock private RankSnapshotRepositoryPort snapshotRepository;
  @Mock private TeamScoreDeltaRepositoryPort deltaRepository;

  private TeamScoreDeltaBatchService service;

  private static final UUID GAME_ID = UUID.randomUUID();
  private static final UUID DRAFT_ID = UUID.randomUUID();
  private static final UUID CREATOR_ID = UUID.randomUUID();
  private static final UUID PARTICIPANT_ID = UUID.randomUUID();
  private static final UUID PLAYER_1 = UUID.randomUUID();
  private static final UUID PLAYER_2 = UUID.randomUUID();

  // Past period: entirely before today so periodEnd = PERIOD_END (deterministic)
  private static final LocalDate PERIOD_START = LocalDate.of(2025, 1, 1);
  private static final LocalDate PERIOD_END = LocalDate.of(2025, 12, 31);

  @BeforeEach
  void setUp() {
    TeamScoreDeltaGameService processor =
        new TeamScoreDeltaGameService(
            participantRepository,
            draftPickRepository,
            playerRepository,
            snapshotRepository,
            deltaRepository);
    service = new TeamScoreDeltaBatchService(gameDomainRepository, processor);
  }

  private Game buildGame() {
    return Game.restore(
        GAME_ID,
        "Test Game",
        null,
        CREATOR_ID,
        8,
        GameStatus.ACTIVE,
        LocalDateTime.now(),
        null,
        null,
        "CODE1234",
        null,
        List.of(),
        List.of(),
        DRAFT_ID,
        false,
        5,
        null,
        2025,
        DraftMode.SNAKE,
        5,
        10,
        true,
        PERIOD_START,
        PERIOD_END);
  }

  private GameParticipant buildParticipant(UUID participantId) {
    GameParticipant p = new GameParticipant();
    p.setId(participantId);
    return p;
  }

  private RankSnapshot buildSnapshot(UUID playerId, int prValue, LocalDate date) {
    return RankSnapshot.restore(UUID.randomUUID(), playerId, "EU", 100, prValue, date);
  }

  @Nested
  @DisplayName("computeAllGameDeltas")
  class ComputeAllGameDeltas {

    @Test
    @DisplayName("whenValidGame_computesAndSavesDelta")
    void whenValidGame_computesAndSavesDelta() {
      Game game = buildGame();
      GameParticipant participant = buildParticipant(PARTICIPANT_ID);

      when(gameDomainRepository.findAllWithCompetitionPeriod()).thenReturn(List.of(game));
      when(participantRepository.findByGameIdOrderByJoinedAt(GAME_ID))
          .thenReturn(List.of(participant));
      when(draftPickRepository.findPlayerIdsByDraftIdAndParticipantId(DRAFT_ID, PARTICIPANT_ID))
          .thenReturn(List.of(PLAYER_1, PLAYER_2));

      when(playerRepository.findById(PLAYER_1))
          .thenReturn(Optional.of(new Player("player1", "Player One", PlayerRegion.EU, "EU")));
      when(playerRepository.findById(PLAYER_2))
          .thenReturn(Optional.of(new Player("player2", "Player Two", PlayerRegion.EU, "EU")));

      when(snapshotRepository.findLatestOnOrBefore(PLAYER_1, PERIOD_START))
          .thenReturn(Optional.of(buildSnapshot(PLAYER_1, 1000, PERIOD_START)));
      when(snapshotRepository.findLatestOnOrBefore(PLAYER_1, PERIOD_END))
          .thenReturn(Optional.of(buildSnapshot(PLAYER_1, 1200, PERIOD_END)));
      when(snapshotRepository.findLatestOnOrBefore(PLAYER_2, PERIOD_START))
          .thenReturn(Optional.of(buildSnapshot(PLAYER_2, 2000, PERIOD_START)));
      when(snapshotRepository.findLatestOnOrBefore(PLAYER_2, PERIOD_END))
          .thenReturn(Optional.of(buildSnapshot(PLAYER_2, 2300, PERIOD_END)));

      when(deltaRepository.findByGameIdAndParticipantId(GAME_ID, PARTICIPANT_ID))
          .thenReturn(Optional.empty());
      when(deltaRepository.save(any(TeamScoreDelta.class))).thenAnswer(inv -> inv.getArgument(0));

      service.computeAllGameDeltas();

      ArgumentCaptor<TeamScoreDelta> captor = ArgumentCaptor.forClass(TeamScoreDelta.class);
      verify(deltaRepository).save(captor.capture());
      TeamScoreDelta saved = captor.getValue();
      // deltaPr = (1200-1000) + (2300-2000) = 200 + 300 = 500
      assertThat(saved.getDeltaPr()).isEqualTo(500);
      assertThat(saved.getGameId()).isEqualTo(GAME_ID);
      assertThat(saved.getParticipantId()).isEqualTo(PARTICIPANT_ID);
    }

    @Test
    @DisplayName("whenPlayerNotFoundInRepository_contributionIsZero")
    void whenPlayerNotFoundInRepository_contributionIsZero() {
      Game game = buildGame();
      GameParticipant participant = buildParticipant(PARTICIPANT_ID);

      when(gameDomainRepository.findAllWithCompetitionPeriod()).thenReturn(List.of(game));
      when(participantRepository.findByGameIdOrderByJoinedAt(GAME_ID))
          .thenReturn(List.of(participant));
      when(draftPickRepository.findPlayerIdsByDraftIdAndParticipantId(DRAFT_ID, PARTICIPANT_ID))
          .thenReturn(List.of(PLAYER_1));

      // Player not found in repository — AC #2: contribution = 0
      when(playerRepository.findById(PLAYER_1)).thenReturn(Optional.empty());

      when(deltaRepository.findByGameIdAndParticipantId(GAME_ID, PARTICIPANT_ID))
          .thenReturn(Optional.empty());
      when(deltaRepository.save(any(TeamScoreDelta.class))).thenAnswer(inv -> inv.getArgument(0));

      service.computeAllGameDeltas();

      ArgumentCaptor<TeamScoreDelta> captor = ArgumentCaptor.forClass(TeamScoreDelta.class);
      verify(deltaRepository).save(captor.capture());
      assertThat(captor.getValue().getDeltaPr()).isEqualTo(0);
      verify(snapshotRepository, never()).findLatestOnOrBefore(any(), any());
    }

    @Test
    @DisplayName("whenNoSnapshotForPlayer_contributionIsZero")
    void whenNoSnapshotForPlayer_contributionIsZero() {
      Game game = buildGame();
      GameParticipant participant = buildParticipant(PARTICIPANT_ID);

      when(gameDomainRepository.findAllWithCompetitionPeriod()).thenReturn(List.of(game));
      when(participantRepository.findByGameIdOrderByJoinedAt(GAME_ID))
          .thenReturn(List.of(participant));
      when(draftPickRepository.findPlayerIdsByDraftIdAndParticipantId(DRAFT_ID, PARTICIPANT_ID))
          .thenReturn(List.of(PLAYER_1));

      when(playerRepository.findById(PLAYER_1))
          .thenReturn(Optional.of(new Player("player1", "Player One", PlayerRegion.EU, "EU")));

      // No snapshot for start date → contribution = 0 (AC #2)
      when(snapshotRepository.findLatestOnOrBefore(PLAYER_1, PERIOD_START))
          .thenReturn(Optional.empty());
      when(snapshotRepository.findLatestOnOrBefore(PLAYER_1, PERIOD_END))
          .thenReturn(Optional.of(buildSnapshot(PLAYER_1, 1500, PERIOD_END)));

      when(deltaRepository.findByGameIdAndParticipantId(GAME_ID, PARTICIPANT_ID))
          .thenReturn(Optional.empty());
      when(deltaRepository.save(any(TeamScoreDelta.class))).thenAnswer(inv -> inv.getArgument(0));

      service.computeAllGameDeltas();

      ArgumentCaptor<TeamScoreDelta> captor = ArgumentCaptor.forClass(TeamScoreDelta.class);
      verify(deltaRepository).save(captor.capture());
      assertThat(captor.getValue().getDeltaPr()).isEqualTo(0);
    }

    @Test
    @DisplayName("whenDeltaAlreadyExists_updatesExistingEntry")
    void whenDeltaAlreadyExists_updatesExistingEntry() {
      Game game = buildGame();
      GameParticipant participant = buildParticipant(PARTICIPANT_ID);
      UUID existingDeltaId = UUID.randomUUID();
      TeamScoreDelta existing =
          TeamScoreDelta.restore(
              existingDeltaId,
              GAME_ID,
              PARTICIPANT_ID,
              PERIOD_START,
              PERIOD_END,
              100,
              LocalDateTime.now());

      when(gameDomainRepository.findAllWithCompetitionPeriod()).thenReturn(List.of(game));
      when(participantRepository.findByGameIdOrderByJoinedAt(GAME_ID))
          .thenReturn(List.of(participant));
      when(draftPickRepository.findPlayerIdsByDraftIdAndParticipantId(DRAFT_ID, PARTICIPANT_ID))
          .thenReturn(List.of(PLAYER_1));

      when(playerRepository.findById(PLAYER_1))
          .thenReturn(Optional.of(new Player("player1", "Player One", PlayerRegion.EU, "EU")));

      when(snapshotRepository.findLatestOnOrBefore(PLAYER_1, PERIOD_START))
          .thenReturn(Optional.of(buildSnapshot(PLAYER_1, 1000, PERIOD_START)));
      when(snapshotRepository.findLatestOnOrBefore(PLAYER_1, PERIOD_END))
          .thenReturn(Optional.of(buildSnapshot(PLAYER_1, 1400, PERIOD_END)));

      when(deltaRepository.findByGameIdAndParticipantId(GAME_ID, PARTICIPANT_ID))
          .thenReturn(Optional.of(existing));
      when(deltaRepository.save(any(TeamScoreDelta.class))).thenAnswer(inv -> inv.getArgument(0));

      service.computeAllGameDeltas();

      ArgumentCaptor<TeamScoreDelta> captor = ArgumentCaptor.forClass(TeamScoreDelta.class);
      verify(deltaRepository).save(captor.capture());
      TeamScoreDelta updated = captor.getValue();
      assertThat(updated.getId()).isEqualTo(existingDeltaId);
      assertThat(updated.getDeltaPr()).isEqualTo(400); // 1400 - 1000
    }

    @Test
    @DisplayName("whenGameHasNoPeriod_isSkipped")
    void whenGameHasNoPeriod_isSkipped() {
      when(gameDomainRepository.findAllWithCompetitionPeriod()).thenReturn(List.of());

      service.computeAllGameDeltas();

      verify(participantRepository, never()).findByGameIdOrderByJoinedAt(any());
      verify(deltaRepository, never()).save(any());
    }

    @Test
    @DisplayName("whenGameHasNoDraft_isSkipped")
    void whenGameHasNoDraft_isSkipped() {
      Game gameNoDraft =
          Game.restore(
              GAME_ID,
              "No Draft Game",
              null,
              CREATOR_ID,
              8,
              GameStatus.ACTIVE,
              LocalDateTime.now(),
              null,
              null,
              "CODE1234",
              null,
              List.of(),
              List.of(),
              null,
              false,
              5,
              null,
              2025,
              DraftMode.SNAKE,
              5,
              10,
              true,
              PERIOD_START,
              PERIOD_END);

      when(gameDomainRepository.findAllWithCompetitionPeriod()).thenReturn(List.of(gameNoDraft));

      service.computeAllGameDeltas();

      verify(participantRepository, never()).findByGameIdOrderByJoinedAt(any());
      verify(deltaRepository, never()).save(any());
    }

    @Test
    @DisplayName("whenOneGameThrows_otherGamesStillProcessed")
    void whenOneGameThrows_otherGamesStillProcessed() {
      UUID game2Id = UUID.randomUUID();
      UUID draft2Id = UUID.randomUUID();
      UUID participant2Id = UUID.randomUUID();

      Game game1 = buildGame();
      Game game2 =
          Game.restore(
              game2Id,
              "Game 2",
              null,
              CREATOR_ID,
              8,
              GameStatus.ACTIVE,
              LocalDateTime.now(),
              null,
              null,
              "CODE5678",
              null,
              List.of(),
              List.of(),
              draft2Id,
              false,
              5,
              null,
              2025,
              DraftMode.SNAKE,
              5,
              10,
              true,
              PERIOD_START,
              PERIOD_END);

      GameParticipant participant2 = buildParticipant(participant2Id);

      when(gameDomainRepository.findAllWithCompetitionPeriod()).thenReturn(List.of(game1, game2));

      when(participantRepository.findByGameIdOrderByJoinedAt(GAME_ID))
          .thenThrow(new RuntimeException("DB error for game1"));

      when(participantRepository.findByGameIdOrderByJoinedAt(game2Id))
          .thenReturn(List.of(participant2));
      when(draftPickRepository.findPlayerIdsByDraftIdAndParticipantId(draft2Id, participant2Id))
          .thenReturn(List.of());
      when(deltaRepository.findByGameIdAndParticipantId(game2Id, participant2Id))
          .thenReturn(Optional.empty());
      when(deltaRepository.save(any(TeamScoreDelta.class))).thenAnswer(inv -> inv.getArgument(0));

      service.computeAllGameDeltas();

      verify(deltaRepository, times(1)).save(any(TeamScoreDelta.class));
    }
  }
}
