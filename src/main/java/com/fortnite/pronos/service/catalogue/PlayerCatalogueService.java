package com.fortnite.pronos.service.catalogue;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;
import com.fortnite.pronos.dto.player.CataloguePlayerDto;

import lombok.RequiredArgsConstructor;

/**
 * Service exposing the player catalogue for all connected users (FR-10). Filters by region using
 * the hexagonal domain port — no direct JPA coupling.
 */
@Service
@RequiredArgsConstructor
public class PlayerCatalogueService {

  static final int MAX_CATALOGUE_SIZE = 1000;

  private final PlayerDomainRepositoryPort playerRepository;

  /**
   * Returns up to {@value #MAX_CATALOGUE_SIZE} players for the given region.
   *
   * @param region the target region
   * @return catalogue entries for the region
   */
  @Cacheable(value = "catalogue-region", key = "#region.name()")
  public List<CataloguePlayerDto> findByRegion(PlayerRegion region) {
    return playerRepository.findByRegion(region).stream()
        .limit(MAX_CATALOGUE_SIZE)
        .map(CataloguePlayerDto::from)
        .toList();
  }

  /**
   * Returns up to {@value #MAX_CATALOGUE_SIZE} players across all regions.
   *
   * @return all catalogue entries
   */
  @Cacheable(value = "catalogue-all", key = "'all'")
  public List<CataloguePlayerDto> findAll() {
    return playerRepository.findAll().stream()
        .limit(MAX_CATALOGUE_SIZE)
        .map(CataloguePlayerDto::from)
        .toList();
  }

  /**
   * Searches players whose nickname contains {@code query}, tolerating case and accents (FR-12).
   * Returns an empty list when the query is blank.
   *
   * @param query the search term (case/accent-insensitive)
   * @return up to {@value #MAX_CATALOGUE_SIZE} matching catalogue entries
   */
  public List<CataloguePlayerDto> searchByNickname(String query) {
    String normalized = normalize(query);
    if (normalized.isEmpty()) {
      return List.of();
    }
    return playerRepository.findAll().stream()
        .filter(p -> normalize(p.getNickname()).contains(normalized))
        .limit(MAX_CATALOGUE_SIZE)
        .map(CataloguePlayerDto::from)
        .toList();
  }

  /**
   * Strips combining diacritical marks (accents) and lowercases the string for comparison.
   *
   * <p>Uses JDK {@link Normalizer} — no external dependency.
   */
  static String normalize(String s) {
    if (s == null) {
      return "";
    }
    String nfd = Normalizer.normalize(s.trim(), Normalizer.Form.NFD);
    return nfd.replaceAll("\\p{InCombiningDiacriticalMarks}", "").toLowerCase(Locale.ROOT);
  }
}
