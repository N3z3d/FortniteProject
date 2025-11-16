package com.fortnite.pronos.exception;

/** General business logic exception for the trading system */
public class BusinessException extends RuntimeException {

  public BusinessException(String message) {
    super(message);
  }

  public BusinessException(String message, Throwable cause) {
    super(message, cause);
  }
}
