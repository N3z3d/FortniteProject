import { Injectable } from '@angular/core';

export interface PlayerWithPoints {
  points: number;
  [key: string]: any;
}

export interface TeamWithPlayers {
  players: PlayerWithPoints[];
  [key: string]: any;
}

@Injectable({
  providedIn: 'root'
})
export class StatsCalculationService {

  /**
   * Calcule le nombre de joueurs d'une équipe dans le top K% global
   * @param team L'équipe à analyser
   * @param allTeams Toutes les équipes pour le calcul global
   * @param percentile Le percentile (par défaut 10 pour top 10%)
   * @returns Le nombre de joueurs de l'équipe dans le top K%
   */
  getTopPercentileCount(team: TeamWithPlayers, allTeams: TeamWithPlayers[], percentile: number = 10): number {
    if (!team.players?.length || !allTeams?.length) return 0;
    
    // Récupérer tous les joueurs de toutes les équipes
    const allPlayers = allTeams.flatMap(t => t.players || []);
    if (allPlayers.length === 0) return 0;
    
    // Trier tous les joueurs par points décroissants
    const sortedAllPlayers = [...allPlayers].sort((a, b) => (b.points || 0) - (a.points || 0));
    
    // Calculer l'index du top K% (Math.ceil pour toujours inclure au moins 1 joueur)
    const topKIndex = Math.ceil(sortedAllPlayers.length * (percentile / 100));
    
    // Si l'index est 0 ou négatif, prendre au moins le premier joueur
    const adjustedIndex = Math.max(0, Math.min(topKIndex - 1, sortedAllPlayers.length - 1));
    
    // Obtenir le seuil de points pour être dans le top K%
    const threshold = sortedAllPlayers[adjustedIndex]?.points || 0;
    
    // Compter les joueurs de cette équipe qui dépassent le seuil
    return team.players.filter(player => (player.points || 0) >= threshold).length;
  }

  /**
   * Calcule le ratio de performance par région
   * @param regionPoints Points obtenus par les joueurs de la région
   * @param regionTotalAvailable Total des points disponibles dans la région
   * @returns Le ratio de performance (0-1)
   */
  getRegionPerformanceRatio(regionPoints: number, regionTotalAvailable: number): number {
    if (regionTotalAvailable === 0) return 0;
    return regionPoints / regionTotalAvailable;
  }

  /**
   * Formate les points pour l'affichage
   * @param points Nombre de points
   * @returns String formaté (ex: "1.2M", "150K", "999")
   */
  formatPoints(points: number): string {
    if (points >= 1000000) {
      return (points / 1000000).toFixed(1) + 'M';
    } else if (points >= 1000) {
      return (points / 1000).toFixed(1) + 'K';
    }
    return points.toLocaleString();
  }
} 