package com.fortnite.pronos.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards all non-API, non-static requests to index.html so Angular can handle client-side
 * routing. Without this, refreshing on /games/123 or /admin/dashboard returns 404.
 *
 * <p>Two mappings cover all Angular route depths:
 *
 * <ul>
 *   <li>Root + single-segment: {@code /}, {@code /games}, {@code /admin}
 *   <li>Multi-segment: {@code /admin/dashboard}, {@code /games/123/draft/snake} — API and actuator
 *       prefixes are excluded via regex so their Spring MVC mappings remain authoritative.
 * </ul>
 *
 * <p>Static files (containing a {@code .} in the last segment, e.g. {@code main.js}) are never
 * matched because the regex {@code [^.]*} forbids dots.
 */
@Controller
public class SpaController {

  private static final String INDEX_HTML = "forward:/index.html";

  /** Root path and single-segment Angular routes: {@code /}, {@code /games}, {@code /catalogue}. */
  @GetMapping(value = {"/", "/{path:[^.]*}"})
  public String spaRoot() {
    return INDEX_HTML;
  }

  /**
   * Multi-segment Angular routes: {@code /admin/dashboard}, {@code /games/123/draft/snake}.
   *
   * <p>Uses Ant-style {@code /**} which is reliably matched by Spring MVC PathPatternParser in
   * Spring Boot 3.x, unlike {@code {*rest}} which exhibited 404 for 2-segment paths. Paths whose
   * first segment is {@code api} or {@code actuator} are excluded so Spring MVC continues to route
   * them to their respective {@code @RestController} mappings.
   */
  @GetMapping(value = {"/{root:(?!api$|actuator$|assets$|ws$)[^.]*}/**"})
  public String spaDeep() {
    return INDEX_HTML;
  }
}
