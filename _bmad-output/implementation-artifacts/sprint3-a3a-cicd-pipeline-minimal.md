# Story Sprint3-A3a: Pipeline CI/CD minimal (GitHub Actions)

Status: ready-for-dev

## Story

As a developer,
I want un pipeline CI/CD GitHub Actions qui valide automatiquement chaque push,
so that les régressions sont détectées avant merge et le build reste toujours vert.

## Acceptance Criteria

1. **Given** un push sur n'importe quelle branche ou une PR, **When** le pipeline se déclenche, **Then** il exécute dans l'ordre : lint backend (Spotless) → tests backend (mvn test) → build Maven → lint/build Angular.

2. **Given** un test backend échoue, **When** le pipeline tourne, **Then** le job échoue avec les logs de test visibles dans GitHub Actions.

3. **Given** le pipeline passe, **When** on consulte les artifacts, **Then** le JAR backend et le dist Angular sont disponibles comme artifacts de build.

4. **Given** le pipeline s'exécute, **When** on consulte la durée, **Then** le cache Maven (~/.m2) et npm (node_modules) sont utilisés pour accélérer les runs suivants.

5. **Given** la configuration Docker est présente, **When** le pipeline tourne, **Then** un job optionnel `docker-build` valide que `docker build` réussit (sans push).

## Technical Context

### Fichier à créer

**`.github/workflows/ci.yml`** — Pipeline principal

### Structure du workflow

```yaml
name: CI — Fortnite Pronos

on:
  push:
    branches: ["**"]
  pull_request:
    branches: [main]

jobs:
  backend:
    name: Backend (Java 21 + Maven)
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'
      - name: Spotless check
        run: mvn spotless:check -B --no-transfer-progress
      - name: Tests + Build
        run: mvn verify -B --no-transfer-progress
      - name: Upload JAR
        uses: actions/upload-artifact@v4
        with:
          name: backend-jar
          path: target/*.jar

  frontend:
    name: Frontend (Node 20 + Angular)
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json
      - name: Install dependencies
        run: npm ci --silent
      - name: Lint
        run: npm run lint
      - name: Build
        run: npm run build
      - name: Upload dist
        uses: actions/upload-artifact@v4
        with:
          name: frontend-dist
          path: frontend/dist/

  docker-build:
    name: Docker build validation
    runs-on: ubuntu-latest
    needs: [backend, frontend]
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4
      - name: Build Docker image (no push)
        run: docker build --target production -t fortnite-pronos:ci .
```

### Contraintes importantes

- **Pas de `mvn test -DskipTests`** — les tests DOIVENT tourner
- **Pas de push Docker** — juste validation build
- **Cache Maven** : `~/.m2` via `actions/setup-java cache: maven`
- **Cache npm** : via `actions/setup-node cache: npm`
- Le job `docker-build` ne tourne que sur `main` pour éviter les builds Docker longs sur chaque PR
- Les pre-existing test failures backend (~15 connues) NE doivent PAS bloquer le CI — utiliser `-Dsurefire.failIfNoSpecifiedTests=false` ou accepter les failures connues avec un commentaire

### Note sur les pre-existing failures

Le projet a ~15 failures backend pre-existantes connues (GameDataIntegrationTest, FortniteTrackerServiceTddTest, etc.). Pour ne pas bloquer le CI sur ces échecs connus :

Option A : `mvn verify -B --no-transfer-progress` et documenter les failures acceptées.
Option B : `mvn verify -B --no-transfer-progress -Dtest='!GameDataIntegrationTest,!FortniteTrackerServiceTddTest'` pour exclure les tests connus comme cassés.

**Recommandation** : Option A avec un commentaire dans le YAML expliquant les failures connues. Le CI ne doit pas échouer sur des tests pre-existants.

### Fichiers à créer/modifier

1. `.github/workflows/ci.yml` — CRÉER
2. Optionnel : `.github/workflows/` (créer le dossier s'il n'existe pas)

### Tests de validation

Ce ticket crée de l'infrastructure CI (pas de code Java/TS), donc pas de tests unitaires. La validation est :
- Le fichier YAML est syntaxiquement valide (`yamllint` ou validation GitHub)
- Le pipeline peut se déclencher sur push
- Documenter dans sprint-status.yaml

### Règles qualité

- YAML bien formaté, indentation 2 espaces
- Commentaires explicatifs dans le YAML pour chaque section non-évidente
- Pas de secrets hardcodés

## Definition of Done

- [ ] `.github/workflows/ci.yml` créé et valide
- [ ] Jobs backend + frontend + docker-build définis
- [ ] Cache Maven et npm configurés
- [ ] Artifacts de build uploadés
- [ ] sprint-status.yaml mis à jour : `sprint3-a3a-cicd-pipeline-minimal: done`
