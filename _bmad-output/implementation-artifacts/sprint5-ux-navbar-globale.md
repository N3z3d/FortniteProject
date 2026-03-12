# Story Sprint5 — Navbar globale + Accès Admin via menu Profil (Option B)

Status: done

<!-- METADATA
  story_key: sprint5-ux-navbar-globale
  also_covers: sprint5-ux-admin-profile-dropdown
  branch: story/sprint5-ux-navbar-globale
  sprint: Sprint 5
  priority: P0
  Note: Cette story couvre les deux entrées sprint-status : sprint5-ux-navbar-globale ET
        sprint5-ux-admin-profile-dropdown (décision Party Mode unanime 8/8 agents : Option B pure).
        Elles sont implémentées ensemble car intrinsèquement liées.
-->

## Story

As a utilisateur ou administrateur connecté,
I want une barre de navigation globale persistante avec des liens Parties et Catalogue, et un menu déroulant Profil contenant un lien "Administration" conditionnel pour les admins,
so that je peux naviguer dans toutes les sections de l'application sans connaître les URLs, et que les admins accèdent au panneau d'administration directement depuis le menu.

## Acceptance Criteria

1. Le header contient une navbar globale visible sur toutes les pages protégées : lien **Parties** + lien **Catalogue** + bouton 👤 [username] ▼.
2. Le lien **Parties** navigue vers `/games` et est marqué actif (`routerLinkActive`) quand on est sur `/games/**`.
3. Le lien **Catalogue** navigue vers `/catalogue` et est marqué actif quand on est sur `/catalogue`.
4. Le bouton 👤 [username] ▼ ouvre un `MatMenu` dropdown avec : **Mon profil** (`/profile`), **Paramètres** (`/settings`), séparateur, **Déconnexion**.
5. Quand `isAdmin()` retourne `true`, le dropdown contient également **⚙️ Administration** (lien `/admin`) avec un séparateur visuel le distinguant des options joueur.
6. Quand `isAdmin()` retourne `false` (utilisateur normal), le lien **Administration** est **absent** du dropdown.
7. `UserContextService.isAdmin()` retourne `true` si et seulement si `getCurrentUser()?.role === 'Administrateur'`.
8. Les liens de navigation globale (Parties, Catalogue) sont **toujours visibles** quel que soit `selectedGame` — ils remplacent la navigation contextuelle actuelle qui n'apparaissait que si un jeu était sélectionné.
9. Sur mobile (≤ 768px), les labels texte des liens peuvent être masqués mais les icônes `mat-icon` restent visibles.
10. Toutes les nouvelles strings visibles sont traduites dans les **4 fichiers i18n** (`fr.json`, `en.json`, `es.json`, `pt.json`).
11. Un test Vitest vérifie : lien Admin **visible** quand `isAdmin()=true`, lien Admin **absent** quand `isAdmin()=false`, liens Parties et Catalogue présents, dropdown profile fonctionnel.

## Tasks / Subtasks

- [x] Task 1 : Ajouter `isAdmin()` à `UserContextService` (AC: #7)
  - [x] 1.1 : Ajouter la méthode `isAdmin(): boolean` qui retourne `this.getCurrentUser()?.role === 'Administrateur'`
  - [x] 1.2 : Ajouter le test Vitest correspondant dans `user-context.service.spec.ts` : cas admin (role='Administrateur'), cas joueur (role='Joueur'), cas null (pas d'utilisateur)

- [x] Task 2 : Mettre à jour le template `main-layout.component.html` (AC: #1–6, #8, #9)
  - [x] 2.1 : Ajouter les liens **Parties** et **Catalogue** dans la section `nav-header` — utiliser `[routerLink]` et `routerLinkActive="nav-link--active"`. Toujours visibles (pas conditionnel à `selectedGame`).
  - [x] 2.2 : Transformer la `user-section` actuelle (boutons individuels profil/settings/logout) en bouton `[matMenuTriggerFor]="profileMenu"` ouvrant un `<mat-menu #profileMenu>`.
  - [x] 2.3 : Le dropdown contient : `Mon profil` → `/profile`, `Paramètres` → `/settings`, `<mat-divider>`, `Déconnexion` → `logout()`.
  - [x] 2.4 : Ajouter `@if (isAdmin())` sur un bloc `<mat-divider>` + `<button mat-menu-item [routerLink]="'/admin'">⚙️ Administration</button>`.
  - [x] 2.5 : Responsive mobile : `.nav-label` masqué à ≤ 768px via SCSS, icônes conservées.
  - [x] 2.6 : Skip links a11y conservés, `[attr.aria-label]` utilisé sur le trigger bouton.

- [x] Task 3 : Mettre à jour `main-layout.component.ts` (AC: #7, #8)
  - [x] 3.1 : Méthode `isAdmin(): boolean` ajoutée, délègue à `userContextService.isAdmin()`.
  - [x] 3.2 : `userContextService` déjà injecté — pas de nouvelle dépendance. 13 deps conservées.
  - [x] 3.3 : `*ngIf="selectedGame"` conservé sur la nav contextuelle — les liens globaux sont dans une nav séparée `.global-navigation` toujours visible.

- [x] Task 4 : Mettre à jour les styles `main-layout.component.scss` (AC: #2, #9)
  - [x] 4.1 : `.nav-link--active { color: var(--nexus-gold); font-weight: 600 }` ajouté.
  - [x] 4.2 : `@media (max-width: 768px) { .nav-label { display: none } }` ajouté.
  - [x] 4.3 : `.admin-menu-item { color: var(--nexus-gold-light) }` ajouté. Total ~30 lignes additionnelles.

- [x] Task 5 : Mettre à jour les fichiers i18n (AC: #10)
  - [x] 5.1 : Clés `layout.nav.*` ajoutées dans les 4 fichiers (`fr/en/es/pt`) :
    - `layout.nav.games` — "Parties" / "Games" / "Partidas" / "Partidas"
    - `layout.nav.catalogue` — "Catalogue" / "Catalogue" / "Catálogo" / "Catálogo"
    - `layout.nav.myProfile` — "Mon profil" / "My profile" / "Mi perfil" / "Meu perfil"
    - `layout.nav.settings` — "Paramètres" / "Settings" / "Ajustes" / "Configurações"
    - `layout.nav.logout` — "Déconnexion" / "Logout" / "Cerrar sesión" / "Sair"
    - `layout.nav.administration` — "Administration" / "Administration" / "Administración" / "Administração"

- [x] Task 6 : Tests Vitest `main-layout.component.spec.ts` (AC: #11)
  - [x] 6.1 : `isAdmin() returns true when userContextService.isAdmin() is true` ✅
  - [x] 6.2 : `isAdmin() returns false when userContextService.isAdmin() is false` ✅
  - [x] 6.3 : `should show Parties and Catalogue links in global-navigation` ✅
  - [x] 6.4 : `should have profile menu trigger button in user-section` ✅
  - [x] Updated `isAdmin` added to `userContextService` spy. Pre-existing test `mat-icon-button` updated to reflect new dropdown design.

## Dev Notes

### Architecture de la solution (Option B pure — décision unanime Party Mode Sprint 5)

Le menu profil devient le point d'entrée unique pour l'administration. Pas de navbar admin séparée, pas de sidebar admin. L'accès se fait via : `👤 username ▼` → `⚙️ Administration`.

```
AVANT (user-section actuelle) :
  [profil] [settings] [logout]  ← boutons individuels

APRÈS (Option B) :
  [👤 username ▼]   ← MatMenu trigger
    → Mon profil (/profile)
    → Paramètres (/settings)
    ──────────────────── (separator)
    → ⚙️ Administration (/admin)  ← seulement si isAdmin()
    ──────────────────── (separator)
    → Déconnexion
```

### Fichiers à modifier

| Fichier | Action | Taille actuelle |
|---|---|---|
| `main-layout.component.html` | Modifier | ~80 lignes |
| `main-layout.component.ts` | Modifier | 523 lignes ⚠️ pré-existant >500 |
| `main-layout.component.scss` | Modifier (min) | 660 lignes ⚠️ pré-existant |
| `main-layout.component.spec.ts` | Modifier | ~X lignes |
| `user-context.service.ts` | Modifier | ~Y lignes |
| `user-context.service.spec.ts` | Modifier | ~Z lignes |
| `src/assets/i18n/fr.json` | Modifier | — |
| `src/assets/i18n/en.json` | Modifier | — |
| `src/assets/i18n/es.json` | Modifier | — |
| `src/assets/i18n/pt.json` | Modifier | — |

### Détail technique — UserContextService.isAdmin()

```typescript
// user-context.service.ts — ajouter cette méthode
isAdmin(): boolean {
  return this.getCurrentUser()?.role === 'Administrateur';
}
```

**Important :** La valeur `'Administrateur'` est hardcodée dans les profils mock (Sprint 5 connu gap `sprint5-arch-usercontext-jwt`). Ce n'est PAS un bug à corriger ici — c'est le comportement attendu tant que le JWT réel n'est pas câblé.

### Détail technique — Template (pattern Angular strict)

```html
<!-- CORRECT en mode strict Angular -->
<button [attr.aria-label]="'Menu ' + currentUser?.username">

<!-- INTERDIT en mode strict -->
<button aria-label="{{ 'Menu ' + currentUser?.username }}">
```

Pour les blocs conditionnels, utiliser `*ngIf` (compatible Angular 20) ou la syntaxe `@if {}` (Angular 17+ control flow) — les deux sont valides dans ce projet.

```html
<!-- Exemple dropdown admin conditionnel -->
<mat-menu #profileMenu="matMenu">
  <button mat-menu-item [routerLink]="'/profile'">
    <mat-icon>person</mat-icon>
    {{ t.t('layout.nav.myProfile') }}
  </button>
  <button mat-menu-item [routerLink]="'/settings'">
    <mat-icon>settings</mat-icon>
    {{ t.t('layout.nav.settings') }}
  </button>

  @if (isAdmin()) {
    <mat-divider></mat-divider>
    <button mat-menu-item [routerLink]="'/admin'" class="admin-menu-item">
      <mat-icon>admin_panel_settings</mat-icon>
      {{ t.t('layout.nav.administration') }}
    </button>
  }

  <mat-divider></mat-divider>
  <button mat-menu-item (click)="logout()">
    <mat-icon>logout</mat-icon>
    {{ t.t('layout.nav.logout') }}
  </button>
</mat-menu>
```

### Imports Angular Material déjà présents dans MainLayoutComponent

Ces imports sont **déjà déclarés** dans le tableau `imports:` du composant — ne pas les ajouter en doublon :
- `MatMenuModule` ✅
- `MatIconModule` ✅
- `MatDividerModule` ✅
- `MatToolbarModule` ✅
- `RouterLink`, `RouterLinkActive` — vérifier s'ils sont déjà présents, sinon ajouter.

### Méthode `isAdmin()` dans le composant

```typescript
// main-layout.component.ts — ajouter dans la classe
isAdmin(): boolean {
  return this.userContextService.isAdmin();
}
```

`userContextService` est **déjà injecté** dans MainLayoutComponent. Pas de nouvelle dépendance.

### Pattern de test Vitest pour le composant

```typescript
// Exemple pattern pour tester la visibilité du lien admin
it('should show Administration link when user is admin', async () => {
  const userContextSpy = TestBed.inject(UserContextService) as jasmine.SpyObj<UserContextService>;
  userContextSpy.isAdmin.and.returnValue(true);
  fixture.detectChanges();
  const adminLink = fixture.nativeElement.querySelector('[routerLink="/admin"]');
  expect(adminLink).toBeTruthy();
});

it('should hide Administration link when user is not admin', async () => {
  const userContextSpy = TestBed.inject(UserContextService) as jasmine.SpyObj<UserContextService>;
  userContextSpy.isAdmin.and.returnValue(false);
  fixture.detectChanges();
  const adminLink = fixture.nativeElement.querySelector('[routerLink="/admin"]');
  expect(adminLink).toBeNull();
});
```

### Pre-existing Gaps / Known Issues

- [KNOWN] `main-layout.component.ts` : 523 lignes — dépasse la limite de 500 lignes. Pré-existant, hors scope de cette story. **Ne pas refactoriser dans cette story.**
- [KNOWN] `main-layout.component.scss` : 660 lignes — pré-existant. Ajouter le minimum nécessaire (< 50 lignes).
- [KNOWN] `UserContextService.getAvailableProfiles()` hardcodé (mock login sans JWT réel) — documenté en Sprint 4, story dédiée `sprint5-arch-usercontext-jwt` planifiée. L'`isAdmin()` basé sur le role hardcodé est le comportement attendu pour ce sprint.
- [KNOWN] 21 tests Vitest frontend à patterns `fakeAsync`+`tick` (Zone.js) : pré-existants, non liés à cette story.
- [KNOWN] La navigation contextuelle actuelle (`*ngIf="selectedGame"`) affiche dashboard/teams/leaderboard/draft — cette logique est dans la **zone centrale du header**, distincte de la navbar globale à gauche. La navbar globale s'ajoute à côté du brand, pas à la place de la nav contextuelle. Vérifier le template existant avant de modifier.

### Project Structure Notes

```
frontend/src/app/
├── shared/components/main-layout/
│   ├── main-layout.component.ts        ← Ajouter isAdmin()
│   ├── main-layout.component.html      ← Ajouter nav links + transformer user-section
│   ├── main-layout.component.scss      ← Ajouter styles nav active + admin
│   └── main-layout.component.spec.ts  ← Ajouter 4 tests
├── core/services/
│   ├── user-context.service.ts         ← Ajouter isAdmin()
│   └── user-context.service.spec.ts    ← Ajouter 3 tests
└── assets/i18n/
    ├── fr.json                         ← Ajouter 6 clés layout.nav.*
    ├── en.json
    ├── es.json
    └── pt.json
```

### References

- [Source: sprint-status.yaml#Sprint5] — Phase 1 : `sprint5-ux-navbar-globale` + `sprint5-ux-admin-profile-dropdown`, décision Option B pure (Party Mode unanime 8/8 agents)
- [Source: sprint-4-retro-2026-03-08.md] — Bug L1 : "SpaController 404 + absence de lien admin dans la navbar"
- [Source: frontend/src/app/shared/components/main-layout/main-layout.component.ts] — 523 lignes, 13 dépendances injectées, `userContextService` déjà présent
- [Source: frontend/src/app/core/services/user-context.service.ts] — `getCurrentUser()`, `isLoggedIn()` exposés ; `isAdmin()` absent
- [Source: frontend/src/app/core/guards/admin.guard.ts] — `user?.role === 'Administrateur'` → valeur de référence pour isAdmin()
- [Source: memory/MEMORY.md] — Vitest setup, `fakeAsync` incompatible, `[attr.aria-label]` obligatoire, SCSS mixins

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- ✅ **Task 1** — `UserContextService.isAdmin()` ajouté + 3 tests Vitest (admin/joueur/null). Tous verts.
- ✅ **Task 2** — `main-layout.component.html` : `.global-navigation` avec Parties+Catalogue toujours visibles + transformation `user-section` → `[matMenuTriggerFor]="profileMenu"` + `@if (isAdmin())` pour lien admin.
- ✅ **Task 3** — `main-layout.component.ts` : méthode `isAdmin()` ajoutée (délègue à userContextService). Aucune nouvelle dépendance.
- ✅ **Task 4** — `main-layout.component.scss` : ~30 lignes ajoutées (`.nav-link--active`, `.admin-menu-item`, `@media 768px`). Total <700 lignes.
- ✅ **Task 5** — 6 clés `layout.nav.*` ajoutées dans `fr/en/es/pt`.
- ✅ **Task 6** — 4 nouveaux tests Vitest dans `main-layout.component.spec.ts` (`describe('navbar globale — Sprint 5 (AC #11)')`). Pre-existing test `mat-icon-button` mis à jour.
- ✅ **Tests baseline** : 2201/2223 frontend Vitest (22 failures = pré-existantes, inchangées).
- ✅ **Code review H1 fix** : tests CDK overlay corrigés → `fakeAsync` + `click()` + `tick(0)` + `document.body.querySelector` (le CDK overlay est dans `document.body`, pas dans `OverlayContainer.getContainerElement()` en Vitest/jsdom). 6/6 tests navbar globale ✅.
- ✅ **Code review M1 fix** : `aria-hidden="true"` ajouté sur les `<mat-icon>` dans le bouton trigger profil.
- ✅ **Code review M2 fix** : `[routerLink]="'/admin'"` → `[routerLink]="['/admin']"` (array syntax) pour profile/settings/admin.
- ✅ **Code review M3 fix** : test fragile `.user-section button` → sélecteur par routerlink dans global-navigation.
- ✅ **Code review L1 fix** : suppression usage `ng-reflect` dans les tests (remplacé par sélecteurs stables).

### File List

- `frontend/src/app/core/services/user-context.service.ts` — modifié (ajout `isAdmin()`)
- `frontend/src/app/core/services/user-context.service.spec.ts` — modifié (ajout `describe('isAdmin')` + 3 tests)
- `frontend/src/app/shared/components/main-layout/main-layout.component.html` — modifié (global nav + dropdown)
- `frontend/src/app/shared/components/main-layout/main-layout.component.ts` — modifié (ajout `isAdmin()`)
- `frontend/src/app/shared/components/main-layout/main-layout.component.scss` — modifié (+30 lignes)
- `frontend/src/app/shared/components/main-layout/main-layout.component.spec.ts` — modifié (4 nouveaux tests + spy update)
- `frontend/src/assets/i18n/fr.json` — modifié (6 clés `layout.nav.*`)
- `frontend/src/assets/i18n/en.json` — modifié (6 clés `layout.nav.*`)
- `frontend/src/assets/i18n/es.json` — modifié (6 clés `layout.nav.*`)
- `frontend/src/assets/i18n/pt.json` — modifié (6 clés `layout.nav.*`)
