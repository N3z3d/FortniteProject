-- Migration V6: Création des tables pour le système de games
-- Date: 2025-01-XX

-- Table des games
CREATE TABLE games (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    creator_id UUID NOT NULL,
    max_participants INTEGER NOT NULL DEFAULT 10,
    status VARCHAR(50) NOT NULL DEFAULT 'CREATING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_games_creator FOREIGN KEY (creator_id) REFERENCES users(id)
);

-- Table des règles régionales par game
CREATE TABLE game_region_rules (
    id UUID PRIMARY KEY,
    game_id UUID NOT NULL,
    region VARCHAR(10) NOT NULL,
    max_players INTEGER NOT NULL,
    CONSTRAINT fk_game_region_rules_game FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE,
    CONSTRAINT uk_game_region UNIQUE (game_id, region),
    CONSTRAINT chk_max_players CHECK (max_players >= 1 AND max_players <= 10)
);

-- Table des participants de game
CREATE TABLE game_participants (
    id UUID PRIMARY KEY,
    game_id UUID NOT NULL,
    user_id UUID NOT NULL,
    draft_order INTEGER,
    last_selection_time TIMESTAMP,
    CONSTRAINT fk_game_participants_game FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE,
    CONSTRAINT fk_game_participants_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_game_participant UNIQUE (game_id, user_id)
);

-- Table de liaison participants-joueurs sélectionnés
CREATE TABLE game_participant_players (
    participant_id UUID NOT NULL,
    player_id UUID NOT NULL,
    CONSTRAINT fk_game_participant_players_participant FOREIGN KEY (participant_id) REFERENCES game_participants(id) ON DELETE CASCADE,
    CONSTRAINT fk_game_participant_players_player FOREIGN KEY (player_id) REFERENCES players(id),
    CONSTRAINT pk_game_participant_players PRIMARY KEY (participant_id, player_id)
);

-- Index pour optimiser les performances
CREATE INDEX idx_games_creator ON games(creator_id);
CREATE INDEX idx_games_status ON games(status);
CREATE INDEX idx_game_region_rules_game ON game_region_rules(game_id);
CREATE INDEX idx_game_participants_game ON game_participants(game_id);
CREATE INDEX idx_game_participants_user ON game_participants(user_id);
CREATE INDEX idx_game_participant_players_participant ON game_participant_players(participant_id);
CREATE INDEX idx_game_participant_players_player ON game_participant_players(player_id); 