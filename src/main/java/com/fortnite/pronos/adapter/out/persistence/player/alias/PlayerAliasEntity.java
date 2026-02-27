package com.fortnite.pronos.adapter.out.persistence.player.alias;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "player_aliases")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerAliasEntity {

  @Id
  @Column(name = "alias_id")
  private UUID id;

  @Column(name = "player_id")
  private UUID playerId;

  @Column(name = "nickname")
  private String nickname;

  @Column(name = "source")
  private String source;

  @Column(name = "current")
  private boolean current;

  @Column(name = "created_at")
  private LocalDateTime createdAt;
}
