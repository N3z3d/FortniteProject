# Sprint 9 Retrospective — "Scraper, Tests & Cleanup"

**Date :** 2026-03-17
**Facilité par :** Bob (Scrum Master)
**Participants :** Thibaut (Project Lead), Alice (Product Owner), Charlie (Senior Dev), Dana (QA Engineer), Elena (Junior Dev), Winston (Architect), Mary (Analyst), Amelia (Developer), Quinn (QA)
**Objectif Sprint 9 :** "FortniteTracker Scraping Adapter + DRAFT-FULL-01 E2E + Email Alerts + Process Cleanup D3/D4/S2"
**Elicitation :** 15 méthodes avancées appliquées (Party Mode)

---

## 1. Résumé du Sprint

### Métriques de livraison

| Métrique | Valeur |
|---|---|
| Stories livrées | 4 / 4 (100%) |
| Tests backend run | 2336 (0 nouvelles régressions) |
| Tests frontend (Vitest) | 2243 / 2243 (baseline maintenu) |
| Nouveaux tests | +32 (26 pipeline + 6 email alerts) |
| Incidents production | 0 |

### Stories livrées

| Story | Description |
|---|---|
| `sprint9-p0-cleanup` | D3+D4+S2 — critère checklist Sprint-N Parent Status + 5 zombies wont-do + §WebSocket Security Pattern doc |
| `sprint9-pipeline-rewrite` | FortniteTrackerScrapingAdapter (JSoup 1.22.1, 3 proxies rotation, @ConfigurationProperties, 26 tests) |
| `sprint9-e2-draft-full-e2e` | DRAFT-FULL-01 — test API-level serpent complet (compromis documenté : UI WebSocket multi-browser hors scope Playwright seul) |
| `sprint9-email-alerts` | EmailAlertService + UnresolvedAlertSchedulerService (@ConditionalOnProperty, cron 6h, 6 tests) |

---

## 2. Suivi des actions Sprint 8

| # | Action promise | Statut | Notes |
|---|---|---|------|
| F3 | sprint-status.yaml : sprint-8 status → done | ✅ Fait | Clôturé avant le début du Sprint 9 |
| D3 | checklist.md : critère Sprint-N Parent Status | ✅ Fait | sprint9-p0-cleanup — §Final Status Verification |
| D4 | Résoudre les zombies deferred-future | ✅ Fait | 5 items → wont-do ; sprint4-sec-r2 → done |
| E2 | DRAFT-FULL-01 E2E complet | ✅ Fait (compromis) | API-level, compromis documenté dans test + story |
| S2 | §WebSocket Security Pattern dans project-context.md | ✅ Fait | Section complète avec code Java+TypeScript |

**Score : 5/5 (100%) — deuxième sprint consécutif avec 100% des actions livrées.**

---

## 3. Ce qui a bien marché

**Alice (Product Owner):** "Le P0 cleanup a soldé des dettes depuis Sprint 3-4. Les zombies `deferred-future` : on l'a dit explicitement. Clarté mentale collective."

**Winston (Architect):** "La session brainstorming a produit 7 décisions D1-D7 documentées avant le premier commit. Une heure de réflexion a évité des jours de refactoring potentiel."

**Amelia (Developer):** "26 tests, 0 Spring context. `FortniteTrackerHtmlParser` et `ProxyUrlBuilder` sont des POJOs purs. `@Component` évite le CouplingTest constraint."

**Dana (QA Engineer):** "`@ConditionalOnProperty(alert.email.enabled=false)` — quand désactivé, le bean n'existe pas. Pattern correct pour features opt-in."

**Mary (Analyst):** "§WebSocket Security Pattern dans project-context.md : comportement documenté, code de référence, raisons du choix, guidance futures stories."

### Victoires clés
- 4/4 stories done (100%) — 4ème sprint consécutif parfait
- 5/5 actions Sprint 8 livrées (100% suivi)
- Pipeline scraping : 7 décisions architecturales documentées avant implémentation
- Zombies process Sprint 3-4 définitivement soldés
- 0 régression sur 2336 tests backend, 2243 frontend

---

## 4. Ce qui n'a pas bien marché / Challenges

**Dana (QA Engineer):** "DRAFT-FULL-01 : test API-level uniquement. Le flux UI WebSocket multi-browser n'est pas testé. La coordination STOMP entre 2 browsers peut casser silencieusement."

**Charlie (Senior Dev):** "Structurel : 35s Playwright + 2 contextes WS + latence SockJS = fragile sans infrastructure dédiée."

**Mary (Analyst):** "`PrIngestionScheduler` ne câble pas encore `PrRegionCsvSourcePort`. Le scraper est implémenté mais pas activé en production."

**Quinn (QA):** "sprint-9 `status: in-progress` avant la rétro — même pattern Sprint 8 répété malgré le critère D3."

**Winston (Architect) [Red Team]:** "L'adapter n'a jamais tourné avec de vraies données. Les fixtures de test ont 3 rows ; la vraie page en a 100. Structure HTML potentiellement différente par région."

**Dana [Red Team]:** "Données corrompues silencieuses : si FT change le format des scores (`1,234,567` avec virgules), le parser extrait `1`. Pas d'exception, juste des données fausses en base."

**Mary [Red Team]:** "Coût proxy non estimé : 10 régions × 4 pages × 8 tentatives max = 320 requêtes potentielles. ~$74/mois non budgétés."

### Challenges clés
- DRAFT-FULL-01 : flux UI WebSocket multi-browser non testé
- Pipeline : adapter non câblé, jamais validé sur données réelles
- Smoke test `rows > 0` absent — ingestion silencieuse possible
- Coût proxy réel non estimé
- sprint-9 parent status mis à jour en rétro (pas avant)

---

## 5. Leçons apprises

### L1 — Une session d'architecture documentée > code immédiat
**Contexte :** La story pipeline-rewrite aurait démarré sans brainstorming. Thibaut a identifié l'absence et l'a demandée.
**Leçon :** Pour toute story avec intégration externe ou nouveau pattern : documenter D1-DN avant la story.
**How to apply :** Dans create-story si intégration externe : brainstorming multi-agent avant template.

### L2 — Les tests E2E WebSocket multi-browser nécessitent une infrastructure dédiée
**Contexte :** DRAFT-FULL-01 — 2 contextes browser WebSocket simultanés en 35s impossible avec Playwright seul.
**Leçon :** Définir dans les ACs si l'approche est UI+WebSocket ou API-level. Décider avant la story.
**How to apply :** Template Dev Notes §E2E WebSocket Multi-User — déclarer l'approche et ses limites.

### L3 — Les features opt-in méritent @ConditionalOnProperty systématique
**Contexte :** `EmailAlertService` sans `@ConditionalOnProperty` chercherait `JavaMailSender` au démarrage.
**Leçon :** Toute feature dépendant d'une config externe optionnelle → `@ConditionalOnProperty`.
**How to apply :** Critère checklist §Technical : "Features opt-in avec dépendance externe → @ConditionalOnProperty obligatoire."

### L4 — Les adapters non câblés doivent générer une story backlog immédiatement
**Contexte :** `FortniteTrackerScrapingAdapter` implémente le port mais `PrIngestionScheduler` ne l'utilise pas.
**Leçon :** Un adapter sans câblage n'est pas livré — il est juste écrit.
**How to apply :** Dans Completion Notes dev-story : documenter explicitement "Integration gaps: ..."

### L5 — Les décisions architecturales implicites sont les plus dangereuses [NEW]
**Contexte :** D2 (RestTemplate dédié 20s) aurait pu être omis sans la session brainstorming.
**Leçon :** Une décision non documentée est une dette masquée. Le format D1-DN obligatoire pour toute intégration externe.
**How to apply :** Checklist §Architecture : "Toute intégration externe → au moins 3 décisions D1-DN documentées."

### L6 — Les compromis E2E doivent être dans project-context.md [NEW]
**Contexte :** Le compromis DRAFT-FULL-01 est dans le test et la story, mais pas dans project-context.md.
**Leçon :** Dans 3 mois, quelqu'un lira `draft-full-flow.spec.ts` sans contexte.
**How to apply :** Section §E2E Limitations dans project-context.md pour chaque compromis de scope E2E.

### L7 — Vérifier pom.xml/package.json avant de créer une nouvelle dépendance [NEW]
**Contexte :** `spring-boot-starter-mail` était déjà dans pom.xml. Feature email quasi-gratuite.
**Leçon :** Les dépendances dormantes sont des features à coût quasi-nul.
**How to apply :** Dans create-story si dépendance externe requise : vérifier pom.xml/package.json d'abord.

### L8 — Valider minimum viable scraping avant de généraliser [NEW]
**Contexte :** On a construit rotation 3 providers + 8 retries avant de valider 1 provider sur données réelles.
**Leçon :** Valider Scrapfly seul sur 1 région d'abord. Généraliser après validation.
**How to apply :** AC obligatoire pour tout adapter d'ingestion : "Au moins 1 région validée avec données réelles."

### L9 — Les invariants de validation appartiennent à l'adapter, pas à une story séparée [NEW]
**Contexte :** Thibaut futur (Hindsight) : smoke test `rows > 0` ajouté en Sprint 10, 2 semaines de cron silencieux perdues.
**Leçon :** La validation des invariants (`rows > 0`, `score > 0`) doit être dans l'adapter au moment de sa création.
**How to apply :** Dans dev-story §Definition of Done pour adapters d'ingestion : "Validation invariants incluse."

### L10 — `pagesPerRegion` doit être basé sur l'usage réel, pas une estimation [NEW]
**Contexte :** Socratic Questioning — `pagesPerRegion=4` (top 400) jamais justifié. Les parties utilisent top 50-100 max.
**Leçon :** Les paramètres de volume doivent être dérivés de l'usage réel du domaine.
**How to apply :** Valider `pagesPerRegion=1` (top 100) en dry-run. Augmenter si insuffisant.

---

## 6. Analyse de risques (Pre-mortem Sprint 10)

### Scénario A — Pipeline silencieux (HIGH RISK)
**Failure :** HTML FT change structure → JSoup parse 0 rows → cron s'exécute "avec succès" → 0 données ingérées → personne ne le sait.
**Prevention :** Smoke test `rows >= MIN_EXPECTED_ROWS` (ex: 10) post-parse. Log CRITICAL + skip ingestion si déclenché.

### Scénario B — Fingerprinting anti-bot (MEDIUM RISK)
**Failure :** FT détecte pattern régulier → ban progressif des 3 providers → plus de données.
**Prevention :** (1) User-Agent rotation dans `ProxyUrlBuilder`, (2) cache fichier CSV du dernier scraping réussi (D8), (3) plan C : ingestion manuelle admin.

### Scénario C — Données corrompues silencieuses (HIGH RISK)
**Failure :** FT change format scores (`1,234,567`) → parser extrait `1` → scores faux en base sans exception.
**Prevention :** Validation `0 < points < 10_000_000` avant insertion. Log WARNING + skip row si invalide.

### Scénario D — Dérive de coût proxy (MEDIUM RISK)
**Failure :** 10 régions × 4 pages × 8 retries = 320 req/run × 4 runs/jour = ~$74/mois non budgétés.
**Prevention :** `pagesPerRegion=1` par défaut. Mesurer coût réel 7 premiers jours. `maxRequestsPerRun` configurable.

### Scénario E — Email alerts spam (LOW RISK)
**Failure :** 50 entries UNRESOLVED en base → même email chaque matin pendant 30 jours.
**Prevention :** Ajouter `lastAlertSentAt` ou cooldown configurable dans `UnresolvedAlertSchedulerService`.

---

## 7. Scoring priorités Sprint 10 (Comparative Analysis Matrix)

| Option | Valeur produit | Risque si non fait | Effort | Urgence | **Total** |
|---|---|---|---|---|---|
| I1-dry-run — Validation proxy+parser réels | 5 | 5 | 2 | 5 | **17** |
| I1 — Câblage PrIngestionScheduler | 5 | 5 | 3 | 5 | **18** |
| Red Team hardening (smoke test + validation) | 4 | 5 | 2 | 4 | **15** |
| E2E multi-user infrastructure | 3 | 4 | 4 | 3 | **14** |
| D5/D6 — Checklist process | 2 | 2 | 1 | 4 | **9** |
| §E2E Limitations project-context.md | 1 | 2 | 1 | 3 | **6** |

**Recommandation :** I1-dry-run → I1 → Hardening en séquence. E2E multi-user = Sprint 11.

---

## 8. Décisions architecturales non prises (Sprint 10)

### D8 — Stratégie de cache si scraping échoue
**Décision → Option B : cache fichier CSV**
Stocker le résultat du dernier scraping réussi en `/tmp/pr-cache-{region}.csv`. Si scraping échoue, utiliser le cache.
Rationale : 0 migration Flyway, résilient, simple. Option C (cache DB) quand besoin d'audit.

### D9 — Stratégie multi-region parallèle vs séquentielle
**Décision → Séquentiel pour Sprint 10**
Parallèle si temps de scraping > 5 minutes mesuré en prod.
Rationale : prématuré d'optimiser sans métriques réelles.

### D10 — pagesPerRegion par défaut
**Décision → 1 page (top 100) pour le dry-run**
Les parties utilisent top 50-100 joueurs max. Augmenter après validation du besoin.
Rationale : divise coût et temps par 4 pour la validation initiale.

---

## 9. Patterns établis (réutiliser en Sprint 10)

### Architecture Decision Record (standard Sprint 9)
```markdown
| D# | Question | Décision | Raison |
|---|---|---|---|
| D1 | API vs Scraping ? | Scraping | API ne donne que lookup individuel |
```

### @ConditionalOnProperty pour features opt-in (standard Sprint 9)
```java
@Service
@ConditionalOnProperty(name = "feature.enabled", havingValue = "true", matchIfMissing = false)
public class OptionalFeatureService { ... }
```

### Smoke test post-ingestion (à implémenter Sprint 10)
```java
if (rows.size() < MIN_EXPECTED_ROWS) {
    log.error("CRITICAL: parse returned {} rows for region {} — possible HTML structure change", rows.size(), region);
    return; // ne pas écraser les bonnes données
}
```

### POJO pur pour logique métier sans Spring (standard Sprint 9)
```java
// Pas @Component — testable sans contexte Spring
class FortniteTrackerHtmlParser {
    List<ScrapedRow> parse(String html) { ... }
}
```

---

## 10. Action Items Sprint 10

═══════════════════════════════════════════════════════════
📝 ACTION ITEMS SPRINT 10 (issus de la rétro Sprint 9)
═══════════════════════════════════════════════════════════

### P0 — Fix immédiat (avant toute story)

| # | Action | Détail |
|---|---|---|
| F4 | sprint-status.yaml : sprint-9 status → done | ✅ Fait dans cette rétro |

### P1 — Pipeline (critique — valeur bloquée)

| # | Action | Détail |
|---|---|---|
| I1a | Dry-run manuel : valider parser + proxy sur données réelles | 1 région, 1 page, log résultats. Avant tout câblage cron. |
| I1b | Red Team hardening : smoke test + score validation | `rows >= 10` post-parse. `0 < points < 10_000_000`. Cache CSV fallback (D8). |
| I1c | Câbler PrIngestionScheduler → FortniteTrackerScrapingAdapter | Seulement après I1a + I1b validés. `pagesPerRegion=1` par défaut. |

### P2 — Tests E2E

| # | Action | Détail |
|---|---|---|
| E3 | 1 test Playwright multi-context minimal | Valider propagation STOMP event `PICK_MADE` sur 2ème browser. Pas le flux complet — juste l'event WebSocket. |

### P3 — Process & Documentation

| # | Action | Détail |
|---|---|---|
| D5 | §E2E Limitations dans project-context.md | Documenter compromis DRAFT-FULL-01 et limites Playwright multi-browser |
| D6 | §Configuration Production dans project-context.md | Comment activer email alerts en prod (SMTP, @ConditionalOnProperty) |
| D7 | Checklist §Technical : adapter DoD + @ConditionalOnProperty | "Adapter sans câblage = not done" + "Features opt-in → @ConditionalOnProperty" |
| D8 | Vérifier CGU FortniteTracker | Confirmer que le scraping est permis, documenter dans project-context.md |

### ⛔ Ne pas re-proposer
- Hébergement externe / staging → `wont-do` définitif (D4 Sprint 9)
- DRAFT-FULL-01 UI multi-browser complet → hors scope Playwright seul (voir E3 minimal)
- Sprint-N status oublié → critère D3 existe dans checklist.md

═══════════════════════════════════════════════════════════

---

## 11. Évaluation Sprint 9

| Dimension | Score | Notes |
|---|---|---|
| Livraison | 10/10 | 4/4 stories done (100%) |
| Qualité technique | 10/10 | 0 régression, 2336 tests backend, 2243 frontend |
| Tests | 9/10 | +32 tests ; DRAFT-FULL-01 UI WebSocket non testé |
| Architecture | 10/10 | Session brainstorming + 7 décisions D1-D7 documentées avant code |
| Process | 9/10 | D3/D4/S2 soldés ; sprint-9 parent status mis à jour en rétro seulement |
| Suivi actions Sprint 8 | 10/10 | 5/5 livrés |
| **Global** | **9.7/10** | 4ème sprint consécutif parfait — cohérence remarquable |

---

## 12. Preview Sprint 10

**Orientations naturelles Sprint 10 :**
1. **I1a — dry-run (P0)** — valider pipeline sur données réelles avant tout
2. **I1b — hardening (P1)** — smoke test, score validation, cache CSV
3. **I1c — câblage (P1)** — activer le cron seulement après I1a+I1b
4. **E3 — E2E WS minimal (P2)** — 1 test multi-context propagation STOMP
5. **D5-D8 — documentation (P3)** — §E2E Limitations, §Config Production, checklist

**Ce qu'on ne fait pas en Sprint 10 :**
- Hébergement externe / staging → wont-do définitif
- DRAFT-FULL-01 flux UI complet multi-browser → hors scope

---

*Rétrospective générée le 2026-03-17. 15 méthodes d'élicitation avancées appliquées (Party Mode). Prochaine action : `/bmad-bmm-sprint-planning` pour Sprint 10.*
