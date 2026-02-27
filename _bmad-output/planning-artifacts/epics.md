---
stepsCompleted: [''step-01-validate-prerequisites'', ''step-02-design-epics'', ''step-03-create-stories'', ''step-04-final-validation'']
inputDocuments:
  - ''_bmad-output/planning-artifacts/prd.md''
  - ''_bmad-output/planning-artifacts/architecture.md''
  - ''_bmad-output/planning-artifacts/ux-design-specification.md''
---

# FortniteProject - Epic Breakdown

## Overview

Ce document fournit la decomposition complete epics + stories pour FortniteProject, en couvrant tous les FR/NFR du PRD avec des stories implementables en sequence.

## Requirements Inventory

### Functional Requirements
- FR-01: Le système peut scraper les leaderboards FortniteTracker pour 8 régions (Asia, Brazil, Europe, MiddleEast, NAC, NAW, Oceania, Total) via job `@Scheduled` (5h–8h), ~1 000 joueurs/région
- FR-02: Le système peut stocker les données brutes de scraping dans une table staging avant résolution
- FR-03: Le système peut résoudre un pseudo FortniteTracker → Epic Account ID via adapter hexagonal swappable (ex : Fortnite-API.com)
- FR-04: Le système peut marquer un joueur `UNRESOLVED` si la résolution échoue, sans bloquer le reste du pipeline
- FR-05: Le système peut stocker l'historique des pseudos depuis 2 sources distinctes : (1) FortniteTracker (display name + pseudo URL) et (2) pseudo in-game Fortnite Epic — avec horodatage pour chaque changement
- FR-06: Le système peut stocker les snapshots PR (append-only) par joueur × région × timestamp
- FR-07: Le système peut assigner automatiquement une région principale (majorité des points sur 12 mois) + admin override
- FR-08: Le système peut exécuter un job quotidien léger de détection de doublons potentiels (mêmes points, même région, pseudos proches)
- FR-09: Le système peut déclencher une alerte admin automatique si un joueur reste `UNRESOLVED` > 24h
- FR-10: Tout utilisateur connecté peut consulter le catalogue filtré par région (lecture seule, accessible à tous les rôles)
- FR-11: Tout utilisateur peut voir le profil détaillé d'un joueur : pseudo actuel, région principale, PR par région, snapshots horodatés
- FR-12: Tout utilisateur peut rechercher un joueur par pseudo dans le catalogue
- FR-13: Le catalogue affiche jusqu'à 1 000 joueurs par région, servi depuis le cache Spring (CacheConfig existant)
- FR-14: Le catalogue reste accessible pendant le draft (consultation + recherche en temps réel)
- FR-15: Un joueur authentifié peut créer une partie en configurant : région, nombre de joueurs par équipe, taille de tranche, période de compétition, mode de draft
- FR-16: Un créateur peut désactiver le système de tranches pour une partie (mode choix libre — aucun plancher de rang)
- FR-17: Un créateur peut inviter des participants via lien ou code d'invitation
- FR-18: Un participant peut rejoindre une partie via lien/code d'invitation
- FR-19: Un utilisateur peut supprimer son compte (libère ses joueurs dans toutes les parties en cours)
- FR-20: Un participant peut signaler un problème à l'admin via bouton dédié
- FR-21: En mode draft serpent, l'ordre de passage est déterminé aléatoirement au démarrage ; les participants choisissent à tour de rôle dans un ordre serpent (A→B→C→…→C→B→A→…) jusqu'à complétion de toutes les équipes
- FR-22: Si les tranches sont activées (modes serpent ET simultané), chaque slot a un plancher = `(slot-1) × taille_tranche + 1` ; le participant peut choisir un joueur de rang égal ou pire au plancher, jamais meilleur
- FR-23: Si les tranches sont activées, le participant peut cliquer "Recommander" pour obtenir automatiquement le meilleur joueur disponible au plancher du slot courant
- FR-24: Le backend valide chaque pick contre les règles de tranche actives (les deux modes) et rejette tout pick hors plancher avec message d'erreur explicite
- FR-25: En mode draft simultané, chaque round = tous les participants soumettent **1 choix** en même temps ; si aucun doublon, le joueur est attribué et on passe au slot suivant
- FR-26: En mode draft simultané, une animation révèle les sélections de tous les participants à chaque round ; en cas de doublon, le système attribue le joueur aléatoirement à un seul participant via animation
- FR-27: En mode draft simultané, le participant qui perd un doublon re-sélectionne son slot manquant ; il peut modifier ses autres choix non encore verrouillés ; la boucle continue jusqu'à résolution complète de toutes les équipes
- FR-28: Un joueur est exclusif à une équipe dans une partie donnée ; il peut être drafté dans plusieurs parties simultanément
- FR-29: L'admin peut assigner manuellement un joueur à n'importe quelle équipe d'une partie, en bypass des règles de tranche
- FR-30: L'admin peut retirer un joueur d'une équipe dans n'importe quelle partie
- FR-31: L'admin peut ajouter un joueur à une équipe dans n'importe quelle partie
- FR-32: Un participant peut proposer un swap solo à tout moment : remplacer l'un de ses joueurs par un joueur **libre** (non assigné dans cette partie), de rang strictement pire, dans la **même région** ; impossible de cibler un joueur déjà dans l'équipe d'un autre participant
- FR-33: Le backend valide le swap solo : joueur cible libre + même région + rang strictement pire — tout swap invalide est rejeté avec message explicite
- FR-34: Un participant peut proposer un trade mutuel à tout moment : **1 joueur contre 1 joueur** avec un autre participant, **sans restriction de région** (échange EU contre NAC : valide) ni de rang
- FR-35: Un trade requiert l'acceptation explicite du participant adverse avant d'être exécuté
- FR-36: Tout swap/trade est enregistré avec horodatage pour traçabilité et contestation
- FR-37: Le système calcule le score de chaque équipe = delta PR (valeur fin − valeur début de période configurée)
- FR-38: Le leaderboard affiche les équipes classées par delta PR décroissant, visible par tous les participants de la partie
- FR-39: L'admin ou le créateur peut configurer la période de compétition (date début / date fin)
- FR-40: Le leaderboard est mis à jour quotidiennement après le scraping nocturne (batch, pas temps-réel en v1)
- FR-41: L'admin peut consulter le tableau de bord pipeline : statut scraping par région, nb joueurs, dernière exécution, nb UNRESOLVED
- FR-42: L'admin peut résoudre manuellement l'Epic Account ID d'un joueur UNRESOLVED en 1 clic
- FR-43: L'admin peut corriger les données d'un joueur (pseudo, région principale)
- FR-44: L'admin peut override la région principale d'un joueur (remplace l'assignation automatique)
- FR-45: Le système envoie des alertes mail escalantes si des joueurs UNRESOLVED persistent > 24h
- FR-46: L'admin peut consulter les logs de scraping détaillés : succès/erreurs par région, nb joueurs, timestamp
- FR-47: L'admin peut accéder en supervision à toutes les parties en cours (vue globale)

### NonFunctional Requirements
- NFR-P01: Les pages catalogue se chargent depuis le cache (Mesure: < 2 secondes (cache chaud))
- NFR-P02: Les endpoints de draft/swap répondent (Mesure: < 500 ms)
- NFR-P03: Le scraping nocturne de 8 régions (~7 000 joueurs) se termine (Mesure: < 3 heures (fenêtre 5h–8h respectée))
- NFR-P04: Le leaderboard d'une partie se charge (Mesure: < 2 secondes pour 20 participants)
- NFR-P05: Le cache catalogue est réchauffé automatiquement post-scraping avant 8h (Mesure: Warmup exécuté avant ouverture du trafic — aucun utilisateur ne subit le cache froid)
- NFR-S01: Toutes les communications sont chiffrées (Mesure: HTTPS obligatoire en prod — 0 communication HTTP clair)
- NFR-S02: Authentification JWT (existant) (Mesure: Tokens signés, durée de vie limitée, invalidation à la déconnexion)
- NFR-S03: Les endpoints `/admin/**` sont protégés par rôle ADMIN (Mesure: 403 pour tout autre rôle)
- NFR-S04: La suppression de compte efface toutes les données personnelles identifiables (Mesure: Conformité RGPD minimale — 0 trace PII après suppression)
- NFR-S05: Les mots de passe sont hashés (bcrypt — existant) (Mesure: 0 mot de passe en clair en base)
- NFR-S06: Les actions d'administration de parties (assign/retrait/ajout joueur — FR-29/30/31) sont réservées au rôle ADMIN uniquement (Mesure: 403 si rôle ≠ ADMIN — un créateur de partie ne peut pas les exécuter)
- NFR-R01: L'application reste disponible si le scraping échoue (Mesure: Données J-1 en fallback — 0 downtime lié au pipeline)
- NFR-R02: Tout échec de scraping déclenche une alerte admin (Mesure: Délai max : 24h après l'échec)
- NFR-R03: Le pipeline de résolution est non-bloquant (Mesure: Un joueur UNRESOLVED ne bloque pas le reste du batch)
- NFR-R04: Les snapshots PR sont append-only (Mesure: Aucune donnée historique ne peut être écrasée ou supprimée)
- NFR-R05: Les données affichées portent un horodatage visible ("mis à jour il y a Xh") (Mesure: Alerte admin déclenchée si > 48h sans scraping réussi)
- NFR-SC01: L'application supporte la cible utilisateur MVP, y compris les pics draft (Mesure: 100 utilisateurs actifs simultanés + 20 soumissions simultanées en mode draft paramétré, sans dégradation notable)
- NFR-SC02: Le catalogue est servi sans requête DB par appel (Mesure: 100% des appels catalogue répondent depuis le cache en prod)
- NFR-SC03: L'ajout d'une 9ème région est possible sans refactoring majeur (Mesure: Changement limité à un adapter hexagonal — 0 modification domaine)
- NFR-I01: L'adapter de scraping (FortniteTracker) est swappable (Mesure: Remplacement = 1 fichier adapter, 0 modification domaine)
- NFR-I02: L'adapter de résolution (Fortnite-API.com) est swappable (Mesure: Remplacement = 1 fichier adapter, 0 modification domaine)
- NFR-I03: Migration Docker → Supabase prod sans modification code (Mesure: Changement de datasource URL uniquement)
- NFR-I04: Tout appel API externe (scraping + résolution) a un timeout configuré (Mesure: Défaut 10s, configurable — dépassement = UNRESOLVED + log, jamais de hang infini)
- NFR-M01: Tout nouveau code respecte l'architecture hexagonale existante (Mesure: ArchUnit en place — 0 violation autorisée)
- NFR-M02: Taille des classes et méthodes (Mesure: Max 500 lignes/classe, max 50 lignes/méthode (enforced))
- NFR-M03: Couverture de tests sur tout nouveau code (Mesure: ≥ 85% lignes — threshold enforced dans le build CI, pas seulement visé)

### Additional Requirements
- Projet brownfield: stack fixee (Java 21, Spring Boot 3.3, Angular 20), pas de starter template greenfield a initialiser.
- Architecture hexagonale obligatoire: domain/usecase/ports/adapters, adapters externes swappables pour scraping et resolution.
- Regles de migration DB incrementales via Flyway: creer/alterer uniquement les tables necessaires a la story courante.
- Conventions API: routes sous /api, ressources au pluriel, nomenclature stable backend/frontend.
- Cache catalogue obligatoire avec warmup post-scraping avant ouverture du trafic.
- Resilience I/O: timeout configurable sur appels externes, pipeline non bloquant en cas de resolution impossible.
- WebSocket draft requis pour synchro de statut, avec resynchronisation backend a la reconnexion.
- Securite admin stricte: /admin/** reserve ADMIN, journalisation des actions sensibles.
- UX: qualite equivalente desktop/mobile, responsive obligatoire sur parcours critiques.
- Accessibilite: cible WCAG 2.1 AA, navigation clavier, labels ARIA, gestion prefers-reduced-motion.
- Budget performance: catalogue et leaderboard < 2s, endpoints draft/swap < 500ms.
- Qualite d implementation: TDD, couverture >= 85% sur code modifie, limites taille classe/methode appliquees.

### FR Coverage Map
- FR-01: Epic 1 - Pipeline de donnees joueurs et resolution d identite
- FR-02: Epic 1 - Pipeline de donnees joueurs et resolution d identite
- FR-03: Epic 1 - Pipeline de donnees joueurs et resolution d identite
- FR-04: Epic 1 - Pipeline de donnees joueurs et resolution d identite
- FR-05: Epic 1 - Pipeline de donnees joueurs et resolution d identite
- FR-06: Epic 1 - Pipeline de donnees joueurs et resolution d identite
- FR-07: Epic 1 - Pipeline de donnees joueurs et resolution d identite
- FR-08: Epic 1 - Pipeline de donnees joueurs et resolution d identite
- FR-09: Epic 1 - Pipeline de donnees joueurs et resolution d identite
- FR-10: Epic 2 - Catalogue joueurs consulteable et performant
- FR-11: Epic 2 - Catalogue joueurs consulteable et performant
- FR-12: Epic 2 - Catalogue joueurs consulteable et performant
- FR-13: Epic 2 - Catalogue joueurs consulteable et performant
- FR-14: Epic 2 - Catalogue joueurs consulteable et performant
- FR-15: Epic 3 - Creation de parties et participation
- FR-16: Epic 3 - Creation de parties et participation
- FR-17: Epic 3 - Creation de parties et participation
- FR-18: Epic 3 - Creation de parties et participation
- FR-19: Epic 3 - Creation de parties et participation
- FR-20: Epic 3 - Creation de parties et participation
- FR-21: Epic 4 - Draft complet (serpent + simultane + regles + admin override)
- FR-22: Epic 4 - Draft complet (serpent + simultane + regles + admin override)
- FR-23: Epic 4 - Draft complet (serpent + simultane + regles + admin override)
- FR-24: Epic 4 - Draft complet (serpent + simultane + regles + admin override)
- FR-25: Epic 4 - Draft complet (serpent + simultane + regles + admin override)
- FR-26: Epic 4 - Draft complet (serpent + simultane + regles + admin override)
- FR-27: Epic 4 - Draft complet (serpent + simultane + regles + admin override)
- FR-28: Epic 4 - Draft complet (serpent + simultane + regles + admin override)
- FR-29: Epic 4 - Draft complet (serpent + simultane + regles + admin override)
- FR-30: Epic 4 - Draft complet (serpent + simultane + regles + admin override)
- FR-31: Epic 4 - Draft complet (serpent + simultane + regles + admin override)
- FR-32: Epic 5 - Trades et swaps securises
- FR-33: Epic 5 - Trades et swaps securises
- FR-34: Epic 5 - Trades et swaps securises
- FR-35: Epic 5 - Trades et swaps securises
- FR-36: Epic 5 - Trades et swaps securises
- FR-37: Epic 6 - Scoring et leaderboard
- FR-38: Epic 6 - Scoring et leaderboard
- FR-39: Epic 6 - Scoring et leaderboard
- FR-40: Epic 6 - Scoring et leaderboard
- FR-41: Epic 7 - Supervision admin et gouvernance des donnees
- FR-42: Epic 7 - Supervision admin et gouvernance des donnees
- FR-43: Epic 7 - Supervision admin et gouvernance des donnees
- FR-44: Epic 7 - Supervision admin et gouvernance des donnees
- FR-45: Epic 7 - Supervision admin et gouvernance des donnees
- FR-46: Epic 7 - Supervision admin et gouvernance des donnees
- FR-47: Epic 7 - Supervision admin et gouvernance des donnees

## Epic List

### Epic 1: Pipeline de donnees joueurs et resolution d identite
Mettre en production un pipeline fiable qui ingere les donnees classements, resout les identites joueurs et maintient la qualite des donnees sans bloquer l exploitation.
**FRs covered:** FR-01 a FR-09

### Epic 2: Catalogue joueurs consultable et performant
Permettre aux utilisateurs connectes de consulter, filtrer et rechercher un catalogue joueurs complet, rapide et exploitable pendant le draft.
**FRs covered:** FR-10 a FR-14

### Epic 3: Creation de parties et participation
Permettre a un joueur de creer une partie parametree, inviter des participants, gerer les entrees/sorties de compte et signaler un probleme.
**FRs covered:** FR-15 a FR-20

### Epic 4: Draft complet (serpent + simultane + regles + admin override)
Offrir un moteur de draft robuste couvrant les deux modes, les regles de tranche, la resolution de conflits et les operations admin de roster.
**FRs covered:** FR-21 a FR-31

### Epic 5: Trades et swaps securises
Permettre des swaps/trades conformes aux regles metier avec validation serveur stricte et traçabilite complete.
**FRs covered:** FR-32 a FR-36

### Epic 6: Scoring et leaderboard
Calculer les deltas PR sur une periode configurable et exposer un leaderboard fiable pour tous les participants.
**FRs covered:** FR-37 a FR-40

### Epic 7: Supervision admin et gouvernance des donnees
Donner a l admin une vision operationnelle complete du pipeline et des parties, avec actions de correction, supervision et alerting.
**FRs covered:** FR-41 a FR-47

## Epic 1: Pipeline de donnees joueurs et resolution d identite

Mettre en production un pipeline fiable qui ingere les donnees classements, resout les identites joueurs et maintient la qualite des donnees sans bloquer l exploitation.

### Story 1.1: Ingestion multi-regions vers staging

As an admin,
I want les leaderboards de 8 regions ingestes automatiquement vers une table staging,
So that le pipeline de donnees dispose d une base brute fiable chaque jour.

**FR refs:** FR-01, FR-02

**Acceptance Criteria:**

**Given** le job de scraping planifie est actif
**When** la fenetre 5h-8h demarre
**Then** les donnees des 8 regions sont collectées et stockees en staging
**And** la duree totale reste dans le budget NFR-P03.

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

### Story 1.2: Resolution pseudo FT vers Epic ID via adapter swappable

As an admin,
I want une resolution identite FT pseudo -> Epic ID via un port hexagonal,
So that l implementation du fournisseur externe reste remplaçable sans toucher le domaine.

**FR refs:** FR-03

**Acceptance Criteria:**

**Given** un lot staging contient des pseudos FortniteTracker
**When** le service de resolution traite le lot
**Then** les comptes resolus sont associes a un Epic ID
**And** l adapter de resolution peut etre remplace sans changer les regles metier (NFR-I02).

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

### Story 1.3: Gestion UNRESOLVED non bloquante

As an admin,
I want les joueurs non resolus marques UNRESOLVED sans casser le lot,
So that l ingestion continue meme en cas d echec partiel de resolution.

**FR refs:** FR-04

**Acceptance Criteria:**

**Given** certains pseudos ne sont pas resolvables
**When** le pipeline traite le batch
**Then** ces joueurs sont marques UNRESOLVED
**And** le reste des joueurs est traite normalement (NFR-R03).

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

### Story 1.4: Historisation alias et snapshots PR append-only

As an admin,
I want conserver l historique des pseudos et snapshots PR par region,
So that les evolutions joueurs restent auditable dans le temps.

**FR refs:** FR-05, FR-06

**Acceptance Criteria:**

**Given** un joueur apparait avec metadonnees mises a jour
**When** le pipeline persiste les donnees
**Then** les alias sources et snapshots PR sont historises
**And** aucune entree historique existante n est ecrasee (NFR-R04).

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

### Story 1.5: Region principale, dedoublonnage et alertes pipeline

As an admin,
I want une region principale auto, une detection de doublons et une alerte UNRESOLVED,
So that la qualite des donnees reste exploitable sans revue manuelle permanente.

**FR refs:** FR-07, FR-08, FR-09

**Acceptance Criteria:**

**Given** les snapshots des 12 derniers mois sont disponibles
**When** le job qualite quotidien s execute
**Then** la region principale est calculee et les doublons potentiels signales
**And** une alerte admin est emise pour tout UNRESOLVED > 24h.

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

## Epic 2: Catalogue joueurs consultable et performant

Permettre aux utilisateurs connectes de consulter, filtrer et rechercher un catalogue joueurs complet, rapide et exploitable pendant le draft.

### Story 2.1: Liste catalogue filtree par region

As a connected user,
I want consulter la liste joueurs avec filtre region,
So that je trouve rapidement des candidats pertinents pour ma partie.

**FR refs:** FR-10

**Acceptance Criteria:**

**Given** le catalogue contient des joueurs multi-regions
**When** l utilisateur applique un filtre region
**Then** seuls les joueurs de la region choisie sont affiches
**And** la consultation est disponible pour tous les roles connectes.

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

### Story 2.2: Detail joueur avec pseudo courant, region et snapshots

As a connected user,
I want ouvrir une fiche detaillee d un joueur,
So that je puisse evaluer son profil et son historique avant de drafter/trader.

**FR refs:** FR-11

**Acceptance Criteria:**

**Given** un joueur existe dans le catalogue
**When** l utilisateur ouvre la vue detail
**Then** le pseudo courant, la region principale, les PR par region et les snapshots s affichent
**And** les donnees sont horodatees pour transparence.

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

### Story 2.3: Recherche pseudo tolerant casse/accents

As a connected user,
I want rechercher un joueur par pseudo meme si je tape differemment,
So that la recherche reste efficace en contexte de draft rapide.

**FR refs:** FR-12

**Acceptance Criteria:**

**Given** des pseudos avec variations de casse/accents
**When** l utilisateur saisit une recherche
**Then** les correspondances pertinentes sont retournees
**And** l interface garde un temps de reponse compatible usage draft.

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

### Story 2.4: Cache catalogue et disponibilite pendant draft

As a connected user,
I want un catalogue rapide et disponible pendant les drafts,
So that je ne suis pas bloque par les traitements batch.

**FR refs:** FR-13, FR-14

**Acceptance Criteria:**

**Given** un scraping nocturne est termine
**When** le trafic utilisateur commence
**Then** le cache catalogue est rechauffe avant exposition
**And** les pages catalogue restent sous 2s en cache chaud (NFR-P01, NFR-P05).

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

## Epic 3: Creation de parties et participation

Permettre a un joueur de creer une partie parametree, inviter des participants, gerer les entrees/sorties de compte et signaler un probleme.

### Story 3.1: Creation de partie parametree

As a game creator,
I want configurer region, taille equipe, tranche, periode et mode de draft,
So that la partie correspond a mon format de competition.

**FR refs:** FR-15, FR-16

**Acceptance Criteria:**

**Given** un utilisateur authentifie
**When** il cree une partie avec les parametres requis
**Then** la partie est enregistree avec toutes les options metier
**And** le mode sans tranches peut etre active explicitement.

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

### Story 3.2: Invitations par lien/code et join flow

As a game creator,
I want inviter des participants via lien ou code,
So that je puisse remplir la partie sans friction.

**FR refs:** FR-17, FR-18

**Acceptance Criteria:**

**Given** une partie existe et accepte des participants
**When** un invite utilise le lien ou code valide
**Then** il rejoint la partie cible
**And** les controles d acces et de validite sont appliques.

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

### Story 3.3: Suppression de compte avec liberation roster

As a user,
I want supprimer mon compte proprement,
So that mes donnees personnelles soient retirees sans bloquer les parties en cours.

**FR refs:** FR-19

**Acceptance Criteria:**

**Given** un utilisateur possede des joueurs dans des parties actives
**When** il confirme la suppression de compte
**Then** ses donnees personnelles sont supprimees selon la politique
**And** ses slots joueurs sont liberes dans les parties concernees.

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

### Story 3.4: Signalement d incident a l admin

As a participant,
I want signaler un probleme depuis l interface,
So that l admin puisse investiguer rapidement.

**FR refs:** FR-20

**Acceptance Criteria:**

**Given** un participant detecte un incident en jeu
**When** il envoie un signalement
**Then** un evenement exploitable est journalise pour l admin
**And** l utilisateur recoit un feedback de prise en compte.

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

## Epic 4: Draft complet (serpent + simultane + regles + admin override)

Offrir un moteur de draft robuste couvrant les deux modes, les regles de tranche, la resolution de conflits et les operations admin de roster.

### Story 4.1: Orchestration draft serpent

As a participant,
I want un ordre serpent aleatoire puis alternant,
So that le draft reste equitable jusqu a completion des equipes.

**FR refs:** FR-21

**Acceptance Criteria:**

**Given** un draft serpent est initialise
**When** l ordre de passage est tire
**Then** les tours suivent A->B->...->B->A par round
**And** la progression continue jusqu a equipes completes.

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

### Story 4.2: Regles de tranche et recommandation

As a participant,
I want des picks valides selon le plancher de tranche et un bouton recommander,
So that je prenne des decisions conformes rapidement.

**FR refs:** FR-22, FR-23, FR-24

**Acceptance Criteria:**

**Given** les tranches sont actives sur la partie
**When** un participant soumet un pick
**Then** le backend rejette tout pick meilleur que le plancher autorise
**And** la recommandation retourne le meilleur joueur disponible conforme.

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

### Story 4.3: Soumission simultanee sans conflit

As a participant,
I want soumettre mon choix en mode simultane,
So that le round avance automatiquement quand il n y a pas de doublon.

**FR refs:** FR-25

**Acceptance Criteria:**

**Given** un round simultane est ouvert
**When** tous les participants soumettent un joueur distinct
**Then** chaque joueur est attribue a son auteur
**And** le slot suivant est ouvert pour tous.

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

### Story 4.4: Resolution de conflit en mode simultane

As a participant,
I want une resolution aleatoire explicite en cas de doublon,
So that l attribution reste transparente et je puisse reselectionner si je perds.

**FR refs:** FR-26, FR-27

**Acceptance Criteria:**

**Given** au moins deux participants ont soumis le meme joueur
**When** le moteur de conflit execute le tirage
**Then** un seul gagnant recoit le joueur
**And** chaque perdant repasse en reselection sur son slot manquant.

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

### Story 4.5: Exclusivite joueur par partie

As a participant,
I want garantir qu un joueur n appartient qu a une equipe dans ma partie,
So that il n y ait aucune incoherence de roster intra-game.

**FR refs:** FR-28

**Acceptance Criteria:**

**Given** un joueur est deja assigne dans une partie
**When** un autre pick vise ce meme joueur dans la meme partie
**Then** la tentative est refusee
**And** ce meme joueur peut rester selectable dans une autre partie distincte.

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

### Story 4.6: Operations admin de roster (assign/retrait/ajout)

As an admin,
I want ajuster manuellement les rosters d une partie,
So that je puisse corriger des situations exceptionnelles.

**FR refs:** FR-29, FR-30, FR-31

**Acceptance Criteria:**

**Given** un utilisateur avec role ADMIN
**When** il execute une action assign/retrait/ajout sur une equipe
**Then** l operation est appliquee avec traçabilite
**And** tout role non-admin recoit 403.

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

## Epic 5: Trades et swaps securises

Permettre des swaps/trades conformes aux regles metier avec validation serveur stricte et traçabilite complete.

### Story 5.1: Swap solo vers joueur libre valide

As a participant,
I want proposer un swap solo vers un joueur libre, meme region et rang pire,
So that j ajuste mon equipe dans le cadre autorise.

**FR refs:** FR-32, FR-33

**Acceptance Criteria:**

**Given** un participant choisit un joueur de remplacement
**When** la proposition de swap est soumise
**Then** le backend valide librete + region + contrainte de rang
**And** toute violation est rejetee avec message explicite.

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

### Story 5.2: Trade mutuel 1v1 avec acceptation adverse

As a participant,
I want proposer un trade 1v1 avec un autre participant,
So that l echange ne s execute qu apres consentement explicite de l autre partie.

**FR refs:** FR-34, FR-35

**Acceptance Criteria:**

**Given** une proposition de trade 1v1 est envoyee
**When** le participant adverse accepte
**Then** les joueurs sont echanges
**And** sans acceptation explicite, aucun transfert n est applique.

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

### Story 5.3: Journal d audit trades/swaps

As an admin,
I want historiser tous les swaps/trades avec horodatage,
So that les litiges puissent etre analyses et arbitres.

**FR refs:** FR-36

**Acceptance Criteria:**

**Given** un swap ou trade est cree, accepte ou refuse
**When** l evenement est traite
**Then** une trace horodatee complete est persistee
**And** l historique est consultable pour contestation.

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

## Epic 6: Scoring et leaderboard

Calculer les deltas PR sur une periode configurable et exposer un leaderboard fiable pour tous les participants.

### Story 6.1: Configuration periode de competition

As an admin or game creator,
I want definir date debut et date fin de competition,
So that le scoring se base sur une fenetre claire et partagee.

**FR refs:** FR-39

**Acceptance Criteria:**

**Given** une partie existe
**When** la periode de competition est enregistree ou modifiee
**Then** le systeme conserve la fenetre active de calcul
**And** la configuration est appliquee au moteur de score.

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

### Story 6.2: Calcul quotidien du delta PR

As a participant,
I want que le score de mon equipe soit calcule par delta PR sur la periode,
So that le classement reflète la progression reelle.

**FR refs:** FR-37, FR-40

**Acceptance Criteria:**

**Given** des snapshots PR sont disponibles sur la periode configuree
**When** le batch quotidien de score s execute
**Then** le delta PR de chaque equipe est recalcule
**And** le process reste compatible avec les contraintes de performance.

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

### Story 6.3: Affichage leaderboard ordonne

As a participant,
I want voir un leaderboard trie par delta decroissant,
So that je connaisse le classement courant de la partie.

**FR refs:** FR-38

**Acceptance Criteria:**

**Given** les scores equipes sont disponibles
**When** un participant ouvre la vue leaderboard
**Then** les equipes sont affichees par ordre decroissant de delta
**And** le chargement reste sous 2s pour 20 participants.

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

## Epic 7: Supervision admin et gouvernance des donnees

Donner a l admin une vision operationnelle complete du pipeline et des parties, avec actions de correction, supervision et alerting.

### Story 7.1: Dashboard pipeline par region

As an admin,
I want visualiser le statut scraping par region et la dette UNRESOLVED,
So that je detecte rapidement les incidents de production data.

**FR refs:** FR-41

**Acceptance Criteria:**

**Given** des runs pipeline ont ete executes
**When** l admin ouvre le dashboard pipeline
**Then** il voit statut par region, volumes, dernier run et nb UNRESOLVED
**And** les informations sont coherentes avec les journaux backend.

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

### Story 7.2: Resolution manuelle et correction fiche joueur

As an admin,
I want resoudre un joueur UNRESOLVED et corriger ses metadonnees,
So that la qualite du catalogue reste exploitable sans attendre un rerun complet.

**FR refs:** FR-42, FR-43, FR-44

**Acceptance Criteria:**

**Given** un joueur est en statut UNRESOLVED ou mal renseigne
**When** l admin soumet une resolution/correction
**Then** l Epic ID et les donnees modifiees sont persistes
**And** la region principale peut etre override explicitement.

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

### Story 7.3: Alerting escalant et logs scraping detailles

As an admin,
I want recevoir des alertes mail escalantes et consulter les logs de scraping,
So that je puisse investiguer les anomalies de facon proactive.

**FR refs:** FR-45, FR-46

**Acceptance Criteria:**

**Given** des UNRESOLVED persistent au dela du seuil
**When** la politique d alerte s applique
**Then** une notification escalante est envoyee
**And** les logs detailles succes/erreurs par region sont accessibles.

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste

### Story 7.4: Supervision globale des parties en cours

As an admin,
I want une vue globale de toutes les parties actives,
So that je puisse piloter l exploitation sans ouvrir chaque game individuellement.

**FR refs:** FR-47

**Acceptance Criteria:**

**Given** plusieurs parties sont en cours d execution
**When** l admin consulte la vue supervision
**Then** il accede a un etat consolide multi-parties
**And** les controles d acces /admin restent strictement reserves au role ADMIN.

**Given** une precondition invalide, une donnee incoherente ou un acces non autorise est detecte
**When** la meme action est soumise
**Then** le systeme rejette la requete avec un message d erreur explicite
**And** aucun etat invalide n est persiste
