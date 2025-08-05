package com.fortnite.pronos.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

  @GetMapping("/")
  public ResponseEntity<Map<String, Object>> home() {
    Map<String, Object> response = new HashMap<>();
    response.put("message", "Bienvenue sur Fortnite Pronos API");
    response.put("version", "1.0.0");
    response.put("status", "running");
    response.put(
        "endpoints",
        Map.of(
            "health", "/actuator/health",
            "players", "/players",
            "teams", "/teams",
            "leaderboard", "/leaderboard",
            "api-docs", "/swagger-ui.html"));
    return ResponseEntity.ok(response);
  }

  @GetMapping("/api")
  public ResponseEntity<Map<String, Object>> apiInfo() {
    Map<String, Object> response = new HashMap<>();
    response.put("name", "Fortnite Pronos API");
    response.put("description", "API pour gérer les pronostics Fortnite");
    response.put("documentation", "/swagger-ui.html");
    return ResponseEntity.ok(response);
  }
}
