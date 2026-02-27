# Code Review — Epic 2 Stories 2.1, 2.2, 2.3

**Date**: 2026-02-26
**Reviewer**: claude-sonnet-4-6 (adversarial review mode)
**Stories**: 2.1 Liste catalogue filtrée par région, 2.2 Détail joueur, 2.3 Recherche tolérante casse/accents
**Tests baseline avant**: 1990 | **après**: 2014 (+24) | Failures: 26 pre-existing, 0 nouveau

---

## Résumé exécutif

Implémentation propre et conforme. Pas de violation SOLID, pas de Clean Code issue, pas de faille de sécurité. Seul fix appliqué : coches de tâches `[ ]` → `[x]` dans les 3 story files (tracking BMAD).

---

## Findings

### [FIXED] F-001 — Task checkboxes non cochés (TRACKING)
**Sévérité**: MEDIUM (impact BMAD tracking, pas code)
**Fichiers**: 2-1-*.md, 2-2-*.md, 2-3-*.md
**Problème**: Toutes les sous-tâches `[ ]` restaient non cochées malgré l'implémentation complète.
**Fix appliqué**: Toutes les cases mises à `[x]` dans les 3 story files.

---

## Review détaillée par story

### Story 2.1 — CataloguePlayerDto + PlayerCatalogueService + GET /players/catalogue

| Critère | Résultat |
|---------|----------|
| SRP | ✅ Service 1 responsabilité (catalogue filtrage), DTO 1 responsabilité (mapping) |
| DIP | ✅ Dépendance sur `PlayerDomainRepositoryPort` (port abstrait), pas JPA |
| Méthodes ≤ 50 lignes | ✅ findByRegion 6 lignes, findAll 5 lignes |
| Classe ≤ 500 lignes | ✅ 83 lignes |
| Nommage | ✅ Explicite et cohérent |
| Tests | ✅ 7 tests (AC #1/#2/#3 + field mapping + cap 1000 + empty) |
| Sécurité | ✅ `@SecurityRequirement` hérité du controller class |
| Architecture | ✅ service/catalogue/ → domain port only |
| CouplingTest | ✅ 1 dépendance (max 7 autorisé) |

**Observation**: Le `try/catch IllegalArgumentException` dans `PlayerController.getCatalogue()` pour la conversion `PlayerRegion.valueOf()` est correct et produit bien le 400 attendu par AC #3.

### Story 2.2 — PlayerDetailDto + PlayerDetailService + findByPlayerRecent + GET /players/{id}/profile

| Critère | Résultat |
|---------|----------|
| SRP | ✅ Service dédié au profil détaillé, séparé de PlayerCatalogueService |
| DIP | ✅ 2 ports abstraits (PlayerDomainRepositoryPort + RankSnapshotRepositoryPort) |
| Méthodes ≤ 50 lignes | ✅ getPlayerDetail 7 lignes, from() 9 lignes |
| Classe ≤ 500 lignes | ✅ PlayerDetailService 44 lignes, PlayerDetailDto 34 lignes |
| Tests | ✅ 5 tests (PR multi-région, empty snapshots, 404, field mapping, latest PR wins same region) |
| Architecture | ✅ JPQL query sur RankSnapshotEntity avec ORDER BY ASC garanti |
| Logique merge | ✅ `(older, newer) -> newer` correct car stream trié ASC — le dernier pour chaque région gagne |
| CouplingTest | ✅ 2 dépendances |

**Observation**: La logique `Collectors.toMap(..., (older, newer) -> newer)` repose sur l'ordre ASC du stream. Cet ordre est garanti par le JPQL `ORDER BY r.snapshotDate ASC` dans `RankSnapshotJpaRepository.findByPlayerSince()`. Le test "keepsLatestPrValueForSameRegion" valide ce comportement explicitement. Pas de fragilité ici.

### Story 2.3 — searchByNickname() + normalize() + GET /players/catalogue/search

| Critère | Résultat |
|---------|----------|
| SRP | ✅ normalize() est une méthode utilitaire cohérente avec la responsabilité du service |
| Null safety | ✅ `if (s == null) return ""` avant Normalizer.normalize() |
| Zero dépendance | ✅ java.text.Normalizer est JDK standard |
| Blank guard | ✅ query blank/empty → retour immédiat List.of() sans appel repository |
| Tests | ✅ 12 tests (8 search + 4 normalize unit) |
| Performance | ✅ O(N) stream filter acceptable jusqu'à ~8000 joueurs (8 régions × 1000) |
| Injection | ✅ Param `q` jamais envoyé en JPQL — stream filter Java uniquement |

---

## Checklist qualité finale

- [x] SOLID respecté (SRP/OCP/LSP/ISP/DIP) — aucune violation
- [x] ≤ 500 lignes par classe / ≤ 50 lignes par méthode
- [x] 0 duplication, 0 code mort
- [x] Nommage explicite, conventions respectées
- [x] Tests unitaires ajoutés (24 nouveaux), cas limites couverts
- [x] Pas de nouvelle dépendance externe introduite
- [x] Architecture hexagonale respectée (service → domain port → adapter → JPA)
- [x] CouplingTest: max 4 deps sur PlayerController, 1 sur PlayerCatalogueService, 2 sur PlayerDetailService
- [x] Task checkboxes mis à jour dans les 3 story files
- [x] Tests: 2014 run, 26 failures pre-existing, 0 nouvelle régression

## Verdict

**APPROVED** — Les 3 stories sont correctement implémentées. Tous les AC couverts. Aucun fix de code requis.
