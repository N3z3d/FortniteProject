# Runtime Validation - Sprint7 Z1 - 2026-03-11

## Scope

Validation runtime/tests de la story `sprint7-z1-fix-zonejs-debounce-failures`.

## Commands Executed

```powershell
cd frontend
npx vitest run src/app/features/catalogue/pages/player-catalogue-page/player-catalogue-page.component.spec.ts
npx vitest run src/app/features/diagnostic/diagnostic.component.spec.ts
npx vitest run src/app/features/game/services/game-detail-actions.service.spec.ts
npm run test:vitest
npx vitest run --reporter=json --outputFile vitest-z1-report.json
```

## Results

- `player-catalogue-page.component.spec.ts` : vert apres remplacement des attentes `fakeAsync/tick()` par un settle `async/await`
- `diagnostic.component.spec.ts` : vert apres ajout d un mock HTTP par defaut et isolation du `runDiagnostic()` du `ngOnInit`
- `game-detail-actions.service.spec.ts` : vert apres `spyOn(...).and.stub()` sur les branches dialogue qui ne doivent pas appeler le service reel
- `npm run test:vitest` : vert
- `frontend/vitest-z1-report.json` :
  - `numTotalTestSuites = 721`
  - `numPassedTestSuites = 721`
  - `numFailedTestSuites = 0`
  - `numTotalTests = 2243`
  - `numPassedTests = 2243`
  - `numFailedTests = 0`
  - `numPendingTests = 0`
  - `success = true`

## Known Non-Blocking Noise

- Warnings jsdom sur `canvas.getContext`, `canvas.toDataURL` et `window.scrollTo`
- Warnings Angular `NG0912` / `NG0914`
- Ces warnings n empechent plus le passage vert de Vitest et restent hors scope de `Z1`

## Conclusion

Le baseline Vitest de Sprint 7 n est plus rouge. La story `sprint7-z1-fix-zonejs-debounce-failures` est prete pour `code-review`.
