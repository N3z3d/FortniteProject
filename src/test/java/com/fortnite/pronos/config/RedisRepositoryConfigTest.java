package com.fortnite.pronos.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

@DisplayName("Redis repositories config")
class RedisRepositoryConfigTest {

  @Test
  @DisplayName("Disables Redis repositories by default")
  void shouldDisableRedisRepositoriesByDefault() throws IOException {
    YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
    List<PropertySource<?>> sources =
        loader.load("application", new ClassPathResource("application.yml"));
    Object value = null;
    for (PropertySource<?> source : sources) {
      value = source.getProperty("spring.data.redis.repositories.enabled");
      if (value != null) {
        break;
      }
    }
    assertEquals("false", String.valueOf(value));
  }
}
