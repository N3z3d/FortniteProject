package com.fortnite.pronos.domain.player.model;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fortnite.pronos.domain.game.model.PlayerRegion;

class PlayerDomainModelTest {

  private Player player;

  @BeforeEach
  void setUp() {
    player = new Player("username1", "Nickname1", PlayerRegion.EU, "1-7");
  }

  @Nested
  class Creation {

    @Test
    void createsPlayerWithValidParameters() {
      assertThat(player.getUsername()).isEqualTo("username1");
      assertThat(player.getNickname()).isEqualTo("Nickname1");
      assertThat(player.getRegion()).isEqualTo(PlayerRegion.EU);
      assertThat(player.getTranche()).isEqualTo("1-7");
      assertThat(player.getId()).isNotNull();
      assertThat(player.getCurrentSeason()).isEqualTo(2025);
      assertThat(player.isLocked()).isFalse();
      assertThat(player.getFortniteId()).isNull();
    }

    @Test
    void trimsUsernameAndNickname() {
      Player p = new Player("  spaced  ", "  nick  ", PlayerRegion.NA, "1-10");
      assertThat(p.getUsername()).isEqualTo("spaced");
      assertThat(p.getNickname()).isEqualTo("nick");
      assertThat(p.getTranche()).isEqualTo("1-10");
    }

    @Test
    void rejectsNullUsername() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new Player(null, "nick", PlayerRegion.EU, "1-7"))
          .withMessageContaining("Username");
    }

    @Test
    void rejectsBlankUsername() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new Player("   ", "nick", PlayerRegion.EU, "1-7"))
          .withMessageContaining("Username");
    }

    @Test
    void rejectsNullNickname() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new Player("user", null, PlayerRegion.EU, "1-7"))
          .withMessageContaining("Nickname");
    }

    @Test
    void rejectsBlankNickname() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new Player("user", "  ", PlayerRegion.EU, "1-7"))
          .withMessageContaining("Nickname");
    }

    @Test
    void rejectsNullRegion() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new Player("user", "nick", null, "1-7"))
          .withMessageContaining("Region");
    }

    @Test
    void rejectsNullTranche() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new Player("user", "nick", PlayerRegion.EU, null))
          .withMessageContaining("Tranche");
    }

    @Test
    void rejectsBlankTranche() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new Player("user", "nick", PlayerRegion.EU, "  "))
          .withMessageContaining("Tranche");
    }
  }

  @Nested
  class Restore {

    @Test
    void restoresFullState() {
      UUID id = UUID.randomUUID();
      Player restored =
          Player.restore(id, "FN123", "user", "nick", PlayerRegion.NAW, "1-10", 2024, true);

      assertThat(restored.getId()).isEqualTo(id);
      assertThat(restored.getFortniteId()).isEqualTo("FN123");
      assertThat(restored.getUsername()).isEqualTo("user");
      assertThat(restored.getNickname()).isEqualTo("nick");
      assertThat(restored.getRegion()).isEqualTo(PlayerRegion.NAW);
      assertThat(restored.getTranche()).isEqualTo("1-10");
      assertThat(restored.getCurrentSeason()).isEqualTo(2024);
      assertThat(restored.isLocked()).isTrue();
    }

    @Test
    void restoresWithNullFortniteId() {
      UUID id = UUID.randomUUID();
      Player restored =
          Player.restore(id, null, "user", "nick", PlayerRegion.EU, "1-7", 2025, false);

      assertThat(restored.getFortniteId()).isNull();
    }

    @Test
    void restoresWithCustomSeason() {
      UUID id = UUID.randomUUID();
      Player restored =
          Player.restore(id, "FN1", "user", "nick", PlayerRegion.EU, "1-7", 2023, false);

      assertThat(restored.getCurrentSeason()).isEqualTo(2023);
    }

    @Test
    void restoresLockedPlayer() {
      UUID id = UUID.randomUUID();
      Player restored =
          Player.restore(id, null, "user", "nick", PlayerRegion.ASIA, "NOUVEAU", 2025, true);

      assertThat(restored.isLocked()).isTrue();
    }
  }

  @Nested
  class BusinessBehavior {

    @Test
    void getNameReturnsNicknameWhenPresent() {
      assertThat(player.getName()).isEqualTo("Nickname1");
    }

    @Test
    void getNameReturnsUsernameWhenNicknameNull() {
      Player restored =
          Player.restore(
              UUID.randomUUID(), null, "user", null, PlayerRegion.EU, "1-7", 2025, false);

      assertThat(restored.getName()).isEqualTo("user");
    }

    @Test
    void lockSetsLockedTrue() {
      player.lock();
      assertThat(player.isLocked()).isTrue();
    }

    @Test
    void unlockSetsLockedFalse() {
      player.lock();
      player.unlock();
      assertThat(player.isLocked()).isFalse();
    }

    @Test
    void getRegionNameReturnsName() {
      assertThat(player.getRegionName()).isEqualTo("EU");
    }

    @Test
    void getRegionNameReturnsNullWhenRegionNull() {
      Player restored =
          Player.restore(UUID.randomUUID(), null, "user", "nick", null, "1-7", 2025, false);

      assertThat(restored.getRegionName()).isNull();
    }
  }

  @Nested
  class Equality {

    @Test
    void equalsByIdOnly() {
      UUID id = UUID.randomUUID();
      Player p1 = Player.restore(id, null, "user1", "nick1", PlayerRegion.EU, "1-7", 2025, false);
      Player p2 = Player.restore(id, "FN1", "user2", "nick2", PlayerRegion.NAW, "1-10", 2024, true);

      assertThat(p1).isEqualTo(p2);
    }

    @Test
    void notEqualWithDifferentId() {
      Player p1 =
          Player.restore(
              UUID.randomUUID(), null, "user", "nick", PlayerRegion.EU, "1-7", 2025, false);
      Player p2 =
          Player.restore(
              UUID.randomUUID(), null, "user", "nick", PlayerRegion.EU, "1-7", 2025, false);

      assertThat(p1).isNotEqualTo(p2);
    }

    @Test
    void hashCodeConsistentWithEquals() {
      UUID id = UUID.randomUUID();
      Player p1 = Player.restore(id, null, "user1", "nick1", PlayerRegion.EU, "1-7", 2025, false);
      Player p2 = Player.restore(id, "FN1", "user2", "nick2", PlayerRegion.NAW, "1-10", 2024, true);

      assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
    }
  }

  @Nested
  class Mutations {

    @Test
    void updateRegionChangesRegion() {
      player.updateRegion(PlayerRegion.NAW);
      assertThat(player.getRegion()).isEqualTo(PlayerRegion.NAW);
    }

    @Test
    void updateRegionRejectsNull() {
      assertThatNullPointerException()
          .isThrownBy(() -> player.updateRegion(null))
          .withMessageContaining("Region");
    }

    @Test
    void updateTrancheChanges() {
      player.updateTranche("1-10");
      assertThat(player.getTranche()).isEqualTo("1-10");
    }

    @Test
    void updateTrancheRejectsNull() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> player.updateTranche(null))
          .withMessageContaining("Tranche");
    }

    @Test
    void updateTrancheRejectsBlank() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> player.updateTranche("   "))
          .withMessageContaining("Tranche");
    }

    @Test
    void updateNicknameChanges() {
      player.updateNickname("NewNick");
      assertThat(player.getNickname()).isEqualTo("NewNick");
    }

    @Test
    void updateNicknameTrims() {
      player.updateNickname("  Spaced  ");
      assertThat(player.getNickname()).isEqualTo("Spaced");
    }

    @Test
    void updateNicknameRejectsNull() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> player.updateNickname(null))
          .withMessageContaining("Nickname");
    }

    @Test
    void updateNicknameRejectsBlank() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> player.updateNickname("  "))
          .withMessageContaining("Nickname");
    }

    @Test
    void setFortniteIdUpdatesValue() {
      player.setFortniteId("FN-456");
      assertThat(player.getFortniteId()).isEqualTo("FN-456");
    }

    @Test
    void setFortniteIdAllowsNull() {
      player.setFortniteId("FN-123");
      player.setFortniteId(null);
      assertThat(player.getFortniteId()).isNull();
    }
  }
}
