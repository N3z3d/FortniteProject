package com.fortnite.pronos.service.catalogue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.domain.player.model.Player;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;
import com.fortnite.pronos.dto.player.CataloguePlayerDto;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {
      PlayerCatalogueService.class,
      PlayerCatalogueServiceCacheIntegrationTest.TestCacheConfig.class
    })
@DisplayName("PlayerCatalogueService — cache effectiveness (Spring AOP)")
class PlayerCatalogueServiceCacheIntegrationTest {

  @Configuration
  @EnableCaching
  static class TestCacheConfig {
    @Bean
    CacheManager cacheManager() {
      return new ConcurrentMapCacheManager("catalogue-all", "catalogue-region");
    }
  }

  @MockBean private PlayerDomainRepositoryPort playerRepository;

  @Autowired private PlayerCatalogueService playerCatalogueService;

  private Player euPlayer() {
    return Player.restore(
        UUID.randomUUID(), null, "user", "NickEU", PlayerRegion.EU, "1-5", 2025, false);
  }

  @BeforeEach
  void clearCaches(@Autowired CacheManager cacheManager) {
    cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
  }

  @Test
  @DisplayName("findAll() calls repository only once across two invocations (cache hit on second)")
  void findAllCallsRepositoryOnlyOnce() {
    when(playerRepository.findAll()).thenReturn(List.of(euPlayer()));

    List<CataloguePlayerDto> first = playerCatalogueService.findAll();
    List<CataloguePlayerDto> second = playerCatalogueService.findAll();

    assertThat(first).isNotEmpty();
    assertThat(second).isEqualTo(first);
    verify(playerRepository, times(1)).findAll();
  }

  @Test
  @DisplayName("findByRegion() calls repository only once per region across two invocations")
  void findByRegionCallsRepositoryOnlyOnce() {
    when(playerRepository.findByRegion(PlayerRegion.EU)).thenReturn(List.of(euPlayer()));

    List<CataloguePlayerDto> first = playerCatalogueService.findByRegion(PlayerRegion.EU);
    List<CataloguePlayerDto> second = playerCatalogueService.findByRegion(PlayerRegion.EU);

    assertThat(first).isNotEmpty();
    assertThat(second).isEqualTo(first);
    verify(playerRepository, times(1)).findByRegion(PlayerRegion.EU);
  }

  @Test
  @DisplayName("Different regions cached independently — EU cache miss does not affect NAW")
  void differentRegionsCachedIndependently() {
    when(playerRepository.findByRegion(PlayerRegion.EU)).thenReturn(List.of(euPlayer()));
    when(playerRepository.findByRegion(PlayerRegion.NAW)).thenReturn(List.of());

    playerCatalogueService.findByRegion(PlayerRegion.EU);
    playerCatalogueService.findByRegion(PlayerRegion.NAW);
    playerCatalogueService.findByRegion(PlayerRegion.EU);
    playerCatalogueService.findByRegion(PlayerRegion.NAW);

    verify(playerRepository, times(1)).findByRegion(PlayerRegion.EU);
    verify(playerRepository, times(1)).findByRegion(PlayerRegion.NAW);
  }

  @Test
  @DisplayName("searchByNickname() calls repository on every invocation (not cached)")
  void searchByNicknameCallsRepositoryEveryTime() {
    when(playerRepository.findAll()).thenReturn(List.of(euPlayer()));

    playerCatalogueService.searchByNickname("nick");
    playerCatalogueService.searchByNickname("nick");

    verify(playerRepository, times(2)).findAll();
  }
}
