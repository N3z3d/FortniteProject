-- ============================================================
-- SEED DEMO : Parties, équipes et scores pour la démo locale
--
-- CE FICHIER NE TOURNE QUE SUR LE PROFIL DEV (classpath:db/seed)
-- IDEMPOTENT : ON CONFLICT ... DO NOTHING (safe sur re-run)
--
-- Dépend de V1001__seed_e2e_users_and_players.sql :
--   users  : thibaut, marcel, teddy (UUIDs dynamiques — ne pas hardcoder)
--   players: 001-025 (EU/NAW/NAC, tranches 1-3)
--
-- Note: Les UUIDs des users sont résolus dynamiquement via subquery
-- pour être robustes même si les users existent déjà avec des UUIDs différents.
-- ============================================================

-- ============================================================
-- PARTIE 1 — Coupe Automne 2025 (FINISHED avec scores)
-- Démontre : leaderboard, équipes, résultats
-- ============================================================
INSERT INTO games (id, name, creator_id, max_participants, status, created_at, finished_at)
SELECT
  'a0000000-0000-0000-0000-000000000001',
  'Coupe Automne 2025',
  id,
  3,
  'FINISHED',
  '2025-10-01 10:00:00',
  '2025-11-01 18:00:00'
FROM users WHERE username = 'thibaut'
ON CONFLICT (id) DO NOTHING;

-- Participants de la partie 1
INSERT INTO game_participants (id, game_id, user_id, draft_order, joined_at, is_creator)
SELECT
  'c0000000-0000-0000-0000-000000000001',
  'a0000000-0000-0000-0000-000000000001',
  id,
  1, '2025-10-01 10:00:00', TRUE
FROM users WHERE username = 'thibaut'
ON CONFLICT (game_id, user_id) DO NOTHING;

INSERT INTO game_participants (id, game_id, user_id, draft_order, joined_at, is_creator)
SELECT
  'c0000000-0000-0000-0000-000000000002',
  'a0000000-0000-0000-0000-000000000001',
  id,
  2, '2025-10-01 10:05:00', FALSE
FROM users WHERE username = 'marcel'
ON CONFLICT (game_id, user_id) DO NOTHING;

INSERT INTO game_participants (id, game_id, user_id, draft_order, joined_at, is_creator)
SELECT
  'c0000000-0000-0000-0000-000000000003',
  'a0000000-0000-0000-0000-000000000001',
  id,
  3, '2025-10-01 10:07:00', FALSE
FROM users WHERE username = 'teddy'
ON CONFLICT (game_id, user_id) DO NOTHING;

-- ============================================================
-- ÉQUIPES liées à la partie 1
-- ============================================================
INSERT INTO teams (id, name, owner_id, game_id, season, total_score)
SELECT
  'b0000000-0000-0000-0000-000000000001',
  'Team Thibaut',
  id,
  'a0000000-0000-0000-0000-000000000001',
  2025,
  450
FROM users WHERE username = 'thibaut'
ON CONFLICT (name) DO NOTHING;

INSERT INTO teams (id, name, owner_id, game_id, season, total_score)
SELECT
  'b0000000-0000-0000-0000-000000000002',
  'Team Marcel',
  id,
  'a0000000-0000-0000-0000-000000000001',
  2025,
  280
FROM users WHERE username = 'marcel'
ON CONFLICT (name) DO NOTHING;

INSERT INTO teams (id, name, owner_id, game_id, season, total_score)
SELECT
  'b0000000-0000-0000-0000-000000000003',
  'Team Teddy',
  id,
  'a0000000-0000-0000-0000-000000000001',
  2025,
  120
FROM users WHERE username = 'teddy'
ON CONFLICT (name) DO NOTHING;

-- ============================================================
-- JOUEURS assignés aux équipes (5 joueurs par équipe)
-- Équilibre régions : EU + NAW + NAC, tranches mixtes
-- ============================================================
INSERT INTO team_players (id, team_id, player_id, position, since)
VALUES
  -- Team Thibaut : Bugha_EU(001), Clix_NAW(011), NACTop1(020), EUMid2(006), NAWMid2(015)
  (gen_random_uuid(), 'b0000000-0000-0000-0000-000000000001',
   '10000000-0000-0000-0000-000000000001', 1, '2025-10-02 09:00:00'),
  (gen_random_uuid(), 'b0000000-0000-0000-0000-000000000001',
   '10000000-0000-0000-0000-000000000011', 2, '2025-10-02 09:00:00'),
  (gen_random_uuid(), 'b0000000-0000-0000-0000-000000000001',
   '10000000-0000-0000-0000-000000000020', 3, '2025-10-02 09:00:00'),
  (gen_random_uuid(), 'b0000000-0000-0000-0000-000000000001',
   '10000000-0000-0000-0000-000000000006', 4, '2025-10-02 09:00:00'),
  (gen_random_uuid(), 'b0000000-0000-0000-0000-000000000001',
   '10000000-0000-0000-0000-000000000015', 5, '2025-10-02 09:00:00'),

  -- Team Marcel : Aqua_EU(002), Rehx_NAW(012), NACTop2(021), EUMid3(007), NAWMid3(016)
  (gen_random_uuid(), 'b0000000-0000-0000-0000-000000000002',
   '10000000-0000-0000-0000-000000000002', 1, '2025-10-02 09:00:00'),
  (gen_random_uuid(), 'b0000000-0000-0000-0000-000000000002',
   '10000000-0000-0000-0000-000000000012', 2, '2025-10-02 09:00:00'),
  (gen_random_uuid(), 'b0000000-0000-0000-0000-000000000002',
   '10000000-0000-0000-0000-000000000021', 3, '2025-10-02 09:00:00'),
  (gen_random_uuid(), 'b0000000-0000-0000-0000-000000000002',
   '10000000-0000-0000-0000-000000000007', 4, '2025-10-02 09:00:00'),
  (gen_random_uuid(), 'b0000000-0000-0000-0000-000000000002',
   '10000000-0000-0000-0000-000000000016', 5, '2025-10-02 09:00:00'),

  -- Team Teddy : Mongraal_EU(003), Edgey_NAW(013), NACMid1(022), EULow1(008), NAWLow1(017)
  (gen_random_uuid(), 'b0000000-0000-0000-0000-000000000003',
   '10000000-0000-0000-0000-000000000003', 1, '2025-10-02 09:00:00'),
  (gen_random_uuid(), 'b0000000-0000-0000-0000-000000000003',
   '10000000-0000-0000-0000-000000000013', 2, '2025-10-02 09:00:00'),
  (gen_random_uuid(), 'b0000000-0000-0000-0000-000000000003',
   '10000000-0000-0000-0000-000000000022', 3, '2025-10-02 09:00:00'),
  (gen_random_uuid(), 'b0000000-0000-0000-0000-000000000003',
   '10000000-0000-0000-0000-000000000008', 4, '2025-10-02 09:00:00'),
  (gen_random_uuid(), 'b0000000-0000-0000-0000-000000000003',
   '10000000-0000-0000-0000-000000000017', 5, '2025-10-02 09:00:00')
ON CONFLICT DO NOTHING;

-- ============================================================
-- SCORES DELTA — Leaderboard de la partie 1
-- period_start/end = durée de la compétition
-- ============================================================
INSERT INTO team_score_deltas
  (id, game_id, participant_id, period_start, period_end, delta_pr, computed_at)
VALUES
  ('d0000000-0000-0000-0000-000000000001',
   'a0000000-0000-0000-0000-000000000001',
   'c0000000-0000-0000-0000-000000000001',  -- thibaut participant
   '2025-10-01', '2025-11-01', 450, '2025-11-01 18:00:00'),
  ('d0000000-0000-0000-0000-000000000002',
   'a0000000-0000-0000-0000-000000000001',
   'c0000000-0000-0000-0000-000000000002',  -- marcel participant
   '2025-10-01', '2025-11-01', 280, '2025-11-01 18:00:00'),
  ('d0000000-0000-0000-0000-000000000003',
   'a0000000-0000-0000-0000-000000000001',
   'c0000000-0000-0000-0000-000000000003',  -- teddy participant
   '2025-10-01', '2025-11-01', 120, '2025-11-01 18:00:00')
ON CONFLICT (game_id, participant_id, period_start, period_end) DO NOTHING;

-- ============================================================
-- PARTIE 2 — Tournoi Hiver 2025 (CREATING — en cours de setup)
-- Démontre : interface de création de partie, invitation
-- ============================================================
INSERT INTO games (id, name, creator_id, max_participants, status, created_at)
SELECT
  'a0000000-0000-0000-0000-000000000002',
  'Tournoi Hiver 2025',
  id,
  4,
  'CREATING',
  '2026-03-10 14:00:00'
FROM users WHERE username = 'thibaut'
ON CONFLICT (id) DO NOTHING;

-- Thibaut est créateur de la partie 2
INSERT INTO game_participants (id, game_id, user_id, draft_order, joined_at, is_creator)
SELECT
  'c0000000-0000-0000-0000-000000000010',
  'a0000000-0000-0000-0000-000000000002',
  id,
  1, '2026-03-10 14:00:00', TRUE
FROM users WHERE username = 'thibaut'
ON CONFLICT (game_id, user_id) DO NOTHING;

-- ============================================================
-- RANK SNAPSHOTS — Historique PR pour sparkline catalogue
-- 5 snapshots sur 5 semaines pour les top joueurs
-- ============================================================
INSERT INTO rank_snapshots (id, player_id, region, rank, pr_value, snapshot_date)
VALUES
  -- Bugha_EU : progression régulière (rank = PR_rank, plus bas = meilleur)
  (gen_random_uuid(), '10000000-0000-0000-0000-000000000001', 'EU', 3, 18200, '2025-09-01'),
  (gen_random_uuid(), '10000000-0000-0000-0000-000000000001', 'EU', 2, 19100, '2025-09-15'),
  (gen_random_uuid(), '10000000-0000-0000-0000-000000000001', 'EU', 1, 19800, '2025-10-01'),
  (gen_random_uuid(), '10000000-0000-0000-0000-000000000001', 'EU', 1, 20400, '2025-10-15'),
  (gen_random_uuid(), '10000000-0000-0000-0000-000000000001', 'EU', 1, 21050, '2025-11-01'),
  -- Aqua_EU : stable rank 2
  (gen_random_uuid(), '10000000-0000-0000-0000-000000000002', 'EU', 2, 17800, '2025-09-01'),
  (gen_random_uuid(), '10000000-0000-0000-0000-000000000002', 'EU', 3, 17500, '2025-09-15'),
  (gen_random_uuid(), '10000000-0000-0000-0000-000000000002', 'EU', 2, 18200, '2025-10-01'),
  (gen_random_uuid(), '10000000-0000-0000-0000-000000000002', 'EU', 2, 18600, '2025-10-15'),
  (gen_random_uuid(), '10000000-0000-0000-0000-000000000002', 'EU', 2, 19100, '2025-11-01'),
  -- Clix_NAW : montée rapide
  (gen_random_uuid(), '10000000-0000-0000-0000-000000000011', 'NAW', 5, 15200, '2025-09-01'),
  (gen_random_uuid(), '10000000-0000-0000-0000-000000000011', 'NAW', 3, 16400, '2025-09-15'),
  (gen_random_uuid(), '10000000-0000-0000-0000-000000000011', 'NAW', 2, 17100, '2025-10-01'),
  (gen_random_uuid(), '10000000-0000-0000-0000-000000000011', 'NAW', 1, 18000, '2025-10-15'),
  (gen_random_uuid(), '10000000-0000-0000-0000-000000000011', 'NAW', 1, 18900, '2025-11-01')
ON CONFLICT (player_id, region, snapshot_date) DO NOTHING;
