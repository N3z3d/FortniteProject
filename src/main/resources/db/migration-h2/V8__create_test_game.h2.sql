-- Migration V8: Création d'une game de test et liaison des équipes
-- Date: 2025-01-XX

-- Création d'une game de test
INSERT INTO games (id, name, creator_id, max_participants, status, created_at) VALUES
('880e8400-e29b-41d4-a716-446655440000', 'Game de Test - Thibaut vs Marcel vs Teddy', '550e8400-e29b-41d4-a716-446655440001', 10, 'CREATING', CURRENT_TIMESTAMP);

-- Règles régionales par défaut (7 joueurs par région)
INSERT INTO game_region_rules (id, game_id, region, max_players) VALUES
('990e8400-e29b-41d4-a716-446655440001', '880e8400-e29b-41d4-a716-446655440000', 'EU', 7),
('990e8400-e29b-41d4-a716-446655440002', '880e8400-e29b-41d4-a716-446655440000', 'NAC', 7),
('990e8400-e29b-41d4-a716-446655440003', '880e8400-e29b-41d4-a716-446655440000', 'BR', 7),
('990e8400-e29b-41d4-a716-446655440004', '880e8400-e29b-41d4-a716-446655440000', 'ASIA', 7),
('990e8400-e29b-41d4-a716-446655440005', '880e8400-e29b-41d4-a716-446655440000', 'OCE', 7),
('990e8400-e29b-41d4-a716-446655440006', '880e8400-e29b-41d4-a716-446655440000', 'NAW', 7),
('990e8400-e29b-41d4-a716-446655440007', '880e8400-e29b-41d4-a716-446655440000', 'ME', 7);

-- Ajout des 3 participants (Thibaut, Marcel, Teddy)
INSERT INTO game_participants (id, game_id, user_id, draft_order, last_selection_time) VALUES
('aa0e8400-e29b-41d4-a716-446655440001', '880e8400-e29b-41d4-a716-446655440000', '550e8400-e29b-41d4-a716-446655440001', 1, CURRENT_TIMESTAMP), -- Thibaut
('aa0e8400-e29b-41d4-a716-446655440002', '880e8400-e29b-41d4-a716-446655440000', '550e8400-e29b-41d4-a716-446655440002', 2, CURRENT_TIMESTAMP), -- Marcel
('aa0e8400-e29b-41d4-a716-446655440003', '880e8400-e29b-41d4-a716-446655440000', '550e8400-e29b-41d4-a716-446655440003', 3, CURRENT_TIMESTAMP); -- Teddy

-- Note: Sarah n'est pas ajoutée car elle a 0 game
-- Les team_players existants restent liés aux équipes
-- Les game_participant_players seront créés lors du draft 