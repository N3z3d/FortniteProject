package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.dto.GameDetailDto;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.model.*;
import com.fortnite.pronos.repository.DraftPickRepository;
import com.fortnite.pronos.repository.DraftRepository;
import com.fortnite.pronos.repository.GameParticipantRepository;
import com.fortnite.pronos.repository.GameRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameDetailService - getGameDetails TDD")
class GameDetailServiceTest {

  @Mock private GameRepository gameRepository;

  @Mock private GameParticipantRepository gameParticipantRepository;

  @Mock private DraftRepository draftRepository;

  @Mock private DraftPickRepository draftPickRepository;

  @InjectMocks private GameDetailService gameDetailService;

  private Game testGame;
  private User thibaut;
  private User teddy;
  private User marcel;
  private GameParticipant participantThibaut;
  private GameParticipant participantTeddy;
  private GameParticipant participantMarcel;
  private Draft draft;

  @BeforeEach
  void setUp() {
    // Créer les utilisateurs
    thibaut = createUser("Thibaut", "thibaut@test.com");
    teddy = createUser("Teddy", "teddy@test.com");
    marcel = createUser("Marcel", "marcel@test.com");

    // Créer la game
    testGame = new Game();
    testGame.setId(UUID.randomUUID());
    testGame.setName("Game Thibaut-Teddy-Marcel");
    testGame.setCreator(thibaut);
    testGame.setStatus(GameStatus.ACTIVE);
    testGame.setMaxParticipants(10);
    testGame.setInvitationCode("TTM2025");
    testGame.setDescription("Game de test entre amis");
    testGame.setCreatedAt(LocalDateTime.now().minusDays(5));
    // testGame.setUpdatedAt(LocalDateTime.now()); // Champ n'existe pas

    // Créer les participants
    participantThibaut = createParticipant(testGame, thibaut, 1);
    participantTeddy = createParticipant(testGame, teddy, 2);
    participantMarcel = createParticipant(testGame, marcel, 3);

    // Ajouter des joueurs sélectionnés
    participantThibaut.setSelectedPlayers(
        Arrays.asList(
            createPlayer("Bugha", Player.Region.NAW),
            createPlayer("Aqua", Player.Region.EU),
            createPlayer("K1ng", Player.Region.BR)));

    participantTeddy.setSelectedPlayers(
        Arrays.asList(
            createPlayer("Tayson", Player.Region.EU),
            createPlayer("Savage", Player.Region.NAC),
            createPlayer("Epikwhale", Player.Region.NAW)));

    participantMarcel.setSelectedPlayers(
        Arrays.asList(
            createPlayer("Kami", Player.Region.EU),
            createPlayer("Mero", Player.Region.NAC),
            createPlayer("Hen", Player.Region.EU)));

    // Créer le draft
    draft = new Draft();
    draft.setId(UUID.randomUUID());
    draft.setGame(testGame);
    draft.setStatus(Draft.Status.FINISHED);
    draft.setStartedAt(LocalDateTime.now().minusDays(4));
    draft.setFinishedAt(LocalDateTime.now().minusDays(3));
    draft.setCurrentRound(3);
    draft.setCurrentPick(9);
    draft.setTotalRounds(3);
    draft.setCreatedAt(LocalDateTime.now().minusDays(5));
    draft.setUpdatedAt(LocalDateTime.now().minusDays(3));
  }

  @Test
  @DisplayName("devrait récupérer les détails complets d'une game")
  void shouldGetCompleteGameDetails() {
    // Given
    when(gameRepository.findById(testGame.getId())).thenReturn(Optional.of(testGame));
    when(gameParticipantRepository.findByGame(testGame))
        .thenReturn(Arrays.asList(participantThibaut, participantTeddy, participantMarcel));
    when(draftRepository.findByGame(testGame)).thenReturn(Optional.of(draft));

    // When
    GameDetailDto result = gameDetailService.getGameDetails(testGame.getId());

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getGameId()).isEqualTo(testGame.getId());
    assertThat(result.getGameName()).isEqualTo("Game Thibaut-Teddy-Marcel");
    assertThat(result.getCreatorName()).isEqualTo("Thibaut");
    assertThat(result.getStatus()).isEqualTo("ACTIVE");
    assertThat(result.getInvitationCode()).isEqualTo("TTM2025");
    assertThat(result.getParticipants()).hasSize(3);

    // Vérifier les participants
    assertThat(result.getParticipants())
        .extracting(GameDetailDto.ParticipantInfo::getUsername)
        .containsExactlyInAnyOrder("Thibaut", "Teddy", "Marcel");
  }

  @Test
  @DisplayName("devrait inclure les détails de chaque participant")
  void shouldIncludeParticipantDetails() {
    // Given
    when(gameRepository.findById(testGame.getId())).thenReturn(Optional.of(testGame));
    when(gameParticipantRepository.findByGame(testGame))
        .thenReturn(Arrays.asList(participantThibaut, participantTeddy, participantMarcel));
    when(draftRepository.findByGame(testGame)).thenReturn(Optional.of(draft));

    // When
    GameDetailDto result = gameDetailService.getGameDetails(testGame.getId());

    // Then
    GameDetailDto.ParticipantInfo thibautInfo =
        result.getParticipants().stream()
            .filter(p -> p.getUsername().equals("Thibaut"))
            .findFirst()
            .orElseThrow();

    assertThat(thibautInfo.getJoinedAt()).isNotNull();
    assertThat(thibautInfo.getSelectedPlayers()).hasSize(3);
    assertThat(thibautInfo.getSelectedPlayers())
        .extracting(GameDetailDto.PlayerInfo::getNickname)
        .containsExactlyInAnyOrder("Bugha", "Aqua", "K1ng");
    assertThat(thibautInfo.getTotalPlayers()).isEqualTo(3);
    assertThat(thibautInfo.getIsCreator()).isTrue();
  }

  @Test
  @DisplayName("devrait appliquer un fallback si un joueur est manquant")
  void shouldFallbackWhenPlayerIsMissing() {
    // Given
    participantTeddy.setSelectedPlayers(
        Arrays.asList(createPlayer("Tayson", Player.Region.EU), null));
    when(gameRepository.findById(testGame.getId())).thenReturn(Optional.of(testGame));
    when(gameParticipantRepository.findByGame(testGame))
        .thenReturn(Arrays.asList(participantThibaut, participantTeddy));
    when(draftRepository.findByGame(testGame)).thenReturn(Optional.of(draft));

    // When
    GameDetailDto result = gameDetailService.getGameDetails(testGame.getId());

    // Then
    GameDetailDto.ParticipantInfo teddyInfo =
        result.getParticipants().stream()
            .filter(p -> p.getUsername().equals("Teddy"))
            .findFirst()
            .orElseThrow();

    assertThat(teddyInfo.getSelectedPlayers()).hasSize(2);
    assertThat(teddyInfo.getSelectedPlayers())
        .extracting(GameDetailDto.PlayerInfo::getNickname)
        .contains("Joueur indisponible");
  }

  @Test
  @DisplayName("devrait inclure les informations du draft")
  void shouldIncludeDraftInformation() {
    // Given
    when(gameRepository.findById(testGame.getId())).thenReturn(Optional.of(testGame));
    when(gameParticipantRepository.findByGame(testGame))
        .thenReturn(Arrays.asList(participantThibaut, participantTeddy, participantMarcel));
    when(draftRepository.findByGame(testGame)).thenReturn(Optional.of(draft));

    // When
    GameDetailDto result = gameDetailService.getGameDetails(testGame.getId());

    // Then
    assertThat(result.getDraftInfo()).isNotNull();
    assertThat(result.getDraftInfo().getStatus()).isEqualTo("FINISHED");
    assertThat(result.getDraftInfo().getStartedAt()).isNotNull();
    assertThat(result.getDraftInfo().getFinishedAt()).isNotNull();
  }

  @Test
  @DisplayName("devrait calculer les statistiques de la game")
  void shouldCalculateGameStatistics() {
    // Given
    when(gameRepository.findById(testGame.getId())).thenReturn(Optional.of(testGame));
    when(gameParticipantRepository.findByGame(testGame))
        .thenReturn(Arrays.asList(participantThibaut, participantTeddy, participantMarcel));
    when(draftRepository.findByGame(testGame)).thenReturn(Optional.of(draft));

    // When
    GameDetailDto result = gameDetailService.getGameDetails(testGame.getId());

    // Then
    assertThat(result.getStatistics()).isNotNull();
    assertThat(result.getStatistics().getTotalParticipants()).isEqualTo(3);
    assertThat(result.getStatistics().getTotalPlayers()).isEqualTo(9);
    assertThat(result.getStatistics().getRegionDistribution()).isNotEmpty();
    assertThat(result.getStatistics().getRegionDistribution().get("EU")).isEqualTo(4);
    assertThat(result.getStatistics().getRegionDistribution().get("NAW")).isEqualTo(2);
    assertThat(result.getStatistics().getRegionDistribution().get("NAC")).isEqualTo(2);
    assertThat(result.getStatistics().getRegionDistribution().get("BR")).isEqualTo(1);
  }

  @Test
  @DisplayName("devrait lever une exception si la game n'existe pas")
  void shouldThrowExceptionIfGameNotFound() {
    // Given
    UUID unknownGameId = UUID.randomUUID();
    when(gameRepository.findById(unknownGameId)).thenReturn(Optional.empty());

    // When/Then
    assertThatThrownBy(() -> gameDetailService.getGameDetails(unknownGameId))
        .isInstanceOf(GameNotFoundException.class)
        .hasMessageContaining("Game non trouvée");
  }

  @Test
  @DisplayName("devrait gérer une game sans draft")
  void shouldHandleGameWithoutDraft() {
    // Given
    testGame.setStatus(GameStatus.CREATING);
    when(gameRepository.findById(testGame.getId())).thenReturn(Optional.of(testGame));
    when(gameParticipantRepository.findByGame(testGame))
        .thenReturn(Arrays.asList(participantThibaut, participantTeddy, participantMarcel));
    when(draftRepository.findByGame(testGame)).thenReturn(Optional.empty());

    // When
    GameDetailDto result = gameDetailService.getGameDetails(testGame.getId());

    // Then
    assertThat(result.getDraftInfo()).isNull();
    assertThat(result.getStatus()).isEqualTo("CREATING");
  }

  // Méthodes utilitaires
  private User createUser(String username, String email) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername(username);
    user.setEmail(email);
    user.setPassword("password123");
    return user;
  }

  private GameParticipant createParticipant(Game game, User user, int joinOrder) {
    GameParticipant participant = new GameParticipant();
    participant.setId(UUID.randomUUID());
    participant.setGame(game);
    participant.setUser(user);
    participant.setDraftOrder(joinOrder);
    participant.setLastSelectionTime(LocalDateTime.now().minusDays(4));
    return participant;
  }

  private Player createPlayer(String nickname, Player.Region region) {
    Player player = new Player();
    player.setId(UUID.randomUUID());
    player.setNickname(nickname);
    player.setUsername(nickname.toLowerCase());
    player.setRegion(region);
    player.setTranche("1-10");
    player.setCurrentSeason(2025);
    return player;
  }
}
