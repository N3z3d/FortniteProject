package com.fortnite.pronos.service.catalogue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.domain.player.model.Player;
import com.fortnite.pronos.domain.player.model.RankSnapshot;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.RankSnapshotRepositoryPort;
import com.fortnite.pronos.dto.player.PlayerDetailDto;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerDetailService — player profile detail")
class PlayerDetailServiceTest {

  @Mock private PlayerDomainRepositoryPort playerRepository;
  @Mock private RankSnapshotRepositoryPort snapshotRepository;

  private PlayerDetailService service;

  private static final UUID PLAYER_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service = new PlayerDetailService(playerRepository, snapshotRepository);
  }

  private Player euPlayer() {
    return Player.restore(
        PLAYER_ID, null, "user1", "NickEU", PlayerRegion.EU, "1-100", 2025, false);
  }

  private RankSnapshot snapshot(String region, int prValue, LocalDate date) {
    return RankSnapshot.restore(UUID.randomUUID(), PLAYER_ID, region, 1, prValue, date);
  }

  @Nested
  @DisplayName("getPlayerDetail()")
  class GetPlayerDetail {

    @Test
    @DisplayName("Returns profile with PR per region when snapshots exist")
    void returnsPrByRegionFromSnapshots() {
      Player player = euPlayer();
      when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));
      when(snapshotRepository.findByPlayerRecent(
              eq(PLAYER_ID), eq(PlayerDetailService.MAX_SNAPSHOT_DAYS)))
          .thenReturn(
              List.of(
                  snapshot("EU", 1000, LocalDate.of(2025, 1, 1)),
                  snapshot("EU", 1200, LocalDate.of(2025, 1, 10)),
                  snapshot("NAW", 800, LocalDate.of(2025, 1, 5))));

      Optional<PlayerDetailDto> result = service.getPlayerDetail(PLAYER_ID);

      assertThat(result).isPresent();
      PlayerDetailDto dto = result.get();
      assertThat(dto.prByRegion()).containsEntry("EU", 1200); // latest EU snapshot wins
      assertThat(dto.prByRegion()).containsEntry("NAW", 800);
      assertThat(dto.lastSnapshotDate()).isEqualTo(LocalDate.of(2025, 1, 10));
    }

    @Test
    @DisplayName("Returns profile with empty prByRegion when no snapshots")
    void returnsEmptyPrByRegionWhenNoSnapshots() {
      Player player = euPlayer();
      when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));
      when(snapshotRepository.findByPlayerRecent(
              eq(PLAYER_ID), eq(PlayerDetailService.MAX_SNAPSHOT_DAYS)))
          .thenReturn(Collections.emptyList());

      Optional<PlayerDetailDto> result = service.getPlayerDetail(PLAYER_ID);

      assertThat(result).isPresent();
      assertThat(result.get().prByRegion()).isEmpty();
      assertThat(result.get().lastSnapshotDate()).isNull();
    }

    @Test
    @DisplayName("Returns empty when player not found")
    void returnsEmptyWhenPlayerNotFound() {
      when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.empty());

      Optional<PlayerDetailDto> result = service.getPlayerDetail(PLAYER_ID);

      assertThat(result).isEmpty();
      verifyNoInteractions(snapshotRepository);
    }

    @Test
    @DisplayName("Maps player domain fields to DTO correctly")
    void mapsPlayerFieldsCorrectly() {
      Player player =
          Player.restore(
              PLAYER_ID, null, "username1", "NickEU", PlayerRegion.EU, "1-100", 2025, true);
      when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));
      when(snapshotRepository.findByPlayerRecent(any(), anyInt()))
          .thenReturn(Collections.emptyList());

      PlayerDetailDto dto = service.getPlayerDetail(PLAYER_ID).orElseThrow();

      assertThat(dto.id()).isEqualTo(PLAYER_ID);
      assertThat(dto.nickname()).isEqualTo("NickEU");
      assertThat(dto.region()).isEqualTo("EU");
      assertThat(dto.tranche()).isEqualTo("1-100");
      assertThat(dto.locked()).isTrue();
    }

    @Test
    @DisplayName("Keeps latest PR value when multiple snapshots exist for same region")
    void keepsLatestPrValueForSameRegion() {
      Player player = euPlayer();
      when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));
      // ASC order by date — last entry is the newest
      when(snapshotRepository.findByPlayerRecent(
              eq(PLAYER_ID), eq(PlayerDetailService.MAX_SNAPSHOT_DAYS)))
          .thenReturn(
              List.of(
                  snapshot("EU", 500, LocalDate.of(2025, 1, 1)),
                  snapshot("EU", 750, LocalDate.of(2025, 1, 15)),
                  snapshot("EU", 900, LocalDate.of(2025, 1, 20))));

      PlayerDetailDto dto = service.getPlayerDetail(PLAYER_ID).orElseThrow();

      assertThat(dto.prByRegion().get("EU")).isEqualTo(900);
    }
  }
}
