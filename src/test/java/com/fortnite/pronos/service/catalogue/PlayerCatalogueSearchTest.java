package com.fortnite.pronos.service.catalogue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.domain.player.model.Player;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;
import com.fortnite.pronos.dto.player.CataloguePlayerDto;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerCatalogueService — nickname search (FR-12)")
class PlayerCatalogueSearchTest {

  @Mock private PlayerDomainRepositoryPort playerRepository;

  private PlayerCatalogueService service;

  @BeforeEach
  void setUp() {
    service = new PlayerCatalogueService(playerRepository);
  }

  private Player playerWithNickname(String nickname) {
    return Player.restore(
        UUID.randomUUID(), null, "user", nickname, PlayerRegion.EU, "1-5", 2025, false);
  }

  @Nested
  @DisplayName("searchByNickname()")
  class SearchByNickname {

    @Test
    @DisplayName("Finds player despite accent difference (éric vs eric)")
    void findsPlayerDespiteAccentDifference() {
      when(playerRepository.findAll())
          .thenReturn(List.of(playerWithNickname("Éric"), playerWithNickname("Bob")));

      List<CataloguePlayerDto> result = service.searchByNickname("eric");

      assertThat(result).hasSize(1);
      assertThat(result.getFirst().nickname()).isEqualTo("Éric");
    }

    @Test
    @DisplayName("Finds player despite case difference (ninjaKING vs NinjaKing)")
    void findsPlayerDespiteCaseDifference() {
      when(playerRepository.findAll())
          .thenReturn(List.of(playerWithNickname("NinjaKing"), playerWithNickname("Bob")));

      List<CataloguePlayerDto> result = service.searchByNickname("ninjaKING");

      assertThat(result).hasSize(1);
      assertThat(result.getFirst().nickname()).isEqualTo("NinjaKing");
    }

    @Test
    @DisplayName("Finds player with combined accent and case difference")
    void findsPlayerWithAccentAndCaseDifference() {
      when(playerRepository.findAll())
          .thenReturn(List.of(playerWithNickname("ÉricKing"), playerWithNickname("Bob")));

      List<CataloguePlayerDto> result = service.searchByNickname("ÉRICKING");

      assertThat(result).hasSize(1);
      assertThat(result.getFirst().nickname()).isEqualTo("ÉricKing");
    }

    @Test
    @DisplayName("Returns empty list for blank query")
    void returnsEmptyListForBlankQuery() {
      List<CataloguePlayerDto> result = service.searchByNickname("   ");

      assertThat(result).isEmpty();
      verifyNoInteractions(playerRepository);
    }

    @Test
    @DisplayName("Returns empty list for empty string query")
    void returnsEmptyListForEmptyQuery() {
      List<CataloguePlayerDto> result = service.searchByNickname("");

      assertThat(result).isEmpty();
      verifyNoInteractions(playerRepository);
    }

    @Test
    @DisplayName("Returns empty list when no player matches")
    void returnsEmptyListWhenNoMatch() {
      when(playerRepository.findAll())
          .thenReturn(List.of(playerWithNickname("Alice"), playerWithNickname("Bob")));

      List<CataloguePlayerDto> result = service.searchByNickname("zorro");

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Caps results at MAX_CATALOGUE_SIZE when many players match")
    void capsResultsAtMaxCatalogueSize() {
      List<Player> manyPlayers =
          IntStream.range(0, 1200).mapToObj(i -> playerWithNickname("ninja" + i)).toList();
      when(playerRepository.findAll()).thenReturn(manyPlayers);

      List<CataloguePlayerDto> result = service.searchByNickname("ninja");

      assertThat(result).hasSize(PlayerCatalogueService.MAX_CATALOGUE_SIZE);
    }

    @Test
    @DisplayName("Performs partial match (query is a substring of nickname)")
    void performsPartialMatch() {
      when(playerRepository.findAll())
          .thenReturn(
              List.of(
                  playerWithNickname("SuperNinja99"),
                  playerWithNickname("TheNinjaKing"),
                  playerWithNickname("Bob")));

      List<CataloguePlayerDto> result = service.searchByNickname("ninja");

      assertThat(result).hasSize(2);
    }
  }

  @Nested
  @DisplayName("normalize() — unit tests")
  class NormalizeMethod {

    @Test
    @DisplayName("Strips accent from é")
    void stripsAccentFromE() {
      assertThat(PlayerCatalogueService.normalize("éric")).isEqualTo("eric");
    }

    @Test
    @DisplayName("Lowercases the result")
    void lowercasesResult() {
      assertThat(PlayerCatalogueService.normalize("ERIC")).isEqualTo("eric");
    }

    @Test
    @DisplayName("Returns empty string for null input")
    void returnsEmptyForNull() {
      assertThat(PlayerCatalogueService.normalize(null)).isEmpty();
    }

    @Test
    @DisplayName("Trims surrounding whitespace")
    void trimsSurroundingWhitespace() {
      assertThat(PlayerCatalogueService.normalize("  ninja  ")).isEqualTo("ninja");
    }
  }
}
