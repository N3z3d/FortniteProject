package com.fortnite.pronos.service.catalogue;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.game.model.PlayerRegion;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerCatalogueWarmupService — cache warmup")
class PlayerCatalogueWarmupServiceTest {

  @Mock private PlayerCatalogueService playerCatalogueService;

  private PlayerCatalogueWarmupService warmupService;

  private static final long KNOWN_REGION_COUNT =
      Arrays.stream(PlayerRegion.values()).filter(r -> r != PlayerRegion.UNKNOWN).count();

  @BeforeEach
  void setUp() {
    warmupService = new PlayerCatalogueWarmupService(playerCatalogueService);
  }

  @Nested
  @DisplayName("warmup()")
  class Warmup {

    @Test
    @DisplayName("Calls findAll() to warm the all-regions cache entry")
    void callsFindAllOnWarmup() {
      warmupService.warmup();

      verify(playerCatalogueService).findAll();
    }

    @Test
    @DisplayName("Calls findByRegion() for every known region, skipping UNKNOWN")
    void callsFindByRegionForEveryKnownRegion() {
      warmupService.warmup();

      for (PlayerRegion region : PlayerRegion.values()) {
        if (region == PlayerRegion.UNKNOWN) {
          verify(playerCatalogueService, never()).findByRegion(PlayerRegion.UNKNOWN);
        } else {
          verify(playerCatalogueService).findByRegion(region);
        }
      }
    }

    @Test
    @DisplayName("Calls findByRegion exactly once per known region (no duplicates, no UNKNOWN)")
    void callsFindByRegionExactlyOncePerKnownRegion() {
      warmupService.warmup();

      verify(playerCatalogueService, times((int) KNOWN_REGION_COUNT))
          .findByRegion(any(PlayerRegion.class));
    }

    @Test
    @DisplayName("Warmup completes without exception even when catalogue is empty")
    void completesWithoutExceptionWhenCatalogueEmpty() {
      when(playerCatalogueService.findAll()).thenReturn(List.of());
      when(playerCatalogueService.findByRegion(any(PlayerRegion.class))).thenReturn(List.of());

      assertThatCode(() -> warmupService.warmup()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Warmup swallows exception from service and does not propagate it")
    void swallowsExceptionFromService() {
      when(playerCatalogueService.findAll()).thenThrow(new RuntimeException("DB unavailable"));

      assertThatCode(() -> warmupService.warmup()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Warmup can be called multiple times (idempotent)")
    void isIdempotent() {
      warmupService.warmup();
      warmupService.warmup();

      verify(playerCatalogueService, times(2)).findAll();
    }
  }
}
