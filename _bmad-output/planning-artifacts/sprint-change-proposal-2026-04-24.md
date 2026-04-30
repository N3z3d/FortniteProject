# Sprint Change Proposal - 2026-04-24
**Statut :** Approuve
**Scope :** Modere - recadrage de perimetre + creation de 2 stories dediees
**Story declencheuse :** `sprint19-feat-invitation-code-advanced`
**Mode :** Batch
**Genere par :** Correct Course workflow (BMAD)
**Approbation utilisateur :** `yes` le 2026-04-24

---

## Section 1 - Resume du probleme

La story `sprint19-feat-invitation-code-advanced` est actuellement en `review` et son scope fonctionnel invitation-code reste globalement valide :
- pas de code auto a la creation ;
- consommation du code apres join reussi ;
- suppression manuelle createur-only ;
- pas de regression sur `sprint19-fix-join-redirect` ;
- pas de regression sur `sprint19-fix-invitation-code-security`.

Le code review BMAD a toutefois revele 2 sujets structurants qui ne doivent pas etre absorbes comme correctifs opportunistes dans cette story :

1. **Invariant domaine `Game` / participants**
   - `src/main/java/com/fortnite/pronos/domain/game/model/Game.java` simplifie `getTotalParticipantCount()` a `participants.size()`.
   - `src/main/java/com/fortnite/pronos/service/game/GameParticipantService.java` conserve `ensureCreatorParticipantPersisted(...)` avec un commentaire explicite de compatibilite de migration.
   - Le systeme n'a donc pas encore une source de verite unique sur "le createur est-il toujours un participant canonique ?".

2. **Contrat metier `createGame()` / `regionRules`**
   - `src/main/java/com/fortnite/pronos/service/game/GameCreationService.java` et `src/main/java/com/fortnite/pronos/core/usecase/CreateGameUseCase.java` injectent des regles par defaut (`PlayerRegion.ACTIVE_REGIONS`, 7 joueurs/region) quand `regionRules` est absent.
   - La story courante interdit explicitement de modifier le flux `createGame()` (`Task 2.7`).
   - La story historique `3-1-creation-de-partie-parametree` traite `regionRules` comme une configuration explicite de creation, pas comme un comportement implicite serveur.

**Nature du probleme :**
- ce n'est pas un bug invitation-code ;
- ce n'est pas un patch local a revert "vite fait" ;
- c'est un double ecart de **contrat** :
  - contrat d'invariant domaine pour les participants ;
  - contrat produit/API pour `regionRules`.

**Preuves concretes :**
- `GameParticipantService.ensureCreatorParticipantPersisted(...)` documente encore la compatibilite legacy.
- `CreateGameUseCaseTest.shouldPopulateDefaultRegionRulesWhenRequestOmitsThem()` verrouille deja le fallback `ACTIVE_REGIONS`.
- `sprint14-fix-draft-crash-tour2.md` documente aussi ce fallback comme un correctif runtime.
- `sprint19-feat-invitation-code-advanced.md` demande pourtant de ne pas changer `createGame()`.

---

## Section 2 - Analyse d'impact

### Checklist de changement

- [x] 1.1 Story declencheuse identifiee : `sprint19-feat-invitation-code-advanced`
- [x] 1.2 Probleme precise : 2 sujets structurants decouverts en review, hors scope de la story
- [x] 1.3 Preuves rassemblees : code, tests, story courante, story Sprint 14, sprint-status
- [x] 2.1 Epic courant encore realisable : oui, si la story invitation-code est strictement limitee a son scope
- [!] 2.2 Changement epic-level requis : oui, 2 stories dediees a creer
- [x] 2.3 Impact sur stories futures : oui, Epic 3 directement ; Epic 4/5/6 indirectement
- [x] 2.4 Aucun epic invalide ; aucun rollback de sprint necessaire
- [x] 2.5 Priorites a resequencer : oui, les 2 sujets doivent sortir de la review invitation-code
- [x] 3.1 Conflit PRD : non obligatoire si on choisit un contrat explicite a la frontiere
- [!] 3.2 Conflit architecture : oui, invariant participants + contrat API `regionRules` a expliciter
- [x] 3.3 Conflit UX : non immediat si les defaults restent explicites cote UI ; sinon oui
- [!] 3.4 Autres artefacts impactes : stories, sprint plan, tests, story notes historiques
- [x] 4.1 Option 1 Direct Adjustment : viable
- [ ] 4.2 Option 2 Potential Rollback : non viable
- [ ] 4.3 Option 3 PRD MVP Review : non requis
- [x] 4.4 Approche recommandee selectionnee
- [x] 5.1-5.5 Proposition et handoff prepares
- [!] 6.1-6.5 En attente d'approbation utilisateur

### Impact sur les epics et stories

| Artefact | Impact | Decision recommandee |
|---|---|---|
| `sprint19-feat-invitation-code-advanced` | Scope fonctionnel valide mais pollue par 2 sujets hors perimetre | **Limiter strictement au scope invitation-code** |
| Epic 3 - Creation de parties et participation | Directement impacte par les 2 sujets | **Ajouter 2 stories dediees** |
| Epic 4 - Draft | Impact indirect par le calcul des rounds, des counts et des regions configurees | Pas de changement d'epic, mais dependance indirecte a documenter |
| Epic 5/6 | Impact indirect via participant counts / disponibilite des games | Pas de changement d'epic |
| `sprint14-fix-participants-count` | Hypothese "createur toujours deja participant" devenue incomplte | Ajouter un follow-up explicite, pas de revert |
| `sprint14-fix-draft-crash-tour2` | A formalise un fallback runtime `regionRules` devenu un contrat implicite | Extraire la decision dans une story dediee |

### Impact sur les artefacts planning

| Artefact | Faut-il le modifier ? | Pourquoi |
|---|---|---|
| `prd.md` | **Non, pas obligatoirement** | Le PRD dit deja que le createur configure la region. Si on retient un contrat explicite a la frontiere, le PRD reste coherent. |
| `epics.md` | **Oui** | Il faut ajouter 2 stories Epic 3 dediees. |
| `architecture.md` | **Oui** | Les 2 sujets sont des decisions d'architecture/contrat, pas de simples details d'implementation. |
| `ux-design-specification.md` | **Non, pas maintenant** | Aucun changement UX requis si le serveur n'invente pas de defaults silencieux. |
| `sprint-status.yaml` | **Oui apres approbation** | Il faut tracer les 2 nouvelles stories et garder la story invitation-code sur sa lane propre. |

### Impact technique detaille

#### Sujet 1 - Participant canonique

Zones touchees :
- `Game`
- `GameParticipantService`
- `GameDomainFacade`
- `DraftDomainFacade`
- services qui utilisent `getTotalParticipantCount()`
- potentiellement les donnees legacy sans ligne createur dans `game_participants`

Risque si on ne recadre pas :
- modele de domaine simplifie sous une hypothese encore fausse ;
- migration legacy cachee par un helper de compatibilite ;
- prochain refactor ou prochain fix peut casser `isFull()`, les rounds de draft, le nombre de places, ou des lectures DTO.

#### Sujet 2 - Contrat `regionRules`

Zones touchees :
- `GameCreationService`
- `CreateGameUseCase`
- `CreateGameUseCaseTest`
- story `3-1-creation-de-partie-parametree`
- story `sprint14-fix-draft-crash-tour2`
- potentiellement l'UI de creation si elle compte sur un fallback implicite

Risque si on ne recadre pas :
- le serveur invente un comportement produit non valide explicitement ;
- le diff d'une story hors scope continue a redefinir le contrat de creation ;
- nouveaux tests et futurs devs prennent `ACTIVE_REGIONS` par defaut comme verite produit sans decision explicite.

---

## Section 3 - Approche recommandee

### Option retenue

**Option 1 - Direct Adjustment**, avec clarification d'architecture et creation de 2 stories dediees.

### Evaluation des options

| Option | Viabilite | Raison |
|---|---|---|
| Direct Adjustment | **Viable** | Permet de fermer proprement le scope invitation-code et de traiter les 2 sujets dans des stories propres. |
| Potential Rollback | **Non viable** | Un revert opportuniste ne resout ni l'invariant participants ni le contrat `regionRules`. |
| PRD MVP Review | **Non requis** | Le MVP reste atteignable ; il faut clarifier des contrats, pas revoir le produit. |

### Recommandation claire

1. **Oui**, `sprint19-feat-invitation-code-advanced` doit etre **limitee au scope invitation-code strict**.
2. **Oui**, les 2 sujets doivent devenir **2 stories dediees**.
3. **Non**, il ne faut pas renvoyer la story invitation-code en `DS` pour y absorber ces sujets.
4. **Oui**, il faut documenter l'invariant et le contrat dans l'architecture.
5. **Non**, il ne faut pas modifier `prd.md` ni `ux-design-specification.md` si on retient la ligne suivante :
   - `regionRules` doit etre **explicite a la frontiere API** ;
   - si l'UI veut des valeurs par defaut, elle les materialise explicitement avant `POST /api/games` ;
   - le serveur **ne synthese pas silencieusement** `ACTIVE_REGIONS`.

### Decisions de fond recommandees

#### A. Invariant participants

**Decision recommandee :**
`Le createur doit toujours etre un participant canonique persiste dans game_participants.`

**Sequence propre :**
1. auditer les games legacy sans createur participant ;
2. executer un backfill/migration explicite ;
3. prouver par tests et verification data que l'invariant est vrai ;
4. seulement ensuite supprimer la compatibilite `ensureCreatorParticipantPersisted(...)`.

#### B. Contrat `createGame()` / `regionRules`

**Decision recommandee :**
`regionRules` ne doit pas etre implicite cote serveur.

**Contrat cible propre :**
- soit le client envoie `regionRules` explicitement ;
- soit la requete est invalide ;
- mais le serveur ne transforme pas silencieusement "absent" en "toutes les regions actives".

**Consequence produit :**
- aucun changement UX n'est impose tant que l'UI envoie une map explicite ;
- si plus tard le produit veut "toutes les regions par defaut", cette decision devra etre visible dans PRD + UX, pas cachee dans le backend.

### Effort, risque, impact calendrier

| Sujet | Effort | Risque | Impact calendrier |
|---|---|---|---|
| Story courante reste borne | Low | Low | Aucun blocage |
| Story dediee `regionRules` | Medium | Medium | 1 story |
| Story dediee participants canoniques + migration | Medium/High | Medium/High | 1 story + verification data |

### Priorite recommandee

1. `createGame()` / `regionRules` - car c'est un contrat produit/API deja derive en runtime.
2. invariant participants canoniques - car c'est un chantier de migration a faire proprement, sans faux invariant.

---

## Section 4 - Propositions de changements detaillees

### 4.1 Story courante - `sprint19-feat-invitation-code-advanced.md`

**Section proposee :** `Pre-existing Gaps / Known Issues` (ou nouvelle section `Correct Course Follow-up`)

**OLD:**
```md
- Il n'y a pas de verrou concurrent explicite sur `Game`; si une protection supplementaire contre double redemption simultanee est jugee necessaire, la documenter clairement avant de l'etendre.
- Certaines traductions invitation-code existantes sont encore en anglais dans `es/pt`; cette story ne doit corriger que les nouvelles chaines visibles qu'elle introduit.
```

**NEW:**
```md
- Il n'y a pas de verrou concurrent explicite sur `Game`; si une protection supplementaire contre double redemption simultanee est jugee necessaire, la documenter clairement avant de l'etendre.
- Sujets volontairement exclus du scope et reroutes par Correct Course:
  - invariant canonique createur-participant + migration/backfill legacy;
  - contrat explicite `createGame()` / `regionRules` quand la requete ne le renseigne pas.
- Certaines traductions invitation-code existantes sont encore en anglais dans `es/pt`; cette story ne doit corriger que les nouvelles chaines visibles qu'elle introduit.
```

**Rationale :**
fermer proprement la story sur son scope reel et empecher toute reouverture opportuniste.

---

### 4.2 `epics.md` - ajouter 2 stories Epic 3

**OLD:**
```md
### Story 3.4: Signalement d'incident a l'admin
...
```

**NEW:**
```md
### Story 3.5: Contrat explicite de creation de partie pour `regionRules`

As a game creator,
I want the game creation API to use an explicit `regionRules` contract,
So that the backend does not invent silent defaults that change the gameplay configuration.

**Acceptance Criteria (proposes):**
- Given `POST /api/games` with explicit `regionRules`, When the game is created, Then the persisted region configuration matches the request exactly.
- Given `POST /api/games` without `regionRules`, When the request reaches the backend, Then the contract is handled explicitly according to the approved product decision, with no silent synthesis of `ACTIVE_REGIONS`.
- Given the UI wants a default template, When it submits the request, Then the default is materialized explicitly in the payload and covered by tests.

### Story 3.6: Invariant canonique createur-participant

As a system maintainer,
I want the game creator to always be persisted as a canonical participant,
So that participant counts, draft readiness, capacity checks and legacy compatibility all rely on one source of truth.

**Acceptance Criteria (proposes):**
- Given legacy games without creator row in `game_participants`, When the migration/backfill runs, Then the missing canonical rows are created safely.
- Given a game is created after the migration, When it is persisted, Then the creator is always a canonical participant row.
- Given the canonical invariant is proven, When the domain/services are simplified, Then `ensureCreatorParticipantPersisted(...)` is removed or retired explicitly and all participant counts remain correct.
```

**Rationale :**
ces 2 sujets deviennent enfin des stories explicites, au bon endroit, sous Epic 3.

---

### 4.3 `architecture.md` - ajouter 2 decisions explicites

**OLD:**
```md
| FR-15..20 | Existant - GameService, GameController | Existant |
```

**NEW:**
```md
### Game participation invariant

Canonical rule: the game creator is always persisted as a canonical participant row.
Migration rule: any legacy game missing the creator row must be backfilled before compatibility layers are removed.
Implementation rule: participant counts, draft readiness and capacity checks must rely on the same canonical source of truth.

### Game creation API contract - `regionRules`

Boundary rule: `POST /api/games` does not synthesize silent region defaults server-side.
If the product wants a default region template, the client must materialize it explicitly in the request payload.
Any future product decision to default to all active regions must be documented in planning artifacts before implementation.
```

**Rationale :**
le coeur du probleme est architectural et contractuel ; il doit vivre dans l'architecture, pas dans une story invitation-code.

---

### 4.4 `sprint-status.yaml` - creer 2 entrees dediees

**OLD:**
```yaml
sprint19-feat-invitation-code-advanced: review
```

**NEW:**
```yaml
sprint19-feat-invitation-code-advanced: review
sprint19-contract-create-game-region-rules: backlog
sprint19-migrate-canonical-game-participants: backlog
```

**Rationale :**
separer la lane de cloture invitation-code de la lane des 2 sujets structurants.

---

### 4.5 Story historique - `sprint14-fix-participants-count.md`

**Section proposee :** `Review Findings` ou `Completion Notes`

**OLD:**
```md
- [x] [Review][Decision] AC-3 divergence - RESOLVED: supprime le fallback +1 de `Game.getTotalParticipantCount()`, simplifie a `return participants.size()`.
```

**NEW:**
```md
- [x] [Review][Decision] AC-3 divergence - RESOLVED localement dans le scope de la story.
- [!] [Correct-Course][Follow-up] L'invariant "createur toujours deja participant canonique" n'est pas encore etabli globalement tant que `ensureCreatorParticipantPersisted(...)` reste actif pour la compatibilite legacy. Une story dediee de migration/backfill est requise avant de considerer ce sujet totalement ferme.
```

**Rationale :**
eviter qu'un lecteur confonde "simplification locale acceptee" avec "migration globale terminee".

---

## Section 5 - Handoff d'implementation

### Classification

**Scope recommande : Moderate**

Pourquoi :
- pas de replan produit majeur ;
- pas de rollback ;
- mais backlog reorganization + clarifications d'architecture + 2 nouvelles stories.

### Handoff recommande

#### Pour la story courante

`sprint19-feat-invitation-code-advanced`
- **Ne retourne pas en `DS`**
- reste sur sa lane `review -> done`
- pre-requis : synchroniser la story avec le correct-course (scope strict + exclusions explicites)

#### Pour les 2 nouveaux sujets

**Route BMAD exacte recommandee apres approbation :**

1. **PO/SM sync**
   - appliquer les updates planning/story/architecture approuves ;
   - garder la story invitation-code strictement borne.
2. **`CS`**
   - creer `sprint19-contract-create-game-region-rules`
   - creer `sprint19-migrate-canonical-game-participants`
3. **`DS` -> `CR`** sur `sprint19-contract-create-game-region-rules`
4. **`DS` -> `CR`** sur `sprint19-migrate-canonical-game-participants`

### Success criteria

- la story invitation-code est cloturee sans absorbtion hors scope ;
- les 2 sujets structurants existent comme stories explicites, chacune avec son propre AC et ses propres preuves ;
- l'architecture documente enfin :
  - la source de verite participant ;
  - le contrat `createGame()` / `regionRules`.

### Notes de sequencing

- Si le produit veut conserver "toutes les regions actives par defaut", alors **PRD + UX devront etre modifies avant `DS`** sur la story `regionRules`.
- Si on suit la recommandation de ce document (contrat explicite a la frontiere), alors **stories + architecture + sprint plan suffisent**.

---

## Conclusion

Le bon recadrage n'est pas de retoucher `sprint19-feat-invitation-code-advanced`, mais de la **borner**.

La recommandation est donc :
- **oui** a la cloture de la story invitation-code sur son scope strict ;
- **oui** a la creation de 2 stories dediees ;
- **non** a toute bidouille locale legacy ou a tout changement opportuniste hors scope.

---

*Document genere le 2026-04-24 via le workflow BMAD Correct Course.*
