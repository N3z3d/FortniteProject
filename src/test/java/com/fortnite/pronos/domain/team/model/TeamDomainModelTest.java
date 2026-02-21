package com.fortnite.pronos.domain.team.model;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"java:S5778"})
class TeamDomainModelTest {

  private static final UUID OWNER_ID = UUID.randomUUID();
  private Team team;

  @BeforeEach
  void setUp() {
    team = new Team("Team Alpha", OWNER_ID, 2025);
  }

  @Nested
  class Creation {

    @Test
    void createsTeamWithValidParameters() {
      assertThat(team.getName()).isEqualTo("Team Alpha");
      assertThat(team.getOwnerId()).isEqualTo(OWNER_ID);
      assertThat(team.getSeason()).isEqualTo(2025);
      assertThat(team.getId()).isNotNull();
      assertThat(team.getCompletedTradesCount()).isZero();
      assertThat(team.getMembers()).isEmpty();
      assertThat(team.getGameId()).isNull();
    }

    @Test
    void trimsName() {
      Team t = new Team("  Spaced Name  ", OWNER_ID, 2025);
      assertThat(t.getName()).isEqualTo("Spaced Name");
    }

    @Test
    void rejectsNullName() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new Team(null, OWNER_ID, 2025))
          .withMessageContaining("name");
    }

    @Test
    void rejectsEmptyName() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new Team("  ", OWNER_ID, 2025))
          .withMessageContaining("name");
    }

    @Test
    void rejectsNullOwnerId() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new Team("Team", null, 2025))
          .withMessageContaining("Owner");
    }

    @Test
    void rejectsZeroSeason() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new Team("Team", OWNER_ID, 0))
          .withMessageContaining("Season");
    }

    @Test
    void rejectsNegativeSeason() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new Team("Team", OWNER_ID, -1))
          .withMessageContaining("Season");
    }
  }

  @Nested
  class Restore {

    @Test
    void restoresFullState() {
      UUID id = UUID.randomUUID();
      UUID gameId = UUID.randomUUID();
      UUID playerId = UUID.randomUUID();
      TeamMember member = TeamMember.restore(playerId, 1, null);

      Team restored = Team.restore(id, "Restored", OWNER_ID, 2024, gameId, 3, List.of(member));

      assertThat(restored.getId()).isEqualTo(id);
      assertThat(restored.getName()).isEqualTo("Restored");
      assertThat(restored.getOwnerId()).isEqualTo(OWNER_ID);
      assertThat(restored.getSeason()).isEqualTo(2024);
      assertThat(restored.getGameId()).isEqualTo(gameId);
      assertThat(restored.getCompletedTradesCount()).isEqualTo(3);
      assertThat(restored.getMembers()).hasSize(1);
    }

    @Test
    void restoresWithNullMembers() {
      UUID id = UUID.randomUUID();
      Team restored = Team.restore(id, "Empty", OWNER_ID, 2025, null, 0, null);

      assertThat(restored.getMembers()).isEmpty();
      assertThat(restored.getGameId()).isNull();
    }

    @Test
    void restoresWithEmptyMembers() {
      UUID id = UUID.randomUUID();
      Team restored = Team.restore(id, "Empty", OWNER_ID, 2025, null, 0, List.of());

      assertThat(restored.getMembers()).isEmpty();
    }
  }

  @Nested
  class MemberManagement {

    @Test
    void addsMemberSuccessfully() {
      UUID playerId = UUID.randomUUID();
      team.addMember(playerId, 1);

      assertThat(team.getMembers()).hasSize(1);
      assertThat(team.hasActiveMember(playerId)).isTrue();
    }

    @Test
    void addsMembersAtDifferentPositions() {
      UUID p1 = UUID.randomUUID();
      UUID p2 = UUID.randomUUID();
      team.addMember(p1, 1);
      team.addMember(p2, 2);

      assertThat(team.getActiveMemberCount()).isEqualTo(2);
    }

    @Test
    void rejectsDuplicatePosition() {
      UUID p1 = UUID.randomUUID();
      UUID p2 = UUID.randomUUID();
      team.addMember(p1, 1);

      assertThatIllegalStateException()
          .isThrownBy(() -> team.addMember(p2, 1))
          .withMessageContaining("Position");
    }

    @Test
    void rejectsAlreadyActiveMember() {
      UUID playerId = UUID.randomUUID();
      team.addMember(playerId, 1);

      assertThatIllegalStateException()
          .isThrownBy(() -> team.addMember(playerId, 2))
          .withMessageContaining("already an active member");
    }

    @Test
    void rejectsNullPlayerIdOnAdd() {
      assertThatNullPointerException().isThrownBy(() -> team.addMember(null, 1));
    }

    @Test
    void rejectsInvalidPositionOnAdd() {
      assertThatIllegalArgumentException().isThrownBy(() -> team.addMember(UUID.randomUUID(), 0));
    }

    @Test
    void removeMemberEndsMembership() {
      UUID playerId = UUID.randomUUID();
      team.addMember(playerId, 1);

      team.removeMember(playerId);

      assertThat(team.hasActiveMember(playerId)).isFalse();
      assertThat(team.getMemberCount()).isEqualTo(1); // still in list, just ended
      assertThat(team.getActiveMemberCount()).isZero();
    }

    @Test
    void removeMemberDoesNothingForUnknownPlayer() {
      team.removeMember(UUID.randomUUID());
      assertThat(team.getMemberCount()).isZero();
    }

    @Test
    void getMemberPositionReturnsCorrectPosition() {
      UUID playerId = UUID.randomUUID();
      team.addMember(playerId, 5);

      assertThat(team.getMemberPosition(playerId)).isEqualTo(5);
    }

    @Test
    void getMemberPositionThrowsForUnknownPlayer() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> team.getMemberPosition(UUID.randomUUID()))
          .withMessageContaining("not an active member");
    }

    @Test
    void getActiveMembersExcludesEnded() {
      UUID p1 = UUID.randomUUID();
      UUID p2 = UUID.randomUUID();
      team.addMember(p1, 1);
      team.addMember(p2, 2);
      team.removeMember(p1);

      List<TeamMember> active = team.getActiveMembers();

      assertThat(active).hasSize(1);
      assertThat(active.get(0).getPlayerId()).isEqualTo(p2);
    }

    @Test
    void allowsReaddingEndedMemberAtNewPosition() {
      UUID playerId = UUID.randomUUID();
      team.addMember(playerId, 1);
      team.removeMember(playerId);

      // Should be able to re-add at a different position
      team.addMember(playerId, 3);

      assertThat(team.hasActiveMember(playerId)).isTrue();
      assertThat(team.getMemberPosition(playerId)).isEqualTo(3);
    }

    @Test
    void allowsReusingPositionAfterMemberRemoved() {
      UUID p1 = UUID.randomUUID();
      UUID p2 = UUID.randomUUID();
      team.addMember(p1, 1);
      team.removeMember(p1);

      team.addMember(p2, 1);
      assertThat(team.hasActiveMember(p2)).isTrue();
    }
  }

  @Nested
  class Mutations {

    @Test
    void renameChangesName() {
      team.rename("New Name");
      assertThat(team.getName()).isEqualTo("New Name");
    }

    @Test
    void renameTrims() {
      team.rename("  Spaced  ");
      assertThat(team.getName()).isEqualTo("Spaced");
    }

    @Test
    void renameRejectsNull() {
      assertThatIllegalArgumentException().isThrownBy(() -> team.rename(null));
    }

    @Test
    void renameRejectsBlank() {
      assertThatIllegalArgumentException().isThrownBy(() -> team.rename("  "));
    }

    @Test
    void setGameIdUpdates() {
      UUID gameId = UUID.randomUUID();
      team.setGameId(gameId);
      assertThat(team.getGameId()).isEqualTo(gameId);
    }

    @Test
    void incrementCompletedTradesIncrementsCount() {
      team.incrementCompletedTrades();
      team.incrementCompletedTrades();
      assertThat(team.getCompletedTradesCount()).isEqualTo(2);
    }
  }

  @Nested
  class Equality {

    @Test
    void equalsByIdOnly() {
      UUID id = UUID.randomUUID();
      Team t1 = Team.restore(id, "Team1", UUID.randomUUID(), 2025, null, 0, null);
      Team t2 = Team.restore(id, "Team2", UUID.randomUUID(), 2024, null, 5, null);

      assertThat(t1).isEqualTo(t2);
    }

    @Test
    void notEqualWithDifferentId() {
      Team t1 = Team.restore(UUID.randomUUID(), "Team", OWNER_ID, 2025, null, 0, null);
      Team t2 = Team.restore(UUID.randomUUID(), "Team", OWNER_ID, 2025, null, 0, null);

      assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    void hashCodeConsistentWithEquals() {
      UUID id = UUID.randomUUID();
      Team t1 = Team.restore(id, "A", OWNER_ID, 2025, null, 0, null);
      Team t2 = Team.restore(id, "B", OWNER_ID, 2024, null, 3, null);

      assertThat(t1).hasSameHashCodeAs(t2);
    }
  }

  @Nested
  class MembersImmutability {

    @Test
    void getMembersReturnsUnmodifiableList() {
      team.addMember(UUID.randomUUID(), 1);

      assertThatExceptionOfType(UnsupportedOperationException.class)
          .isThrownBy(() -> team.getMembers().add(new TeamMember(UUID.randomUUID(), 99)));
    }
  }
}
