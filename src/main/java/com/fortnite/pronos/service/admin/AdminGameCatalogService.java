package com.fortnite.pronos.service.admin;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
class AdminGameCatalogService {

  private final UserRepository userRepository;
  private final GameRepository gameRepository;

  List<User> getAllUsers() {
    return userRepository.findAll();
  }

  List<Game> getAllGames(String status) {
    if (status == null || status.isBlank()) {
      return gameRepository.findAll();
    }
    try {
      GameStatus gameStatus = GameStatus.valueOf(status);
      return gameRepository.findByStatus(gameStatus);
    } catch (IllegalArgumentException e) {
      log.warn("Invalid game status filter: {}", status);
      return gameRepository.findAll();
    }
  }
}
