/**
 * Interfaces pour les données des joueurs Fortnite basées sur le CSV
 */

/**
 * Joueur Fortnite avec ses statistiques complètes
 */
export interface FortnitePlayer {
  /** Nom du joueur */
  name: string;
  /** Région assignée (EU, NAC, NAW, BR, ASIA, ME, OCE) */
  region: string;
  /** Score PR (Performance Rating) */
  score: number;
  /** Classement du joueur */
  ranking: number;
  /** Catégorie basée sur les performances 2024 */
  based2024: string;
}

/**
 * Équipe complète d'un pronostiqueur
 */
export interface FortniteTeam {
  /** Nom du pronostiqueur */
  pronostiqueur: string;
  /** Liste de tous ses joueurs */
  players: FortnitePlayer[];
  /** Score total de l'équipe */
  totalScore: number;
  /** Nombre de joueurs dans l'équipe */
  playerCount: number;
  /** Répartition par région */
  regionDistribution: { [region: string]: number };
}

/**
 * Données complètes du championnat
 */
export interface ChampionshipData {
  /** Équipe Marcel */
  marcel: FortniteTeam;
  /** Équipe Teddy */
  teddy: FortniteTeam;
  /** Équipe Thibaut */
  thibaut: FortniteTeam;
  /** Nombre total de joueurs */
  totalPlayers: number;
  /** Date de dernière mise à jour */
  lastUpdated: string;
}

/**
 * Statistiques par région
 */
export interface RegionStats {
  /** Code de la région */
  region: string;
  /** Nombre de joueurs dans cette région */
  playerCount: number;
  /** Score total des joueurs de cette région */
  totalScore: number;
  /** Score moyen des joueurs de cette région */
  averageScore: number;
}