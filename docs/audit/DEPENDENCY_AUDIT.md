# Dependency Audit Report - JIRA-AUDIT-007

**Date**: 2026-02-15
**Scope**: Backend (Maven/Java 21) + Frontend (npm/Angular 20)

## Summary

| Category | Backend | Frontend |
|----------|---------|----------|
| Direct dependencies | 29 | 25+ |
| Outdated (patch) | 8 | 5 |
| Outdated (minor) | 5 | 8 |
| Outdated (major) | 4 | 3 |
| Security vulnerabilities | 0 direct | 23 (via `npm audit`) |
| EOL concerns | 1 (Spring Boot 3.3) | 0 |

---

## 1. Security Vulnerabilities

### Frontend (23 vulnerabilities from `npm audit`)

**HIGH severity (16)**

| Package | Vulnerability | Fix |
|---------|--------------|-----|
| `@angular/common` 20.0.3 | XSRF Token Leakage via Protocol-Relative URLs ([GHSA-58c5-g7wp-6w37](https://github.com/advisories/GHSA-58c5-g7wp-6w37)) | Update to 20.3.16 |
| `@angular/compiler` 20.0.3 | Stored XSS via SVG Animation/MathML Attributes ([GHSA-v4hv-rgfq-gp49](https://github.com/advisories/GHSA-v4hv-rgfq-gp49)) | Update to 20.3.16 |
| `@angular/compiler` 20.0.3 | XSS via Unsanitized SVG Script Attributes ([GHSA-jrmj-c5cx-3cw6](https://github.com/advisories/GHSA-jrmj-c5cx-3cw6)) | Update to 20.3.16 |
| `@angular/ssr` 20.0.2 | SSR Race Condition - Cross-Request Data Leakage ([GHSA-68x2-mx4q-78m7](https://github.com/advisories/GHSA-68x2-mx4q-78m7)) | Update to 20.3.16 |
| `@angular/ssr` 20.0.2 | SSR Server-Side Request Forgery ([GHSA-q63q-pgmf-mxhr](https://github.com/advisories/GHSA-q63q-pgmf-mxhr)) | Update to 20.3.16 |
| `body-parser` <=1.20.3 | DoS via URL encoding ([GHSA-wqch-xfxh-vrr4](https://github.com/advisories/GHSA-wqch-xfxh-vrr4)) | `npm audit fix` |
| `glob` 10.2.0-10.4.5 | Command injection via -c/--cmd ([GHSA-5j98-mcp5-4vw2](https://github.com/advisories/GHSA-5j98-mcp5-4vw2)) | `npm audit fix` |
| Angular transitive deps (9) | Cascading from @angular/core, platform-browser, forms, router, animations, platform-server, compiler-cli | Update Angular to 20.3.16 |

**MODERATE severity (3)**

| Package | Vulnerability | Fix |
|---------|--------------|-----|
| `lodash` 4.17.21 | Prototype Pollution in `_.unset`/`_.omit` ([GHSA-xxjr-mmjv-4gpg](https://github.com/advisories/GHSA-xxjr-mmjv-4gpg)) | Awaiting upstream fix |
| `lodash-es` 4.17.22 | Same as above | Awaiting upstream fix |
| `vite` | Path traversal vulnerability | `npm audit fix` |

**LOW severity (4)**

| Package | Vulnerability | Fix |
|---------|--------------|-----|
| `qs` (transitive) | Query string parsing issues | `npm audit fix` |

**Recommended action**: Run `npm audit fix` to resolve 21/23 vulnerabilities. The 2 lodash vulns require awaiting upstream fix or replacing lodash with native JS alternatives.

### Backend

No known CVEs in direct dependencies at current versions. However, Spring Boot 3.3.0 is 8+ months past EOL (ended June 30, 2025) and no longer receives security patches.

---

## 2. Backend Dependencies (Maven)

### Spring Boot Parent (EOL)

| Current | Latest 3.3.x | Latest 3.4.x | Latest Stable | EOL Status |
|---------|-------------|-------------|---------------|------------|
| **3.3.0** | 3.3.13 | 3.4.x | **4.0.2** | 3.3.x EOL June 2025 |

**Recommendation**: Upgrade to 3.3.13 (minimal risk, same minor) as immediate step. Plan migration to 3.4.x or 4.0.x for long-term support.

### Direct Dependencies Update Matrix

| Dependency | Current | Latest Stable | Gap | Priority |
|------------|---------|---------------|-----|----------|
| `spring-boot-starter-parent` | 3.3.0 | 3.3.13 | 13 patches | **P0** - EOL + security |
| `postgresql` | 42.7.3 | 42.7.10 | 7 patches | **P1** - database driver |
| `flyway-core` | 10.11.0 | 12.0.1 | 2 majors | P3 - major upgrade |
| `jjwt-*` | 0.12.5 | 0.13.0 | 1 minor | P2 - auth library |
| `lombok` | 1.18.32 | 1.18.42 | 10 patches | P2 - compile-time |
| `opencsv` | 5.11.1 | 5.12.0 | 1 minor | P3 - low risk |
| `h2` | 2.2.224 | 2.4.240 | 2 minors | P3 - test-only |
| `archunit-junit5` | 1.2.1 | 1.4.1 | 2 minors | P3 - test-only |
| `spring-retry` | 2.0.6 | 2.0.12 | 6 patches | P2 - managed by Boot |
| `springdoc-openapi` | 2.5.0 | 3.0.1 | 1 major | P3 - major upgrade |
| `logstash-logback-encoder` | 7.4 | 9.0 | 2 majors | P3 - major upgrade |

### Maven Plugins Update Matrix

| Plugin | Current | Latest Stable | Priority |
|--------|---------|---------------|----------|
| `spotless-maven-plugin` | 2.43.0 | 3.2.1 | P3 |
| `spotbugs-maven-plugin` | 4.8.3.1 | latest 4.8.x | P3 |
| `jacoco-maven-plugin` | 0.8.11 | 0.8.14 | P2 |
| `dependency-check-maven` | 9.0.9 | latest 9.x/10.x | P2 |
| `maven-compiler-plugin` | 3.13.0 | current | OK |
| `maven-surefire-plugin` | 3.2.5 | current | OK |

### Managed Dependencies (upgraded via Spring Boot parent)

These are auto-managed by `spring-boot-starter-parent`. Upgrading the parent BOM from 3.3.0 to 3.3.13 will automatically update:
- Jackson 2.17.1 -> 2.17.x (managed)
- Logback 1.5.6 -> 1.5.x (managed)
- Spring Security 6.3.0 -> 6.3.x (managed)
- Hibernate/JPA (managed)
- Micrometer 1.13.0 -> 1.13.x (managed)
- Tomcat (managed)

---

## 3. Frontend Dependencies (npm)

### Angular Framework Update

| Package | Current | Wanted (^20) | Latest | Priority |
|---------|---------|-------------|--------|----------|
| `@angular/core` | 20.0.3 | **20.3.16** | 21.1.4 | **P0** - security |
| `@angular/cli` | 20.0.2 | **20.3.16** | 21.1.4 | **P0** - security |
| `@angular/build` | 20.0.2 | **20.3.16** | 21.1.4 | **P0** - security |
| `@angular/cdk` | 20.0.3 | **20.2.14** | 21.1.4 | P1 |
| `@angular/material` | 20.0.3 | **20.2.14** | 21.1.4 | P1 |
| `@angular/ssr` | 20.0.2 | **20.3.16** | 21.1.4 | **P0** - security |

**Recommended action**: `ng update @angular/core@20 @angular/cli@20` resolves 16/23 npm audit vulnerabilities.

### Other npm Updates

| Package | Current | Wanted | Latest | Priority |
|---------|---------|--------|--------|----------|
| `@analogjs/vite-plugin-angular` | 1.17.1 | 1.22.5 | 2.2.3 | P3 |
| `@stomp/stompjs` | 7.2.1 | 7.3.0 | 7.3.0 | P3 |
| `@types/jasmine` | 5.1.8 | 5.1.15 | 6.0.0 | P3 |
| `@types/node` | 20.19.1 | 20.19.33 | 25.2.3 | P3 |
| `chart.js` | 4.5.0 | 4.5.1 | 4.5.1 | P3 |
| `express` | 5.1.0 | 5.2.1 | 5.2.1 | P2 |
| `typescript` | 5.8.3 | 5.8.3 | 5.9.3 | P3 - major semver |
| `zone.js` | 0.15.1 | 0.15.1 | 0.16.0 | P3 - major semver |
| `jasmine-core` | 5.7.1 | 5.7.1 | 6.0.1 | P3 - major |
| `karma-jasmine-html-reporter` | 2.1.0 | 2.1.0 | 2.2.0 | P3 |

---

## 4. Recommended Action Plan (Priority Order)

### P0 - Fix immediately (security vulnerabilities)

| # | Action | Impact | Risk |
|---|--------|--------|------|
| 1 | `cd frontend && npm audit fix` | Fixes 21/23 npm vulns (body-parser, glob, qs, vite) | Low |
| 2 | `cd frontend && ng update @angular/core@20 @angular/cli@20` | Updates Angular 20.0.3 -> 20.3.16, fixes XSRF/XSS/SSR vulns | Low (same major) |

### P1 - Fix soon (EOL / database drivers)

| # | Action | Impact | Risk |
|---|--------|--------|------|
| 3 | Upgrade `spring-boot-starter-parent` 3.3.0 -> 3.3.13 | Gets last patches for 3.3.x (EOL), auto-upgrades Jackson/Logback/Spring Security | Low (same minor) |
| 4 | Upgrade `postgresql` 42.7.3 -> 42.7.10 | Database driver patches | Low |
| 5 | Upgrade `@angular/cdk` + `@angular/material` to 20.2.14 | Component library patches | Low |

### P2 - Improve (non-critical updates)

| # | Action | Impact | Risk |
|---|--------|--------|------|
| 6 | Upgrade `jjwt-*` 0.12.5 -> 0.13.0 | JWT library minor update | Medium |
| 7 | Upgrade `lombok` 1.18.32 -> 1.18.42 | Compile-time annotations | Low |
| 8 | Upgrade `jacoco-maven-plugin` 0.8.11 -> 0.8.14 | Coverage tool patches | Low |
| 9 | Upgrade `express` 5.1.0 -> 5.2.1 | Dev server patches | Low |

### P3 - Plan for later (major upgrades)

| # | Action | Notes |
|---|--------|-------|
| 10 | Spring Boot 3.3 -> 3.4.x/4.0.x migration | Requires dedicated sprint, possible breaking changes |
| 11 | Flyway 10.x -> 12.x | Major version, check migration guide |
| 12 | SpringDoc 2.x -> 3.x | Major version, API changes |
| 13 | Logstash Logback 7.x -> 9.x | Major version, config changes |
| 14 | Angular 20.x -> 21.x | Major version, dedicated upgrade sprint |
| 15 | Replace lodash with native JS | Resolves last 2 npm vulns with no upstream fix |

---

## 5. Checklist

- [x] npm audit executed (23 vulnerabilities identified)
- [x] npm outdated executed (25 packages with updates)
- [x] Maven dependency updates scanned (29 direct dependencies checked)
- [x] Maven plugin updates scanned (6 plugins checked)
- [x] Security vulnerabilities classified by severity
- [x] EOL status checked for all frameworks
- [x] Prioritized action plan produced (P0-P3)
- [x] No changes made to pom.xml or package.json (audit only)
