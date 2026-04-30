# Lessons

## 2026-04-02

- Quand l'utilisateur demande d'installer ou de reinstaller BMAD, ne pas supposer qu'un `quick-update` suffit.
- Verifier explicitement si l'utilisateur veut une reparation legere, une recompilation, ou une installation complete.
- Pour une installation complete sur une base existante, privilegier la voie officielle `npx bmad-method install` avec l'action complete adaptee, pas l'option de maintenance la plus conservative.

## 2026-04-03

- Quand un bug touche le timer, le websocket, ou un flux multi-comptes, ne jamais considerer la correction comme valide sur les seules suites backend/frontend.
- Avant de conclure qu'un fix draft temps reel est bon, exiger un re-test Docker manuel a 2 comptes et noter explicitement si cette validation reste a faire.

## 2026-04-04

- Quand l'utilisateur delegue explicitement l'execution du workflow BMAD, ne pas se limiter a donner un protocole ou des commandes a lancer.
- Reprendre directement la story BMAD active, executer les verifications et appliquer les corrections soi-meme jusqu'au prochain vrai blocage runtime ou produit.
- Quand l'utilisateur demande "quelle est la suite ?", ne pas supposer qu'il parle uniquement du prochain blocage technique immediat.
- Distinguer explicitement: suite de la story courante, suite du workflow BMAD (review/code-review), et prochaine story du sprint.

## 2026-04-23

- Quand l'utilisateur travaille via BMAD en francais, repondre en francais par defaut, y compris pour les sorties de review et les resumes de validation.
- Avant d'envoyer la conclusion d'une review BMAD, relire la langue de sortie et corriger immediatement si elle ne correspond pas au contexte projet/utilisateur.
- Avant de qualifier un changement comme hors-scope BMAD, verifier son origine dans `sprint-status.yaml` et les stories precedentes pour distinguer une vraie derive de perimetre d'une feature heritee deja livree.

## 2026-04-24

- Quand l'utilisateur rejette une correction percue comme une bidouille, reformuler le sujet en termes de contrat, d'invariant et de cible d'architecture au lieu de proposer un simple revert local.
- Si un finding touche une compatibilite de migration encore vivante, ne pas presenter sa preservation comme un "patch legacy"; expliciter la source de verite cible, la migration necessaire, puis la simplification finale.
- Ne pas laisser une story fonctionnelle introduire en douce un changement de contrat metier annexe; isoler clairement ce qui releve du scope de la story et ce qui doit faire l'objet d'une decision produit/architecture dediee.

## 2026-04-25

- Quand l'utilisateur demande la commande exacte "sans l'executer", ne lancer aucun test ni aucune commande associee, meme si cette execution serait utile a la verification.
- Dans ce cas, repondre avec la commande exacte seulement, puis attendre une instruction explicite avant toute action locale.
