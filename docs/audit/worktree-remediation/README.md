# Sprint 19 Worktree Remediation Pathspecs

Updated: 2026-05-09 after hygiene review follow-up
Purpose: keep the remaining worktree actionable without staging blindly.

## Current Counts

- Visible status entries after cleanup and review follow-up: 191
- Application/tests entries: 129 (128 under `src`/`frontend` plus `docker-compose.local.yml`)
- Shared touchpoints requiring hunk review: 14
- Application hold entries: 34
- Tracked tooling deletions remaining: 0
- Tracked tooling modifications remaining: 0
- Current BMAD sidecar artifact entries outside remediation docs: 2
- Historical BMAD artifact hold entries: 38
- Modified BMAD artifact hold entries: 1
- BMAD tooling untracked hold entries: 6
- Local cache ignored entries: 1
- Local generated-directory hold entries: 28
- Remediation docs pathspec entries: 32
- Remediation commit pathspec entries: 34

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

Do not rely on `git diff -- <path>` for every status: it returns an empty diff for untracked files and can miss staged-only changes. Use a status-aware review before staging, based on NUL-delimited porcelain and the full `XY` state:

```powershell
function Convert-GitStatusZ($text) {
  $records = $text -split "`0" | Where-Object { $_ }
  for ($i = 0; $i -lt $records.Count; $i++) {
    if ($records[$i].Length -lt 4) { throw "Malformed git status record: $($records[$i])" }
    $xy = $records[$i].Substring(0, 2)
    $path = $records[$i].Substring(3)
    if ($xy -match '[RC]') {
      $source = if ($i + 1 -lt $records.Count) { $records[++$i] } else { '<missing-source>' }
      [pscustomobject]@{ XY = $xy; Path = $path; Source = $source }
    } else {
      [pscustomobject]@{ XY = $xy; Path = $path; Source = $null }
    }
  }
}

Get-Content docs\audit\worktree-remediation\story-sprint19-suggest-epic-id-robustness.pathspec.txt |
  Where-Object { $_ -and -not $_.StartsWith('#') } |
  ForEach-Object {
    $entries = @(Convert-GitStatusZ (git status --porcelain=v1 -z -uall -- $_))
    if ($entries.Count -eq 0) { throw "No status entry for pathspec: $_" }
    foreach ($entry in $entries) {
      switch ($entry.XY) {
        ' M' { git diff -- $entry.Path }
        '??' { git diff --no-index -- NUL $entry.Path }
        'M ' { git diff --cached -- $entry.Path; throw "Already staged: $($entry.Path) needs an explicit staging decision." }
        'MM' { git diff --cached -- $entry.Path; git diff -- $entry.Path; throw "Staged and unstaged: $($entry.Path) needs an explicit staging decision." }
        default { throw "Manual review required for status '$($entry.XY)' on '$($entry.Path)'." }
      }
    }
  }
```

When this remediation lot is ready to stage, use the single commit-ready aggregate. Prefer Git's pathspec reader so paths with spaces stay intact:

```powershell
git add --pathspec-from-file=docs\audit\worktree-remediation\remediation-commit.pathspec.txt
git diff --cached --name-status
git diff --cached --check
git status --porcelain=v1 -uall
```

`remediation-docs.pathspec.txt` and `bmad-artifacts.pathspec.txt` are classification inputs. Do not use them alone for staging this remediation commit.

After staging, every non-clean entry reported by `git status --porcelain=v1 -uall` must be intentionally outside this remediation commit and covered by a story pathspec, a hold file, or a documented future decision. Keep the full `XY` status; do not reduce the check to only `??` or `M`.

For a single path containing spaces, quote it explicitly:

```powershell
git diff --no-index -- NUL 'code scraping.txt'  # Windows/PowerShell null path
git add -- 'code scraping.txt'
```

Do not stage `shared-touchpoints-needing-hunk-review.pathspec.txt` blindly. These files likely contain changes for multiple stories and require hunk-level review.

Do not stage `bmad-tooling-untracked-hold.pathspec.txt` with application code. Treat it as a future tooling/config decision.

The file `bmad-artifacts-modified-hold.pathspec.txt` is staged by `remediation-commit.pathspec.txt` as documentation. Do not stage the content listed inside that hold file with this remediation lot; the modified `sprint19-fix-draft-button-guard.md` story artifact needs a separate story-history decision.
