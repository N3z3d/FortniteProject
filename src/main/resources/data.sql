-- Script de données de test pour H2
-- Ce fichier sera exécuté automatiquement au démarrage de l'application

-- Créer les utilisateurs de test (syntaxe H2)
INSERT INTO users (id, username, email, password, role, current_season, created_at, updated_at)
SELECT 
    RANDOM_UUID(), 'Thibaut', 'thibaut@fortnite-pronos.com', '$2a$10$dummy.password.hash.for.testing', 'PARTICIPANT', 2025, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'Thibaut');

INSERT INTO users (id, username, email, password, role, current_season, created_at, updated_at)
SELECT 
    RANDOM_UUID(), 'Teddy', 'teddy@fortnite-pronos.com', '$2a$10$dummy.password.hash.for.testing', 'PARTICIPANT', 2025, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'Teddy');

INSERT INTO users (id, username, email, password, role, current_season, created_at, updated_at)
SELECT 
    RANDOM_UUID(), 'Marcel', 'marcel@fortnite-pronos.com', '$2a$10$dummy.password.hash.for.testing', 'PARTICIPANT', 2025, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'Marcel');

-- Vérifier que les utilisateurs ont été créés
SELECT 'Users créés:' as info, username, email, role FROM users WHERE username IN ('Thibaut', 'Teddy', 'Marcel'); 