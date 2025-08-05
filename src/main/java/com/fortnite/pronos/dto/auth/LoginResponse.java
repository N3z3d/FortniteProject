package com.fortnite.pronos.dto.auth;

import java.util.UUID;

import com.fortnite.pronos.model.User;

import lombok.Data;

@Data
public class LoginResponse {
  private String token;
  private String refreshToken;
  private UserDto user;

  @Data
  public static class UserDto {
    private UUID id;
    private String email;
    private User.UserRole role;

    public static UserDto from(User user) {
      UserDto dto = new UserDto();
      dto.setId(user.getId());
      dto.setEmail(user.getEmail());
      dto.setRole(user.getRole());
      return dto;
    }
  }
}
