# Story 7.2 Runtime Validation

Story: `7-2-resolution-manuelle-et-correction-fiche-joueur`
Date: `2026-03-07`
Status: `closed`

## Observation

- La story etait marquee `done`, mais sans preuve runtime sur de vraies entrees `UNRESOLVED`.
- L'ingestion locale revalidee en Sprint 4 a enfin fourni des entrees pipeline realistes a corriger.

## Validation

### AC story 7.2 - correction metadata

- `POST /api/auth/login` avec `username=admin`, `password=Admin1234` -> `200 OK`
- `GET /api/admin/players/unresolved?page=0&size=10` -> `200 OK`
  - entree cible observee: `playerUsername=regionUpgrade`, `playerRegion=UNKNOWN`
- `PATCH /api/admin/players/6ab0ecfc-a8a6-464d-840d-f76ea0411bb3/metadata` avec body
  - `playerId=6ab0ecfc-a8a6-464d-840d-f76ea0411bb3`
  - `newUsername=regionUpgradeEU`
  - `newRegion=EU`
  -> `200 OK`
- Reponse observee:
  - `status=UNRESOLVED`
  - `correctedUsername=regionUpgradeEU`
  - `correctedRegion=EU`
  - `correctedBy=admin`
  - `correctedAt` renseigne

### Complement FR-42 - resolve/reject admin sur entrees UNRESOLVED

- `GET /api/admin/players/unresolved?page=0&size=10` -> `200 OK`
  - entree resolvee: `playerId=dab89f34-2d0c-4b22-bfeb-6b52a49e5767`, `playerUsername=muz`, `playerRegion=NAC`
  - entree rejetee: `playerId=66e9f319-6154-4b71-b8e4-91a9a3767c7c`, `playerUsername=white`, `playerRegion=BR`
- `POST /api/admin/players/resolve` avec body
  - `playerId=dab89f34-2d0c-4b22-bfeb-6b52a49e5767`
  - `epicId=muz_fn_verified_01`
  -> `200 OK`
- Reponse resolve observee:
  - `status=RESOLVED`
  - `epicId=muz_fn_verified_01`
  - `resolvedBy=admin`
  - `resolvedAt` renseigne
- `POST /api/admin/players/reject` avec body
  - `playerId=66e9f319-6154-4b71-b8e4-91a9a3767c7c`
  - `reason=Joueur non retrouve sur la saison locale`
  -> `200 OK`
- Reponse reject observee:
  - `status=REJECTED`
  - `resolvedBy=admin`
  - `rejectionReason=Joueur non retrouve sur la saison locale`
  - `rejectedAt` renseigne
- `GET /api/admin/players/pipeline/regional-status` -> `200 OK`
  - `NAC`: `resolvedCount=1`
  - `BR`: `rejectedCount=1`
- `GET /api/admin/audit-log?limit=10` -> `200 OK`
  - trace `RESOLVE_PLAYER` presente pour `dab89f34-2d0c-4b22-bfeb-6b52a49e5767`
  - trace `REJECT_PLAYER` presente pour `66e9f319-6154-4b71-b8e4-91a9a3767c7c`
  - trace `CORRECT_METADATA` precedente toujours presente

## BMAD Handling

- La story 7.2 reste `done`, mais elle est maintenant revalidee en runtime local sur des donnees issues d'une vraie ingestion, pour la correction metadata et pour le flow admin FR-42 `resolve/reject`.
- Aucun sujet hebergement/staging n'est rouvert.
