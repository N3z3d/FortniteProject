package com.fortnite.pronos.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Filtre de secours pour les tests : il ne s'active que si l'entête X-Test-User est présent. Aucun
 * fallback automatique sinon, afin de ne pas masquer les 401/404 attendus.
 */
@Profile({"test", "local", "dev"})
@Component
@RequiredArgsConstructor
@Slf4j
public class TestFallbackAuthenticationFilter extends OncePerRequestFilter {

  private final UserDetailsService userDetailsService;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    if (SecurityContextHolder.getContext().getAuthentication() != null || hasBearer(request)) {
      filterChain.doFilter(request, response);
      return;
    }

    String username = request.getHeader("X-Test-User");
    if (username == null || username.isBlank()) {
      String paramUser = request.getParameter("user");
      if (paramUser != null && !paramUser.isBlank()) {
        username = paramUser;
      }
    }

    if (username == null || username.isBlank()) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      UserDetails userDetails = userDetailsService.loadUserByUsername(username.trim());
      UsernamePasswordAuthenticationToken authToken =
          new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
      authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authToken);
      log.debug("Authentification fallback appliquée pour l'utilisateur {}", username);
    } catch (Exception e) {
      log.warn("Impossible d'appliquer le fallback utilisateur {} : {}", username, e.getMessage());
    }

    filterChain.doFilter(request, response);
  }

  private boolean hasBearer(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    return authHeader != null && authHeader.startsWith("Bearer ");
  }
}
