package com.fortnite.pronos.exception;

import com.fortnite.pronos.model.Draft;

/** Exception levée lorsque le draft n'est pas dans un état valide pour l'opération */
public class InvalidDraftStateException extends RuntimeException {

  public InvalidDraftStateException(String message) {
    super(message);
  }

  public InvalidDraftStateException(Draft.Status currentStatus, String operation) {
    super(
        String.format(
            "Le draft n'est pas dans un état valide pour %s. État actuel : %s",
            operation, currentStatus));
  }
}
