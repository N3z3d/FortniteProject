# Sprint 19 Worktree Remediation Pathspecs

Updated: 2026-05-08 after re-review follow-up
Purpose: keep the remaining worktree actionable without staging blindly.

## Current Counts

- Visible status entries after cleanup and re-review follow-up: 214
- Application/tests entries: 129
- Shared touchpoints requiring hunk review: 14
- Application hold entries: 34
- Tracked tooling deletions remaining: 0
- Tracked tooling modifications remaining: 0
- Current BMAD sidecar artifact entries outside remediation docs: 1
- Historical BMAD artifact hold entries: 38
- Modified BMAD artifact hold entries: 1
- BMAD tooling untracked hold entries: 6
- Local cache ignored entries: 1
- Local generated-directory hold entries: 28
- Remediation docs pathspec entries: 32
- Remediation commit pathspec entries: 33

## Recommended BMAD Prompt

This is a BMAD chat prompt, not a PowerShell command:

```text
$bmad-code-review _bmad-output/implementation-artifacts/sprint19-worktree-remediation.md
```

## Cleanup Applied

- Kept only cache/user-scoped ignores in `.gitignore`.
- Moved broad local agent/IDE generated-directory exclusions to `.git/info/exclude`.
- Restored tracked tooling deletions from `HEAD`.
- Refreshed false-positive tracked tooling modifications out of the index.
- Ignored `_bmad/**/*.bak` generated backup files.
- Split current BMAD process artifacts from historical BMAD hold artifacts.
- Snapshotted local `.git/info/exclude` patterns in `local-exclude-patterns.snapshot.txt`.
- Added `review-verification.snapshot.txt` so local verification claims have versioned proof.
- Added `remediation-commit.pathspec.txt` as the single commit-ready aggregate for this process lot.

## Usage

Inspect a pathspec first:

```powershell
Get-Content docs\audit\worktree-remediation\story-sprint19-suggest-epic-id-robustness.pathspec.txt
```

Do not rely on `git diff -- <path>` for untracked files: it returns an empty diff. Use a status-aware review before staging:

```powershell
Get-Content docs\audit\worktree-remediation\story-sprint19-suggest-epic-id-robustness.pathspec.txt |
  ForEach-Object {
    $status = git status --porcelain=v1 -- $_
    if ($status -match '^\?\?') { git diff --no-index -- NUL $_ }
    else { git diff -- $_ }
  }
```

When this remediation lot is ready to stage, use the single commit-ready aggregate. Prefer Git's pathspec reader so paths with spaces stay intact:

```powershell
git add --pathspec-from-file=docs\audit\worktree-remediation\remediation-commit.pathspec.txt
git diff --cached --name-status
git diff --cached --check
```

`remediation-docs.pathspec.txt` and `bmad-artifacts.pathspec.txt` are classification inputs. Do not use them alone for staging this remediation commit.

For a single path containing spaces, quote it explicitly:

```powershell
git diff --no-index -- NUL 'code scraping.txt'
git add -- 'code scraping.txt'
```

Do not stage `shared-touchpoints-needing-hunk-review.pathspec.txt` blindly. These files likely contain changes for multiple stories and require hunk-level review.

Do not stage `bmad-tooling-untracked-hold.pathspec.txt` with application code. Treat it as a future tooling/config decision.

Do not stage `bmad-artifacts-modified-hold.pathspec.txt` with this remediation lot. It contains the modified `sprint19-fix-draft-button-guard.md` story artifact and needs a separate story-history decision.
