package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests - UserService")
class UserServiceTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private UserService userService;

  private User testUser;
  private User otherUser;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(UUID.randomUUID());
    testUser.setUsername("testuser");

    otherUser = new User();
    otherUser.setId(UUID.randomUUID());
    otherUser.setUsername("otheruser");
  }

  @Test
  @DisplayName("Devrait retourner tous les utilisateurs")
  void shouldReturnAllUsers() {
    // Given
    List<User> users = Arrays.asList(testUser, otherUser);
    when(userRepository.findAll()).thenReturn(users);

    // When
    List<User> result = userService.getAllUsers();

    // Then
    assertThat(result).hasSize(2);
    verify(userRepository).findAll();
  }

  @Test
  @DisplayName("Devrait retourner une liste vide quand aucun utilisateur")
  void shouldReturnEmptyListWhenNoUsers() {
    // Given
    when(userRepository.findAll()).thenReturn(Collections.emptyList());

    // When
    List<User> result = userService.getAllUsers();

    // Then
    assertThat(result).isEmpty();
    verify(userRepository).findAll();
  }

  @Test
  @DisplayName("Devrait retourner un utilisateur par ID")
  void shouldReturnUserById() {
    // Given
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

    // When
    Optional<User> result = userService.findUserById(testUser.getId());

    // Then
    assertThat(result).contains(testUser);
    verify(userRepository).findById(testUser.getId());
  }

  @Test
  @DisplayName("Devrait retourner vide quand l'utilisateur est introuvable")
  void shouldReturnEmptyWhenUserNotFound() {
    // Given
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.empty());

    // When
    Optional<User> result = userService.findUserById(testUser.getId());

    // Then
    assertThat(result).isEmpty();
    verify(userRepository).findById(testUser.getId());
  }

  @Test
  @DisplayName("Devrait propager l'erreur repository")
  void shouldPropagateRepositoryFailure() {
    // Given
    when(userRepository.findById(testUser.getId())).thenThrow(new IllegalStateException("boom"));

    // When & Then
    assertThatThrownBy(() -> userService.findUserById(testUser.getId()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("boom");
    verify(userRepository).findById(testUser.getId());
  }

  @Test
  @DisplayName("Devrait trouver un utilisateur par email")
  void shouldFindUserByEmail() {
    // Given
    when(userRepository.findByEmail("teddy@example.com")).thenReturn(Optional.of(testUser));

    // When
    Optional<User> result = userService.findUserByEmailOrUsername("teddy@example.com");

    // Then
    assertThat(result).contains(testUser);
    verify(userRepository).findByEmail("teddy@example.com");
  }

  @Test
  @DisplayName("Devrait fallback sur username quand email introuvable")
  void shouldFallbackToUsernameWhenEmailMissing() {
    // Given
    when(userRepository.findByEmail("Teddy")).thenReturn(Optional.empty());
    when(userRepository.findByUsernameIgnoreCase("Teddy")).thenReturn(Optional.of(testUser));

    // When
    Optional<User> result = userService.findUserByEmailOrUsername("Teddy");

    // Then
    assertThat(result).contains(testUser);
    verify(userRepository).findByEmail("Teddy");
    verify(userRepository).findByUsernameIgnoreCase("Teddy");
  }

  @Test
  @DisplayName("Devrait retourner vide quand email et username introuvables")
  void shouldReturnEmptyWhenEmailAndUsernameMissing() {
    // Given
    when(userRepository.findByEmail("missing")).thenReturn(Optional.empty());
    when(userRepository.findByUsernameIgnoreCase("missing")).thenReturn(Optional.empty());

    // When
    Optional<User> result = userService.findUserByEmailOrUsername("missing");

    // Then
    assertThat(result).isEmpty();
    verify(userRepository).findByEmail("missing");
    verify(userRepository).findByUsernameIgnoreCase("missing");
  }

  @Test
  @DisplayName("Devrait trouver un utilisateur par username")
  void shouldFindUserByUsername() {
    // Given
    when(userRepository.findByUsernameIgnoreCase("Tester")).thenReturn(Optional.of(testUser));

    // When
    Optional<User> result = userService.findUserByUsername("Tester");

    // Then
    assertThat(result).contains(testUser);
    verify(userRepository).findByUsernameIgnoreCase("Tester");
  }

  @Test
  @DisplayName("Devrait sauvegarder un utilisateur")
  void shouldSaveUser() {
    // Given
    when(userRepository.save(testUser)).thenReturn(testUser);

    // When
    User result = userService.saveUser(testUser);

    // Then
    assertThat(result).isSameAs(testUser);
    verify(userRepository).save(testUser);
  }
}
