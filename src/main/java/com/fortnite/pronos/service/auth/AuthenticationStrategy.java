package com.fortnite.pronos.service.auth;

import com.fortnite.pronos.dto.auth.LoginRequest;
import com.fortnite.pronos.dto.auth.LoginResponse;

/**
 * Interface for authentication strategies Implements Strategy pattern for different authentication
 * approaches
 */
public interface AuthenticationStrategy {

  /**
   * Authenticate user with given credentials
   *
   * @param request Login request containing email and password
   * @return Login response with tokens and user info
   */
  LoginResponse login(LoginRequest request);

  /**
   * Refresh authentication token
   *
   * @param refreshToken The refresh token
   * @return New login response with refreshed tokens
   */
  LoginResponse refreshToken(String refreshToken);

  /**
   * Check if this strategy is active for current profile
   *
   * @return true if strategy should be used
   */
  boolean isActive();
}
