package com.fortnite.pronos.exception;

/** Exception thrown when an invitation code is expired or otherwise invalid. */
public class InvalidInvitationCodeException extends RuntimeException {

  public InvalidInvitationCodeException(String message) {
    super(message);
  }
}
