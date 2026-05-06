package com.fortnite.pronos.domain.game.model;

import java.util.List;

/** Domain enum representing player regions. Framework-free. */
public enum PlayerRegion {
  UNKNOWN,
  EU,
  NAW,
  BR,
  ASIA,
  OCE,
  NAC,
  ME,
  NA;

  /** The 7 active regions used in snake draft. Excludes UNKNOWN and NA (legacy). */
  public static final List<PlayerRegion> ACTIVE_REGIONS = List.of(EU, NAW, BR, ASIA, OCE, NAC, ME);
}
