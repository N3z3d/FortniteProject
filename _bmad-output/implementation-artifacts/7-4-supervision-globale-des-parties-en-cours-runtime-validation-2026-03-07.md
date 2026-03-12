# Story 7.4 Runtime Validation

Story: `7-4-supervision-globale-des-parties-en-cours`
Date: `2026-03-07`
Status: `closed`

## Observation

- La story etait marquee `done`, mais sans preuve runtime locale recente cote frontend.
- Une incoherence entre les profils locaux du front et les comptes seedes du backend permettait d'ouvrir `/admin/games` sans exposer les vraies donnees admin.
- Les profils de dev ont ete realignes sur les comptes seedes (`admin`, `thibaut`, `marcel`, `teddy`) pour que `AdminGuard`, l'intercepteur API et la securite backend parlent enfin le meme langage.

## Validation

### Backend admin supervision API

- `POST /api/auth/login` avec `username=admin`, `password=Admin1234` -> `200 OK`
- `GET /api/admin/supervision/games?user=admin` -> `200 OK`
  - 2 parties observees:
    - `test | DRAFTING | SNAKE | 2/5 | thibaut`
    - `Partie principale - 147 joueurs | ACTIVE | SNAKE | 3/3 | thibaut`
- `GET /api/admin/supervision/games?status=CREATING&user=admin` -> `200 OK`
  - resultat observe: `0` entree
- `GET /api/admin/supervision/games?user=thibaut` -> `403`

### Frontend route `/admin/games`

- Scenario `admin` avec session locale `admin@fortnite-pronos.com`
  - navigation vers `http://localhost:4200/admin/games`
  - appel observe: `GET http://localhost:8080/api/admin/supervision/games?user=admin` -> `200`
  - rendu observe:
    - titre `Supervision des parties`
    - `2` lignes affichees
    - statuts visibles: `DRAFTING`, `ACTIVE`
    - noms visibles: `test`, `Partie principale - 147 joueurs`
- Scenario `non-admin` avec session locale `thibaut@fortnite-pronos.com`
  - navigation vers `http://localhost:4200/admin/games`
  - redirection observee vers `http://localhost:4200/games`
  - aucun appel backend admin emis
- Scenario `anonyme`
  - navigation vers `http://localhost:4200/admin/games`
  - redirection observee vers `http://localhost:4200/login?returnUrl=%2Fadmin%2Fgames`
  - aucun appel backend admin emis

### Frontend unit tests ciblees

- `npx ng test --watch=false --browsers=ChromeHeadless --include src/app/core/services/user-context.service.spec.ts --include src/app/core/services/auth-switch.service.spec.ts --include src/app/core/services/team.service.spec.ts --include src/app/features/admin/games-supervision/admin-games-supervision/admin-games-supervision.component.spec.ts`
  - resultat: `48 SUCCESS`

## BMAD Handling

- La story 7.4 reste `done`, mais elle est maintenant revalidee en runtime local avec des identites front coherentes avec les comptes seedes backend.
- Le dernier follow-up review `L3` est ferme: le test CREATING verifie maintenant explicitement le statut retourne.
- Aucun besoin d'hebergement externe n'est rouvert par ce lot.
