package com.fortnite.pronos.config;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

@DisplayName("Hibernate dialect config")
class HibernateDialectConfigTest {

  private static final String DIALECT_PROPERTY = "spring.jpa.properties.hibernate.dialect";

  @Test
  @DisplayName("Does not set dialect in application.yml")
  void shouldNotSetDialectInDefaultConfig() throws IOException {
    assertNull(loadYamlProperty("application.yml", DIALECT_PROPERTY));
  }

  @Test
  @DisplayName("Does not set dialect in application-dev.yml")
  void shouldNotSetDialectInDevConfig() throws IOException {
    assertNull(loadYamlProperty("application-dev.yml", DIALECT_PROPERTY));
  }

  @Test
  @DisplayName("Does not set dialect in application-prod.yml")
  void shouldNotSetDialectInProdConfig() throws IOException {
    assertNull(loadYamlProperty("application-prod.yml", DIALECT_PROPERTY));
  }

  @Test
  @DisplayName("Does not set dialect in application.properties")
  void shouldNotSetDialectInPropertiesConfig() throws IOException {
    assertNull(loadPropertiesProperty("application.properties", DIALECT_PROPERTY));
  }

  private Object loadYamlProperty(String resourceName, String key) throws IOException {
    YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
    List<PropertySource<?>> sources =
        loader.load(resourceName, new ClassPathResource(resourceName));
    for (PropertySource<?> source : sources) {
      Object value = source.getProperty(key);
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private Object loadPropertiesProperty(String resourceName, String key) throws IOException {
    PropertiesPropertySourceLoader loader = new PropertiesPropertySourceLoader();
    List<PropertySource<?>> sources =
        loader.load(resourceName, new ClassPathResource(resourceName));
    for (PropertySource<?> source : sources) {
      Object value = source.getProperty(key);
      if (value != null) {
        return value;
      }
    }
    return null;
  }
}
