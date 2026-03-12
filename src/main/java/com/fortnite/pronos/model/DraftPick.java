package com.fortnite.pronos.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "draft_picks")
public class DraftPick {

  public enum DraftRegionSlot {
    EU,
    NAC,
    NAW,
    BR,
    ASIA,
    OCE,
    ME;

    static DraftRegionSlot fromPlayerRegion(Player.Region region) {
      if (region == null) {
        return null;
      }
      try {
        return DraftRegionSlot.valueOf(region.name());
      } catch (IllegalArgumentException ex) {
        return null;
      }
    }
  }

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "draft_id", nullable = false)
  private Draft draft;

  @ManyToOne
  @JoinColumn(name = "participant_id", nullable = false)
  private GameParticipant participant;

  @Column(name = "participant", nullable = false)
  private String participantLabel;

  @ManyToOne
  @JoinColumn(name = "player_id", nullable = false)
  private Player player;

  @Enumerated(EnumType.STRING)
  @Column(name = "region_slot", nullable = false, columnDefinition = "draft_region_slot")
  @ColumnTransformer(write = "CAST(? AS draft_region_slot)")
  private DraftRegionSlot regionSlot;

  @Column(name = "round_number", nullable = false)
  private Integer round;

  @Column(name = "pick_number", nullable = false)
  private Integer pickNumber;

  @Column(name = "selection_time", nullable = false)
  private LocalDateTime selectionTime;

  @Column(name = "time_taken_seconds")
  private Integer timeTakenSeconds;

  @Column(name = "auto_pick", nullable = false)
  private Boolean autoPick = false;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  // Constructeurs
  public DraftPick() {
    this.createdAt = LocalDateTime.now();
    this.selectionTime = LocalDateTime.now();
    this.autoPick = false;
  }

  public DraftPick(
      Draft draft, GameParticipant participant, Player player, Integer round, Integer pickNumber) {
    this();
    this.draft = draft;
    setParticipant(participant);
    setPlayer(player);
    this.round = round;
    this.pickNumber = pickNumber;
  }

  @PrePersist
  @PreUpdate
  void syncLegacyColumns() {
    this.participantLabel = resolveParticipantLabel(participant);
    this.regionSlot = resolveRegionSlot(player);
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
    if (selectionTime == null) {
      selectionTime = LocalDateTime.now();
    }
    if (autoPick == null) {
      autoPick = false;
    }
  }

  // Méthodes utilitaires
  public boolean isAutoPick() {
    return this.autoPick;
  }

  public void markAsAutoPick() {
    this.autoPick = true;
  }

  public void setTimeTaken(Integer seconds) {
    this.timeTakenSeconds = seconds;
  }

  // Getters et Setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Draft getDraft() {
    return draft;
  }

  public void setDraft(Draft draft) {
    this.draft = draft;
  }

  public GameParticipant getParticipant() {
    return participant;
  }

  public void setParticipant(GameParticipant participant) {
    this.participant = participant;
    this.participantLabel = resolveParticipantLabel(participant);
  }

  public Player getPlayer() {
    return player;
  }

  public void setPlayer(Player player) {
    this.player = player;
    this.regionSlot = resolveRegionSlot(player);
  }

  public String getParticipantLabel() {
    return participantLabel;
  }

  public void setParticipantLabel(String participantLabel) {
    this.participantLabel = participantLabel;
  }

  public DraftRegionSlot getRegionSlot() {
    return regionSlot;
  }

  public void setRegionSlot(DraftRegionSlot regionSlot) {
    this.regionSlot = regionSlot;
  }

  public Integer getRound() {
    return round;
  }

  public void setRound(Integer round) {
    this.round = round;
  }

  public Integer getPickNumber() {
    return pickNumber;
  }

  public void setPickNumber(Integer pickNumber) {
    this.pickNumber = pickNumber;
  }

  public LocalDateTime getSelectionTime() {
    return selectionTime;
  }

  public void setSelectionTime(LocalDateTime selectionTime) {
    this.selectionTime = selectionTime;
  }

  public Integer getTimeTakenSeconds() {
    return timeTakenSeconds;
  }

  public void setTimeTakenSeconds(Integer timeTakenSeconds) {
    this.timeTakenSeconds = timeTakenSeconds;
  }

  public Boolean getAutoPick() {
    return autoPick;
  }

  public void setAutoPick(Boolean autoPick) {
    this.autoPick = autoPick;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  private String resolveParticipantLabel(GameParticipant currentParticipant) {
    if (currentParticipant == null) {
      return null;
    }
    String username = currentParticipant.getUsername();
    if (username != null && !username.isBlank()) {
      return username;
    }
    UUID userId = currentParticipant.getUserId();
    if (userId != null) {
      return userId.toString();
    }
    return currentParticipant.getId() != null ? currentParticipant.getId().toString() : null;
  }

  private DraftRegionSlot resolveRegionSlot(Player currentPlayer) {
    return currentPlayer == null
        ? null
        : DraftRegionSlot.fromPlayerRegion(currentPlayer.getRegion());
  }

  @Override
  public String toString() {
    return "DraftPick{"
        + "id="
        + id
        + ", draft="
        + (draft != null ? draft.getId() : "null")
        + ", participant="
        + (participant != null ? participant.getUsername() : "null")
        + ", participantLabel="
        + participantLabel
        + ", player="
        + (player != null ? player.getNickname() : "null")
        + ", regionSlot="
        + regionSlot
        + ", round="
        + round
        + ", pickNumber="
        + pickNumber
        + ", autoPick="
        + autoPick
        + '}';
  }
}
