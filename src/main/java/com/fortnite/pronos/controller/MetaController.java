package com.fortnite.pronos.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fortnite.pronos.domain.game.model.PlayerRegion;

/** Public metadata endpoints (no authentication required). */
@RestController
@RequestMapping("/api/meta")
public class MetaController {

  /** Returns the list of valid player regions. Used by the frontend to populate region selects. */
  @GetMapping("/regions")
  public ResponseEntity<List<String>> getRegions() {
    List<String> regions = Arrays.stream(PlayerRegion.values()).map(PlayerRegion::name).toList();
    return ResponseEntity.ok(regions);
  }
}
