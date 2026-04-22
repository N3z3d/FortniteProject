package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.fortnite.pronos.adapter.out.resolution.FortniteApiResolutionAdapter;
import com.fortnite.pronos.adapter.out.resolution.StubResolutionAdapter;
import com.fortnite.pronos.domain.port.out.FortniteApiPort;
import com.fortnite.pronos.domain.port.out.ResolutionPort;

@DisplayName("Resolution adapter configuration")
class ResolutionAdapterConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withUserConfiguration(
              ResolutionAdapterConfiguration.class,
              StubResolutionAdapter.class,
              FortniteApiResolutionAdapter.class)
          .withBean(FortniteApiPort.class, () -> mock(FortniteApiPort.class));

  @Nested
  @DisplayName("valid adapter values")
  class ValidAdapterValues {

    @Test
    @DisplayName("uses stub when resolution.adapter is absent")
    void usesStubWhenAdapterPropertyIsAbsent() {
      contextRunner.run(
          context -> {
            assertThat(context).hasSingleBean(ResolutionPort.class);
            assertThat(context).hasSingleBean(StubResolutionAdapter.class);
            assertThat(context).doesNotHaveBean(FortniteApiResolutionAdapter.class);
            assertThat(context.getBean(ResolutionPort.class).adapterName()).isEqualTo("stub");
          });
    }

    @Test
    @DisplayName("uses stub when resolution.adapter=stub")
    void usesStubWhenAdapterIsStub() {
      contextRunner
          .withPropertyValues("resolution.adapter=stub")
          .run(
              context -> {
                assertThat(context).hasSingleBean(ResolutionPort.class);
                assertThat(context).hasSingleBean(StubResolutionAdapter.class);
                assertThat(context).doesNotHaveBean(FortniteApiResolutionAdapter.class);
                assertThat(context.getBean(ResolutionPort.class).adapterName()).isEqualTo("stub");
              });
    }

    @Test
    @DisplayName("uses Fortnite API adapter when resolution.adapter=fortnite-api")
    void usesFortniteApiWhenAdapterIsFortniteApi() {
      contextRunner
          .withPropertyValues("resolution.adapter=fortnite-api")
          .run(
              context -> {
                assertThat(context).hasSingleBean(ResolutionPort.class);
                assertThat(context).hasSingleBean(FortniteApiResolutionAdapter.class);
                assertThat(context).doesNotHaveBean(StubResolutionAdapter.class);
                assertThat(context.getBean(ResolutionPort.class).adapterName())
                    .isEqualTo("fortnite-api");
              });
    }
  }

  @Nested
  @DisplayName("invalid adapter values")
  class InvalidAdapterValues {

    @Test
    @DisplayName("fails fast when resolution.adapter is empty")
    void failsFastWhenAdapterIsEmpty() {
      assertInvalidAdapterFailsFast("");
    }

    @Test
    @DisplayName("fails fast when resolution.adapter is whitespace")
    void failsFastWhenAdapterIsWhitespace() {
      assertInvalidAdapterFailsFast("   ");
    }

    @Test
    @DisplayName("fails fast when resolution.adapter is unknown")
    void failsFastWhenAdapterIsUnknown() {
      assertInvalidAdapterFailsFast("bogus");
    }

    private void assertInvalidAdapterFailsFast(String invalidValue) {
      contextRunner
          .withPropertyValues("resolution.adapter=" + invalidValue)
          .run(
              context -> {
                assertThat(context).hasFailed();
                assertInvalidStartupFailure(context.getStartupFailure(), invalidValue);
              });
    }
  }

  private static void assertInvalidStartupFailure(Throwable startupFailure, String invalidValue) {
    assertThat(startupFailure)
        .hasMessageContaining("resolution.adapter")
        .hasMessageContaining("value " + expectedInvalidValue(invalidValue))
        .hasMessageContaining("stub")
        .hasMessageContaining("fortnite-api")
        .hasMessageContaining("RESOLUTION_ADAPTER");
    assertThat(hasThrowableOfType(startupFailure, IllegalStateException.class)).isTrue();
    assertThat(
            hasThrowableOfType(
                startupFailure,
                org.springframework.beans.factory.NoSuchBeanDefinitionException.class))
        .isFalse();
    assertThat(
            hasThrowableOfType(
                startupFailure,
                org.springframework.beans.factory.UnsatisfiedDependencyException.class))
        .isFalse();
  }

  private static String expectedInvalidValue(String invalidValue) {
    return invalidValue.isBlank() ? "''" : "'" + invalidValue + "'";
  }

  private static boolean hasThrowableOfType(Throwable failure, Class<? extends Throwable> type) {
    Throwable current = failure;
    while (current != null) {
      if (type.isInstance(current)) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }
}
