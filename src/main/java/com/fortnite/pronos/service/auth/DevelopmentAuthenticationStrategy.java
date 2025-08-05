package com.fortnite.pronos.service.auth;

import java.util.Arrays;

import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.fortnite.pronos.dto.auth.LoginRequest;
import com.fortnite.pronos.dto.auth.LoginResponse;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.UserRepository;
import com.fortnite.pronos.service.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Development authentication strategy Simplified authentication without Spring Security complexity
 * for development
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DevelopmentAuthenticationStrategy implements AuthenticationStrategy {

  private final UserRepository userRepository;
  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;
  private final PasswordEncoder passwordEncoder;
  private final Environment environment;

  @Override
  public boolean isActive() {
    return Arrays.asList(environment.getActiveProfiles()).contains("dev");
  }

  @Override
  public LoginResponse login(LoginRequest request) {
    log.info("Authentification dev pour: {}", request.getEmail());

    // Authentification directe sans Spring Security pour le mode dev
    User user =
        userRepository
            .findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      throw new RuntimeException("Mot de passe incorrect");
    }

    UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
    String jwtToken = jwtService.generateToken(userDetails);
    String refreshToken = jwtService.generateRefreshToken(userDetails);

    LoginResponse response = new LoginResponse();
    response.setToken(jwtToken);
    response.setRefreshToken(refreshToken);
    response.setUser(LoginResponse.UserDto.from(user));

    log.info("Authentification réussie pour l'utilisateur: {}", user.getUsername());
    return response;
  }

  @Override
  public LoginResponse refreshToken(String refreshToken) {
    final String userEmail = jwtService.extractUsername(refreshToken);

    if (userEmail != null) {
      UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

      if (jwtService.isTokenValid(refreshToken, userDetails)) {
        String newToken = jwtService.generateToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        User user =
            userRepository
                .findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        LoginResponse response = new LoginResponse();
        response.setToken(newToken);
        response.setRefreshToken(newRefreshToken);
        response.setUser(LoginResponse.UserDto.from(user));

        return response;
      }
    }

    throw new RuntimeException("Token de rafraîchissement invalide");
  }
}
