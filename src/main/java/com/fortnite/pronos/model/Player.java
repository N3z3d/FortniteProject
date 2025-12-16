package com.fortnite.pronos.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "players")
public class Player {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "fortnite_id", unique = true)
  private String fortniteId;

  @NotBlank private String username;

  @Column(nullable = false, unique = true)
  private String nickname;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private Region region;

  @Column(nullable = false)
  private String tranche; // Flexible: 1-7, 1-10, NOUVEAU, etc.

  @Column(name = "current_season", nullable = false)
  private Integer currentSeason = 2025;

  @OneToMany(mappedBy = "player")
  @Builder.Default
  private List<TeamPlayer> teamPlayers = new ArrayList<>();

  @OneToMany(mappedBy = "playerOut")
  @Builder.Default
  private List<Trade> outgoingTrades = new ArrayList<>();

  @OneToMany(mappedBy = "playerIn")
  @Builder.Default
  private List<Trade> incomingTrades = new ArrayList<>();

  @OneToMany(mappedBy = "player", cascade = CascadeType.ALL)
  @Builder.Default
  private List<Score> scores = new ArrayList<>();

  @Column(name = "locked")
  @Builder.Default
  private Boolean locked = false;

  public enum Region {
    EU,
    NAW,
    BR,
    ASIA,
    OCE,
    NAC,
    ME,
    NA
  }

  @PrePersist
  @PreUpdate
  public void validateTranche() {
    if (tranche != null && tranche.trim().isEmpty()) {
      throw new IllegalArgumentException("La tranche ne peut pas être vide");
    }
    if (currentSeason == null || currentSeason <= 0) {
      currentSeason = 2025;
    }
  }

  /**
   * Méthode de convenance pour obtenir le nom d'affichage du joueur Retourne le nickname si
   * disponible, sinon le username
   */
  public String getName() {
    return nickname != null ? nickname : username;
  }

  /** Convenience method for tests to get region as string */
  public String getRegionName() {
    return region != null ? region.name() : null;
  }

  /** Convenience method for tests to set region from string */
  public void setRegion(String regionName) {
    if (regionName != null) {
      this.region = Region.valueOf(regionName);
    }
  }

  /** Convenience setter for enum */
  public void setRegion(Region region) {
    this.region = region;
  }

  /** Convenience method for tests to check if player is locked */
  public boolean isLocked() {
    return locked != null && locked;
  }

  /** Convenience method for tests to set locked status */
  public void setLocked(boolean locked) {
    this.locked = locked;
  }

  /** Convenience method for tests to set name (sets nickname) */
  public void setName(String name) {
    this.nickname = name;
  }
}
