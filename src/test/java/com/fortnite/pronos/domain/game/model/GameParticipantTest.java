package com.fortnite.pronos.domain.game.model;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameParticipantTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private GameParticipant participant;

  @BeforeEach
  void setUp() {
    participant = new GameParticipant(USER_ID, "testuser", false);
  }

  @Test
  void createsParticipant() {
    assertThat(participant.getUserId()).isEqualTo(USER_ID);
    assertThat(participant.getUsername()).isEqualTo("testuser");
    assertThat(participant.isCreator()).isFalse();
    assertThat(participant.getJoinedAt()).isNotNull();
    assertThat(participant.getSelectedPlayerIds()).isEmpty();
  }

  @Test
  void rejectsNullUserId() {
    assertThatNullPointerException().isThrownBy(() -> new GameParticipant(null, "test", false));
  }

  @Test
  void addsSelectedPlayer() {
    UUID playerId = UUID.randomUUID();
    participant.addSelectedPlayer(playerId);
    assertThat(participant.getSelectedPlayersCount()).isEqualTo(1);
    assertThat(participant.hasSelectedPlayer(playerId)).isTrue();
  }

  @Test
  void doesNotAddDuplicatePlayer() {
    UUID playerId = UUID.randomUUID();
    participant.addSelectedPlayer(playerId);
    participant.addSelectedPlayer(playerId);
    assertThat(participant.getSelectedPlayersCount()).isEqualTo(1);
  }

  @Test
  void removesSelectedPlayer() {
    UUID playerId = UUID.randomUUID();
    participant.addSelectedPlayer(playerId);
    participant.removeSelectedPlayer(playerId);
    assertThat(participant.hasSelectedPlayer(playerId)).isFalse();
  }

  @Test
  void hasNotTimedOutByDefault() {
    assertThat(participant.hasTimedOut()).isFalse();
  }

  @Test
  void selectedPlayerIdsAreUnmodifiable() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> participant.getSelectedPlayerIds().add(UUID.randomUUID()));
  }

  @Test
  void equalityByUserId() {
    GameParticipant other = new GameParticipant(USER_ID, "different", true);
    assertThat(participant).isEqualTo(other);
    assertThat(participant.hashCode()).isEqualTo(other.hashCode());
  }

  @Test
  void restoresFromPersistence() {
    UUID id = UUID.randomUUID();
    UUID playerId = UUID.randomUUID();
    LocalDateTime joined = LocalDateTime.now().minusDays(1);
    LocalDateTime lastSel = LocalDateTime.now().minusHours(1);

    GameParticipant restored =
        GameParticipant.restore(id, USER_ID, "user", 3, joined, lastSel, true, List.of(playerId));

    assertThat(restored.getId()).isEqualTo(id);
    assertThat(restored.getUserId()).isEqualTo(USER_ID);
    assertThat(restored.getDraftOrder()).isEqualTo(3);
    assertThat(restored.getJoinedAt()).isEqualTo(joined);
    assertThat(restored.getLastSelectionTime()).isEqualTo(lastSel);
    assertThat(restored.isCreator()).isTrue();
    assertThat(restored.getSelectedPlayerIds()).containsExactly(playerId);
  }

  @Test
  void restoresWithNullPlayerIds() {
    GameParticipant restored =
        GameParticipant.restore(UUID.randomUUID(), USER_ID, "u", null, null, null, false, null);
    assertThat(restored.getSelectedPlayerIds()).isEmpty();
  }

  @Test
  void setsDraftOrder() {
    participant.setDraftOrder(5);
    assertThat(participant.getDraftOrder()).isEqualTo(5);
  }
}
