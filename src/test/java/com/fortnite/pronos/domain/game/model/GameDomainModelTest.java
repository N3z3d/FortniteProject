package com.fortnite.pronos.domain.game.model;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"java:S5778"})
class GameDomainModelTest {

  private static final UUID CREATOR_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private Game game;

  @BeforeEach
  void setUp() {
    game = new Game("Test Game", CREATOR_ID, 4);
  }

  @Nested
  class Creation {

    @Test
    void createsGameWithValidParameters() {
      assertThat(game.getName()).isEqualTo("Test Game");
      assertThat(game.getCreatorId()).isEqualTo(CREATOR_ID);
      assertThat(game.getMaxParticipants()).isEqualTo(4);
      assertThat(game.getStatus()).isEqualTo(GameStatus.CREATING);
      assertThat(game.getId()).isNotNull();
      assertThat(game.getCreatedAt()).isNotNull();
    }

    @Test
    void trimsGameName() {
      Game g = new Game("  Spaced Name  ", CREATOR_ID, 2);
      assertThat(g.getName()).isEqualTo("Spaced Name");
    }

    @Test
    void rejectsNullName() {
      assertThatIllegalArgumentException().isThrownBy(() -> new Game(null, CREATOR_ID, 4));
    }

    @Test
    void rejectsEmptyName() {
      assertThatIllegalArgumentException().isThrownBy(() -> new Game("  ", CREATOR_ID, 4));
    }

    @Test
    void rejectsNullCreator() {
      assertThatIllegalArgumentException().isThrownBy(() -> new Game("Test", null, 4));
    }

    @Test
    void rejectsTooFewParticipants() {
      assertThatIllegalArgumentException().isThrownBy(() -> new Game("Test", CREATOR_ID, 1));
    }

    @Test
    void rejectsTooManyParticipants() {
      assertThatIllegalArgumentException().isThrownBy(() -> new Game("Test", CREATOR_ID, 51));
    }

    @Test
    void acceptsBoundaryParticipants() {
      assertThatCode(() -> new Game("Min", CREATOR_ID, 2)).doesNotThrowAnyException();
      assertThatCode(() -> new Game("Max", CREATOR_ID, 50)).doesNotThrowAnyException();
    }
  }

  @Nested
  class Participants {

    @Test
    void addsParticipantSuccessfully() {
      GameParticipant participant = new GameParticipant(USER_ID, "player1", false);
      assertThat(game.addParticipant(participant)).isTrue();
      assertThat(game.getParticipants()).hasSize(1);
    }

    @Test
    void rejectsCreatorAsParticipant() {
      GameParticipant creator = new GameParticipant(CREATOR_ID, "creator", false);
      assertThat(game.addParticipant(creator)).isFalse();
    }

    @Test
    void rejectsDuplicateParticipant() {
      GameParticipant p = new GameParticipant(USER_ID, "player1", false);
      game.addParticipant(p);
      GameParticipant duplicate = new GameParticipant(USER_ID, "player1", false);
      assertThat(game.addParticipant(duplicate)).isFalse();
    }

    @Test
    void rejectsParticipantWhenFull() {
      // maxParticipants = 4 (including creator counted as 1)
      game.addParticipant(new GameParticipant(UUID.randomUUID(), "p1", false));
      game.addParticipant(new GameParticipant(UUID.randomUUID(), "p2", false));
      game.addParticipant(new GameParticipant(UUID.randomUUID(), "p3", false));
      // Total = 4 (creator + 3 participants) = full
      assertThat(game.addParticipant(new GameParticipant(UUID.randomUUID(), "p4", false)))
          .isFalse();
    }

    @Test
    void rejectsParticipantAfterDraftStarted() {
      game.addParticipant(new GameParticipant(UUID.randomUUID(), "p1", false));
      game.startDraft();
      assertThat(game.addParticipant(new GameParticipant(UUID.randomUUID(), "p2", false)))
          .isFalse();
    }

    @Test
    void isParticipantReturnsTrueForCreator() {
      assertThat(game.isParticipant(CREATOR_ID)).isTrue();
    }

    @Test
    void isParticipantReturnsTrueForAdded() {
      game.addParticipant(new GameParticipant(USER_ID, "player1", false));
      assertThat(game.isParticipant(USER_ID)).isTrue();
    }

    @Test
    void isParticipantReturnsFalseForUnknown() {
      assertThat(game.isParticipant(UUID.randomUUID())).isFalse();
    }

    @Test
    void totalParticipantCountIncludesCreator() {
      assertThat(game.getTotalParticipantCount()).isEqualTo(1); // creator only
      game.addParticipant(new GameParticipant(USER_ID, "player1", false));
      assertThat(game.getTotalParticipantCount()).isEqualTo(2);
    }

    @Test
    void availableSpotsDecreases() {
      assertThat(game.getAvailableSpots()).isEqualTo(3); // 4 - 1 creator
      game.addParticipant(new GameParticipant(USER_ID, "p1", false));
      assertThat(game.getAvailableSpots()).isEqualTo(2);
    }

    @Test
    void removesParticipant() {
      GameParticipant p = new GameParticipant(USER_ID, "p1", false);
      game.addParticipant(p);
      game.removeParticipant(p);
      assertThat(game.getParticipants()).isEmpty();
    }
  }

  @Nested
  class StateTransitions {

    @Test
    void startsDraftWithEnoughParticipants() {
      game.addParticipant(new GameParticipant(USER_ID, "p1", false));
      assertThat(game.startDraft()).isTrue();
      assertThat(game.getStatus()).isEqualTo(GameStatus.DRAFTING);
    }

    @Test
    void cannotStartDraftWithTooFewParticipants() {
      // Only creator (1 participant) - need at least 2
      assertThat(game.startDraft()).isFalse();
      assertThat(game.getStatus()).isEqualTo(GameStatus.CREATING);
    }

    @Test
    void cannotStartDraftTwice() {
      game.addParticipant(new GameParticipant(USER_ID, "p1", false));
      game.startDraft();
      assertThat(game.startDraft()).isFalse();
    }

    @Test
    void completesDraftToActive() {
      game.addParticipant(new GameParticipant(USER_ID, "p1", false));
      game.startDraft();
      assertThat(game.completeDraft()).isTrue();
      assertThat(game.getStatus()).isEqualTo(GameStatus.ACTIVE);
    }

    @Test
    void cannotCompleteDraftFromCreating() {
      assertThat(game.completeDraft()).isFalse();
    }

    @Test
    void finishesActiveGame() {
      game.addParticipant(new GameParticipant(USER_ID, "p1", false));
      game.startDraft();
      game.completeDraft();
      assertThat(game.finishGame()).isTrue();
      assertThat(game.getStatus()).isEqualTo(GameStatus.FINISHED);
      assertThat(game.getFinishedAt()).isNotNull();
    }

    @Test
    void cannotFinishNonActiveGame() {
      assertThat(game.finishGame()).isFalse();
    }

    @Test
    void isDraftingReturnsCorrectly() {
      assertThat(game.isDrafting()).isFalse();
      game.addParticipant(new GameParticipant(USER_ID, "p1", false));
      game.startDraft();
      assertThat(game.isDrafting()).isTrue();
    }

    @Test
    void isActiveReturnsCorrectly() {
      assertThat(game.isActive()).isFalse();
      game.addParticipant(new GameParticipant(USER_ID, "p1", false));
      game.startDraft();
      game.completeDraft();
      assertThat(game.isActive()).isTrue();
    }
  }

  @Nested
  class SoftDelete {

    @Test
    void softDeleteSetsTimestamp() {
      assertThat(game.isDeleted()).isFalse();
      game.softDelete();
      assertThat(game.isDeleted()).isTrue();
      assertThat(game.getDeletedAt()).isNotNull();
    }
  }

  @Nested
  class InvitationCode {

    @Test
    void generatesCode() {
      String code = game.generateInvitationCode();
      assertThat(code).hasSize(8).matches("[A-Z0-9]+");
    }

    @Test
    void generatesCodeOnlyOnce() {
      String first = game.generateInvitationCode();
      String second = game.generateInvitationCode();
      assertThat(first).isEqualTo(second);
    }

    @Test
    void codeInvalidWhenNull() {
      assertThat(game.isInvitationCodeValid()).isFalse();
    }

    @Test
    void codeValidWhenSetWithoutExpiry() {
      game.setInvitationCode("ABCD1234");
      assertThat(game.isInvitationCodeValid()).isTrue();
    }

    @Test
    void codeInvalidWhenExpired() {
      game.setInvitationCode("ABCD1234");
      game.setInvitationCodeExpiresAt(LocalDateTime.now().minusHours(1));
      assertThat(game.isInvitationCodeValid()).isFalse();
      assertThat(game.isInvitationCodeExpired()).isTrue();
    }

    @Test
    void codeNotExpiredWhenNoExpiry() {
      game.setInvitationCode("ABCD1234");
      assertThat(game.isInvitationCodeExpired()).isFalse();
    }
  }

  @Nested
  class RegionRules {

    @Test
    void addsRegionRule() {
      GameRegionRule rule = new GameRegionRule(PlayerRegion.EU, 3);
      game.addRegionRule(rule);
      assertThat(game.getRegionRules()).hasSize(1);
    }

    @Test
    void removesRegionRule() {
      GameRegionRule rule = new GameRegionRule(PlayerRegion.EU, 3);
      game.addRegionRule(rule);
      game.removeRegionRule(rule);
      assertThat(game.getRegionRules()).isEmpty();
    }

    @Test
    void regionRulesListIsUnmodifiable() {
      assertThatExceptionOfType(UnsupportedOperationException.class)
          .isThrownBy(() -> game.getRegionRules().add(new GameRegionRule(PlayerRegion.EU, 1)));
    }
  }

  @Nested
  class Rename {

    @Test
    void renamesSuccessfully() {
      game.rename("New Name");
      assertThat(game.getName()).isEqualTo("New Name");
    }

    @Test
    void rejectsNullRename() {
      assertThatIllegalArgumentException().isThrownBy(() -> game.rename(null));
    }

    @Test
    void rejectsEmptyRename() {
      assertThatIllegalArgumentException().isThrownBy(() -> game.rename(""));
    }
  }

  @Nested
  class Restore {

    @Test
    void restoresFullGameState() {
      UUID id = UUID.randomUUID();
      LocalDateTime now = LocalDateTime.now();
      GameRegionRule rule = new GameRegionRule(UUID.randomUUID(), PlayerRegion.EU, 2);
      GameParticipant p = new GameParticipant(USER_ID, "player1", false);

      Game restored =
          Game.restore(
              id,
              "Restored",
              "desc",
              CREATOR_ID,
              10,
              GameStatus.ACTIVE,
              now,
              null,
              null,
              "CODE1234",
              null,
              List.of(rule),
              List.of(p),
              UUID.randomUUID(),
              true,
              3,
              now,
              2025);

      assertThat(restored.getId()).isEqualTo(id);
      assertThat(restored.getName()).isEqualTo("Restored");
      assertThat(restored.getDescription()).isEqualTo("desc");
      assertThat(restored.getCreatorId()).isEqualTo(CREATOR_ID);
      assertThat(restored.getMaxParticipants()).isEqualTo(10);
      assertThat(restored.getStatus()).isEqualTo(GameStatus.ACTIVE);
      assertThat(restored.getRegionRules()).hasSize(1);
      assertThat(restored.getParticipants()).hasSize(1);
      assertThat(restored.isTradingEnabled()).isTrue();
      assertThat(restored.getMaxTradesPerTeam()).isEqualTo(3);
      assertThat(restored.getCurrentSeason()).isEqualTo(2025);
    }

    @Test
    void restoresWithNullCollections() {
      Game restored =
          Game.restore(
              UUID.randomUUID(),
              "Test",
              null,
              CREATOR_ID,
              4,
              GameStatus.CREATING,
              LocalDateTime.now(),
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              false,
              5,
              null,
              2025);

      assertThat(restored.getRegionRules()).isEmpty();
      assertThat(restored.getParticipants()).isEmpty();
    }
  }
}
