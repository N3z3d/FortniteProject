# Story Sprint 12 — A: canDeactivate Guard sur Pages Draft

Status: done

<!-- METADATA
  story_key: sprint12-candeactivate-draft-guard
  sprint: Sprint 12
  priority: P2
-->

## Story

As a draft participant (Thibaut, Teddy, Marcel),
I want a confirmation dialog when I try to navigate away from an active draft,
so that I don't accidentally break the draft flow for all other participants.

## Acceptance Criteria

1. Naviguer hors d'une page SnakeDraft active → dialog de confirmation s'affiche
2. Naviguer hors d'une page SimultaneousDraft active (phases submitting/waiting/reselecting) → dialog de confirmation
3. L'utilisateur clique "Rester" → navigation annulée, draft continue
4. L'utilisateur clique "Quitter" → navigation autorisée, composant détruit proprement (disconnect STOMP)
5. Quand le draft est terminé (phase done / draft null) → pas de dialog, navigation libre
6. Bouton retour du navigateur → même comportement que navigation interne
7. 3 tests unitaires : (a) guard retourne false quand draft actif + dialog annule, (b) guard retourne true quand draft actif + dialog confirme, (c) guard retourne true quand draft terminé
8. Sprint-status.yaml mis à jour: `sprint12-candeactivate-draft-guard: done`

## Tasks / Subtasks

- [x] Task 1: Créer le guard `DraftActiveGuard` (AC: #1-#6)
  - [x] 1.1: Créer `frontend/src/app/core/guards/draft-active.guard.ts`
  - [x] 1.2: Définir interface `ComponentWithDraftState { isDraftActive(): boolean }`
  - [x] 1.3: Implémenter `canDeactivateDraftGuard: CanDeactivateFn<ComponentWithDraftState>`
  - [x] 1.4: Ouvrir `MatDialog` avec `ConfirmLeaveDialogComponent` (à créer) — retourner `Observable<boolean>`
  - [x] 1.5: Si `isDraftActive()` retourne false → retourner `true` directement sans dialog

- [x] Task 2: Créer `ConfirmLeaveDialogComponent` (AC: #3, #4)
  - [x] 2.1: Créer `frontend/src/app/shared/components/confirm-leave-dialog/confirm-leave-dialog.component.ts`
  - [x] 2.2: Template: titre "Draft en cours", message traduit, boutons "Rester" (false) + "Quitter" (true)
  - [x] 2.3: Standalone component, inject `MatDialogRef`
  - [x] 2.4: Ajouter clés i18n dans les fichiers de traduction FR/EN/PT/ES :
    - `draft.leaveConfirm.title` — "Draft en cours"
    - `draft.leaveConfirm.message` — "Vous êtes en train de drafter. Quitter maintenant peut bloquer les autres participants."
    - `draft.leaveConfirm.stay` — "Rester"
    - `draft.leaveConfirm.leave` — "Quitter quand même"

- [x] Task 3: Implémenter `isDraftActive()` dans SnakeDraftPageComponent (AC: #1, #5)
  - [x] 3.1: Implémenter `ComponentWithDraftState` sur `SnakeDraftPageComponent`
  - [x] 3.2: `isDraftActive()`: retourne `true` si `this.draft !== null && !this.isDraftDone()`
  - [x] 3.3: Définir `isDraftDone()`: vérifie `status === 'COMPLETED' | 'CANCELLED' | 'ACTIVE'`

- [x] Task 4: Implémenter `isDraftActive()` dans SimultaneousDraftPageComponent (AC: #2, #5)
  - [x] 4.1: Implémenter `ComponentWithDraftState` sur `SimultaneousDraftPageComponent`
  - [x] 4.2: `isDraftActive()`: retourne `true` si `this.phase !== 'done'`

- [x] Task 5: Enregistrer le guard sur les routes draft (AC: #1, #2, #6)
  - [x] 5.1: Ouvrir le fichier de routing des parties (game-routing.module.ts ou app.routes.ts)
  - [x] 5.2: Ajouter `canDeactivate: [canDeactivateDraftGuard]` sur la route SnakeDraft
  - [x] 5.3: Ajouter `canDeactivate: [canDeactivateDraftGuard]` sur la route SimultaneousDraft

- [x] Task 6: Tests unitaires (AC: #7)
  - [x] 6.1: Créer `draft-active.guard.spec.ts` — 3 tests : tous verts
  - [x] 6.2: Créer `confirm-leave-dialog.component.spec.ts` — 3 tests : tous verts

- [x] Task 7: Mettre à jour sprint-status.yaml (AC: #8)
  - [x] 7.1: `sprint12-candeactivate-draft-guard: done`

## Dev Notes

### Pattern CanDeactivateFn Angular 19

```typescript
// core/guards/draft-active.guard.ts
import { CanDeactivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { map } from 'rxjs/operators';

export interface ComponentWithDraftState {
  isDraftActive(): boolean;
}

export const canDeactivateDraftGuard: CanDeactivateFn<ComponentWithDraftState> = (component) => {
  if (!component.isDraftActive()) return true;

  const dialog = inject(MatDialog);
  const dialogRef = dialog.open(ConfirmLeaveDialogComponent, {
    width: '400px',
    disableClose: true,
  });
  return dialogRef.afterClosed().pipe(map(result => result === true));
};
```

### Pattern ConfirmLeaveDialogComponent

```typescript
// shared/components/confirm-leave-dialog/confirm-leave-dialog.component.ts
@Component({
  selector: 'app-confirm-leave-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, TranslatePipe],
  template: `
    <h2 mat-dialog-title>{{ t.t('draft.leaveConfirm.title') }}</h2>
    <mat-dialog-content>{{ t.t('draft.leaveConfirm.message') }}</mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false">{{ t.t('draft.leaveConfirm.stay') }}</button>
      <button mat-raised-button color="warn" [mat-dialog-close]="true">{{ t.t('draft.leaveConfirm.leave') }}</button>
    </mat-dialog-actions>
  `
})
export class ConfirmLeaveDialogComponent {
  public readonly t = inject(TranslationService);
}
```

### Fichiers de traduction i18n

Les fichiers sont dans `frontend/src/assets/i18n/` (fr.json, en.json, de.json, es.json).
Ajouter sous la clé `"draft"` :
```json
"leaveConfirm": {
  "title": "Draft en cours",
  "message": "Vous êtes en train de drafter. Quitter maintenant peut bloquer les autres participants.",
  "stay": "Rester",
  "leave": "Quitter quand même"
}
```

### Routing actuel — où ajouter canDeactivate

Chercher le fichier de routing des jeux. Les routes snake/simultaneous sont définies comme :
```
/games/:id/draft/snake     → SnakeDraftPageComponent
/games/:id/draft/simultaneous → SimultaneousDraftPageComponent
```
Ajouter `canDeactivate: [canDeactivateDraftGuard]` sur chacune.

### Pattern `inject()` dans les guards Angular 19

`CanDeactivateFn` est une fonction (pas une classe). `inject()` fonctionne à l'intérieur car elle s'exécute dans un contexte d'injection Angular. Pas besoin de `DestroyRef` — le guard n'a pas de cycle de vie propre.

### TranslationService

Injecté comme : `public readonly t = inject(TranslationService)` dans les templates.
Utilisation template : `{{ t.t('clé') }}`

### Pre-existing Gaps / Known Issues

- [KNOWN] 534 pre-existing frontend Vitest failures (localStorage/Zone.js) — ne pas chercher à fixer
- [KNOWN] `fakeAsync(async ()=>{})` incompatible avec Vitest — utiliser Pattern A ou B
- [KNOWN] `[disabled]` sur FormControl → utiliser `control.disable()` dans ngOnChanges()
- [KNOWN] `aria-label="{{ expr }}"` en mode strict → utiliser `[attr.aria-label]`

### Project Structure Notes

```
frontend/src/app/
├── core/
│   └── guards/
│       ├── auth.guard.ts           ← pattern à suivre (CanActivateFn)
│       ├── admin.guard.ts
│       ├── game-selection.guard.ts
│       └── draft-active.guard.ts   ← À CRÉER
├── shared/
│   └── components/
│       ├── confirm-leave-dialog/   ← À CRÉER
│       │   ├── confirm-leave-dialog.component.ts
│       │   └── confirm-leave-dialog.component.spec.ts
│       └── main-layout/
└── features/
    └── draft/
        └── components/
            ├── snake-draft-page/   ← Modifier: add isDraftActive()
            └── simultaneous-draft-page/  ← Modifier: add isDraftActive()
```

### References

- Angular CanDeactivateFn: https://angular.dev/api/router/CanDeactivateFn
- Pattern guard existant: `frontend/src/app/core/guards/auth.guard.ts`
- SnakeDraftPageComponent: `frontend/src/app/features/draft/components/snake-draft-page/snake-draft-page.component.ts`
- SimultaneousDraftPageComponent: `frontend/src/app/features/draft/components/simultaneous-draft-page/simultaneous-draft-page.component.ts`
- app.routes.ts: `frontend/src/app/app.routes.ts`

## Dev Agent Record

### Agent Model Used
claude-sonnet-4-6

### Completion Notes List
- Implemented `canDeactivateDraftGuard` as `CanDeactivateFn<ComponentWithDraftState>` (functional guard pattern, Angular 19)
- `ConfirmLeaveDialogComponent`: standalone, uses `TranslationService.t()`, [mat-dialog-close] directives
- `SnakeDraftPageComponent.isDraftDone()`: checks status COMPLETED|CANCELLED|ACTIVE
- `SimultaneousDraftPageComponent.isDraftActive()`: returns `this.phase !== 'done'`
- Guard added on both snake and simultaneous draft routes in `game-routing.module.ts`
- i18n keys added to FR/EN/ES/PT (project uses pt not de)
- 6 new tests: 3 guard + 3 dialog — all green
- Pre-existing 534 failures unchanged (localStorage/Zone.js baseline)

### File List
- frontend/src/app/core/guards/draft-active.guard.ts (NEW)
- frontend/src/app/core/guards/draft-active.guard.spec.ts (NEW)
- frontend/src/app/shared/components/confirm-leave-dialog/confirm-leave-dialog.component.ts (NEW)
- frontend/src/app/shared/components/confirm-leave-dialog/confirm-leave-dialog.component.spec.ts (NEW)
- frontend/src/app/features/draft/components/snake-draft-page/snake-draft-page.component.ts (MODIFY)
- frontend/src/app/features/draft/components/simultaneous-draft-page/simultaneous-draft-page.component.ts (MODIFY)
- frontend/src/app/features/game/game-routing.module.ts (MODIFY — add canDeactivate on snake + simultaneous routes)
- frontend/src/assets/i18n/fr.json (MODIFY — add draft.leaveConfirm keys)
- frontend/src/assets/i18n/en.json (MODIFY)
- frontend/src/assets/i18n/es.json (MODIFY)
- frontend/src/assets/i18n/pt.json (MODIFY)
- _bmad-output/implementation-artifacts/sprint-status.yaml (MODIFY)
