package com.fortnite.pronos.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards all non-API, non-static requests to index.html so Angular can handle client-side
 * routing. Without this, refreshing on /games/123 returns 404.
 */
@Controller
public class SpaController {

  // Single-level paths: /dashboard, /profile, /games
  @GetMapping(value = {"/{path:[^.]*}"})
  public String spaRoot() {
    return "forward:/index.html";
  }

  // Deep paths: /games/123/draft/snake — exclude /api/** to avoid intercepting REST endpoints
  @GetMapping(value = {"/{root:(?!api)[^.]*}/{*rest}"})
  public String spaDeep() {
    return "forward:/index.html";
  }
}
