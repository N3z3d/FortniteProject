package com.fortnite.pronos.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JoinGameWithCodeRequest {

  @Size(max = 32, message = "Le code d'invitation est trop long")
  @Pattern(
      regexp = "^[A-Za-z0-9-]*$",
      message = "Le code d'invitation ne peut contenir que des lettres, chiffres et tirets")
  private String code;

  @Size(max = 32, message = "Le code d'invitation est trop long")
  @Pattern(
      regexp = "^[A-Za-z0-9-]*$",
      message = "Le code d'invitation ne peut contenir que des lettres, chiffres et tirets")
  private String invitationCode;

  @AssertTrue(message = "Le code d'invitation est requis")
  public boolean hasInvitationCode() {
    return isNotBlank(code) || isNotBlank(invitationCode);
  }

  public String resolveInvitationCode() {
    if (isNotBlank(code)) {
      return code.trim();
    }
    if (isNotBlank(invitationCode)) {
      return invitationCode.trim();
    }
    return null;
  }

  private boolean isNotBlank(String value) {
    return value != null && !value.trim().isEmpty();
  }
}
