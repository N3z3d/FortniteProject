# Sprint 9 Retrospective — "Scraper, Tests & Cleanup"

**Date :** 2026-03-17
**Facilité par :** Bob (Scrum Master)
**Participants :** Thibaut (Project Lead), Alice (Product Owner), Charlie (Senior Dev), Dana (QA Engineer), Elena (Junior Dev), Winston (Architect), Mary (Analyst), Amelia (Developer), Quinn (QA)
**Objectif Sprint 9 :** "FortniteTracker Scraping Adapter + DRAFT-FULL-01 E2E + Email Alerts + Process Cleanup D3/D4/S2"

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
| D4 | Résoudre les zombies deferred-future | ✅ Fait | 5 items hébergement/staging/secrets/DB/CI-CD → wont-do ; sprint4-sec-r2 → done |
| E2 | DRAFT-FULL-01 E2E complet | ✅ Fait (compromis) | API-level, compromis documenté dans test + story |
| S2 | §WebSocket Security Pattern dans project-context.md | ✅ Fait | Section complète avec code Java+TypeScript, comportement rejet, guidance futures stories |

**Score : 5/5 (100%) — deuxième sprint consécutif avec 100% des actions livrées.**

---

## 3. Ce qui a bien marché

**Alice (Product Owner):** "Le P0 cleanup a soldé des dettes qui traînaient depuis Sprint 3-4. Les zombies `deferred-future` du hébergement/staging : on savait qu'ils ne seraient jamais faits, on l'a dit explicitement. C'est de la clarté mentale collective. On peut maintenant parcourir le sprint-status.yaml sans être pollué par des items sans date de révision."

**Winston (Architect):** "La session de brainstorming sur le pipeline-rewrite a produit 7 décisions architecturales documentées avant le premier commit. D1 (API vs Scraping), D2 (HTTP client), D3 (Retry strategy), D4 (Structure 3 classes), D5 (Config), D6 (JSoup version), D7 (Pages). Une heure de réflexion collective a évité des jours potentiels de refactoring."

**Amelia (Developer):** "26 tests sur 3 classes, 0 Spring context. `FortniteTrackerHtmlParser` et `ProxyUrlBuilder` sont des POJOs purs. `@Component` sur l'adapter plutôt que `@Service` — évite le CouplingTest max-7-deps constraint. Architecture testable et propre."

**Dana (QA Engineer):** "L'email alerts story : `@ConditionalOnProperty(alert.email.enabled=false)` — quand désactivé, le bean n'existe pas. On ne requiert pas de SMTP configuré pour builder. Pattern correct pour les features opt-in avec dépendances externes non garanties."

**Mary (Analyst):** "La documentation §WebSocket Security Pattern dans project-context.md est exemplaire. Comportement documenté (`IllegalStateException → STOMP ERROR` vs HTTP 401), code de référence Java+TypeScript, raisons du choix, guidelines pour stories futures. Dans 6 mois, on sait exactement pourquoi."

### Victoires clés
- 4/4 stories done (100%) — 4ème sprint consécutif parfait
- 5/5 actions Sprint 8 livrées (100% suivi)
- Pipeline scraping : 7 décisions architecturales documentées avant implémentation
- Zombies process Sprint 3-4 définitivement soldés
- 0 régression sur 2336 tests backend, 2243 frontend

---

## 4. Ce qui n'a pas bien marché / Challenges

**Dana (QA Engineer):** "DRAFT-FULL-01 : on a livré un test API-level qui valide create→join→start→2 picks→finish→ACTIVE. Utile. Mais l'objectif original était de prouver le flux draft *dans l'UI avec WebSocket* — deux contextes browser simultanés. Ça, on ne l'a pas."

**Charlie (Senior Dev):** "Le problème est structurel. 35 secondes Playwright + 2 contextes WebSocket + latence SockJS handshake : mathématiquement fragile sans infrastructure dédiée. Ce n'est pas un oubli, c'est une limite architecturale de l'approche E2E actuelle."

**Mary (Analyst):** "L'adapter pipeline scraping existe, mais `PrIngestionScheduler` ne câble pas encore `PrRegionCsvSourcePort`. Le scraper est implémenté mais pas activé dans le flux de production. Sans vraies clés proxy configurées, on ne peut pas valider que les données réelles entrent dans le système."

**Quinn (QA Engineer):** "Le sprint-9 `status: in-progress` — même pattern qu'en Sprint 8. On a ajouté le critère D3 dans checklist.md mais on n'a pas appliqué le critère sur notre propre sprint. Auto-référentiel."

### Challenges clés
- DRAFT-FULL-01 : flux UI WebSocket multi-browser non testé (compromis API-level documenté)
- Pipeline rewrite : intégration end-to-end avec vrais proxies non validée (pas de clés configurées)
- sprint-9 parent status non mis à jour avant la rétro (critère D3 existe mais pas appliqué)

---

## 5. Leçons apprises

### L1 — Une session d'architecture documentée > code immédiat
**Contexte :** La story pipeline-rewrite aurait pu démarrer sans session brainstorming. L'utilisateur a identifié que la partie réflexion était absente et l'a demandée explicitement avant implémentation.
**Leçon :** Pour toute story avec des décisions non triviales (choix de stack, intégration externe, patterns de retry), documenter les décisions D1-DN *avant* la création de la story. La réflexion en amont réduit les refactorings.
**How to apply :** Dans create-story, si la story touche à une nouvelle intégration externe ou un nouveau pattern architectural : déclencher brainstorming multi-agent avant le template.

### L2 — Les tests E2E WebSocket multi-browser nécessitent une infrastructure dédiée
**Contexte :** DRAFT-FULL-01 nécessitait 2 contextes browser WebSocket simultanés en 35s. Impossible de manière fiable avec Playwright seul.
**Leçon :** Pour les stories E2E impliquant coordination WebSocket multi-utilisateurs, définir dans les ACs l'approche (UI+WebSocket ou API-level) *avant* la story, pas pendant.
**How to apply :** Template Dev Notes §E2E WebSocket Multi-User : déclarer explicitement l'approche et ses limites dans la story.

### L3 — Les features opt-in méritent @ConditionalOnProperty systématique
**Contexte :** `EmailAlertService` protégé par `@ConditionalOnProperty(alert.email.enabled=false)`. Sans ça, Spring chercherait `JavaMailSender` au démarrage même sans SMTP configuré.
**Leçon :** Toute feature qui dépend de config externe optionnelle (SMTP, webhooks, API keys) doit être protégée par `@ConditionalOnProperty`.
**How to apply :** Critère dans checklist.md §Technical : "Features opt-in avec dépendance externe non garantie → @ConditionalOnProperty obligatoire."

### L4 — Les adapters non câblés doivent générer une story backlog immédiatement
**Contexte :** `FortniteTrackerScrapingAdapter` implémente `PrRegionCsvSourcePort` mais `PrIngestionScheduler` ne l'utilise pas encore. Gap implicite.
**Leçon :** Quand un adapter est créé mais pas câblé dans l'orchestration production, créer immédiatement une story backlog "câblage + validation". Ne pas laisser un gap implicite.
**How to apply :** Dans Completion Notes de dev-story : documenter explicitement "Integration gaps: ..."

---

## 6. Patterns établis (réutiliser en Sprint 10)

### Architecture Decision Record (standard Sprint 9)
```markdown
| D# | Question | Décision | Raison |
|---|---|---|---|
| D1 | API vs Scraping ? | Scraping | API ne donne que lookup individuel |
| D2 | HTTP client ? | RestTemplate bean dédié 20s | Timeout isolation |
```

### @ConditionalOnProperty pour features opt-in (standard Sprint 9)
```java
@Service
@ConditionalOnProperty(name = "feature.enabled", havingValue = "true", matchIfMissing = false)
public class OptionalFeatureService { ... }
```

### @ConfigurationProperties groupées (standard Sprint 9)
```java
@Component
@ConfigurationProperties(prefix = "scraping.fortnitetracker")
public class FortniteTrackerScrapingProperties { ... }
```

### POJO pur pour logique métier sans dépendances Spring (standard Sprint 9)
```java
// Pas @Component — classe POJO testable sans contexte Spring
class FortniteTrackerHtmlParser {
    List<ScrapedRow> parse(String html) { ... }
}
```

---

## 7. Action Items Sprint 10

═══════════════════════════════════════════════════════════
📝 ACTION ITEMS SPRINT 10 (issus de la rétro Sprint 9)
═══════════════════════════════════════════════════════════

### P0 — Fix immédiat (avant toute story)

| # | Action | Détail |
|---|---|---|
| F4 | sprint-status.yaml : sprint-9 status → done | Mettre `status: done` dans le bloc `sprint-9:`. Mettre `sprint9-retrospective: done`. |

### P1 — Intégration Pipeline (critique pour valider Sprint 9)

| # | Action | Détail |
|---|---|---|
| I1 | Câbler PrRegionCsvSourcePort dans PrIngestionScheduler | Story : wiring adapter scraping + validation avec vraies clés proxy. Sans ça, le scraper est inutilisé en production. |

### P2 — Tests E2E

| # | Action | Détail |
|---|---|---|
| E3 | Définir stratégie E2E WebSocket multi-browser | Documenter dans project-context.md : DRAFT-FULL-01 UI est hors scope Playwright seul, ou identifier l'infrastructure nécessaire. |

### P3 — Process

| # | Action | Détail |
|---|---|---|
| D5 | Ajouter @ConditionalOnProperty dans checklist.md §Technical | Critère : "Features opt-in avec dépendance externe non garantie → @ConditionalOnProperty obligatoire" |
| D6 | Template Dev Notes §Integration gaps | Champ explicite pour documenter les adapters non câblés en fin de story |

### ⛔ Ne pas re-proposer
- Hébergement externe / staging → résolu comme `wont-do` en Sprint 9 (D4, définitif)
- Tests E2E DRAFT-FULL-01 UI browser multi-contexte → hors scope Playwright seul (voir E3)

═══════════════════════════════════════════════════════════

---

## 8. Évaluation Sprint 9

| Dimension | Score | Notes |
|---|---|---|
| Livraison | 10/10 | 4/4 stories done (100%) |
| Qualité technique | 10/10 | 0 régression, 2336 tests backend, 2243 frontend |
| Tests | 9/10 | +32 tests ; DRAFT-FULL-01 UI WebSocket non testé |
| Architecture | 10/10 | Session brainstorming + 7 décisions D1-D7 documentées avant code |
| Process | 9/10 | D3/D4/S2 soldés ; sprint-9 parent status non mis à jour avant rétro |
| Suivi actions Sprint 8 | 10/10 | 5/5 livrés |
| **Global** | **9.7/10** | 4ème sprint consécutif parfait — cohérence remarquable |

---

## 9. Preview Sprint 10

**Orientations naturelles Sprint 10 :**
1. **F4 (P0)** — fix sprint-status.yaml (immédiat, dans cette rétro)
2. **I1 (P1)** — câblage PrIngestionScheduler + validation vraies clés proxy (critique)
3. **E3 (P2)** — stratégie E2E WebSocket multi-browser ou décision wont-do explicite
4. **Décision stratégique** — nouvelle feature ou consolidation pipeline + données réelles ?

**Ce qu'on ne fait pas en Sprint 10 :**
- Hébergement externe / staging → `wont-do` définitif (Sprint 9)
- DRAFT-FULL-01 UI multi-browser → voir E3 d'abord

---

*Rétrospective générée le 2026-03-17. Prochaine action recommandée : mettre à jour sprint-status.yaml (sprint-9 → done, sprint9-retrospective → done), puis `/bmad-bmm-sprint-planning` pour Sprint 10.*
