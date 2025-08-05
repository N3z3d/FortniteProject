package com.fortnite.pronos.model;

/** Enum représentant les différents états d'une game */
public enum GameStatus {
  /** Game en cours de création/configuration */
  CREATING,

  /** Game en cours de draft (sélection des joueurs) */
  DRAFTING,

  /** Game avec draft en cours */
  DRAFT_IN_PROGRESS,

  /** Game en attente de début du draft */
  DRAFT_PENDING,

  /** Game en attente de joueurs */
  WAITING_FOR_PLAYERS,

  /** Game active (compétition en cours) */
  ACTIVE,

  /** Game terminée */
  FINISHED,

  /** Game annulée */
  CANCELLED
}
