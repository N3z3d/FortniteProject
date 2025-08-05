package com.fortnite.pronos.core.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Interface de base pour toutes les entités du domaine. Suit les principes du Domain-Driven Design
 * (DDD).
 *
 * @author Fortnite Pronos Team
 * @version 1.0
 * @since 2025-06-09
 */
public abstract class DomainEntity {

  protected final UUID id;

  protected DomainEntity(UUID id) {
    this.id = Objects.requireNonNull(id, "L'identifiant ne peut pas être null");
  }

  protected DomainEntity() {
    this.id = UUID.randomUUID();
  }

  public UUID getId() {
    return id;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    DomainEntity that = (DomainEntity) obj;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return String.format("%s{id=%s}", getClass().getSimpleName(), id);
  }

  /**
   * Valide l'état de l'entité selon les règles métier. À implémenter dans chaque entité concrète.
   *
   * @throws IllegalStateException si l'entité n'est pas dans un état valide
   */
  public abstract void validateInvariants();
}
