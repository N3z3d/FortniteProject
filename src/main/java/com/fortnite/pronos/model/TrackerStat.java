package com.fortnite.pronos.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "tracker_stats")
public class TrackerStat {

  @Id
  @Column(name = "stat_id", columnDefinition = "uuid")
  private UUID statId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "player_id")
  private Player player;

  private int season;
  private int prValue;
  private OffsetDateTime fetchedAt;
  private String source;

  @PrePersist
  public void prePersist() {
    if (statId == null) statId = UUID.randomUUID();
    if (fetchedAt == null) fetchedAt = OffsetDateTime.now();
    if (source == null) source = "FT";
  }
}
