package com.fortnite.pronos.config;

import java.util.Set;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class ResolutionAdapterConfiguration {

  private static final String PROPERTY_NAME = "resolution.adapter";
  private static final String ENV_NAME = "RESOLUTION_ADAPTER";
  private static final String STUB_ADAPTER = "stub";
  private static final String FORTNITE_API_ADAPTER = "fortnite-api";
  private static final String SUPPORTED_VALUES = STUB_ADAPTER + ", " + FORTNITE_API_ADAPTER;
  private static final Set<String> SUPPORTED_ADAPTERS = Set.of(STUB_ADAPTER, FORTNITE_API_ADAPTER);

  @Bean
  static BeanFactoryPostProcessor resolutionAdapterValidator(Environment environment) {
    return beanFactory -> validateAdapter(environment);
  }

  private static void validateAdapter(Environment environment) {
    String configuredAdapter = environment.getProperty(PROPERTY_NAME);
    if (configuredAdapter == null) {
      return;
    }

    String normalizedAdapter = configuredAdapter.trim();
    if (normalizedAdapter.isEmpty() || !SUPPORTED_ADAPTERS.contains(normalizedAdapter)) {
      throw new IllegalStateException(invalidAdapterMessage(configuredAdapter));
    }
  }

  private static String invalidAdapterMessage(String invalidValue) {
    return "Invalid "
        + PROPERTY_NAME
        + " value '"
        + invalidValue
        + "'. Supported values: "
        + SUPPORTED_VALUES
        + ". Set "
        + ENV_NAME
        + "="
        + STUB_ADAPTER
        + " for local stub mode or "
        + ENV_NAME
        + "="
        + FORTNITE_API_ADAPTER
        + " with FORTNITE_API_KEY configured.";
  }
}
