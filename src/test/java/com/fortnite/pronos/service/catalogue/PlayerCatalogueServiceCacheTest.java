package com.fortnite.pronos.service.catalogue;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.Cacheable;

import com.fortnite.pronos.domain.game.model.PlayerRegion;

@DisplayName("PlayerCatalogueService — @Cacheable annotation verification")
class PlayerCatalogueServiceCacheTest {

  @Nested
  @DisplayName("findAll() cache annotation")
  class FindAllCache {

    @Test
    @DisplayName("findAll() is annotated with @Cacheable")
    void findAllHasCacheableAnnotation() throws Exception {
      Method m = PlayerCatalogueService.class.getMethod("findAll");
      Cacheable cacheable = m.getAnnotation(Cacheable.class);

      assertThat(cacheable).isNotNull();
    }

    @Test
    @DisplayName("findAll() uses cache name 'catalogue-all'")
    void findAllUsesCatalogueAllCacheName() throws Exception {
      Method m = PlayerCatalogueService.class.getMethod("findAll");
      Cacheable cacheable = m.getAnnotation(Cacheable.class);

      assertThat(cacheable.value()).contains("catalogue-all");
    }

    @Test
    @DisplayName("findAll() uses key 'all'")
    void findAllUsesAllKey() throws Exception {
      Method m = PlayerCatalogueService.class.getMethod("findAll");
      Cacheable cacheable = m.getAnnotation(Cacheable.class);

      assertThat(cacheable.key()).isEqualTo("'all'");
    }
  }

  @Nested
  @DisplayName("findByRegion() cache annotation")
  class FindByRegionCache {

    @Test
    @DisplayName("findByRegion() is annotated with @Cacheable")
    void findByRegionHasCacheableAnnotation() throws Exception {
      Method m = PlayerCatalogueService.class.getMethod("findByRegion", PlayerRegion.class);
      Cacheable cacheable = m.getAnnotation(Cacheable.class);

      assertThat(cacheable).isNotNull();
    }

    @Test
    @DisplayName("findByRegion() uses cache name 'catalogue-region'")
    void findByRegionUsesCatalogueRegionCacheName() throws Exception {
      Method m = PlayerCatalogueService.class.getMethod("findByRegion", PlayerRegion.class);
      Cacheable cacheable = m.getAnnotation(Cacheable.class);

      assertThat(cacheable.value()).contains("catalogue-region");
    }

    @Test
    @DisplayName("findByRegion() uses key '#region.name()'")
    void findByRegionUsesRegionNameKey() throws Exception {
      Method m = PlayerCatalogueService.class.getMethod("findByRegion", PlayerRegion.class);
      Cacheable cacheable = m.getAnnotation(Cacheable.class);

      assertThat(cacheable.key()).isEqualTo("#region.name()");
    }
  }

  @Nested
  @DisplayName("searchByNickname() — no cache (real-time search)")
  class SearchByNicknameNoCache {

    @Test
    @DisplayName("searchByNickname() is NOT annotated with @Cacheable (real-time FR-14)")
    void searchByNicknameIsNotCached() throws Exception {
      Method m = PlayerCatalogueService.class.getMethod("searchByNickname", String.class);
      Cacheable cacheable = m.getAnnotation(Cacheable.class);

      assertThat(cacheable).isNull();
    }
  }
}
