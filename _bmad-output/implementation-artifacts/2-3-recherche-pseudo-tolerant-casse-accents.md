# Story 2.3: Recherche pseudo tolerant casse/accents

Status: done

## Story

As a connected user,
I want rechercher un joueur par pseudo meme si je tape differemment,
so that la recherche reste efficace en contexte de draft rapide (FR-12).

## Acceptance Criteria

1. **Given** un joueur avec le pseudo "Éric", **When** l'utilisateur cherche "eric", **Then** le joueur est retourne (tolerance aux accents).

2. **Given** un joueur avec le pseudo "NinjaKing", **When** l'utilisateur cherche "ninjaKING", **Then** le joueur est retourne (tolerance a la casse).

3. **Given** une requete vide ou blanks-only, **When** `GET /players/catalogue/search?q=` est appele, **Then** une liste vide est retournee.

4. **Given** aucun joueur ne correspond a la recherche, **When** la requete est soumise, **Then** une liste vide est retournee (pas d'erreur).

5. **Given** plus de 1000 correspondances, **When** la recherche est effectuee, **Then** le resultat est plafonne a 1000.

## Tasks / Subtasks

- [x] Task 1: Ajouter `searchByNickname(String query)` a `PlayerCatalogueService` (AC: #1, #2, #3, #4, #5)
  - [x] Normaliser la query: strip accents (NFD), lowercase, trim
  - [x] Normaliser chaque player.getNickname() de la meme facon
  - [x] Filtre: normalized nickname CONTAINS normalized query
  - [x] Query vide/blank -> retourner liste vide
  - [x] Cap a MAX_CATALOGUE_SIZE (1000)
- [x] Task 2: Ajouter `GET /players/catalogue/search?q={query}` a `PlayerController` (AC: #1-#5)
  - [x] Param `q` obligatoire
  - [x] Retourne `List<CataloguePlayerDto>`
- [x] Task 3: Tests `PlayerCatalogueSearchTest` (AC: #1-#5)

## Dev Notes

### Normalisation accent + casse
```java
static String normalize(String s) {
    String nfd = java.text.Normalizer.normalize(s.trim(), java.text.Normalizer.Form.NFD);
    return nfd.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "").toLowerCase(java.util.Locale.ROOT);
}
```
- `java.text.Normalizer` est dans le JDK — 0 nouvelle dependance
- NFD decompose les caracteres accentues en base + combining marks
- `\\p{InCombiningDiacriticalMarks}` supprime les combining marks (accents)

### Strategie de recherche
- Charger via `PlayerDomainRepositoryPort.findAll()` + stream filter en Java
- Acceptable jusqu'a ~8000 joueurs (8 regions x 1000) — en memoire
- Pas de nouvelle methode de port necessaire

### Ajout dans PlayerCatalogueService (methode 3)
Classe actuelle: 2 methodes (findByRegion, findAll). Ajout de searchByNickname reste coherent (meme SRP: servir les donnees catalogue). Toujours 1 dep.

### Previous story learnings
- Spotless: `mvn spotless:apply -q --no-transfer-progress && mvn test`
- `Player.restore(UUID, null, username, nickname, PlayerRegion, tranche, 2025, false)` pour les tests

### Project Structure Notes
- `src/main/java/com/fortnite/pronos/service/catalogue/PlayerCatalogueService.java` (modifie — ajout searchByNickname)
- `src/main/java/com/fortnite/pronos/controller/PlayerController.java` (modifie — ajout GET /players/catalogue/search)
- `src/test/java/com/fortnite/pronos/service/catalogue/PlayerCatalogueSearchTest.java` (nouveau)

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### File List

- `_bmad-output/implementation-artifacts/2-3-recherche-pseudo-tolerant-casse-accents.md`
- `src/main/java/com/fortnite/pronos/service/catalogue/PlayerCatalogueService.java` (modifie — ajout searchByNickname + normalize())
- `src/main/java/com/fortnite/pronos/controller/PlayerController.java` (modifie — ajout GET /players/catalogue/search)
- `src/test/java/com/fortnite/pronos/service/catalogue/PlayerCatalogueSearchTest.java` (nouveau — 12 tests: 8 search + 4 normalize)
