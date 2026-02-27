package com.fortnite.pronos.controller;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.UserDeletionService;
import com.fortnite.pronos.service.UserResolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Account management controller — allows users to manage their own account. */
@Slf4j
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

  private final UserDeletionService userDeletionService;
  private final UserResolver userResolver;

  /**
   * Soft-deletes the authenticated user's account and liberates their game participations.
   *
   * @return 204 No Content on success, 401 if not authenticated
   */
  @DeleteMapping
  public ResponseEntity<Void> deleteAccount(
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {

    User user = userResolver.resolve(username, httpRequest);
    if (user == null) {
      log.warn(
          "AccountController: deleteAccount unauthorized - remoteAddr={}",
          httpRequest.getRemoteAddr());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    userDeletionService.deleteAccount(user.getId());
    log.info("AccountController: account deleted for user {}", user.getUsername());
    return ResponseEntity.noContent().build();
  }
}
