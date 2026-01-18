package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fortnite.pronos.model.User;

/**
 * Tests TDD pour UserContextService Focus sur la résolution du problème d'authentification par
 * paramètre
 */
@ExtendWith(MockitoExtension.class)
class UserContextServiceTddTest {

  @Mock private UserService userService;

  @Mock private Authentication authentication;

  @Mock private SecurityContext securityContext;

  @InjectMocks private UserContextService userContextService;

  private User testUser;
  private UUID testUserId;

  @BeforeEach
  void setUp() {
    testUserId = UUID.randomUUID();
    testUser = new User();
    testUser.setId(testUserId);
    testUser.setUsername("Thibaut");
    testUser.setEmail("thibaut@fortnite-pronos.com");
    testUser.setRole(User.UserRole.USER);
    testUser.setCurrentSeason(2025);
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Devrait récupérer l'utilisateur depuis le paramètre username valide")
  void shouldGetUserFromValidUsernameParam() {
    // Given
    String usernameParam = "Thibaut";
    when(userService.findUserByUsername(usernameParam)).thenReturn(Optional.of(testUser));

    // When
    User result = userContextService.getCurrentUserFromParam(usernameParam);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getUsername()).isEqualTo("Thibaut");
    assertThat(result.getId()).isEqualTo(testUserId);
    verify(userService).findUserByUsername(usernameParam);
  }

  @Test
  @DisplayName("Devrait lancer une exception quand le paramètre username est null")
  void shouldUseDefaultFallbackWhenUsernameParamIsNull() {
    // Given - paramètre null doit lever une exception

    // When & Then - getCurrentUserFromParam avec null doit lancer une exception
    assertThatThrownBy(() -> userContextService.getCurrentUserFromParam(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Parametre username requis pour l'authentification");
  }

  @Test
  @DisplayName("Devrait lancer une exception quand le paramètre username est vide")
  void shouldUseDefaultFallbackWhenUsernameParamIsEmpty() {
    // Given - paramètre vide doit lever une exception

    // When & Then - getCurrentUserFromParam avec espace vide doit lancer une exception
    assertThatThrownBy(() -> userContextService.getCurrentUserFromParam("   "))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Parametre username requis pour l'authentification");
  }

  @Test
  @DisplayName("Devrait lancer une exception quand l'utilisateur n'est pas trouvé en base")
  void shouldThrowExceptionWhenUserNotFoundInDatabase() {
    // Given
    String usernameParam = "UtilisateurInexistant";
    when(userService.findUserByUsername(usernameParam)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> userContextService.getCurrentUserFromParam(usernameParam))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Utilisateur non trouve: " + usernameParam);
  }

  @Test
  @DisplayName("Devrait récupérer l'ID utilisateur depuis le paramètre username")
  void shouldGetUserIdFromUsernameParam() {
    // Given
    String usernameParam = "Thibaut";
    when(userService.findUserByUsername(usernameParam)).thenReturn(Optional.of(testUser));

    // When
    UUID result = userContextService.getCurrentUserIdFromParam(usernameParam);

    // Then
    assertThat(result).isEqualTo(testUserId);
    verify(userService).findUserByUsername(usernameParam);
  }

  @Test
  @DisplayName("Devrait retourner false quand aucun utilisateur n'est authentifié")
  void shouldReturnFalseWhenNoUserAuthenticated() {
    // Given
    when(securityContext.getAuthentication()).thenReturn(null);
    SecurityContextHolder.setContext(securityContext);

    // When
    boolean result = userContextService.isUserAuthenticated();

    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Devrait retourner true quand un utilisateur est authentifié")
  void shouldReturnTrueWhenUserAuthenticated() {
    // Given
    when(authentication.isAuthenticated()).thenReturn(true);
    doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER")))
        .when(authentication)
        .getAuthorities();
    when(authentication.getName()).thenReturn("Thibaut");
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    // When
    boolean result = userContextService.isUserAuthenticated();

    // Then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Devrait retourner false pour une authentification anonyme")
  void shouldReturnFalseForAnonymousAuthentication() {
    Authentication anonymous =
        new AnonymousAuthenticationToken(
            "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonymous);

    boolean result = userContextService.isUserAuthenticated();

    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Devrait retourner false quand les autorites sont nulles")
  void shouldReturnFalseWhenAuthoritiesAreNull() {
    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getAuthorities()).thenReturn(null);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    boolean result = userContextService.isUserAuthenticated();

    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Devrait utiliser l'authentification quand disponible et aucun paramètre fourni")
  void shouldUseAuthenticationWhenAvailable() {
    // Given - Authentication active mais pas de paramètre
    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getName()).thenReturn("Thibaut");
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
    when(userService.findUserByUsername("Thibaut")).thenReturn(Optional.of(testUser));
    when(userService.findUserById(testUserId)).thenReturn(Optional.of(testUser));

    // When - Pas de paramètre, doit utiliser l'auth
    User result = userContextService.getCurrentUserWithFallback(null);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getUsername()).isEqualTo("Thibaut");
    verify(userService).findUserByUsername("Thibaut");
    verify(userService).findUserById(testUserId);
  }

  @Test
  @DisplayName(
      "Devrait utiliser le paramètre username quand l'authentification n'est pas disponible")
  void shouldUseUsernameParamWhenAuthenticationNotAvailable() {
    // Given
    User marcel = new User();
    marcel.setId(UUID.randomUUID());
    marcel.setUsername("Marcel");
    marcel.setEmail("marcel@test.com");

    when(userService.findUserByUsername("Marcel")).thenReturn(Optional.of(marcel));

    // When
    User result = userContextService.getCurrentUserWithFallback("Marcel");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getUsername()).isEqualTo("Marcel");
    verify(userService).findUserByUsername("Marcel");
  }

  @Test
  @DisplayName(
      "Devrait lancer une exception quand ni l'authentification ni le paramètre ne sont disponibles")
  void shouldUseDefaultFallbackWhenNeitherAuthenticationNorParamAvailable() {
    // Given - Pas d'auth et pas de paramètre
    when(securityContext.getAuthentication()).thenReturn(null);
    SecurityContextHolder.setContext(securityContext);

    // When & Then - Doit lancer une exception
    assertThatThrownBy(() -> userContextService.getCurrentUserWithFallback(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Authentification requise - aucun utilisateur connecte");
  }

  @Test
  @DisplayName("Devrait récupérer l'ID utilisateur avec fallback")
  void shouldGetUserIdWithFallback() {
    // Given
    when(userService.findUserByUsername("Thibaut")).thenReturn(Optional.of(testUser));

    // When
    UUID result = userContextService.getCurrentUserIdWithFallback("Thibaut");

    // Then
    assertThat(result).isEqualTo(testUserId);
    verify(userService).findUserByUsername("Thibaut");
  }

  @Test
  @DisplayName("Devrait gérer les erreurs de sécurité gracieusement")
  void shouldHandleSecurityErrorsGracefully() {
    // Given
    SecurityContextHolder.setContext(securityContext);
    when(securityContext.getAuthentication()).thenThrow(new RuntimeException("Erreur de sécurité"));

    // When
    boolean result = userContextService.isUserAuthenticated();

    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Devrait lancer une exception quand aucun paramètre et pas d'auth")
  void shouldUseDefaultFallbackWhenNoParamProvided() {
    // Given - Pas d'auth et pas de paramètre
    when(securityContext.getAuthentication()).thenReturn(null);
    SecurityContextHolder.setContext(securityContext);

    // When & Then - Doit lancer une exception
    assertThatThrownBy(() -> userContextService.getCurrentUserWithFallback(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Authentification requise - aucun utilisateur connecte");
  }

  @Test
  @DisplayName("Devrait lancer une exception quand le paramètre est vide et pas d'auth")
  void shouldUseDefaultFallbackWhenParamIsEmpty() {
    // Given - Paramètre vide et pas d'auth
    when(securityContext.getAuthentication()).thenReturn(null);
    SecurityContextHolder.setContext(securityContext);

    // When & Then - Doit lancer une exception car paramètre vide équivaut à null
    assertThatThrownBy(() -> userContextService.getCurrentUserWithFallback("   "))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Authentification requise - aucun utilisateur connecte");
  }

  @Test
  @DisplayName("Devrait refuser une authentification sans nom d'utilisateur")
  void shouldRejectAuthenticationWithBlankUsername() {
    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getName()).thenReturn("   ");
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    assertThatThrownBy(() -> userContextService.getCurrentUserId())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Nom d'utilisateur");
  }

  @Test
  @DisplayName("Devrait refuser une authentification anonyme pour l'identifiant utilisateur")
  void shouldRejectAnonymousAuthenticationForUserId() {
    Authentication anonymous =
        new AnonymousAuthenticationToken(
            "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonymous);

    assertThatThrownBy(() -> userContextService.getCurrentUserId())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Utilisateur non authentifie");
  }
}
