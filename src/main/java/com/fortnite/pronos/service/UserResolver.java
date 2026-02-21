package com.fortnite.pronos.service;

import java.util.Arrays;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

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
  public com.fortnite.pronos.model.User resolve(String username, HttpServletRequest httpRequest) {
    if (allowNonAuthenticatedIdentitySources()) {
      com.fortnite.pronos.model.User userFromParameter = resolveByUsername(username);
      if (userFromParameter != null) {
        return userFromParameter;
      }

      com.fortnite.pronos.model.User userFromHeader = resolveFromTestHeader(httpRequest);
      if (userFromHeader != null) {
        return userFromHeader;
      }
    }

    com.fortnite.pronos.model.User userFromBearer =
        resolveByUsername(extractUserFromAuthorization(httpRequest));
    if (userFromBearer != null) {
      return userFromBearer;
    }

    return resolveFromSecurityContext();
  }

  /**
   * Resolves the current user, throwing if required and not found.
   *
   * @param username Optional username parameter
   * @param required If true, returns null when user not found (caller handles)
   * @param httpRequest The HTTP request
   * @return The resolved User, or null if not found and required
   */
  public com.fortnite.pronos.model.User resolveOrNull(
      String username, boolean required, HttpServletRequest httpRequest) {
    com.fortnite.pronos.model.User user = resolve(username, httpRequest);
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

  private com.fortnite.pronos.model.User resolveByUsername(String username) {
    if (username == null || username.isBlank()) {
      return null;
    }
    return userService.findUserByUsername(username).orElse(null);
  }

  private com.fortnite.pronos.model.User resolveFromTestHeader(HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    return resolveByUsername(request.getHeader("X-Test-User"));
  }

  private com.fortnite.pronos.model.User resolveFromSecurityContext() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || "anonymousUser".equals(authentication.getName())) {
      return null;
    }
    return resolveByUsername(authentication.getName());
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
