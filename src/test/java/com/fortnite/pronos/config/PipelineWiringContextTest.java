package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;

import com.fortnite.pronos.adapter.out.scraping.FortniteTrackerScrapingAdapter;
import com.fortnite.pronos.service.ingestion.PrIngestionOrchestrationService;
import com.fortnite.pronos.service.ingestion.PrRegionCsvSourcePort;

/**
 * Verifies the pipeline wiring configuration via reflection — no Spring context needed.
 *
 * <p>Checks that: - PrIngestionOrchestrationService is gated by @ConditionalOnProperty -
 * FortniteTrackerScrapingAdapter implements PrRegionCsvSourcePort - The @Scheduled cron method
 * exists and references the correct property
 */
@DisplayName("Pipeline Wiring — Configuration Checks")
class PipelineWiringContextTest {

  @Nested
  @DisplayName("PrIngestionOrchestrationService")
  class OrchestrationServiceTests {

    @Test
    @DisplayName("is gated by @ConditionalOnProperty(ingestion.pr.scheduled.enabled=true)")
    void orchestrationService_hasCorrectConditionalOnProperty() {
      ConditionalOnProperty annotation =
          PrIngestionOrchestrationService.class.getAnnotation(ConditionalOnProperty.class);

      assertThat(annotation)
          .as("PrIngestionOrchestrationService must have @ConditionalOnProperty")
          .isNotNull();
      assertThat(annotation.name()).containsExactly("ingestion.pr.scheduled.enabled");
      assertThat(annotation.havingValue()).isEqualTo("true");
      assertThat(annotation.matchIfMissing()).isFalse();
    }

    @Test
    @DisplayName("has @Scheduled method referencing ingestion.pr.scheduled.cron")
    void orchestrationService_hasScheduledMethod() {
      boolean hasScheduledMethod =
          Arrays.stream(PrIngestionOrchestrationService.class.getDeclaredMethods())
              .anyMatch(
                  method -> {
                    Scheduled scheduled = method.getAnnotation(Scheduled.class);
                    return scheduled != null
                        && scheduled.cron().contains("ingestion.pr.scheduled.cron");
                  });

      assertThat(hasScheduledMethod)
          .as(
              "PrIngestionOrchestrationService must have a @Scheduled method with cron"
                  + " referencing ingestion.pr.scheduled.cron")
          .isTrue();
    }

    @Test
    @DisplayName("exposes runAllRegions() as public method (callable from controller)")
    void orchestrationService_exposesRunAllRegionsPublicMethod() throws NoSuchMethodException {
      Method runAllRegions = PrIngestionOrchestrationService.class.getMethod("runAllRegions");

      assertThat(runAllRegions).isNotNull();
      assertThat(java.lang.reflect.Modifier.isPublic(runAllRegions.getModifiers())).isTrue();
    }
  }

  @Nested
  @DisplayName("FortniteTrackerScrapingAdapter")
  class ScrapingAdapterTests {

    @Test
    @DisplayName("implements PrRegionCsvSourcePort (hexagonal port contract)")
    void scrapingAdapter_implementsCsvSourcePort() {
      assertThat(PrRegionCsvSourcePort.class)
          .as("FortniteTrackerScrapingAdapter must implement PrRegionCsvSourcePort")
          .isAssignableFrom(FortniteTrackerScrapingAdapter.class);
    }
  }
}
