package com.fortnite.pronos.config;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class TestActuatorController {

  @GetMapping("/actuator/health")
  ResponseEntity<Map<String, String>> health() {
    return ResponseEntity.ok(Map.of("status", "UP"));
  }

  @GetMapping("/actuator/info")
  ResponseEntity<Map<String, String>> info() {
    return ResponseEntity.ok(Map.of("app", "fortnite-pronos"));
  }
}
