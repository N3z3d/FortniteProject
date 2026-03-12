# TECH-016 — Audit SonarQube — Quality Gate

> Audité le 2026-03-04
> Source : `tmp_sonar_backend_issues_latest.json` (SonarQube Community Edition)

---

## 1. Résultat global

| Métrique | Valeur |
|---|---|
| **Quality Gate** | ❌ ERROR |
| **Total issues** | 500 |
| **CODE_SMELL MAJOR** | 354 (71%) |
| **CODE_SMELL MINOR** | 146 (29%) |
| **Bugs** | 0 |
| **Vulnerabilités** | 0 |

> Bonne nouvelle : **0 bug, 0 vulnérabilité** — la dette est uniquement cosmétique/maintenabilité.

---

## 2. Top 15 règles violées

| # | Règle | Occurrences | Description |
|---|---|---|---|
| 1 | `java:S109` | 141 | **Nombres magiques** — utiliser des constantes nommées |
| 2 | `java:S1142` | 36 | Trop de `return` dans une méthode (>3) |
| 3 | `java:S6204` | 33 | Utiliser des text blocks Java 15+ à la place de `String.format` |
| 4 | `java:S5838` | 31 | Ordre des arguments d'assertion (expected, actual) inversé |
| 5 | `java:S5853` | 30 | Utiliser des messages descriptifs dans les assertions de tests |
| 6 | `java:S5778` | 27 | `assertThrows` doit être utilisé avec une exception précise |
| 7 | `java:S1449` | 22 | Utiliser `Locale` dans les opérations `String.toLowerCase/toUpperCase` |
| 8 | `java:S864` | 15 | Dépendance à la priorité des opérateurs — parenthèses manquantes |
| 9 | `java:S1448` | 13 | Méthodes avec trop de paramètres (>7) |
| 10 | `java:S135` | 13 | `continue` dans les boucles — lisibilité réduite |
| 11 | `java:S1166` | 11 | Exceptions `catch` sans re-throw ni log de la cause originale |
| 12 | `java:S3242` | 11 | Paramètre de méthode qui shadow un type |
| 13 | `java:S112` | 10 | `throw new RuntimeException` — utiliser exceptions spécifiques |
| 14 | `java:S1941` | 9 | Variable déclarée avant utilisation |
| 15 | `java:S4248` | 8 | `Pattern.compile` appelé dans boucle — pré-compiler |

**Analyse :** Le problème principal est `java:S109` (141 magic numbers) — souvent dans les seed/config/mapper. Les violations de tests (S5838, S5853, S5778 = 88 violations) sont un risque de qualité de tests.

---

## 3. Fichiers les plus problématiques

| Fichier | Issues |
|---|---|
| `service/seed/GameSeedService.java` | 20 |
| `config/TestDataInitializerConfig.java` | 19 |
| `service/CsvDataLoaderService.java` | 17 |
| `service/MockDataGeneratorService.java` | 16 |
| `service/admin/AdminDashboardService.java` | 14 |
| `service/leaderboard/PlayerLeaderboardService.java` | 12 |
| `core/error/ErrorCode.java` | 11 |
| `service/admin/GeoResolutionService.java` | 11 |
| `service/ingestion/PrIngestionService.java` | 11 |
| `config/CacheConfig.java` | 10 |

> Note : Les fichiers seed/config/test dominent le top 10. Le code métier productif est relativement propre.

---

## 4. Répartition par couche

| Couche | Issues (estimé) | Priorité |
|---|---|---|
| Tests Java | ~88 | MEDIUM — assertions mal formées |
| Seed/Config | ~55 | LOW — non-critique en prod |
| Services admin | ~45 | MEDIUM |
| Services core | ~40 | MEDIUM |
| Services ingestion | ~30 | MEDIUM |
| Domain/Adapters | ~25 | MEDIUM |
| Controllers | ~20 | LOW |
| Exceptions/Error | ~15 | MEDIUM |
| Autre | ~182 | LOW |

---

## 5. Plan de correction (roadmap)

### Sprint 3 — Batch 1 : Corrections automatisables (≈150 issues)

Ces violations sont mécaniques et corrigeables en lot :

1. **`java:S109` (141 magic numbers)** — Extraire en constantes `static final` nommées dans chaque classe concernée. Ex : `16`, `24`, `3600`, `500`, etc. Priorité : seed + config.

2. **`java:S1449` (22 Locale manquants)** — Remplacer `s.toLowerCase()` par `s.toLowerCase(Locale.ROOT)` dans 22 occurrences.

3. **`java:S864` (15 opérateurs)** — Ajouter parenthèses explicites.

### Sprint 3 — Batch 2 : Qualité des tests (≈88 issues)

4. **`java:S5838` (31) + `java:S5853` (30) + `java:S5778` (27)** — Réécrire assertions avec message descriptif + ordre correct `(expected, actual)` + `assertThrowsExactly`. Profiler par classe de test (top 5 classes).

### Sprint 3 — Batch 3 : Refactoring structurel (≈50 issues)

5. **`java:S1142` (36 returns)** — Extraire méthodes privées pour réduire les branches de retour.
6. **`java:S1166` (11 exceptions sans cause)** — Corriger les `catch (Exception e) { log.error(...) }` sans re-throw.
7. **`java:S112` (10)** — Remplacer `throw new RuntimeException` par exceptions métier spécifiques.

---

## 6. Conclusion

La base de code est **saine** (0 bug, 0 vuln). La dette SonarQube est principalement du code smell de niveau **maintenabilité** concentrée dans les couches seed/test. Corriger les 141 magic numbers + 88 violations de tests ferait passer le Quality Gate de ERROR → **WARNING ou SUCCESS**.
