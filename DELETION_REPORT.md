# RAPPORT DE SUPPRESSION - Fichiers dupliqués et inutiles

Date: 2026-01-18
Opération: Nettoyage code mort et duplications
Approuvé par: Claude Code (Agent)

## FICHIERS À SUPPRIMER

### 1. PlayerLeaderboardEntryDTO (version package leaderboard)

| Attribut | Valeur |
|----------|--------|
| **Fichier** | `src/main/java/com/fortnite/pronos/dto/leaderboard/PlayerLeaderboardEntryDTO.java` |
| **Lignes** | 63 |
| **Raison** | Duplication du DTO racine `com.fortnite.pronos.dto.PlayerLeaderboardEntryDTO` |
| **Références** | ✅ AUCUNE (vérifié via grep) |
| **Impact** | Zéro - le code utilise uniquement la version racine |
| **Décision** | ✅ **SUPPRIMER** |

#### Détails de la duplication

**DTO racine** (`dto/PlayerLeaderboardEntryDTO.java` - 25 lignes) :
- ✅ Utilisé par `LeaderboardService.java` (lignes 553, 650)
- ✅ Utilisé par `LeaderboardController.java` (import ligne 13)
- Structure : `String playerId`, `Player.Region region`, `List<TeamInfo> teams`, `List<String> pronostiqueurs`

**DTO package leaderboard** (`dto/leaderboard/PlayerLeaderboardEntryDTO.java` - 63 lignes) :
- ❌ Non importé par aucun fichier Java
- ❌ Non utilisé dans le code
- Structure incompatible : `UUID playerId`, `String region`, pas de champs `teams`/`pronostiqueurs`
- Semble être une tentative de refactoring abandonnée

#### Analyse de sécurité
- ✅ Pas de dépendances entrantes
- ✅ Pas de références dans les tests
- ✅ Pas de références dans les controllers
- ✅ Suppression sans risque

---

## VALIDATION

| Critère | Statut |
|---------|--------|
| Fichier non référencé | ✅ Confirmé |
| Aucune importation | ✅ Confirmé |
| Aucun test dépendant | ✅ Confirmé |
| Aucun impact runtime | ✅ Confirmé |
| Approuvé pour suppression | ✅ OUI |

---

## ACTIONS RÉALISÉES

1. ✅ Grep de toutes les références `import com.fortnite.pronos.dto.leaderboard.PlayerLeaderboardEntryDTO`
2. ✅ Grep de toutes les utilisations de `PlayerLeaderboardEntryDTO` dans le code
3. ✅ Vérification que la version racine est bien utilisée
4. ⏳ Suppression du fichier dupliqué (en attente d'approbation)

---

## COMMIT SUGGÉRÉ

```bash
git rm src/main/java/com/fortnite/pronos/dto/leaderboard/PlayerLeaderboardEntryDTO.java
git commit -m "refactor: remove duplicate PlayerLeaderboardEntryDTO

- Delete unused DTO in leaderboard package (63 lines)
- Keep active version in root dto package (25 lines)
- Zero impact: no references found in codebase
- Fixes JIRA-ARCH-011

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## NOTES

Ce fichier fait partie du ticket **JIRA-ARCH-011** (Deduplicate leaderboard DTOs).
La suppression réduit la dette technique et élimine le risque de divergence entre deux versions du même DTO.
