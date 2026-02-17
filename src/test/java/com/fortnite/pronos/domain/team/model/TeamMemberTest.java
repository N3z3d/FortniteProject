package com.fortnite.pronos.domain.team.model;

import static org.assertj.core.api.Assertions.*;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TeamMemberTest {

  private static final UUID PLAYER_ID = UUID.randomUUID();

  @Nested
  class Creation {

    @Test
    void createsActiveMember() {
      TeamMember member = new TeamMember(PLAYER_ID, 3);

      assertThat(member.getPlayerId()).isEqualTo(PLAYER_ID);
      assertThat(member.getPosition()).isEqualTo(3);
      assertThat(member.isActive()).isTrue();
      assertThat(member.getUntil()).isNull();
    }

    @Test
    void rejectsNullPlayerId() {
      assertThatNullPointerException()
          .isThrownBy(() -> new TeamMember(null, 1))
          .withMessageContaining("Player ID");
    }

    @Test
    void rejectsZeroPosition() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new TeamMember(PLAYER_ID, 0))
          .withMessageContaining("Position");
    }

    @Test
    void rejectsNegativePosition() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new TeamMember(PLAYER_ID, -1))
          .withMessageContaining("Position");
    }
  }

  @Nested
  class Restore {

    @Test
    void restoresActiveMember() {
      TeamMember member = TeamMember.restore(PLAYER_ID, 2, null);

      assertThat(member.isActive()).isTrue();
      assertThat(member.getPosition()).isEqualTo(2);
    }

    @Test
    void restoresEndedMember() {
      OffsetDateTime endTime = OffsetDateTime.now().minusDays(1);
      TeamMember member = TeamMember.restore(PLAYER_ID, 5, endTime);

      assertThat(member.isActive()).isFalse();
      assertThat(member.getUntil()).isEqualTo(endTime);
    }
  }

  @Nested
  class BusinessBehavior {

    @Test
    void endMembershipSetsUntil() {
      TeamMember member = new TeamMember(PLAYER_ID, 1);

      member.endMembership();

      assertThat(member.isActive()).isFalse();
      assertThat(member.getUntil()).isNotNull();
    }

    @Test
    void isActiveReturnsTrueWhenUntilNull() {
      TeamMember member = new TeamMember(PLAYER_ID, 1);
      assertThat(member.isActive()).isTrue();
    }

    @Test
    void isActiveReturnsFalseAfterEndMembership() {
      TeamMember member = new TeamMember(PLAYER_ID, 1);
      member.endMembership();
      assertThat(member.isActive()).isFalse();
    }
  }

  @Nested
  class Equality {

    @Test
    void equalsByPlayerId() {
      TeamMember m1 = new TeamMember(PLAYER_ID, 1);
      TeamMember m2 = new TeamMember(PLAYER_ID, 5);

      assertThat(m1).isEqualTo(m2);
    }

    @Test
    void notEqualWithDifferentPlayerId() {
      TeamMember m1 = new TeamMember(UUID.randomUUID(), 1);
      TeamMember m2 = new TeamMember(UUID.randomUUID(), 1);

      assertThat(m1).isNotEqualTo(m2);
    }

    @Test
    void hashCodeConsistentWithEquals() {
      TeamMember m1 = new TeamMember(PLAYER_ID, 1);
      TeamMember m2 = new TeamMember(PLAYER_ID, 5);

      assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
    }
  }
}
