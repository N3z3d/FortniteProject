# Story 5.1 Runtime Validation

Story: `5-1-swap-solo-vers-joueur-libre-valide`
Date: `2026-03-07`
Status: `closed`

## Observation

- La story etait marquee `done`, mais sans preuve runtime locale recente sur le flow `swap-solo`.
- La priorite Sprint 4 ayant ete explicitement recentree sur des validations locales "comme en prod", le flow devait etre rejoue avec vraie authentification JWT, vraies donnees seed et persistance PostgreSQL.

## Validation

- `POST /api/auth/login` avec `username=thibaut`, `password=Admin1234` -> `200 OK`
- `POST /api/games/b551d1dd-5061-4f52-a71e-35b318219aca/draft/swap-solo` avec JWT `thibaut`
  - `playerOutId=10000000-0000-0000-0000-000000000001` (`Bugha_EU`, region `EU`, tranche `1`)
  - `playerInId=10000000-0000-0000-0000-000000000005` (`EUMid1`, region `EU`, tranche `2`)
  -> `200 OK`
- Reponse observee:
  - `draftId=5c91d709-2d8a-449e-b304-0b4b45dd020a`
  - `participantId=9c700a4c-97d8-4f62-b2b5-66c7cb18ab66`
  - `playerOutId=10000000-0000-0000-0000-000000000001`
  - `playerInId=10000000-0000-0000-0000-000000000005`
- Verification DB locale apres swap:
  - `thibaut` possede maintenant `EUMid1` (`EU`, tranche `2`)
  - `marcel` conserve `EULow1`
- `GET /api/games/b551d1dd-5061-4f52-a71e-35b318219aca/draft/audit` avec JWT `thibaut` -> `200 OK`
  - derniere entree observee: `type=SWAP_SOLO`, `playerOutId=10000000-0000-0000-0000-000000000001`, `playerInId=10000000-0000-0000-0000-000000000005`
- Cas invalide revalide:
  - `POST /api/games/b551d1dd-5061-4f52-a71e-35b318219aca/draft/swap-solo`
    - `playerOutId=10000000-0000-0000-0000-000000000005` (`EUMid1`, tranche `2`)
    - `playerInId=10000000-0000-0000-0000-000000000002` (`Aqua_EU`, tranche `1`)
    -> `400 INVALID_SWAP`
  - payload observe:
    - `code=INVALID_SWAP`
    - `message=Target player must have a strictly worse rank (higher tranche number)`

## BMAD Handling

- La story 5.1 reste `done`, mais elle est maintenant revalidee en runtime local sur un flow authentifie et persiste.
- Aucun besoin Railway/staging n'est introduit par ce lot: le comportement critique est validable en local avec Docker + JWT + PostgreSQL.
