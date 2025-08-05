package com.fortnite.pronos.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import com.fortnite.pronos.model.enums.AliasStatus;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "player_aliases")
public class PlayerAlias {

  @Id
  @Column(name = "alias_id", columnDefinition = "uuid")
  private UUID aliasId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "player_id")
  private Player player;

  private String nickname;
  private String source; // FT / EPIC / MANUAL

  @Enumerated(EnumType.STRING)
  private AliasStatus status;

  private boolean current = true;

  private OffsetDateTime createdAt;

  @PrePersist
  public void prePersist() {
    if (aliasId == null) aliasId = UUID.randomUUID();
    if (createdAt == null) createdAt = OffsetDateTime.now();
    if (status == null) status = AliasStatus.APPROVED;
  }
}
