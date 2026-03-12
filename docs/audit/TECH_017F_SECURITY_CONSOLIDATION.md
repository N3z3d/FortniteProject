# TECH-017F — Consolidation Sécurité Finale

> Audité le 2026-03-04
> Dépend de : TECH-017A, 017B, 017C, TECH-024, TECH-025, TECH-017D, TECH-017E

---

## 1. Bilan des sous-tickets

| Ticket | Titre | Statut | Livraisons clés |
|---|---|---|---|
| TECH-017A | Audit dépendances vulnérables | ✅ DONE | Inventaire vulns frontend/backend |
| TECH-024 | Fiabiliser scan CVE backend | ✅ DONE | `nvdApiKey=${env.NVD_API_KEY}` + owasp-suppression.xml |
| TECH-025 | Corriger vulns npm | ✅ DONE | vitest 2.1.9→3.2.4 ; **0 vuln npm** (était 6 moderate) |
| TECH-017B | Secrets & configuration | ✅ DONE | `.env` gitignored ; prod = vars sans fallback ; dev scoped |
| TECH-017C | Authn/AuthZ admin | ✅ DONE | Matrice `/admin` auditée ; 3 suites d'autorisation créées |
| TECH-017D | CORS/CSRF/XSS/Validation | ✅ DONE | `@Valid` sur 5 endpoints ; 5 DTOs renforcés ; rapport dédié |
| TECH-017E | Hardening Docker/Compose | ✅ DONE | `.dockerignore` ; exec-form ENTRYPOINT ; Redis auth ; monitoring localhost-only |

---

## 2. Surface d'attaque — État final

### 2.1 Dépendances

| Cible | Avant | Après |
|---|---|---|
| Frontend npm | 6 vulnérabilités moderate (esbuild/vite/vitest) | **0 vulnérabilité** |
| Backend Maven | Scan NVD échouait (403) | Scan opérationnel avec clé NVD |

### 2.2 Secrets & configuration

| Point | Statut |
|---|---|
| `.env` jamais commis (gitignored) | ✅ Conforme |
| Profil `prod` : toutes les vars sans fallback (`${VAR}`) | ✅ Conforme |
| JWT_SECRET non loggué | ✅ Conforme |
| Mots de passe DB/Redis/Grafana dans `.env` uniquement | ✅ Conforme |

### 2.3 Authentification / Autorisation

| Point | Statut |
|---|---|
| JWT Bearer + BCrypt | ✅ Conforme |
| `STATELESS` — pas de session | ✅ Conforme |
| `/api/admin/**` → `ROLE_ADMIN` uniquement | ✅ Conforme |
| `/api/users/**` → `ROLE_ADMIN` uniquement | ✅ Conforme |
| Tests d'autorisation sur 13 suites (`SecurityConfig*AuthorizationTest`) | ✅ Conforme |

### 2.4 CORS / CSRF / Headers

| Point | Statut |
|---|---|
| CORS : origines prod HTTPS-only | ✅ Conforme |
| CSRF désactivé (stateless JWT — correct) | ✅ Justifié |
| HSTS 31 536 000s + includeSubDomains + preload | ✅ Excellent |
| X-Frame-Options DENY | ✅ Conforme |
| X-Content-Type-Options nosniff | ✅ Conforme |

### 2.5 Validation des entrées

| Point | Statut |
|---|---|
| `@Valid` sur tous les endpoints modifiés (5 gaps corrigés) | ✅ Conforme |
| Contraintes Jakarta sur DTOs critiques | ✅ Conforme |

### 2.6 Infrastructure Docker

| Point | Avant | Après |
|---|---|---|
| `.dockerignore` | Absent | ✅ Créé (`.env`, `target/`, secrets exclus) |
| ENTRYPOINT | Shell-form (injection possible) | ✅ Exec-form |
| `JAVA_OPTS` expansion | Shell requis | ✅ `JAVA_TOOL_OPTIONS` (JVM natif) |
| Redis | Sans mot de passe | ✅ `--requirepass ${REDIS_PASSWORD}` |
| Prometheus | 0.0.0.0:9090 (public) | ✅ 127.0.0.1:9090 (localhost only) |
| Grafana | 0.0.0.0:3000 + fallback "admin" | ✅ 127.0.0.1:3000 + pas de fallback |

---

## 3. Points résiduels (hors scope sprint 2)

Ces points sont identifiés mais non bloquants pour la mise en production — à planifier en sprint 3.

### 3.1 Medium — Absence de rate limiting sur `/api/auth/**`

- **Risque** : brute-force des mots de passe possible (aucun throttle sur `POST /api/auth/login`)
- **Recommandation** : intégrer `bucket4j` ou Spring Security rate limiting
- **Impact** : medium — BCrypt ralentit déjà les attaques, mais sans limite de tentatives

### 3.2 Low — `X-Test-User` dans la liste CORS allowed-headers

- **Risque** : header de test exposé en prod dans la configuration CORS
- **Recommandation** : conditionner à `!isProd` dans `corsConfigurationSource()`
- **Impact** : low — header ignoré si non émis par le client ; pas de vecteur d'attaque direct

### 3.3 Low — WebSocket `/ws/**` en `permitAll()`

- **Risque** : connexions WebSocket non authentifiées possibles au niveau HTTP upgrade
- **Contexte** : le handshake WS lui-même n'est pas protégé par JWT
- **Recommandation** : valider le token JWT dans `WebSocketHandshakeInterceptor`
- **Impact** : low — les messages applicatifs sont scoped par gameId ; pas de fuite de données connue

### 3.4 Low — HPKP activé (déprécié)

- **Contexte** : `httpPublicKeyPinning(hpkp -> hpkp.includeSubDomains(true))` — HTTP Public Key Pinning est supprimé de tous les navigateurs modernes
- **Impact** : technique — aucun risque de sécurité, mais entretien inutile
- **Recommandation** : supprimer lors du prochain refactoring de `SecurityConfig`

---

## 4. Scorecard final TECH-017

| Catégorie | Score | Évolution |
|---|---|---|
| Dépendances | 🟢 10/10 | ↑ (0 vuln npm, scan backend opérationnel) |
| Secrets & config | 🟢 10/10 | ↑ (prod sans fallback, .env sécurisé) |
| Authn/AuthZ | 🟢 9/10 | → (rate limiting manquant) |
| CORS/CSRF/Headers | 🟢 9/10 | → (X-Test-User header résiduel) |
| Validation entrées | 🟢 9/10 | ↑ (5 gaps corrigés) |
| Infrastructure | 🟢 10/10 | ↑ (Docker/Compose durci) |
| **Score global** | **🟢 57/60** | **↑ depuis ~40/60** |

---

## 5. Décision

**Feu vert pour déploiement prod** sous réserve que :
1. Les variables `REDIS_PASSWORD`, `GRAFANA_PASSWORD`, `JWT_SECRET`, `DB_PASSWORD` soient définies dans l'environnement de déploiement (pas de fallback).
2. La clé NVD soit configurée pour le scan CVE continu.
3. Les 3 points résiduels (rate limiting, WS auth, X-Test-User) soient planifiés au sprint suivant.
