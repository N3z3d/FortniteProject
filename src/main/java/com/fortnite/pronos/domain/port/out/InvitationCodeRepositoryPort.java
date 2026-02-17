package com.fortnite.pronos.domain.port.out;

/** Port for invitation code persistence checks. */
public interface InvitationCodeRepositoryPort {
  boolean existsByInvitationCode(String invitationCode);
}
