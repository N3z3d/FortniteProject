package com.fortnite.pronos.service;

import java.util.Arrays;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.fortnite.pronos.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility component for resolving the current user from various authentication sources.
 *
 * <p>Resolution order:
 *
 * <p>- In test/dev/h2 profiles: username parameter, then X-Test-User, then JWT/SecurityContext.
 *
 * <p>- In all other profiles: JWT/SecurityContext only.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserResolver {

  private final UserService userService;
  private final JwtService jwtService;
  private final Environment environment;

  /**
   * Resolves the current user from the request context.
   *
   * @param username Optional username parameter
   * @param httpRequest The HTTP request
   * @return The resolved User, or null if not found
   */
  public User resolve(String username, HttpServletRequest httpRequest) {
    User user = null;

    if (allowNonAuthenticatedIdentitySources()) {
      // 1. Check username parameter
      if (username != null && !username.isBlank()) {
        user = userService.findUserByUsername(username).orElse(null);
        if (user != null) {
          return user;
        }
      }

      // 2. Check X-Test-User header
      if (httpRequest != null) {
        String headerUser = httpRequest.getHeader("X-Test-User");
        if (headerUser != null && !headerUser.isBlank()) {
          user = userService.findUserByUsername(headerUser).orElse(null);
          if (user != null) {
            return user;
          }
        }
      }
    }

    // 3. Check Authorization Bearer token
    String bearerUser = extractUserFromAuthorization(httpRequest);
    if (bearerUser != null) {
      user = userService.findUserByUsername(bearerUser).orElse(null);
      if (user != null) {
        return user;
      }
    }

    // 4. Check SecurityContext
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.isAuthenticated()
        && !"anonymousUser".equals(authentication.getName())) {
      user = userService.findUserByUsername(authentication.getName()).orElse(null);
      if (user != null) {
        return user;
      }
    }

    // No automatic test user creation - callers should handle null
    return null;
  }

  /**
   * Resolves the current user, throwing if required and not found.
   *
   * @param username Optional username parameter
   * @param required If true, returns null when user not found (caller handles)
   * @param httpRequest The HTTP request
   * @return The resolved User, or null if not found and required
   */
  public User resolveOrNull(String username, boolean required, HttpServletRequest httpRequest) {
    User user = resolve(username, httpRequest);
    if (required && user == null) {
      return null;
    }
    return user;
  }

  private String extractUserFromAuthorization(HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return null;
    }
    String token = authHeader.substring("Bearer ".length());
    try {
      return jwtService.extractUsername(token);
    } catch (Exception e) {
      log.warn("Impossible d'extraire l'utilisateur du token JWT: {}", e.getMessage());
      return null;
    }
  }

  private boolean allowNonAuthenticatedIdentitySources() {
    String[] activeProfiles = environment.getActiveProfiles();
    return Arrays.stream(activeProfiles)
        .anyMatch(
            profile ->
                profile.equalsIgnoreCase("dev")
                    || profile.equalsIgnoreCase("h2")
                    || profile.equalsIgnoreCase("test"));
  }
}
