package com.fortnite.pronos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fortnite.supabase")
public class SupabaseProperties {

  private String url;
  private String anonKey;
  private String schema = "public";
  private String seedGameId;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getAnonKey() {
    return anonKey;
  }

  public void setAnonKey(String anonKey) {
    this.anonKey = anonKey;
  }

  public String getSchema() {
    return schema;
  }

  public void setSchema(String schema) {
    this.schema = schema;
  }

  public String getSeedGameId() {
    return seedGameId;
  }

  public void setSeedGameId(String seedGameId) {
    this.seedGameId = seedGameId;
  }

  public boolean isConfigured() {
    return url != null && !url.isBlank() && anonKey != null && !anonKey.isBlank();
  }

  public boolean hasSeedGameId() {
    return seedGameId != null && !seedGameId.isBlank();
  }
}
