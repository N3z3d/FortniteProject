package com.fortnite.pronos.exception;

/** Exception levée lorsqu'on essaie de terminer un draft incomplet */
public class DraftIncompleteException extends RuntimeException {

  public DraftIncompleteException(String message) {
    super(message);
  }

  public DraftIncompleteException(int incomplete, int total) {
    super(
        String.format(
            "%d participants sur %d n'ont pas terminé leur sélection", incomplete, total));
  }
}
