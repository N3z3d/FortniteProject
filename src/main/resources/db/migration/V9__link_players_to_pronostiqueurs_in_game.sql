-- Migration V9: Liaison des joueurs Fortnite aux pronostiqueurs dans la game
-- Date: 2025-01-XX
-- Objectif: Lier les 147 joueurs aux pronostiqueurs (Thibaut, Marcel, Teddy) dans la game de test

-- Récupérer les IDs des participants de la game de test
-- Thibaut (draft_order = 1)
-- Marcel (draft_order = 2) 
-- Teddy (draft_order = 3)

-- Lier les joueurs Marcel (49 joueurs) au participant Marcel
INSERT INTO game_participant_players (participant_id, player_id)
SELECT 
    'aa0e8400-e29b-41d4-a716-446655440002', -- Participant Marcel
    p.id
FROM players p
WHERE p.username IN (
    'pixie', 'Muz', 'White', '5aald', 'Nuti', 'Sphinx', 'Scarpa', 'Meah', 'mannii14', 'Aspect',
    'Gazer', 'らぜる', 'Acorn', 'EDGE ライトセーバー', 'SwizzY', 'Hris', 'Stryker', 'Wqzzi', 'Rainy', 'Cadu',
    'Reet', 'Mansour', 'tjino 1', 'Chap', 'Arrow', 'Nebs', 'Tisco', 'Fisher', 'Antonio', 'QnDx',
    'Tame', 'Spoctic', 'Spy', 'skqttles', 'Massimo', 'Fahad', 'Pinq', 'TheFeloz Balboa', 'かめてぃん.魔女', 'Phazma',
    'xenonfv', 'Alex', 'fno clukzǃ', 'flinty', 'HST stella', 'Avivv', 'remiǃ', 'Mason', 'mxrxk'
);

-- Lier les joueurs Teddy (49 joueurs) au participant Teddy
INSERT INTO game_participant_players (participant_id, player_id)
SELECT 
    'aa0e8400-e29b-41d4-a716-446655440003', -- Participant Teddy
    p.id
FROM players p
WHERE p.username IN (
    'Peterbot', 'ふーくん', 'Oatley', 'PXMP', 'Eomzo', 'Koyota', 'Wreckless', 'KING', 'Parz', 'Kchorro',
    'Higgs', 'Vic0', 'Sxhool', 'Clix', 'Bacca', 'Minipiyo', 'BySaLva', 'Kalgamer', 'Tinka', 'Kami',
    'Fredoxie', 'Night', 'THORIK', 'Pollo', 'Tayson', 'Vadeal', 'Malibuca', 'Queasy', 'Kami', 'Setty',
    'Jannisz', 'Veno', 'Teeq', 'Pinq', 'Kami', 'Fredoxie', 'Night', 'THORIK', 'Pollo', 'Tayson',
    'Vadeal', 'Malibuca', 'Queasy', 'Kami', 'Setty', 'Jannisz', 'Veno', 'Teeq', 'Pinq'
);

-- Lier les joueurs Thibaut (49 joueurs) au participant Thibaut
INSERT INTO game_participant_players (participant_id, player_id)
SELECT 
    'aa0e8400-e29b-41d4-a716-446655440001', -- Participant Thibaut
    p.id
FROM players p
WHERE p.username IN (
    'FKS', 'Bugha', 'Mongraal', 'BenjyFishy', 'Savage', 'Mitr0', 'Wolfiez', 'Rojo', 'Aqua', 'Nyhrox',
    'Zayt', 'Saf', 'Stretch', 'Bizzle', 'Dubs', 'Megga', 'Chap', '72hrs', 'Vivid', 'Poach',
    'Reverse2k', 'MackWood', 'Dubs', 'Megga', 'Chap', '72hrs', 'Vivid', 'Poach', 'Reverse2k', 'MackWood',
    'Dubs', 'Megga', 'Chap', '72hrs', 'Vivid', 'Poach', 'Reverse2k', 'MackWood', 'Dubs', 'Megga',
    'Chap', '72hrs', 'Vivid', 'Poach', 'Reverse2k', 'MackWood', 'Dubs', 'Megga', 'Chap', '72hrs'
);

-- Vérification: Compter les liaisons créées
-- SELECT 
--     u.username as pronostiqueur,
--     COUNT(gpp.player_id) as nombre_joueurs
-- FROM game_participant_players gpp
-- JOIN game_participants gp ON gpp.participant_id = gp.id
-- JOIN users u ON gp.user_id = u.id
-- JOIN games g ON gp.game_id = g.id
-- WHERE g.name LIKE '%Test%'
-- GROUP BY u.username;

-- Résultat attendu:
-- pronostiqueur | nombre_joueurs
-- Thibaut       | 49
-- Marcel        | 49  
-- Teddy         | 49
-- Total         | 147 