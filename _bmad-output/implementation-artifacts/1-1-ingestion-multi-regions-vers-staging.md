# Story 1.1: Ingestion multi-regions vers staging

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As an admin,
I want les leaderboards de 8 regions ingestes automatiquement vers une table staging,
so that le pipeline de donnees dispose d une base brute fiable chaque jour.

## Acceptance Criteria

1. **Given** le job de scraping planifie est actif, **When** la fenetre 5h-8h demarre, **Then** les donnees des 8 regions sont collectees et stockees en staging, **And** la duree totale reste dans le budget NFR-P03.
2. **Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte, **When** la meme action est soumise, **Then** le systeme rejette la requete avec un message d erreur explicite, **And** aucun etat invalide n est persiste.
3. **Given** une region source echoue, **When** le pipeline poursuit le run, **Then** les autres regions sont traitees, **And** le run est trace en statut partiel/non-bloquant.

## FR refs

- FR-01
- FR-02

## NFR refs

- NFR-P03
- NFR-R01
- NFR-R03
- NFR-I01
- NFR-M01
- NFR-M03

## Tasks / Subtasks

- [ ] Task 1: Definir le flux d ingestion multi-regions et son contrat (AC: #1, #3)
- [ ] Subtask 1.1: Verifier/ajuster le point d entree ordonnance (`@Scheduled`) pour la fenetre nocturne.
- [ ] Subtask 1.2: Formaliser les entrees/sorties du flux d ingestion (region, lot brut, statut de run).
- [ ] Subtask 1.3: Garantir la poursuite multi-regions en cas d echec d une region.

- [ ] Task 2: Persister les donnees brutes en staging (AC: #1)
- [ ] Subtask 2.1: Mapper les champs du scraping vers le modele staging.
- [ ] Subtask 2.2: Ajouter/ajuster les operations de persistence bulk pour limiter le cout I/O.
- [ ] Subtask 2.3: Enregistrer les metadonnees de run (source, debut/fin, statut, erreur).

- [ ] Task 3: Gerer les erreurs fonctionnelles et techniques (AC: #2, #3)
- [ ] Subtask 3.1: Normaliser les erreurs de validation d entree (region invalide, payload incomplet).
- [ ] Subtask 3.2: Capturer les erreurs techniques de scraping/persistence sans stop global.
- [ ] Subtask 3.3: Exposer des logs exploitables pour l admin et le diagnostic.

- [ ] Task 4: Couvrir en tests (AC: #1, #2, #3)
- [ ] Subtask 4.1: Test nominal 8 regions vers staging.
- [ ] Subtask 4.2: Test erreur d entree avec rejet explicite et zero persistence invalide.
- [ ] Subtask 4.3: Test de resilience: echec region N, continuation des autres regions.
- [ ] Subtask 4.4: Test budget temporel (mock clock / fake scraper) pour valider le contrat de run.

## Dev Notes

- Cette story couvre l ingestion brute uniquement (pas la resolution Epic ID complete de l epic).
- Ne pas deriver vers des endpoints UI ici: focus pipeline d ingestion et traces d execution.
- Respecter l architecture hexagonale: orchestration via services/ports, pas de couplage direct domaine -> adapters concrets.
- Le run doit etre observabilise: statut run, erreurs, volumes par region.

### Architecture Compliance

- Pattern attendu: `IngestionOrchestrationService` (entree `@Scheduled`) delegue vers pipeline/service metier.
- Port attendu pour scraping: `ScrapingPort` (adapter swappable, NFR-I01).
- Non-blocant: ne pas stopper tout le batch sur echec local region.
- Respect strict des limites de taille classes/methodes et tests associes.

### Candidate File Structure to Touch

- `src/main/java/com/fortnite/pronos/service/ingestion/PrIngestionService.java`
- `src/main/java/com/fortnite/pronos/service/ingestion/PrIngestionRowProcessor.java`
- `src/main/java/com/fortnite/pronos/service/ingestion/PrIngestionCounters.java`
- `src/main/java/com/fortnite/pronos/service/ingestion/PrCsvParser.java`
- `src/test/java/com/fortnite/pronos/service/ingestion/PrIngestionServiceTddTest.java`
- `src/test/java/com/fortnite/pronos/service/ingestion/PrIngestionServiceRuntimePortsTest.java`

### Testing Requirements

- TDD strict: test rouge minimal -> implementation -> refactor.
- Couvrir nominal + erreurs d entree + continuation en mode partiel.
- Tests deterministes: pas de reseau reel, pas d horloge systeme non controlee.
- Garder la couverture au-dessus du seuil projet sur code modifie.

### Project Structure Notes

- Le repo contient deja des composants ingestion en `service/ingestion` ; preferer extension incremental plutot que re-ecriture totale.
- Si un port/adapter est introduit, conserver la frontiere claire entre domaine et infra.
- Conserver nommage explicite et convention existante des tests `*TddTest` / `*RuntimePortsTest`.

### References

- `_bmad-output/planning-artifacts/epics.md` (Story 1.1, FR refs, AC)
- `_bmad-output/planning-artifacts/prd.md` (FR-01, FR-02, NFR-P03, NFR-R01, NFR-R03)
- `_bmad-output/planning-artifacts/architecture.md` (sections pipeline async, ScrapingPort, INGESTION_RUNS, contraintes non-bloquantes)
- `_bmad-output/project-context.md` (regles TDD, quality gates, conventions stack)

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- Story context generated from BMAD artifacts (`epics.md`, `prd.md`, `architecture.md`, `project-context.md`).

### Completion Notes List

- Story context initialized and ready for `dev-story`.
- Added `PrIngestionOrchestrationService` with 5h-8h guard, 8-region orchestration and partial continuation.
- Added `PrRegionCsvSourcePort` as swappable CSV source port per region.
- Added `PrIngestionOrchestrationServiceTest` (red-green) for nominal 8 regions, partial on one region failure, and out-of-window skip behavior.
- Verified non-regression on ingestion tests (`PrIngestionServiceTddTest`, `PrIngestionServiceRuntimePortsTest`).

### File List

- `_bmad-output/implementation-artifacts/1-1-ingestion-multi-regions-vers-staging.md`
- `src/main/java/com/fortnite/pronos/service/ingestion/PrRegionCsvSourcePort.java`
- `src/main/java/com/fortnite/pronos/service/ingestion/PrIngestionOrchestrationService.java`
- `src/test/java/com/fortnite/pronos/service/ingestion/PrIngestionOrchestrationServiceTest.java`
