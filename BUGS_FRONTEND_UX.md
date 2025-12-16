# Bugs Frontend / UX - Analyse et Plan de Correction

Date: 2025-12-10
Testeur: Thibaut
Environnement: localhost:4200

## ✅ BUGS BACKEND IDENTIFIÉS

### 1. CRITIQUE - Routing conflict `/api/games/{gameId}` vs `/api/games/my-games`
**Priorité**: 🔴 CRITIQUE

**Problème**:
```
ERROR: Invalid UUID string: my-games
ERROR: Invalid UUID string: mock-game-1
ERROR: Invalid UUID string: mock-game-2
```

Le endpoint `/api/games/{gameId}` attend un UUID mais reçoit des strings comme:
- `my-games`
- `mock-game-1`, `mock-game-2` (données de test)

**Cause racine**:
Spring route `/api/games/my-games` vers `getGameById(@PathVariable UUID gameId)` au lieu d'un endpoint dédié.

**Solution**:
1. Créer endpoint `/api/v1/games/my-games` ou `/api/games/user/{userId}`
2. Mettre `@GetMapping("/{gameId}")` APRÈS les endpoints spécifiques dans GameController
3. Utiliser `@GetMapping("/{gameId:[a-f0-9-]{36}}")` pour forcer le pattern UUID

---

## 🐛 BUGS FRONTEND IDENTIFIÉS

### 2. Menu déroulant affiche game sélectionnée alors que My Games affiché
**Priorité**: 🟠 HAUTE

**Problème**:
- Menu gauche montre "Game démo de Thibaut" sélectionnée
- Contenu affiche My Games avec message "choisir une game"
- État incohérent

**Solution**:
- Ajouter état "aucune game sélectionnée" dans dropdown
- Synchroniser sélection menu avec contenu affiché
- Afficher "Sélectionner une partie" si aucune game active

**Fichier**: `frontend/src/app/shared/components/main-layout/main-layout.component.ts`

---

### 3. Boutons My Games non fonctionnels
**Priorité**: 🟠 HAUTE

**Problème**:
Boutons sur cartes My Games ne fonctionnent pas:
- "View Details" → aucune action
- "Dashboard" → aucune action

**Labels**: En anglais (besoin traduction FR)

**Solution**:
1. Implémenter navigation vers game-detail
2. Implémenter navigation vers dashboard avec gameId
3. Traduire: "Détails" / "Tableau de bord"

**Fichier**: `frontend/src/app/features/game/game-home/game-home.component.html`

---

### 4. Dashboard UI cassée
**Priorité**: 🟡 MOYENNE

**Problème**:
- Mise en page non alignée
- Design pas conforme au thème noir & doré

**Solution**:
- Réviser CSS dashboard
- Appliquer design system Nexus
- Vérifier responsive

**Fichier**: `frontend/src/app/features/dashboard/dashboard.component.scss`

---

### 5. Team - Affichage équipes peu clair
**Priorité**: 🟡 MOYENNE

**Problème**:
Écran Team ne montre pas clairement:
- Équipe de Thibaut
- Équipe de Teddy
- Équipe de Marcel
- Liste des joueurs par équipe

**Solution**:
- Afficher clairement toutes les équipes de la game
- Lister les joueurs de chaque équipe
- Améliorer labels/titres

**Fichier**: `frontend/src/app/features/teams/`

---

### 6. Team → View Details → 404 + ancien design
**Priorité**: 🟡 MOYENNE

**Problème**:
URLs 404:
- `http://localhost:4200/teams/team1`
- `http://localhost:4200/teams/team1/edit`

Design: Ancien thème (pas noir & doré)

**Solution**:
1. Créer/corriger routes team-detail
2. Migrer design vers thème Nexus
3. Vérifier routing Angular

**Fichiers**:
- `frontend/src/app/features/teams/team-detail/`
- `frontend/src/app/features/teams/teams-routing.module.ts`

---

### 7. Leaderboard vide
**Priorité**: 🔵 BASSE

**Problème**:
Titre "classement des joueurs saison 2025" mais pas de contenu

**Solution**:
Option A: Masquer si pas prêt
Option B: Placeholder "Classement à venir"
Option C: Implémenter vrai leaderboard

**Fichier**: `frontend/src/app/features/leaderboard/simple-leaderboard.component.ts`

---

### 8. Profil - Rôle non affiché
**Priorité**: 🔵 BASSE

**Problème**:
Section "Informations profil" affiche "Rôle" mais valeur vide/inutile

**Solution**:
- Afficher vraiment le rôle (USER/ADMIN)
- OU supprimer cette info si non pertinente

**Fichier**: `frontend/src/app/features/profile/profile.component.html`

---

### 9. Boutons Profil non fonctionnels
**Priorité**: 🔵 BASSE

**Problème**:
Boutons sans action:
- "Modifier le profil"
- "Changer de mot de passe"
- "Voir les statistiques"

**Solution**:
- Implémenter routes/modals
- OU désactiver visuellement si pas prêt

**Fichier**: `frontend/src/app/features/profile/profile.component.ts`

---

### 10. Déconnexion partielle
**Priorité**: 🟠 HAUTE

**Problème**:
Bouton "Déconnexion" déconnecte seulement de la game, pas du site

**Solution**:
- Appeler vraie déconnexion (logout auth service)
- Rediriger vers `/login`
- Nettoyer token/session

**Fichier**: `frontend/src/app/core/services/auth.service.ts`

---

### 11. Create Game - Textes anglais
**Priorité**: 🟢 FACILE

**Problème**:
Textes en anglais:
- "New game"
- "Create your game in 10 seconds"

**Solution**:
Passer par TranslationService:
- "Nouvelle partie"
- "Crée ta partie en 10 secondes"

**Fichier**: `frontend/src/app/features/game/create-game/create-game.component.html`

---

### 12. Join Game - UX panneau vs page dédiée
**Priorité**: 🟢 FACILE

**Problème**:
"Rejoindre avec code" ouvre panneau dans même page

**Suggestion UX**:
Page dédiée comme Create Game avec même design

**Solution**:
- Créer route `/games/join`
- Composant dédié avec design Nexus
- Formulaire code d'invitation

**Fichier**: Nouveau composant `join-game.component.ts`

---

## 📋 PLAN D'ACTION

### Phase 1 - CRITIQUES (Faire en premier)
1. ✅ Corriger routing `/api/games/my-games` vs `/{gameId}`
2. ✅ Implémenter boutons My Games
3. ✅ Corriger déconnexion complète

### Phase 2 - HAUTES (UX importantes)
4. ✅ Menu déroulant état cohérent
5. ✅ Dashboard design fix

### Phase 3 - MOYENNES (Fonctionnalités)
6. ✅ Team affichage clair
7. ✅ Team View Details routes + design

### Phase 4 - FACILES (Polish)
8. ✅ Traductions FR (Create Game, boutons)
9. ✅ Join Game page dédiée

### Phase 5 - BASSES (Nice to have)
10. ✅ Leaderboard placeholder/implémentation
11. ✅ Profil rôle + boutons

---

## 🔧 FICHIERS À MODIFIER

### Backend
- `src/main/java/com/fortnite/pronos/controller/GameController.java`

### Frontend - Components
- `frontend/src/app/shared/components/main-layout/main-layout.component.ts`
- `frontend/src/app/features/game/game-home/game-home.component.ts`
- `frontend/src/app/features/dashboard/dashboard.component.ts`
- `frontend/src/app/features/teams/team-list/team-list.component.ts`
- `frontend/src/app/features/teams/team-detail/team-detail.component.ts`
- `frontend/src/app/features/leaderboard/simple-leaderboard.component.ts`
- `frontend/src/app/features/profile/profile.component.ts`
- `frontend/src/app/features/game/create-game/create-game.component.ts`

### Frontend - Services
- `frontend/src/app/core/services/auth.service.ts`
- `frontend/src/app/core/services/translation.service.ts`

### Frontend - Routing
- `frontend/src/app/features/teams/teams-routing.module.ts`
- `frontend/src/app/features/game/game-routing.module.ts`

---

## ⏱️ ESTIMATION

- Phase 1 (Critiques): 2-3h
- Phase 2 (Hautes): 2-3h
- Phase 3 (Moyennes): 3-4h
- Phase 4 (Faciles): 1-2h
- Phase 5 (Basses): 2-3h

**Total estimé**: 10-15h de développement
