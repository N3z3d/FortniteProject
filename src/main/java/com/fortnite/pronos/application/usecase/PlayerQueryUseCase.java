package com.fortnite.pronos.application.usecase;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.fortnite.pronos.dto.player.PlayerDto;
import com.fortnite.pronos.model.Player;

/**
 * Application use case for Player query operations. Defines the public API for querying players.
 */
public interface PlayerQueryUseCase {

  Page<PlayerDto> getAllPlayers(Pageable pageable);

  Page<PlayerDto> getAllPlayers();

  PlayerDto getPlayerById(UUID id);

  List<PlayerDto> getPlayersByRegion(Player.Region region);

  List<PlayerDto> getPlayersByTranche(String tranche);

  Page<PlayerDto> searchPlayers(
      String query, Player.Region region, String tranche, boolean available, Pageable pageable);

  List<PlayerDto> getActivePlayers();

  Map<String, Object> getPlayersStats();
}
