package com.fortnite.pronos.service.auth;

import java.util.Arrays;

import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import com.fortnite.pronos.dto.auth.LoginRequest;
import com.fortnite.pronos.dto.auth.LoginResponse;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.UserRepository;
import com.fortnite.pronos.service.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring Security based authentication strategy Uses AuthenticationManager for standard Spring
 * Security authentication
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SpringSecurityAuthenticationStrategy implements AuthenticationStrategy {

  private final UserRepository userRepository;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;
  private final UserDetailsService userDetailsService;
  private final Environment environment;

  @Override
  public boolean isActive() {
    // Use this strategy for profiles that are not dev and have AuthenticationManager configured
    return !Arrays.asList(environment.getActiveProfiles()).contains("dev");
  }

  @Override
  public LoginResponse login(LoginRequest request) {
    log.info("Tentative de connexion pour: {}", request.getEmail());

    // Authentifier avec Spring Security
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

    // Récupérer l'utilisateur
    User user =
        userRepository
            .findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());

    // Générer les tokens
    String jwtToken = jwtService.generateToken(userDetails);
    String refreshToken = jwtService.generateRefreshToken(userDetails);

    log.info("Connexion réussie pour: {}", request.getEmail());

    // Créer la réponse
    LoginResponse response = new LoginResponse();
    response.setToken(jwtToken);
    response.setRefreshToken(refreshToken);
    response.setUser(LoginResponse.UserDto.from(user));

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
