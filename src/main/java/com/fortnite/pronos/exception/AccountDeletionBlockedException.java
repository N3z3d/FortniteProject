package com.fortnite.pronos.exception;

/** Exception thrown when account deletion is blocked because the user owns active games. */
public class AccountDeletionBlockedException extends RuntimeException {

  public AccountDeletionBlockedException(String message) {
    super(message);
  }
}
