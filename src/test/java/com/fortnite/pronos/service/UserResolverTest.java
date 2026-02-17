package com.fortnite.pronos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

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
class UserResolverTest {

  @Mock private UserService userService;
  @Mock private JwtService jwtService;
  @Mock private Environment environment;
  @Mock private HttpServletRequest request;

  private UserResolver userResolver;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    userResolver = new UserResolver(userService, jwtService, environment);
  }

  @Test
  void shouldResolveFromUsernameParamInDevProfile() {
    User user = buildUser("thibaut");
    when(environment.getActiveProfiles()).thenReturn(new String[] {"dev"});
    when(userService.findUserByUsername("thibaut")).thenReturn(Optional.of(user));

    User resolved = userResolver.resolve("thibaut", request);

    assertEquals(user.getId(), resolved.getId());
  }

  @Test
  void shouldResolveFromTestHeaderInTestProfile() {
    User user = buildUser("teddy");
    when(environment.getActiveProfiles()).thenReturn(new String[] {"test"});
    when(request.getHeader("X-Test-User")).thenReturn("teddy");
    when(userService.findUserByUsername("teddy")).thenReturn(Optional.of(user));

    User resolved = userResolver.resolve(null, request);

    assertEquals(user.getId(), resolved.getId());
  }

  @Test
  void shouldIgnoreUsernameParamAndTestHeaderInProdAndUseJwt() {
    User user = buildUser("jwt-user");
    when(environment.getActiveProfiles()).thenReturn(new String[] {"prod"});
    when(request.getHeader("Authorization")).thenReturn("Bearer token123");
    when(jwtService.extractUsername("token123")).thenReturn("jwt-user");
    when(userService.findUserByUsername("jwt-user")).thenReturn(Optional.of(user));

    User resolved = userResolver.resolve("spoof-query", request);

    assertEquals(user.getId(), resolved.getId());
  }

  @Test
  void shouldIgnoreSpoofedSourcesInProdWhenNoJwtOrSecurityContext() {
    when(environment.getActiveProfiles()).thenReturn(new String[] {"prod"});
    when(request.getHeader("Authorization")).thenReturn(null);

    User resolved = userResolver.resolve("spoof-query", request);

    assertNull(resolved);
  }

  @Test
  void shouldResolveFromSecurityContextInProd() {
    User user = buildUser("secure-user");
    when(environment.getActiveProfiles()).thenReturn(new String[] {"prod"});
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("secure-user", null, java.util.List.of()));
    when(userService.findUserByUsername("secure-user")).thenReturn(Optional.of(user));

    User resolved = userResolver.resolve(null, request);

    assertEquals(user.getId(), resolved.getId());
  }

  private User buildUser(String username) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername(username);
    return user;
  }
}
