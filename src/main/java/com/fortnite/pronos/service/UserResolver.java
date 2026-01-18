package com.fortnite.pronos.service;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.fortnite.pronos.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility component for resolving the current user from various authentication sources.
 *
 * <p>Resolution order: 1. Username parameter (if provided) 2. X-Test-User header 3. Authorization
 * Bearer token 4. SecurityContext authentication 5. Test user fallback for mock requests
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserResolver {

  private final UserService userService;
  private final JwtService jwtService;

  /**
   * Resolves the current user from the request context.
   *
   * @param username Optional username parameter
   * @param httpRequest The HTTP request
   * @return The resolved User, or null if not found
   */
  public User resolve(String username, HttpServletRequest httpRequest) {
    User user = null;

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
}
