# Sprint 4 API Fortnite Runtime Validation

Story: `sprint4-api-fortnite-wiring-check`
Date: `2026-03-07`
Status: `closed`

## Observation

- Le runtime local repondait bien sur `GET /players/fortnite-search`, mais avec un corps vide.
- Les logs backend confirmaient: `Fortnite API key not configured - skipping lookup`.

## Root Cause

- Une entree `FORTNITE_API_KEY` etait bien presente dans `.env`.
- Le conteneur `fortnite-app-local` ne recevait pas cette variable, car `docker-compose.local.yml` ne la transmettait pas a l'application.

## Resolution

- `docker-compose.local.yml`
  - injection de `FORTNITE_API_KEY`
  - injection explicite de `FORTNITE_API_URL` pour les validations locales

## Validation

- `docker inspect fortnite-app-local ...` -> variable `FORTNITE_API_KEY` presente
- `GET /players/fortnite-search?name=bugha` -> `200 OK`
- Reponse live observee: `displayName=Bugha`, `wins=373`, `kills=28502`, `matches=8911`

## BMAD Handling

- Ce point reste dans la priorite locale Sprint 4 voulue par l'utilisateur: validation trades/scraping/API avant hebergement.
- Aucune action d'hebergement n'a ete ouverte.
