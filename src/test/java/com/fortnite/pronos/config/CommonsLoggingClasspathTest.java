package com.fortnite.pronos.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;

import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Logging classpath")
class CommonsLoggingClasspathTest {

  @Test
  @DisplayName("Does not load commons-logging from its own jar")
  void shouldNotLoadCommonsLoggingFromCommonsLoggingJar() {
    URL location = LogFactory.class.getProtectionDomain().getCodeSource().getLocation();
    assertNotNull(location, "Expected LogFactory to have a code source");
    String path = location.toString().toLowerCase();
    assertTrue(!path.contains("commons-logging"), "Unexpected commons-logging jar: " + path);
  }
}
