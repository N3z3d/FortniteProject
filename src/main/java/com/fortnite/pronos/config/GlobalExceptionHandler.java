package com.fortnite.pronos.config;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.fortnite.pronos.core.error.FortnitePronosException;
import com.fortnite.pronos.exception.UserNotFoundException;
import com.fortnite.pronos.service.admin.ErrorEntry;
import com.fortnite.pronos.service.admin.ErrorJournalService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for common and infrastructure exceptions. Domain-specific exception
 * handlers are in DomainExceptionHandler.
 */
@ControllerAdvice
@Order(3)
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({"java:S2589"})
public class GlobalExceptionHandler {

  private final ErrorJournalService errorJournalService;

  /**
   * Gestion des exceptions mÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©tier personnalisÃƒÆ’Ã†â€™Ãƒâ€
   * Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©es
   */
  @ExceptionHandler(FortnitePronosException.class)
  public ResponseEntity<ErrorResponse> handleFortnitePronosException(
      FortnitePronosException ex, HttpServletRequest request) {

    log.warn("Business exception: {} - {}", ex.getErrorCode(), ex.getMessage());

    int statusCode = ex.getErrorCode().getStatusCode();
    HttpStatus httpStatus = HttpStatus.valueOf(statusCode);

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(statusCode)
            .error(httpStatus.getReasonPhrase())
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .code(ex.getErrorCode().name())
            .build();

    recordToJournal(ex, request, errorResponse);
    return ResponseEntity.status(httpStatus).body(errorResponse);
  }

  /** Gestion des erreurs d'authentification */
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthenticationException(
      AuthenticationException ex, HttpServletRequest request) {

    log.warn("Authentication failed: {}", ex.getMessage());

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.UNAUTHORIZED.value())
            .error("Unauthorized")
            .message("Authentication required")
            .path(request.getRequestURI())
            .accessibilityHint("Utilisez Tab pour naviguer vers le formulaire de connexion")
            .keyboardAction("Appuyez sur Entree pour vous connecter")
            .requiresUserAction(true)
            .build();

    recordToJournal(ex, request, errorResponse);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
  }

  /** Gestion des erreurs utilisateur non trouvÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â© (login) */
  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleUserNotFoundException(
      UserNotFoundException ex, HttpServletRequest request) {

    log.warn("User not found: {}", ex.getMessage());

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.UNAUTHORIZED.value())
            .error("Authentication Failed")
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .code("USER_NOT_FOUND")
            .accessibilityHint("Verifiez votre nom d\'utilisateur ou creez un compte")
            .requiresUserAction(true)
            .build();

    recordToJournal(ex, request, errorResponse);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
  }

  /** Gestion des erreurs d'autorisation */
  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDeniedException(
      Throwable ex, HttpServletRequest request) {

    log.warn("Access denied: {}", ex.getMessage());

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("Forbidden")
            .message("Access denied")
            .path(request.getRequestURI())
            .build();

    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
  }

  /** Gestion des erreurs de validation */
  @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      BindException ex, HttpServletRequest request) {

    log.warn("Validation failed: {}", ex.getMessage());

    Map<String, String> fieldErrors = new HashMap<>();
    ex.getAllErrors()
        .forEach(
            error -> {
              String fieldName = resolveValidationErrorKey(error);
              String errorMessage =
                  error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage();
              fieldErrors.put(fieldName, errorMessage);
            });

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("Invalid input parameters")
            .path(request.getRequestURI())
            .validationErrors(fieldErrors)
            .build();

    return ResponseEntity.badRequest().body(errorResponse);
  }

  /** Gestion des erreurs de validation sur parametres de methode */
  @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolationException(
      Throwable ex, HttpServletRequest request) {

    log.warn("Constraint violation: {}", ex.getMessage());

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("Invalid request parameters")
            .path(request.getRequestURI())
            .build();

    return ResponseEntity.badRequest().body(errorResponse);
  }

  /** Gestion des erreurs de type de paramÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¨tre */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatchException(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

    log.warn("Type mismatch: {} for parameter {}", ex.getValue(), ex.getName());

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .message("Invalid parameter type: " + ex.getName())
            .path(request.getRequestURI())
            .build();

    return ResponseEntity.badRequest().body(errorResponse);
  }

  /** Gestion des erreurs IllegalArgumentException */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
      Throwable ex, HttpServletRequest request) {

    log.warn("Illegal argument: {}", ex.getMessage());

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .message("Invalid request parameter")
            .path(request.getRequestURI())
            .build();

    return ResponseEntity.badRequest().body(errorResponse);
  }

  /**
   * Gestion des 404 (routes ou ressources non trouvÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©es)
   */
  @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
  public ResponseEntity<ErrorResponse> handleNotFound(Exception ex, HttpServletRequest request) {
    log.debug("Resource not found: {}", request.getRequestURI());

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Not Found")
            .message("Resource not found")
            .path(request.getRequestURI())
            .build();

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
  }

  /** PHASE 1B: Additional exception handlers for comprehensive error coverage */
  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResponse> handleIllegalStateException(
      Throwable ex, HttpServletRequest request) {

    log.warn("Illegal state: {}", ex.getMessage());

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.CONFLICT.value())
            .error("Conflict")
            .message("Operation not allowed in current state")
            .path(request.getRequestURI())
            .accessibilityHint("Verifiez l\'etat actuel avant de recommencer")
            .build();

    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
  }

  /** PHASE 1B: Handle SecurityException */
  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<ErrorResponse> handleSecurityException(
      Throwable ex, HttpServletRequest request) {

    log.warn("Security violation: {}", ex.getMessage());

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("Security Violation")
            .message("Security constraints violated")
            .path(request.getRequestURI())
            .build();

    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
  }

  /**
   * Gestion des erreurs gÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©nÃƒÆ’Ã†â€™Ãƒâ€
   * Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©riques
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGlobalException(
      Exception ex, HttpServletRequest request) {

    if (AlreadyInGameFallbackHelper.isAlreadyInGameFallback(ex, request)) {
      log.warn("Fallback mapping for already-in-game exception: {}", ex.getMessage());
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(
              AlreadyInGameFallbackHelper.buildAlreadyInGameErrorResponse(
                  AlreadyInGameFallbackHelper.extractAlreadyInGameMessage(ex), request));
    }

    log.error("Unexpected error: {}", ex.getMessage(), ex);

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred")
            .path(request.getRequestURI())
            .build();

    recordToJournal(ex, request, errorResponse);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }

  private void recordToJournal(
      Exception ex, HttpServletRequest request, ErrorResponse errorResponse) {
    errorJournalService.recordError(ErrorEntry.from(ex, request, errorResponse));
  }

  private String resolveValidationErrorKey(ObjectError error) {
    if (error instanceof FieldError fieldError) {
      return fieldError.getField();
    }
    if (error.getObjectName() != null && !error.getObjectName().isBlank()) {
      return error.getObjectName();
    }
    return "request";
  }

  /** Standard error response payload with WCAG-oriented metadata for assistive technologies. */
  @lombok.Data
  @lombok.Builder
  @lombok.AllArgsConstructor
  @lombok.NoArgsConstructor
  public static class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private String code;
    private Map<String, String> validationErrors;
    private String requestId;

    // Champs spÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©cifiques pour l'accessibilitÃƒÆ’Ã†â€™Ãƒâ€
    //  Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©
    private String accessibilityHint; // Conseil pour les technologies d'assistance
    private String
        keyboardAction; // Action recommandÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©e au clavier
    private Boolean requiresUserAction; // Indique si une action utilisateur est requise
  }
}
