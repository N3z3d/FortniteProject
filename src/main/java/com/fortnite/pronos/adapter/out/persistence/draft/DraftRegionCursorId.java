package com.fortnite.pronos.adapter.out.persistence.draft;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Composite primary key for {@link DraftRegionCursorEntity}. */
@Embeddable
public class DraftRegionCursorId implements Serializable {

  @Column(name = "draft_id", nullable = false)
  private UUID draftId;

  @Column(name = "region", nullable = false, length = 10)
  private String region;

  protected DraftRegionCursorId() {}

  public DraftRegionCursorId(UUID draftId, String region) {
    this.draftId = Objects.requireNonNull(draftId);
    this.region = Objects.requireNonNull(region);
  }

  public UUID getDraftId() {
    return draftId;
  }

  public String getRegion() {
    return region;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DraftRegionCursorId that)) return false;
    return Objects.equals(draftId, that.draftId) && Objects.equals(region, that.region);
  }

  @Override
  public int hashCode() {
    return Objects.hash(draftId, region);
  }
}
