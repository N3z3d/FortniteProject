package com.fortnite.pronos.adapter.out.persistence.draft;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.draft.model.DraftRegionCursor;

/** Converts between {@link DraftRegionCursorEntity} and {@link DraftRegionCursor}. */
@Component
public class DraftRegionCursorEntityMapper {

  public DraftRegionCursor toDomain(DraftRegionCursorEntity entity) {
    DraftRegionCursorId pk = entity.getId();
    List<UUID> snakeOrder = parseSnakeOrder(entity.getSnakeOrder());
    return DraftRegionCursor.restore(
        pk.getDraftId(),
        pk.getRegion(),
        entity.getCurrentRound(),
        entity.getCurrentPick(),
        snakeOrder);
  }

  public DraftRegionCursorEntity toEntity(DraftRegionCursor cursor) {
    DraftRegionCursorId pk = new DraftRegionCursorId(cursor.getDraftId(), cursor.getRegion());
    String snakeOrder = serializeSnakeOrder(cursor.getSnakeOrder());
    return new DraftRegionCursorEntity(
        pk, cursor.getCurrentRound(), cursor.getCurrentPick(), snakeOrder);
  }

  private List<UUID> parseSnakeOrder(String raw) {
    return Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(UUID::fromString)
        .toList();
  }

  private String serializeSnakeOrder(List<UUID> order) {
    return order.stream().map(UUID::toString).collect(Collectors.joining(","));
  }
}
