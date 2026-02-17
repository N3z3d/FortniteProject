package com.fortnite.pronos.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductionConfigurationSecurityTest {

  private static final Path APPLICATION_YML = Path.of("src/main/resources/application.yml");
  private static final Path APPLICATION_PROD_YML =
      Path.of("src/main/resources/application-prod.yml");

  @Test
  @DisplayName("Production config keeps strict secret placeholders with no fallback")
  void productionConfigHasNoSensitiveFallbacks() throws IOException {
    String content = readFile(APPLICATION_PROD_YML);

    assertTrue(content.contains("secret: ${JWT_SECRET}"));
    assertFalse(content.contains("secret: ${JWT_SECRET:"));

    assertTrue(content.contains("username: ${DB_USERNAME}"));
    assertFalse(content.contains("username: ${DB_USERNAME:"));

    assertTrue(content.contains("password: ${DB_PASSWORD}"));
    assertFalse(content.contains("password: ${DB_PASSWORD:"));
  }

  @Test
  @DisplayName("Default profile actuator exposure excludes sensitive endpoints")
  void defaultConfigExcludesSensitiveActuatorEndpoints() throws IOException {
    String content = readFile(APPLICATION_YML);
    String include = extractActuatorInclude(content);

    assertEquals("health,info,metrics,prometheus", include);
    assertFalse(include.contains("datasource"));
    assertFalse(include.contains("configprops"));
    assertFalse(include.contains("env"));
    assertFalse(include.contains("loggers"));
  }

  @Test
  @DisplayName("Production profile actuator exposure is minimal")
  void productionConfigUsesMinimalActuatorExposure() throws IOException {
    String content = readFile(APPLICATION_PROD_YML);
    String include = extractActuatorInclude(content);

    assertEquals("health,info,prometheus", include);
    assertFalse(include.contains("metrics"));
    assertFalse(include.contains("datasource"));
    assertFalse(include.contains("configprops"));
    assertFalse(include.contains("env"));
    assertFalse(include.contains("loggers"));
  }

  private static String readFile(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  private static String extractActuatorInclude(String yamlContent) {
    Pattern pattern =
        Pattern.compile(
            "management:\\s*[\\s\\S]*?endpoints:\\s*[\\s\\S]*?web:\\s*[\\s\\S]*?exposure:\\s*[\\s\\S]*?include:\\s*([^\\r\\n]+)");
    Matcher matcher = pattern.matcher(yamlContent);
    if (!matcher.find()) {
      throw new IllegalStateException("Unable to find actuator exposure include in YAML content");
    }
    return matcher.group(1).trim();
  }
}
