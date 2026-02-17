package com.fortnite.pronos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RenameGameRequest {

  @NotBlank(message = "Le nom de la partie est requis")
  @Size(min = 3, max = 50, message = "Le nom doit contenir entre 3 et 50 caracteres")
  private String name;

  public String sanitizedName() {
    return name == null ? null : name.trim();
  }
}
