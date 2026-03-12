package com.fortnite.pronos.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DraftPick legacy column synchronization")
class DraftPickModelTest {

  @Test
  @DisplayName("constructor syncs participant label and region slot")
  void constructorSyncsParticipantLabelAndRegionSlot() {
    Draft draft = new Draft();
    GameParticipant participant = buildParticipant("marcel");
    Player player = buildPlayer(Player.Region.EU);

    DraftPick pick = new DraftPick(draft, participant, player, 1, 1);

    assertThat(pick.getParticipantLabel()).isEqualTo("marcel");
    assertThat(pick.getRegionSlot()).isEqualTo(DraftPick.DraftRegionSlot.EU);
  }

  @Test
  @DisplayName("participant label falls back to user id when username is missing")
  void participantLabelFallsBackToUserIdWhenUsernameIsMissing() {
    DraftPick pick = new DraftPick();
    GameParticipant participant = new GameParticipant();
    User user = new User();
    UUID userId = UUID.randomUUID();
    user.setId(userId);
    participant.setUser(user);

    pick.setParticipant(participant);

    assertThat(pick.getParticipantLabel()).isEqualTo(userId.toString());
  }

  @Test
  @DisplayName("setPlayer updates region slot from player region")
  void setPlayerUpdatesRegionSlotFromPlayerRegion() {
    DraftPick pick = new DraftPick();

    pick.setPlayer(buildPlayer(Player.Region.NAW));

    assertThat(pick.getRegionSlot()).isEqualTo(DraftPick.DraftRegionSlot.NAW);
  }

  private GameParticipant buildParticipant(String username) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername(username);

    GameParticipant participant = new GameParticipant();
    participant.setId(UUID.randomUUID());
    participant.setUser(user);
    return participant;
  }

  private Player buildPlayer(Player.Region region) {
    Player player = new Player();
    player.setId(UUID.randomUUID());
    player.setRegion(region);
    player.setUsername("player");
    player.setNickname("player");
    player.setTranche("1");
    return player;
  }
}
