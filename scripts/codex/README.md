# BMAD for Codex CLI

This project contains a Codex-only BMAD setup under `<CODEX_HOME>/prompts`.
Default target is `.codex`, with automatic fallback to `.codex-home` if `.codex` is not writable.

## One-time generation

```powershell
.\scripts\codex\install-bmad-prompts.ps1
```

If PowerShell execution policy blocks scripts on your machine, use:

```bat
.\scripts\codex\install-bmad-prompts.cmd
```

This generates BMAD prompt command files from `_bmad/_config/*.csv`.
You can force a target home with:

```powershell
.\scripts\codex\install-bmad-prompts.ps1 -CodexHomePath .\.codex-home
```

## Recommended launch (project-scoped)

```powershell
.\scripts\codex\start-codex-with-bmad.ps1
```

Execution-policy-safe variant:

```bat
.\scripts\codex\start-codex-with-bmad.cmd
```

This does two things:
1. Ensures BMAD prompts exist in `<CODEX_HOME>/prompts`.
2. Launches Codex with a project-scoped `CODEX_HOME`.

Using project-scoped `CODEX_HOME` keeps BMAD prompts isolated and avoids touching Cursor/Claude files.

## Optional sync to user home

If you want BMAD prompts available globally:

```powershell
.\scripts\codex\sync-prompts-to-user-home.ps1
```

Execution-policy-safe variant:

```bat
.\scripts\codex\sync-prompts-to-user-home.cmd
```

This copies `bmad-*.md` from project `<CODEX_HOME>/prompts` to `$HOME/.codex/prompts`.
