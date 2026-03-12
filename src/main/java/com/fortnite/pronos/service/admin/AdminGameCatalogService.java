package com.fortnite.pronos.service.admin;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.dto.admin.AdminUserDto;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
class AdminGameCatalogService {

  private final UserRepositoryPort userRepository;
  private final GameRepositoryPort gameRepository;

  List<AdminUserDto> getAllUsers() {
    return userRepository.findAll().stream()
        .map(
            u ->
                AdminUserDto.builder()
                    .id(u.getId())
                    .username(u.getUsername())
                    .email(u.getEmail())
                    .role(u.getRole().name())
                    .currentSeason(u.getCurrentSeason())
                    .deleted(u.getDeletedAt() != null)
                    .build())
        .toList();
  }

  List<Game> getAllGames(String status) {
    String normalizedStatus = normalizeStatus(status);
    if (normalizedStatus == null) {
      return gameRepository.findAll();
    }
    try {
      GameStatus gameStatus = GameStatus.valueOf(normalizedStatus);
      return gameRepository.findByStatus(gameStatus);
    } catch (IllegalArgumentException e) {
      log.warn("Invalid game status filter: {}", status);
      return gameRepository.findAll();
    }
  }

  private String normalizeStatus(String status) {
    if (status == null) {
      return null;
    }
    String trimmedStatus = status.trim();
    if (trimmedStatus.isEmpty()) {
      return null;
    }
    return trimmedStatus.toUpperCase(Locale.ROOT);
  }
}
