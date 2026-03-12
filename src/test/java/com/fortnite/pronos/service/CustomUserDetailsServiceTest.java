package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

  @Mock private UserRepository userRepository;

  private CustomUserDetailsService service;

  @BeforeEach
  void setUp() {
    service = new CustomUserDetailsService(userRepository);
  }

  @Test
  void loadsUserByEmailWhenEmailMatches() {
    User user = buildUser("thibaut", "thibaut@fortnite-pronos.com", User.UserRole.USER);
    when(userRepository.findByEmail("thibaut@fortnite-pronos.com")).thenReturn(Optional.of(user));

    UserDetails userDetails = service.loadUserByUsername("thibaut@fortnite-pronos.com");

    assertThat(userDetails.getUsername()).isEqualTo("thibaut");
    assertThat(userDetails.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    verify(userRepository).findByEmail("thibaut@fortnite-pronos.com");
  }

  @Test
  void loadsUserByUsernameIgnoringCaseWhenFallbackAuthProvidesDisplayName() {
    User user = buildUser("thibaut", "thibaut@fortnite-pronos.com", User.UserRole.USER);
    when(userRepository.findByEmail("Thibaut")).thenReturn(Optional.empty());
    when(userRepository.findByUsernameIgnoreCase("Thibaut")).thenReturn(Optional.of(user));

    UserDetails userDetails = service.loadUserByUsername("Thibaut");

    assertThat(userDetails.getUsername()).isEqualTo("thibaut");
    assertThat(userDetails.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    verify(userRepository).findByUsernameIgnoreCase("Thibaut");
  }

  @Test
  void keepsAdminAuthorityWhenLoadingUser() {
    User user = buildUser("admin", "admin@fortnite-pronos.com", User.UserRole.ADMIN);
    when(userRepository.findByEmail("admin")).thenReturn(Optional.empty());
    when(userRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(user));

    UserDetails userDetails = service.loadUserByUsername("admin");

    assertThat(userDetails.getUsername()).isEqualTo("admin");
    assertThat(userDetails.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
  }

  @Test
  void throwsWhenNeitherEmailNorUsernameMatches() {
    when(userRepository.findByEmail("missing")).thenReturn(Optional.empty());
    lenient().when(userRepository.findByUsernameIgnoreCase("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.loadUserByUsername("missing"))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessageContaining("missing");
  }

  private User buildUser(String username, String email, User.UserRole role) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername(username);
    user.setEmail(email);
    user.setPassword("$2a$10$seedHash");
    user.setRole(role);
    user.setCurrentSeason(2025);
    return user;
  }
}
