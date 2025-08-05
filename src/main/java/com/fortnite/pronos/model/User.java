package com.fortnite.pronos.model;

import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotBlank
  @Size(min = 3, max = 50)
  @Column(unique = true)
  private String username;

  @NotBlank
  @Email
  @Column(unique = true)
  private String email;

  @NotBlank private String password;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserRole role = UserRole.PARTICIPANT;

  @Column(name = "current_season", nullable = false)
  private Integer currentSeason = 2025;

  public enum UserRole {
    ADMIN,
    PARTICIPANT,
    SPECTATEUR
  }

  @PrePersist
  @PreUpdate
  public void validateRole() {
    if (role == null) {
      role = UserRole.PARTICIPANT;
    }
    if (currentSeason == null || currentSeason <= 0) {
      currentSeason = 2025;
    }
  }

  public String getPasswordHash() {
    return password;
  }
}
