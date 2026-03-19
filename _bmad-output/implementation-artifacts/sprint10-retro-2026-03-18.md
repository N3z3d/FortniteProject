# Sprint 10 Retrospective — "Pipeline Production : Validation, Hardening & Câblage"

**Date :** 2026-03-18
**Facilité par :** Bob (Scrum Master)
**Participants :** Thibaut (Project Lead), Alice (Product Owner), Charlie (Senior Dev), Dana (QA Engineer), Elena (Junior Dev), Winston (Architect), Mary (Analyst), Amelia (Developer), Quinn (QA)
**Objectif Sprint 10 :** "Activer FortniteTrackerScrapingAdapter en production avec données réelles validées"
**Élicitation :** Party Mode — 9 agents, méthodes Comparative Analysis Matrix + Red Team + Hindsight

---

## 1. Résumé du Sprint

### Métriques de livraison

| Métrique | Valeur |
|---|---|
| Stories livrées | 5 / 5 (100%) |
| Tests backend (fin sprint) | 2 367 (0 nouvelles régressions) |
| Tests frontend Vitest | 2 185 / 2 206 (21 Zone.js pre-existing inchangés) |
| Nouveaux tests backend | +29 (dry-run: 7+3, hardening: 8+1, wiring: 6+1, docs: 0) |
| Nouveaux tests E2E | +1 (WS-01 Playwright) |
| Incidents production | 0 |

### Stories livrées

| Story | Description | Score matrice Sprint 9 |
|---|---|---|
| `sprint10-pipeline-dry-run` | Endpoint admin dry-run : `POST /api/admin/scraping/dry-run`, smoke ≥10 rows, score 1–9,999,999, 13 tests | 17 |
| `sprint10-pipeline-hardening` | CsvCachePort + InMemoryCsvCacheAdapter, User-Agent rotation (exchange), processRegion() rewrite, 8+ tests | 15 |
| `sprint10-pipeline-wiring` | runAllRegions() + POST /trigger, pagesPerRegion :4→:1, Optional<OrchestratorService>, 6 tests | 18 |
| `sprint10-e2e-websocket-minimal` | WS-01 Playwright : 2 × browser.newContext(), propagation STOMP PICK_MADE sans reload, 5 code review fixes | 14 |
| `sprint10-p0-process-docs` | project-context.md : §E2E Limitations, §Config Production, Adapter DoD, @ConditionalOnProperty, ToS | 6–9 |

---

## 2. Suivi des actions Sprint 9

| # | Action promise (Sprint 9) | Statut | Notes |
|---|---|---|---|
| I1-dry-run | Valider adapter sur données réelles (score 17) | ✅ Fait | sprint10-pipeline-dry-run — dry-run endpoint + 7 tests unitaires + 3 sécurité |
| I1 | Câbler PrIngestionScheduler → FortniteTrackerScrapingAdapter (score 18) | ✅ Fait | sprint10-pipeline-wiring — runAllRegions() + POST /trigger + pagesPerRegion :4→:1 |
| Hardening | Smoke test rows≥10, score validation, cache CSV fallback D8, User-Agent rotation (score 15) | ✅ Fait | sprint10-pipeline-hardening — CsvCachePort + InMemoryCsvCacheAdapter + exchange() |
| E2E multi-user | 1 test Playwright newContext() × 2 (score 14) | ✅ Fait | sprint10-e2e-websocket-minimal — WS-01, STOMP propagation, 5 fixes code review |
| D5/D6 checklist | §E2E Limitations + §Config Production + Adapter DoD (score 6–9) | ✅ Fait | sprint10-p0-process-docs — project-context.md mis à jour |
| §E2E Limitations | Section project-context.md pour compromis E2E (L6 Sprint 9) | ✅ Fait | §E2E Tests — Limitations et Patterns + timing STOMP caveat documenté |

**Score : 6/6 (100%) — troisième sprint consécutif avec 100% des actions livrées.**

---

## 3. Ce qui a bien marché

**Alice (Product Owner):** "Le sprint a livré dans l'ordre exact de la matrice Sprint 9 : dry-run → hardening → wiring. La séquence préventive a fonctionné : le dry-run a validé les données réelles avant d'activer le cron. C'est de la rigueur produit."

**Winston (Architect):** "Trois décisions architecturales critiques bien exécutées. D8 (cache CSV fallback via ConcurrentHashMap en mémoire, 0 migration Flyway) — simple, thread-safe, testable. D10 (pagesPerRegion=1 par défaut) — divise le coût proxy par 4 en validation initiale. Optional<PrIngestionOrchestrationService> — le pattern @ConditionalOnProperty est maintenant documenté comme référence canonique dans project-context.md."

**Charlie (Senior Dev):** "Le code review sur sprint10-pipeline-dry-run a trouvé 4 issues réelles (H1 message erreur générique, M1 List.copyOf immutabilité, M2 ResponseEntity<Object>, M3 détection lignes malformées) et a ajouté 2 tests. La revue adversariale fonctionne — 0 finding serait suspect sur une story de cette complexité."

**Amelia (Developer):** "sprint10-e2e-websocket-minimal : le pattern browser.newContext() × 2 est maintenant documenté et testé. C'est la première fois qu'on a un test E2E qui vérifie réellement la propagation STOMP entre 2 sessions indépendantes. La distinction newContext() vs newPage() est critique et maintenant dans project-context.md."

**Dana (QA Engineer):** "5 issues trouvées en code review sur WS-01 (M1 timing STOMP, M2 guard nom vide, M3 confirmation pick, L1 setTimeout redondant, L2 intervals explicites). Toutes fixées avant merge. La couverture de timing STOMP était le risque principal du test — M1 a ciblé exactement ça."

**Mary (Analyst):** "Le document p0-process-docs a consolidé 5 sections critiques dans project-context.md. Le code review H2 (ToS présenté comme vérifié alors que tracker.gg/legal retourne 403) était une vraie correction de rigueur factuelle. On ne présente pas une inférence comme un fait."

**Quinn (QA):** "2 367 tests backend, 0 nouvelles régressions. Chaque story a maintenu le contrat de non-régression. PrIngestionOrchestrationServiceTest a correctement été mis à jour (4e arg CsvCachePort, CSV fixtures 1→11 rows) sans casser les 3 tests existants."

**Elena (Junior Dev):** "La story p0-process-docs m'a permis de comprendre @ConditionalOnProperty en profondeur — le code Java est maintenant dans project-context.md avec les cas edge (Optional injection, @WebMvcTest behavior, two-constructor @Autowired rule)."

### Victoires clés
- 5/5 stories done (100%) — 5ème sprint consécutif sans régression
- 6/6 actions Sprint 9 livrées (100% suivi)
- Pipeline complet câblé : scraping → smoke check → cache → ingestion → trigger admin
- Pattern browser.newContext() documenté et testé (référence canonique E2E WebSocket)
- project-context.md augmenté de 5 sections (documentation durables, pas éphémères)
- Code review adversariale : 4+5+4 issues trouvées et corrigées sur les 3 stories les plus critiques

---

## 4. Ce qui n'a pas bien marché / Challenges

**Winston (Architect) [Red Team]:** "Le pipeline est câblé mais n'a JAMAIS tourné en production avec le vrai cron. `INGESTION_PR_SCHEDULED_ENABLED=true` n'est pas activé en `.env` — l'orchestrateur existe mais reste conditionnel à false. On a validé le dry-run mais pas un cycle cron complet : scraping → ingestion → persisted rows en base."

**Charlie (Senior Dev):** "`runAllRegions()` était initialement package-private mais `AdminScrapeController` est dans un package différent — erreur de compilation en dev. Symptôme classique : on écrit le code sans vérifier les visibilités cross-package. Fix trivial (→ public) mais le debug log indique que ça a coûté du temps."

**Dana (QA Engineer):** "WS-01 ne peut pas s'exécuter sans le Docker stack (app :4200 + backend :8080). Le test compile et apparaît dans `--list` mais ne peut pas passer en CI. C'est une E2E qui restera dans la catégorie 'manuelle ou Docker-only' jusqu'à qu'on ait un CI runner avec Docker Compose."

**Mary (Analyst):** "tracker.gg/legal retourne 403 à toute requête automatisée — on ne peut pas vérifier les CGU FortniteTracker programmatiquement. La section §FortniteTracker ToS est labellisée 'Inférence non-vérifiée' après code review H2. Un humain doit lire cette page manuellement."

**Quinn (QA):** "Le sprint-10 `status: in-progress` au moment de la rétro — même pattern que Sprint 8 et Sprint 9. C'est le 3ème sprint consécutif où ce champ n'est pas mis à jour avant la rétro."

**Alice (Product Owner) [Hindsight]:** "On a toutes les pièces du pipeline en place mais aucun monitoring post-activation. Si le cron s'exécute à 05h00 UTC et échoue silencieusement malgré le smoke check, qui est notifié ? L'email alert service existe (Sprint 9) mais est-il câblé sur les échecs du pipeline ?"

**Bob (Scrum Master) [Devil's Advocate]:** "Email alerts cooldown (Scénario E Sprint 9) non traité : si 50 UNRESOLVED entries persistent, le même email part chaque matin pendant 30 jours. `UnresolvedAlertSchedulerService` n'a pas de `lastAlertSentAt`. Ce scénario LOW RISK de Sprint 9 devient MEDIUM si l'ingestion génère des non-résolus en volume."

### Challenges clés
- Pipeline câblé mais cron jamais activé (INGESTION_PR_SCHEDULED_ENABLED=false en prod)
- WS-01 E2E non exécutable en CI (Docker stack requis)
- sprint-10 status: in-progress à la rétro (3ème fois)
- ToS FortniteTracker non vérifiable automatiquement
- Email alerts sans cooldown (risque spam montant)
- Monitoring post-activation du cron absent

---

## 5. Leçons apprises

### L1 — Le cache in-memory est la bonne première étape pour la résilience [CONFIRME]
**Contexte :** D8 Sprint 9 proposait "cache fichier CSV /tmp/". L'implémentation a choisi ConcurrentHashMap en mémoire — 0 I/O, 0 migration, thread-safe.
**Leçon :** In-memory > fichier pour la v1 de tout cache. Le fichier vient si besoin de persistence au redémarrage.
**How to apply :** Pour tout nouveau besoin de fallback/cache : commencer in-memory ConcurrentHashMap. Évoluer vers persistence si le besoin est prouvé en production.

### L2 — La visibilité package-private cross-package est une dette de design [NOUVEAU]
**Contexte :** `runAllRegions()` package-private → `AdminScrapeController` dans package différent → erreur compilation.
**Leçon :** Package-private ne devrait être utilisé que pour des méthodes appelées uniquement par des classes du même package. Si un controller doit appeler un service, la méthode doit être `public`.
**How to apply :** Dans dev-story : si une méthode est appelée depuis un autre package (controller → service), la déclarer `public` directement. Ne pas commencer package-private "pour voir".

### L3 — Le code review adversarial doit cibler les tests de timing E2E en priorité [CONFIRME]
**Contexte :** M1 sur WS-01 (timing STOMP) était le fix le plus critique du code review websocket — sans lui, le test aurait des faux positifs ou des timeouts selon la vitesse du serveur.
**Leçon :** Les tests E2E WebSocket ont un profil de risque élevé sur le timing. La revue doit toujours demander : "À quel moment exactement le STOMP subscription est-il établi ?"
**How to apply :** Checklist code review E2E WebSocket : (1) est-ce que l'observer attend que les données soient rendues (pas juste le DOM visible) avant que le picker agisse ? (2) est-ce que l'expect.poll a des intervals explicites ?

### L4 — Les inférences non vérifiées doivent être labellisées comme telles [NOUVEAU]
**Contexte :** Code review H2 sur p0-process-docs — la section ToS présentait des inférences comme des conclusions. tracker.gg/legal retourne 403 → impossible de vérifier.
**Leçon :** Toute affirmation dans la documentation qui n'a pas été vérifiée directement doit être labellisée "[Inférence non-vérifiée]" ou "[À vérifier manuellement]".
**How to apply :** Dans create-story pour stories de documentation : si une section requiert une vérification externe impossible à automatiser, la labelliser explicitement. Ne pas présenter l'absence de preuve contraire comme une confirmation.

### L5 — Le Spring Relaxed Binding est documenté mais rarement appliqué [NOUVEAU]
**Contexte :** Code review M1 sur p0-process-docs — la doc disait "décommenter application.properties". En réalité, `INGESTION_PR_SCHEDULED_ENABLED=true` comme variable d'environnement suffit — Spring Boot mappe automatiquement (relaxed binding).
**Leçon :** Spring Boot relaxed binding : `SNAKE_CASE_ENV_VAR` → `kebab.case.property` automatiquement. Ne jamais demander à l'utilisateur de "décommenter application.properties" pour activer une feature — l'env var suffit.
**How to apply :** Dans §Config Production documentation : montrer uniquement la commande env var. Mentionner application.properties seulement comme référence commentée pour la valeur par défaut.

### L6 — Les 3 sprints consécutifs avec sprint status 'in-progress' à la rétro sont un signal [CONFIRME]
**Contexte :** Sprint 8, 9, 10 : même pattern — status non mis à jour avant la rétro malgré le critère D3 dans la checklist.
**Leçon :** Ce n'est pas un oubli ponctuel — c'est une friction dans le process. Le critère D3 est dans la checklist mais pas automatisé.
**How to apply :** En création de rétro : première action = mettre sprint-N `status: done` dans sprint-status.yaml. Ce n'est pas optionnel. L'agent rétro le fait avant de générer le document.

---

## 6. Analyse de risques (Pre-mortem Sprint 11)

### Scénario A — Premier cron silencieusement raté (HIGH RISK)
**Failure :** INGESTION_PR_SCHEDULED_ENABLED=true activé → cron 05h00 UTC → HTML FT change structure → JSoup parse < 10 rows → smoke check déclenché → `"smoke_check_failed"` dans regionFailures → PERSONNE N'EST NOTIFIÉ car email alert n'est pas câblé sur les échecs de pipeline.
**Prevention :** Câbler `EmailAlertService` sur les `regionFailures` non vides dans `runAllRegions()`. Envoyer un email admin si au moins 1 région échoue.

### Scénario B — Email alerts spam (MEDIUM RISK — monte en priorité)
**Failure :** Pipeline génère 30 UNRESOLVED entries → `UnresolvedAlertSchedulerService` envoie le même email chaque matin pendant 30 jours.
**Prevention :** Ajouter `lastAlertSentAt` field dans l'état ou un cooldown configurable (ex: 24h entre deux emails sur le même batch d'unresolved).

### Scénario C — Coût proxy en dérive après activation (MEDIUM RISK)
**Failure :** pagesPerRegion=1 défaut → validation initiale OK → admin augmente à 4 → 8 régions × 4 pages × 8 retries max = 256 req/cycle → coût proxy $50+/mois sans visibilité.
**Prevention :** Ajouter logging du nombre de requêtes effectuées par cycle dans `runAllRegions()` ou `processRegion()`. Pas de code supplémentaire — juste un log INFO avec le compteur.

### Scénario D — WS-01 E2E donne faux résultat en CI (LOW RISK)
**Failure :** CI runner sans Docker stack → WS-01 ne peut pas s'exécuter → soit il passe (skip implicite), soit il échoue avec un message trompeur.
**Prevention :** Ajouter `test.skip(process.env.CI === 'true', 'Requires Docker stack')` dans WS-01. Documenter dans CI comments que les E2E WebSocket sont manuelle-only.

### Scénario E — FortniteTracker change la structure HTML (MEDIUM RISK)
**Failure :** FT déploie un redesign → `FortniteTrackerHtmlParser` ne trouve plus les rows → `fetchCsv()` retourne Optional.empty() pour toutes les régions → fallback CSV (cache mémoire) utilisé → données périmées ingérées sans alerte.
**Prevention :** Le cache mémoire est volatile (redémarrage = cache vide). En cas de redémarrage + HTML cassé, on est en `"no_data"` complet. Monitoring de la taille du cache actif serait utile.

---

## 7. Scoring priorités Sprint 11 (Comparative Analysis Matrix)

| Option | Valeur produit | Risque si non fait | Effort | Urgence | **Total** |
|---|---|---|---|---|---|
| Activer cron prod + monitoring email pipeline | 5 | 5 | 2 | 5 | **17** |
| Email alerts cooldown (lastAlertSentAt) | 3 | 4 | 1 | 4 | **12** |
| Log compteur proxy par cycle | 2 | 3 | 1 | 3 | **9** |
| WS-01 skip en CI (guard test.skip) | 2 | 2 | 1 | 3 | **8** |
| Vérification manuelle ToS tracker.gg | 3 | 2 | 1 | 2 | **8** |
| Backend pre-existing failures ×15 (FortniteTrackerServiceTddTest French msgs) | 2 | 1 | 3 | 2 | **8** |
| SEC-R2 WebSocket auth (JWT handshake) | 3 | 4 | 4 | 2 | **13** |
| sprint3-a6-jpa-legacy-migration (5 services) | 2 | 2 | 4 | 1 | **9** |
| E2E multi-user complet (Docker CI runner) | 3 | 3 | 5 | 2 | **13** |

**Recommandation Sprint 11 :** Activer le cron prod + câbler email alert sur pipeline failures (score 17) en priorité absolue. Email cooldown (score 12) en deuxième. SEC-R2 + E2E CI runner si capacity le permet (score 13 chacun).

---

## 8. Décisions architecturales non prises (Sprint 11)

### D11 — Stratégie monitoring pipeline post-activation
**Question :** Comment savoir si le cron 05h00 UTC a réussi sans regarder les logs ?
**Options :**
- A) Email on failure (câbler EmailAlertService sur regionFailures non vides) — effort 1
- B) Dashboard admin avec statut du dernier run (nouveau endpoint + composant) — effort 4
- C) Métriques Prometheus (counter ingestion_runs_total{status="success|failure"}) — effort 3
**Recommandation :** Option A d'abord (effort 1, valeur immédiate). Option B en Sprint 12 si l'admin veut une visibilité riche.

### D12 — Cooldown email alerts
**Question :** Faut-il stocker `lastAlertSentAt` en mémoire ou en base ?
**Options :**
- A) En mémoire (champ statique dans UnresolvedAlertSchedulerService) — volatile, reset au redémarrage
- B) En base (colonne `last_alert_sent_at` sur une table de config) — Flyway migration
- C) Fichier /tmp/ — compromis
**Recommandation :** Option A pour Sprint 11 (0 migration, effort minimal). Option B si le service redémarre souvent.

---

## 9. Patterns établis (réutiliser en Sprint 11)

### browser.newContext() — multi-user E2E WebSocket (standard Sprint 10)
```typescript
// CORRECT : sessions indépendantes (cookies/localStorage séparés)
contextA = await browser.newContext();
pageA = await contextA.newPage();
contextB = await browser.newContext();
pageB = await contextB.newPage();

// INTERDIT : partage la session → 1 seul utilisateur authentifié
pageA = await browser.newPage();
pageB = await browser.newPage();
```

### STOMP subscription timing caveat (standard Sprint 10)
```typescript
// INTERDIT : #player-list visible ≠ subscription STOMP active
await expect(pageB.locator('#player-list')).toBeVisible();
// CORRECT : attendre que les données soient rendues (player-card = WS subscription établie)
await expect(pageB.locator('.player-card').first()).toBeVisible({ timeout: 10_000 });
```

### Optional<T> injection pour beans @ConditionalOnProperty (standard Sprint 10)
```java
public AdminScrapeController(
    ScrapeLogService scrapeLogService,
    Optional<PrIngestionOrchestrationService> orchestrationService) {
  // Spring injecte Optional.empty() si le bean est absent
}
```

### expect.poll avec intervals explicites (standard Sprint 10)
```typescript
await expect.poll(() => pageB.locator('.player-card--taken').count(), {
  timeout: 15_000,
  intervals: [500, 1_000, 2_000, 2_000, 2_000, 2_000, 2_000],
}).toBeGreaterThan(0);
```

### Smoke check + cache dans processRegion() (standard Sprint 10)
```java
// 1. fetchCsv() vide → fallback cache
// 2. smoke check rows >= 10 → "smoke_check_failed"
// 3. smoke OK → csvCachePort.save() → ingestion
```

---

## 10. Action items Sprint 11

| # | Action | Priorité | Effort | Assigné |
|---|---|---|---|---|
| A1 | Câbler EmailAlertService sur regionFailures non vides dans runAllRegions() | P0 | 1 | Dev |
| A2 | Activer INGESTION_PR_SCHEDULED_ENABLED=true en .env + valider 1er cycle cron | P0 | 1 | DevOps/Lead |
| A3 | Ajouter cooldown 24h dans UnresolvedAlertSchedulerService (lastAlertSentAt en mémoire) | P1 | 1 | Dev |
| A4 | WS-01 : ajouter test.skip pour CI (guard process.env.CI) | P1 | 1 | Dev |
| A5 | Vérifier manuellement tracker.gg/legal (lecture humaine, documenter résultat) | P2 | 0.5 | Lead |
| A6 | sprint-10 status: done dans sprint-status.yaml (à faire EN PREMIER dans ce doc) | P0 | 0 | BMAD |

---

## 11. Score global Sprint 10

| Critère | Score | Commentaire |
|---|---|---|
| Livraison (5/5 stories) | 10/10 | 100%, 5ème sprint parfait consécutif |
| Qualité (0 régression) | 10/10 | 2367 tests, 0 nouvelles régressions |
| Code review | 9/10 | 4+5+4 findings pertinents sur les 3 stories critiques |
| Process (actions Sprint 9) | 10/10 | 6/6 (100%) livrées |
| Documentation | 9/10 | project-context.md +5 sections durables ; ToS non vérifiable (-1) |
| Suivi statut sprint | 7/10 | 3ème fois consécutive que sprint status reste in-progress jusqu'à la rétro |
| **Total** | **9.2/10** | Sprint de consolidation réussi — pipeline prêt pour l'activation |

---

*Rétro générée par les agents BMAD — Sprint 10 — 2026-03-18*
