package com.fortnite.pronos.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.fortnite.pronos.model.Player;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("test")
class PlayerRepositoryTest {

  @Autowired private PlayerRepository playerRepository;

  @Test
  void rejectsDuplicateNickname() {
    playerRepository.saveAndFlush(buildPlayer("pixie"));

    Throwable thrown = catchThrowable(() -> playerRepository.saveAndFlush(buildPlayer("pixie")));

    assertThat(thrown)
        .satisfiesAnyOf(
            error -> assertThat(error).isInstanceOf(DataIntegrityViolationException.class),
            error -> assertThat(error).isInstanceOf(ConstraintViolationException.class),
            error -> assertThat(error).hasCauseInstanceOf(ConstraintViolationException.class));
  }

  private Player buildPlayer(String nickname) {
    Player player = new Player();
    player.setUsername(nickname.toLowerCase());
    player.setNickname(nickname);
    player.setRegion(Player.Region.EU);
    player.setTranche("1-5");
    player.setCurrentSeason(2025);
    return player;
  }
}
