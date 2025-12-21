package com.fortnite.pronos.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("GameDto - serialization")
class GameDtoSerializationTest {

  @Test
  @DisplayName("should serialize currentParticipantCount without conflict")
  void shouldSerializeCurrentParticipantCountWithoutConflict() throws Exception {
    GameDto dto =
        GameDto.builder()
            .id(UUID.randomUUID())
            .name("Test Game")
            .currentParticipantCount(2)
            .build();

    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(mapper.writeValueAsString(dto));

    assertThat(node.get("currentParticipantCount").asInt()).isEqualTo(2);
    assertThat(node.has("participantCount")).isFalse();
  }
}
