package com.fortnite.pronos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fortnite.seed")
public class SeedProperties {

  private boolean enabled;
  private String mode = "reference";
  private boolean legacyEnabled;
  private boolean fakeGameEnabled;
  private boolean reset;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public boolean isLegacyEnabled() {
    return legacyEnabled;
  }

  public void setLegacyEnabled(boolean legacyEnabled) {
    this.legacyEnabled = legacyEnabled;
  }

  public boolean isFakeGameEnabled() {
    return fakeGameEnabled;
  }

  public void setFakeGameEnabled(boolean fakeGameEnabled) {
    this.fakeGameEnabled = fakeGameEnabled;
  }

  public boolean isReset() {
    return reset;
  }

  public void setReset(boolean reset) {
    this.reset = reset;
  }

  public boolean isResetMode() {
    if (mode == null) {
      return false;
    }
    return "reset".equalsIgnoreCase(mode.trim());
  }
}
