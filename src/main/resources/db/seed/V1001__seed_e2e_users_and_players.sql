-- ============================================================
-- SEED E2E : Utilisateurs de test + Joueurs Fortnite
--
-- CE FICHIER NE TOURNE QUE SUR LE PROFIL DEV (classpath:db/seed)
-- Le profil prod n'inclut PAS ce répertoire dans flyway.locations.
--
-- IDEMPOTENT : ON CONFLICT (column) DO NOTHING (safe sur re-run)
--
-- Mot de passe de tous les comptes : Admin1234
-- Hash généré via pgcrypto crypt('Admin1234', gen_salt('bf', 10))
-- ============================================================

-- pgcrypto requis pour BCrypt
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- USERS (4 comptes de test)
-- Colonnes : id, username, email, password, role, current_season
-- deleted_at est nullable — omis (NULL par défaut)
-- ============================================================
INSERT INTO users (id, username, email, password, role, current_season)
VALUES
  ('00000000-0000-0000-0000-000000000001',
   'admin',
   'admin@fortnite-pronos.com',
   crypt('Admin1234', gen_salt('bf', 10)),
   'ADMIN',
   2025),
  ('00000000-0000-0000-0000-000000000002',
   'thibaut',
   'thibaut@fortnite-pronos.com',
   crypt('Admin1234', gen_salt('bf', 10)),
   'USER',
   2025),
  ('00000000-0000-0000-0000-000000000003',
   'marcel',
   'marcel@fortnite-pronos.com',
   crypt('Admin1234', gen_salt('bf', 10)),
   'USER',
   2025),
  ('00000000-0000-0000-0000-000000000004',
   'teddy',
   'teddy@fortnite-pronos.com',
   crypt('Admin1234', gen_salt('bf', 10)),
   'USER',
   2025)
ON CONFLICT (username) DO NOTHING;

-- ============================================================
-- PLAYERS (25 joueurs Fortnite — catalogue pour les drafts)
-- Couvre EU (10), NAW (9), NAC (6) — tranches 1, 2, 3
-- ============================================================
INSERT INTO players (id, fortnite_id, username, nickname, region, tranche, current_season)
VALUES
  -- ---- EU Tranche 1 (top EU) ----
  ('10000000-0000-0000-0000-000000000001', 'eu-001', 'Bugha_EU',       'Bugha_EU',       'EU', '1', 2025),
  ('10000000-0000-0000-0000-000000000002', 'eu-002', 'Aqua_EU',        'Aqua_EU',        'EU', '1', 2025),
  ('10000000-0000-0000-0000-000000000003', 'eu-003', 'Mongraal_EU',    'Mongraal_EU',    'EU', '1', 2025),
  ('10000000-0000-0000-0000-000000000004', 'eu-004', 'Benjyfishy_EU',  'Benjyfishy_EU',  'EU', '1', 2025),

  -- ---- EU Tranche 2 ----
  ('10000000-0000-0000-0000-000000000005', 'eu-005', 'EUMid1',         'EUMid1',         'EU', '2', 2025),
  ('10000000-0000-0000-0000-000000000006', 'eu-006', 'EUMid2',         'EUMid2',         'EU', '2', 2025),
  ('10000000-0000-0000-0000-000000000007', 'eu-007', 'EUMid3',         'EUMid3',         'EU', '2', 2025),

  -- ---- EU Tranche 3 ----
  ('10000000-0000-0000-0000-000000000008', 'eu-008', 'EULow1',         'EULow1',         'EU', '3', 2025),
  ('10000000-0000-0000-0000-000000000009', 'eu-009', 'EULow2',         'EULow2',         'EU', '3', 2025),
  ('10000000-0000-0000-0000-000000000010', 'eu-010', 'EULow3',         'EULow3',         'EU', '3', 2025),

  -- ---- NAW Tranche 1 (top NAW) ----
  ('10000000-0000-0000-0000-000000000011', 'naw-001', 'Clix_NAW',      'Clix_NAW',      'NAW', '1', 2025),
  ('10000000-0000-0000-0000-000000000012', 'naw-002', 'Rehx_NAW',      'Rehx_NAW',      'NAW', '1', 2025),
  ('10000000-0000-0000-0000-000000000013', 'naw-003', 'Edgey_NAW',     'Edgey_NAW',     'NAW', '1', 2025),

  -- ---- NAW Tranche 2 ----
  ('10000000-0000-0000-0000-000000000014', 'naw-004', 'NAWMid1',       'NAWMid1',       'NAW', '2', 2025),
  ('10000000-0000-0000-0000-000000000015', 'naw-005', 'NAWMid2',       'NAWMid2',       'NAW', '2', 2025),
  ('10000000-0000-0000-0000-000000000016', 'naw-006', 'NAWMid3',       'NAWMid3',       'NAW', '2', 2025),

  -- ---- NAW Tranche 3 ----
  ('10000000-0000-0000-0000-000000000017', 'naw-007', 'NAWLow1',       'NAWLow1',       'NAW', '3', 2025),
  ('10000000-0000-0000-0000-000000000018', 'naw-008', 'NAWLow2',       'NAWLow2',       'NAW', '3', 2025),
  ('10000000-0000-0000-0000-000000000019', 'naw-009', 'NAWLow3',       'NAWLow3',       'NAW', '3', 2025),

  -- ---- NAC Tranche 1 ----
  ('10000000-0000-0000-0000-000000000020', 'nac-001', 'NACTop1',       'NACTop1',       'NAC', '1', 2025),
  ('10000000-0000-0000-0000-000000000021', 'nac-002', 'NACTop2',       'NACTop2',       'NAC', '1', 2025),

  -- ---- NAC Tranche 2 ----
  ('10000000-0000-0000-0000-000000000022', 'nac-003', 'NACMid1',       'NACMid1',       'NAC', '2', 2025),
  ('10000000-0000-0000-0000-000000000023', 'nac-004', 'NACMid2',       'NACMid2',       'NAC', '2', 2025),

  -- ---- NAC Tranche 3 ----
  ('10000000-0000-0000-0000-000000000024', 'nac-005', 'NACLow1',       'NACLow1',       'NAC', '3', 2025),
  ('10000000-0000-0000-0000-000000000025', 'nac-006', 'NACLow2',       'NACLow2',       'NAC', '3', 2025)
ON CONFLICT (nickname) DO NOTHING;
