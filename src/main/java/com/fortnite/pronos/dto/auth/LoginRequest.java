package com.fortnite.pronos.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class LoginRequest {
  @NotBlank(message = "Le nom d'utilisateur est requis")
  private String username;

  @Email(message = "Format d'email invalide")
  private String email;

  @NotBlank(message = "Le mot de passe est requis")
  private String password;
}
