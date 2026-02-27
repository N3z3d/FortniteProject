package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fortnite.pronos.model.User;

@ExtendWith(MockitoExtension.class)
class FlexibleAuthenticationServiceTest {

  @Mock private UnifiedAuthService unifiedAuthService;
  @Mock private Environment environment;

  private FlexibleAuthenticationService service;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    service = new FlexibleAuthenticationService(unifiedAuthService, environment);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getCurrentUser_returnsDefaultUserInDevelopmentWhenAuthenticationMissing() {
    when(environment.getActiveProfiles()).thenReturn(new String[] {"dev"});

    User user = service.getCurrentUser();

    assertThat(user.getUsername()).isEqualTo("dev-user");
    assertThat(user.getCurrentSeason()).isEqualTo(2025);
    assertThat(user.getRole()).isEqualTo(User.UserRole.ADMIN);
  }

  @Test
  void getCurrentUser_throwsWhenAuthenticationMissingOutsideDevelopment() {
    when(environment.getActiveProfiles()).thenReturn(new String[] {"prod"});

    assertThatThrownBy(() -> service.getCurrentUser())
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Utilisateur non authentifie");
  }

  @Test
  void getCurrentUser_returnsTemporaryUserWhenAuthenticatedUserMissingInDatabase() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("alice", "n/a", List.of()));
    when(unifiedAuthService.findUserByUsername("alice")).thenReturn(Optional.empty());

    User user = service.getCurrentUser();

    assertThat(user.getUsername()).isEqualTo("alice");
    assertThat(user.getEmail()).isEqualTo("alice@temp.com");
    assertThat(user.getRole()).isEqualTo(User.UserRole.USER);
  }

  @Test
  void getCurrentUser_returnsPersistedUserWhenPresent() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("bob", "n/a", List.of()));
    User persistedUser = new User();
    persistedUser.setId(UUID.randomUUID());
    persistedUser.setUsername("bob");
    persistedUser.setEmail("bob@test.com");
    when(unifiedAuthService.findUserByUsername("bob")).thenReturn(Optional.of(persistedUser));

    User result = service.getCurrentUser();

    assertThat(result).isSameAs(persistedUser);
  }

  @Test
  void getEnvironmentInfo_resolvesDatabaseTypeFromDatasourceUrl() {
    when(environment.getActiveProfiles()).thenReturn(new String[] {"prod"});
    when(environment.getProperty("spring.application.name", "fortnite-pronos"))
        .thenReturn("pronos");
    when(environment.getProperty("app.version", "0.1.0-SNAPSHOT")).thenReturn("1.0.0");
    when(environment.getProperty("spring.datasource.url", "Unknown"))
        .thenReturn("jdbc:postgresql://localhost:5432/pronos");
    when(environment.getProperty("jwt.enabled", "true")).thenReturn("true");

    Map<String, Object> info = service.getEnvironmentInfo();

    assertThat(info).containsEntry("database", "PostgreSQL");
  }
}
