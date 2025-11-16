package com.fortnite.pronos.model;

import lombok.Data;

/**
 * Simple RegionRule for compatibility with tests This is a simplified version of GameRegionRule for
 * test purposes
 */
@Data
public class RegionRule {
  private String region;
  private Integer maxPlayers;
}
