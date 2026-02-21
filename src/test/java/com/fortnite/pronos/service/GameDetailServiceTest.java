package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.draft.model.Draft;
import com.fortnite.pronos.domain.draft.model.DraftStatus;
import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameParticipant;
import com.fortnite.pronos.domain.game.model.GameStatus;
import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.domain.port.out.DraftDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.ScoreRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.dto.GameDetailDto;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.model.User;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameDetailService - domain migration")
class GameDetailServiceTest {

  @Mock private GameDomainRepositoryPort gameRepository;
  @Mock private DraftDomainRepositoryPort draftRepository;
  @Mock private PlayerDomainRepositoryPort playerRepository;
  @Mock private ScoreRepositoryPort scoreRepository;
  @Mock private UserRepositoryPort userRepository;

  @InjectMocks private GameDetailService service;

  private UUID gameId;
  private UUID creatorId;
  private UUID teddyId;
  private UUID marcelId;

  private UUID bughaId;
  private UUID aquaId;
  private UUID unknownId;

  private Game testGame;
  private GameParticipant creatorParticipant;
  private GameParticipant teddyParticipant;
  private GameParticipant marcelParticipant;

  @BeforeEach
  void setUp() {
    gameId = UUID.randomUUID();
    creatorId = UUID.randomUUID();
    teddyId = UUID.randomUUID();
    marcelId = UUID.randomUUID();

    bughaId = UUID.randomUUID();
    aquaId = UUID.randomUUID();
    unknownId = UUID.randomUUID();

    creatorParticipant =
        GameParticipant.restore(
            UUID.randomUUID(),
            creatorId,
            "Thibaut",
            1,
            LocalDateTime.now().minusDays(3),
            null,
            true,
            List.of(bughaId));
    teddyParticipant =
        GameParticipant.restore(
            UUID.randomUUID(),
            teddyId,
            "Teddy",
            2,
            LocalDateTime.now().minusDays(2),
            null,
            false,
            List.of(aquaId));
    marcelParticipant =
        GameParticipant.restore(
            UUID.randomUUID(),
            marcelId,
            "Marcel",
            3,
            LocalDateTime.now().minusDays(1),
            null,
            false,
            List.of());

    testGame =
        Game.restore(
            gameId,
            "Game Thibaut-Teddy-Marcel",
            "Game de test entre amis",
            creatorId,
            10,
            GameStatus.ACTIVE,
            LocalDateTime.now().minusDays(5),
            null,
            null,
            "TTM2025",
            null,
            List.of(),
            List.of(creatorParticipant, teddyParticipant, marcelParticipant),
            null,
            false,
            5,
            null,
            2025);
  }

  @Test
  @DisplayName("retourne les details complets d'une game")
  void shouldReturnCompleteGameDetails() {
    Draft draft =
        Draft.restore(
            UUID.randomUUID(),
            gameId,
            DraftStatus.FINISHED,
            3,
            9,
            3,
            LocalDateTime.now().minusDays(5),
            LocalDateTime.now().minusDays(3),
            LocalDateTime.now().minusDays(4),
            LocalDateTime.now().minusDays(3));

    when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
    when(draftRepository.findByGameId(gameId)).thenReturn(Optional.of(draft));
    when(userRepository.findById(creatorId))
        .thenReturn(Optional.of(createUser(creatorId, "Thibaut")));
    when(userRepository.findById(teddyId)).thenReturn(Optional.of(createUser(teddyId, "Teddy")));
    when(userRepository.findById(marcelId)).thenReturn(Optional.of(createUser(marcelId, "Marcel")));

    when(playerRepository.findById(bughaId))
        .thenReturn(Optional.of(createPlayer(bughaId, "Bugha", PlayerRegion.NAW)));
    when(playerRepository.findById(aquaId))
        .thenReturn(Optional.of(createPlayer(aquaId, "Aqua", PlayerRegion.EU)));

    when(scoreRepository.sumPointsByPlayerAndSeason(eq(bughaId), anyInt())).thenReturn(15000);
    when(scoreRepository.sumPointsByPlayerAndSeason(eq(aquaId), anyInt())).thenReturn(12000);

    GameDetailDto result = service.getGameDetails(gameId);

    assertThat(result).isNotNull();
    assertThat(result.getGameId()).isEqualTo(gameId);
    assertThat(result.getGameName()).isEqualTo("Game Thibaut-Teddy-Marcel");
    assertThat(result.getCreatorName()).isEqualTo("Thibaut");
    assertThat(result.getParticipants()).hasSize(3);
    assertThat(result.getDraftInfo()).isNotNull();
    assertThat(result.getDraftInfo().getStatus()).isEqualTo("FINISHED");

    assertThat(result.getStatistics().getTotalParticipants()).isEqualTo(3);
    assertThat(result.getTotalPlayers()).isEqualTo(2);
    assertThat(result.getStatistics().getRegionDistribution())
        .containsEntry("NAW", 1)
        .containsEntry("EU", 1);
  }

  @Test
  @DisplayName("applique un fallback quand un joueur est introuvable")
  void shouldFallbackWhenPlayerIsMissing() {
    GameParticipant participantWithMissingPlayer =
        GameParticipant.restore(
            UUID.randomUUID(),
            teddyId,
            "Teddy",
            2,
            LocalDateTime.now().minusDays(2),
            null,
            false,
            List.of(aquaId, unknownId));
    Game gameWithMissingPlayer =
        Game.restore(
            gameId,
            "Fallback Game",
            null,
            creatorId,
            10,
            GameStatus.ACTIVE,
            LocalDateTime.now().minusDays(5),
            null,
            null,
            "CODE1234",
            null,
            List.of(),
            List.of(creatorParticipant, participantWithMissingPlayer),
            null,
            false,
            5,
            null,
            2025);

    when(gameRepository.findById(gameId)).thenReturn(Optional.of(gameWithMissingPlayer));
    when(draftRepository.findByGameId(gameId)).thenReturn(Optional.empty());
    when(userRepository.findById(creatorId))
        .thenReturn(Optional.of(createUser(creatorId, "Thibaut")));
    when(userRepository.findById(teddyId)).thenReturn(Optional.of(createUser(teddyId, "Teddy")));

    when(playerRepository.findById(aquaId))
        .thenReturn(Optional.of(createPlayer(aquaId, "Aqua", PlayerRegion.EU)));
    when(playerRepository.findById(bughaId))
        .thenReturn(Optional.of(createPlayer(bughaId, "Bugha", PlayerRegion.NAW)));
    when(playerRepository.findById(unknownId)).thenReturn(Optional.empty());

    when(scoreRepository.sumPointsByPlayerAndSeason(eq(aquaId), anyInt())).thenReturn(4000);
    when(scoreRepository.sumPointsByPlayerAndSeason(eq(bughaId), anyInt())).thenReturn(5000);

    GameDetailDto result = service.getGameDetails(gameId);

    GameDetailDto.ParticipantInfo teddyInfo =
        result.getParticipants().stream()
            .filter(p -> "Teddy".equals(p.getUsername()))
            .findFirst()
            .orElseThrow();

    assertThat(teddyInfo.getSelectedPlayers()).hasSize(2);
    assertThat(teddyInfo.getSelectedPlayers())
        .extracting(GameDetailDto.PlayerInfo::getNickname)
        .contains("Joueur indisponible");
  }

  @Test
  @DisplayName("leve une exception si la game est introuvable")
  void shouldThrowWhenGameNotFound() {
    when(gameRepository.findById(gameId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getGameDetails(gameId))
        .isInstanceOf(GameNotFoundException.class)
        .hasMessageContaining("Game non trouvee");
  }

  @Test
  @DisplayName("gere une game sans draft")
  void shouldHandleGameWithoutDraft() {
    when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
    when(draftRepository.findByGameId(gameId)).thenReturn(Optional.empty());
    when(userRepository.findById(creatorId))
        .thenReturn(Optional.of(createUser(creatorId, "Thibaut")));
    when(userRepository.findById(teddyId)).thenReturn(Optional.of(createUser(teddyId, "Teddy")));
    when(userRepository.findById(marcelId)).thenReturn(Optional.of(createUser(marcelId, "Marcel")));
    when(playerRepository.findById(bughaId))
        .thenReturn(Optional.of(createPlayer(bughaId, "Bugha", PlayerRegion.NAW)));
    when(playerRepository.findById(aquaId))
        .thenReturn(Optional.of(createPlayer(aquaId, "Aqua", PlayerRegion.EU)));

    GameDetailDto result = service.getGameDetails(gameId);

    assertThat(result.getDraftInfo()).isNull();
    assertThat(result.getStatus()).isEqualTo("ACTIVE");
  }

  @Test
  @DisplayName("retourne 0 quand aucun score n'existe")
  void shouldReturnZeroWhenNoScoreExists() {
    when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
    when(draftRepository.findByGameId(gameId)).thenReturn(Optional.empty());
    when(userRepository.findById(creatorId))
        .thenReturn(Optional.of(createUser(creatorId, "Thibaut")));
    when(userRepository.findById(teddyId)).thenReturn(Optional.of(createUser(teddyId, "Teddy")));
    when(userRepository.findById(marcelId)).thenReturn(Optional.of(createUser(marcelId, "Marcel")));

    when(playerRepository.findById(bughaId))
        .thenReturn(Optional.of(createPlayer(bughaId, "Bugha", PlayerRegion.NAW)));
    when(playerRepository.findById(aquaId))
        .thenReturn(Optional.of(createPlayer(aquaId, "Aqua", PlayerRegion.EU)));
    when(scoreRepository.sumPointsByPlayerAndSeason(eq(bughaId), anyInt())).thenReturn(null);
    when(scoreRepository.sumPointsByPlayerAndSeason(eq(aquaId), anyInt())).thenReturn(null);

    GameDetailDto result = service.getGameDetails(gameId);

    GameDetailDto.ParticipantInfo thibautInfo =
        result.getParticipants().stream()
            .filter(p -> "Thibaut".equals(p.getUsername()))
            .findFirst()
            .orElseThrow();

    assertThat(thibautInfo.getSelectedPlayers().get(0).getCurrentScore()).isZero();
  }

  @Test
  @DisplayName("utilise le fallback creator depuis UserRepository si username manquant")
  void shouldResolveCreatorFromUserRepositoryFallback() {
    GameParticipant creatorWithoutUsername =
        GameParticipant.restore(
            UUID.randomUUID(),
            creatorId,
            null,
            1,
            LocalDateTime.now().minusDays(3),
            null,
            true,
            List.of());
    Game gameWithoutCreatorName =
        Game.restore(
            gameId,
            "Fallback creator game",
            null,
            creatorId,
            8,
            GameStatus.CREATING,
            LocalDateTime.now().minusDays(2),
            null,
            null,
            null,
            null,
            List.of(),
            List.of(creatorWithoutUsername),
            null,
            false,
            5,
            null,
            2025);

    when(gameRepository.findById(gameId)).thenReturn(Optional.of(gameWithoutCreatorName));
    when(draftRepository.findByGameId(gameId)).thenReturn(Optional.empty());
    when(userRepository.findById(creatorId))
        .thenReturn(Optional.of(createUser(creatorId, "CreatorFromRepo")));

    GameDetailDto result = service.getGameDetails(gameId);

    assertThat(result.getCreatorName()).isEqualTo("CreatorFromRepo");
  }

  private User createUser(UUID userId, String username) {
    User user = new User();
    user.setId(userId);
    user.setUsername(username);
    user.setEmail(username.toLowerCase() + "@test.com");
    return user;
  }

  private com.fortnite.pronos.domain.player.model.Player createPlayer(
      UUID playerId, String nickname, PlayerRegion region) {
    return com.fortnite.pronos.domain.player.model.Player.restore(
        playerId, null, nickname.toLowerCase(), nickname, region, "1-10", 2025, false);
  }
}
