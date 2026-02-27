package com.fortnite.pronos.service.catalogue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.domain.player.model.Player;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;
import com.fortnite.pronos.dto.player.CataloguePlayerDto;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerCatalogueService — catalogue filtering")
class PlayerCatalogueServiceTest {

  @Mock private PlayerDomainRepositoryPort playerRepository;

  private PlayerCatalogueService service;

  @BeforeEach
  void setUp() {
    service = new PlayerCatalogueService(playerRepository);
  }

  private Player playerWithRegion(PlayerRegion region) {
    return Player.restore(
        UUID.randomUUID(), null, "user-" + region, "Nick" + region, region, "1-5", 2025, false);
  }

  @Nested
  @DisplayName("findByRegion()")
  class FindByRegion {

    @Test
    @DisplayName("Returns only players of the requested region")
    void returnsPlayersOfRequestedRegion() {
      Player eu1 = playerWithRegion(PlayerRegion.EU);
      Player eu2 = playerWithRegion(PlayerRegion.EU);
      when(playerRepository.findByRegion(PlayerRegion.EU)).thenReturn(List.of(eu1, eu2));

      List<CataloguePlayerDto> result = service.findByRegion(PlayerRegion.EU);

      assertThat(result).hasSize(2);
      assertThat(result).allMatch(dto -> "EU".equals(dto.region()));
    }

    @Test
    @DisplayName("Returns empty list when no players in region")
    void returnsEmptyListWhenNoPlayersInRegion() {
      when(playerRepository.findByRegion(PlayerRegion.OCE)).thenReturn(Collections.emptyList());

      List<CataloguePlayerDto> result = service.findByRegion(PlayerRegion.OCE);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Caps result at MAX_CATALOGUE_SIZE when region has more than 1000 players")
    void capsResultAtMaxCatalogueSize() {
      List<Player> manyPlayers =
          IntStream.range(0, 1200).mapToObj(i -> playerWithRegion(PlayerRegion.EU)).toList();
      when(playerRepository.findByRegion(PlayerRegion.EU)).thenReturn(manyPlayers);

      List<CataloguePlayerDto> result = service.findByRegion(PlayerRegion.EU);

      assertThat(result).hasSize(PlayerCatalogueService.MAX_CATALOGUE_SIZE);
    }

    @Test
    @DisplayName("Maps domain Player fields to CataloguePlayerDto correctly")
    void mapsPlayerFieldsCorrectly() {
      Player player =
          Player.restore(
              UUID.randomUUID(), null, "username1", "NickEU", PlayerRegion.EU, "1-100", 2025, true);
      when(playerRepository.findByRegion(PlayerRegion.EU)).thenReturn(List.of(player));

      List<CataloguePlayerDto> result = service.findByRegion(PlayerRegion.EU);

      CataloguePlayerDto dto = result.getFirst();
      assertThat(dto.id()).isEqualTo(player.getId());
      assertThat(dto.nickname()).isEqualTo("NickEU");
      assertThat(dto.region()).isEqualTo("EU");
      assertThat(dto.tranche()).isEqualTo("1-100");
      assertThat(dto.locked()).isTrue();
    }
  }

  @Nested
  @DisplayName("findAll()")
  class FindAll {

    @Test
    @DisplayName("Returns players from all regions")
    void returnsAllPlayers() {
      Player eu = playerWithRegion(PlayerRegion.EU);
      Player naw = playerWithRegion(PlayerRegion.NAW);
      when(playerRepository.findAll()).thenReturn(List.of(eu, naw));

      List<CataloguePlayerDto> result = service.findAll();

      assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Caps result at MAX_CATALOGUE_SIZE when total exceeds 1000")
    void capsResultAtMaxCatalogueSizeForAll() {
      List<Player> manyPlayers =
          IntStream.range(0, 1500).mapToObj(i -> playerWithRegion(PlayerRegion.EU)).toList();
      when(playerRepository.findAll()).thenReturn(manyPlayers);

      List<CataloguePlayerDto> result = service.findAll();

      assertThat(result).hasSize(PlayerCatalogueService.MAX_CATALOGUE_SIZE);
    }

    @Test
    @DisplayName("Returns empty list when catalogue is empty")
    void returnsEmptyListWhenCatalogueEmpty() {
      when(playerRepository.findAll()).thenReturn(Collections.emptyList());

      List<CataloguePlayerDto> result = service.findAll();

      assertThat(result).isEmpty();
    }
  }
}
