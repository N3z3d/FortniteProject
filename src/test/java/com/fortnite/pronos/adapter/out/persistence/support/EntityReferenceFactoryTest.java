package com.fortnite.pronos.adapter.out.persistence.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EntityReferenceFactory")
class EntityReferenceFactoryTest {

  @Nested
  @DisplayName("userRef(UUID)")
  class UserRefById {

    @Test
    @DisplayName("returns null when userId is null")
    void returnsNullWhenIdIsNull() {
      assertThat(EntityReferenceFactory.userRef((UUID) null)).isNull();
    }

    @Test
    @DisplayName("returns User with correct id")
    void returnsUserWithCorrectId() {
      UUID id = UUID.randomUUID();
      assertThat(EntityReferenceFactory.userRef(id)).isNotNull();
      assertThat(EntityReferenceFactory.userRef(id).getId()).isEqualTo(id);
    }
  }

  @Nested
  @DisplayName("userRef(UUID, String)")
  class UserRefByIdAndUsername {

    @Test
    @DisplayName("returns null when userId is null")
    void returnsNullWhenIdIsNull() {
      assertThat(EntityReferenceFactory.userRef(null, "alice")).isNull();
    }

    @Test
    @DisplayName("returns User with correct id and username")
    void returnsUserWithCorrectIdAndUsername() {
      UUID id = UUID.randomUUID();
      var user = EntityReferenceFactory.userRef(id, "alice");
      assertThat(user).isNotNull();
      assertThat(user.getId()).isEqualTo(id);
      assertThat(user.getUsername()).isEqualTo("alice");
    }
  }

  @Nested
  @DisplayName("playerRef")
  class PlayerRef {

    @Test
    @DisplayName("returns null when playerId is null")
    void returnsNullWhenIdIsNull() {
      assertThat(EntityReferenceFactory.playerRef(null)).isNull();
    }

    @Test
    @DisplayName("returns Player with correct id")
    void returnsPlayerWithCorrectId() {
      UUID id = UUID.randomUUID();
      assertThat(EntityReferenceFactory.playerRef(id)).isNotNull();
      assertThat(EntityReferenceFactory.playerRef(id).getId()).isEqualTo(id);
    }
  }

  @Nested
  @DisplayName("gameRef")
  class GameRef {

    @Test
    @DisplayName("returns null when gameId is null")
    void returnsNullWhenIdIsNull() {
      assertThat(EntityReferenceFactory.gameRef(null)).isNull();
    }

    @Test
    @DisplayName("returns Game with correct id")
    void returnsGameWithCorrectId() {
      UUID id = UUID.randomUUID();
      assertThat(EntityReferenceFactory.gameRef(id)).isNotNull();
      assertThat(EntityReferenceFactory.gameRef(id).getId()).isEqualTo(id);
    }
  }

  @Nested
  @DisplayName("draftRef")
  class DraftRef {

    @Test
    @DisplayName("returns null when draftId is null")
    void returnsNullWhenIdIsNull() {
      assertThat(EntityReferenceFactory.draftRef(null)).isNull();
    }

    @Test
    @DisplayName("returns Draft with correct id")
    void returnsDraftWithCorrectId() {
      UUID id = UUID.randomUUID();
      assertThat(EntityReferenceFactory.draftRef(id)).isNotNull();
      assertThat(EntityReferenceFactory.draftRef(id).getId()).isEqualTo(id);
    }
  }
}
