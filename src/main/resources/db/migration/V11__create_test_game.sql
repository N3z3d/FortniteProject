-- V11__create_test_game.sql
-- Créer une game de test avec Thibaut, Teddy et Marcel

-- Créer la game
INSERT INTO games (id, name, description, creator_id, max_participants, status, created_at, invitation_code) 
VALUES (
    'a1234567-8901-2345-6789-012345678901',
    'Game Thibaut-Teddy-Marcel',
    'Game de test entre amis pour le développement',
    '5764d97a-b1d4-449b-8127-d449b4d2ede6', -- Thibaut
    10,
    'CREATING',
    CURRENT_TIMESTAMP,
    'TTM2025'
);

-- Ajouter les participants
INSERT INTO game_participants (id, game_id, user_id, draft_order) VALUES
('b1234567-8901-2345-6789-012345678901', 'a1234567-8901-2345-6789-012345678901', '5764d97a-b1d4-449b-8127-d449b4d2ede6', 1), -- Thibaut
('b1234567-8901-2345-6789-012345678902', 'a1234567-8901-2345-6789-012345678901', '713f500c-dacd-4856-ba5f-eeb03f0e1219', 2), -- Teddy
('b1234567-8901-2345-6789-012345678903', 'a1234567-8901-2345-6789-012345678901', '7daa2318-6110-49f2-944b-2129fd57d732', 3); -- Marcel

-- Ajouter des règles par région
INSERT INTO game_region_rules (id, game_id, region, max_players) VALUES
('c1234567-8901-2345-6789-012345678901', 'a1234567-8901-2345-6789-012345678901', 'EU', 5),
('c1234567-8901-2345-6789-012345678902', 'a1234567-8901-2345-6789-012345678901', 'NAW', 3),
('c1234567-8901-2345-6789-012345678903', 'a1234567-8901-2345-6789-012345678901', 'BR', 2); 