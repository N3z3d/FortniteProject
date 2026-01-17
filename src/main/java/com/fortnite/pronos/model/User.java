package com.fortnite.pronos.model;

import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.ColumnTransformer;

import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User {
  @Id private UUID id;

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
  @Column(nullable = false, columnDefinition = "user_role")
  @ColumnTransformer(write = "CAST(? AS user_role)")
  private UserRole role = UserRole.USER;

  @Column(name = "current_season", nullable = false)
  private Integer currentSeason = 2025;

  public enum UserRole {
    USER, // Regular user - can create/join games, participate in drafts
    ADMIN, // Administrator - full access to all features
    SPECTATOR // Spectator - can view games but cannot participate actively
  }

  @PrePersist
  @PreUpdate
  public void validateRole() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (role == null) {
      role = UserRole.USER;
    }
    if (currentSeason == null || currentSeason <= 0) {
      currentSeason = 2025;
    }
  }

  public String getPasswordHash() {
    return password;
  }
}
