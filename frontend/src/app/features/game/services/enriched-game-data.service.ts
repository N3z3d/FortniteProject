import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CsvDataService } from './csv-data.service';
import { FortniteTeam, ChampionshipData, RegionStats } from '../models/fortnite-player.interface';

/**
 * Service pour fournir des données enrichies basées sur le CSV
 * Combine les données brutes CSV avec des statistiques calculées
 */
@Injectable({
  providedIn: 'root'
})
export class EnrichedGameDataService {

  constructor(private csvDataService: CsvDataService) { }

  /**
   * Obtient les données complètes du championnat avec 147 joueurs
   */
  getChampionshipData(): Observable<ChampionshipData> {
    return new Observable(observer => {
      setTimeout(() => {
        const data = this.csvDataService.parseChampionshipData();
        observer.next(data);
        observer.complete();
      }, 100);
    });
  }

  /**
   * Obtient les statistiques détaillées d'une équipe
   */
  getTeamDetailedStats(pronostiqueur: 'Marcel' | 'Teddy' | 'Thibaut'): Observable<any> {
    return new Observable(observer => {
      setTimeout(() => {
        const championship = this.csvDataService.parseChampionshipData();
        const team = championship[pronostiqueur.toLowerCase() as keyof ChampionshipData] as FortniteTeam;
        const stats = this.csvDataService.getTeamDetailedStats(team);
        observer.next(stats);
        observer.complete();
      }, 100);
    });
  }

  /**
   * Obtient les statistiques par région pour tous les joueurs
   */
  getRegionStatistics(): Observable<RegionStats[]> {
    return new Observable(observer => {
      setTimeout(() => {
        const championship = this.csvDataService.parseChampionshipData();
        const regionStats = this.csvDataService.calculateRegionStats(championship);
        observer.next(regionStats);
        observer.complete();
      }, 100);
    });
  }

  /**
   * Obtient le classement des équipes par score total
   */
  getTeamRankings(): Observable<Array<{rank: number, team: FortniteTeam, averageScore: number}>> {
    return new Observable(observer => {
      setTimeout(() => {
        const championship = this.csvDataService.parseChampionshipData();
        
        const rankings = [
          { team: championship.marcel, name: 'Marcel' },
          { team: championship.teddy, name: 'Teddy' },
          { team: championship.thibaut, name: 'Thibaut' }
        ]
        .sort((a, b) => b.team.totalScore - a.team.totalScore)
        .map((item, index) => ({
          rank: index + 1,
          team: item.team,
          averageScore: Math.round(item.team.totalScore / item.team.playerCount)
        }));

        observer.next(rankings);
        observer.complete();
      }, 100);
    });
  }

  /**
   * Obtient les meilleurs joueurs toutes équipes confondues
   */
  getTopPlayers(limit: number = 10): Observable<Array<{player: any, team: string, rank: number}>> {
    return new Observable(observer => {
      setTimeout(() => {
        const championship = this.csvDataService.parseChampionshipData();
        
        // Rassembler tous les joueurs avec leur équipe
        const allPlayers: Array<{player: any, team: string}> = [];
        
        championship.marcel.players.forEach(player => {
          allPlayers.push({ player, team: 'Marcel' });
        });
        
        championship.teddy.players.forEach(player => {
          allPlayers.push({ player, team: 'Teddy' });
        });
        
        championship.thibaut.players.forEach(player => {
          allPlayers.push({ player, team: 'Thibaut' });
        });

        // Trier par score et prendre les meilleurs
        const topPlayers = allPlayers
          .sort((a, b) => b.player.score - a.player.score)
          .slice(0, limit)
          .map((item, index) => ({
            ...item,
            rank: index + 1
          }));

        observer.next(topPlayers);
        observer.complete();
      }, 100);
    });
  }

  /**
   * Obtient la répartition des joueurs par région
   */
  getRegionDistribution(): Observable<Array<{region: string, count: number, percentage: number}>> {
    return new Observable(observer => {
      setTimeout(() => {
        const regionStats = this.csvDataService.calculateRegionStats(
          this.csvDataService.parseChampionshipData()
        );
        
        const total = regionStats.reduce((sum, stat) => sum + stat.playerCount, 0);
        
        const distribution = regionStats.map(stat => ({
          region: stat.region,
          count: stat.playerCount,
          percentage: Math.round((stat.playerCount / total) * 100)
        }));

        observer.next(distribution);
        observer.complete();
      }, 100);
    });
  }

  /**
   * Obtient les statistiques de performance par région
   */
  getRegionPerformanceStats(): Observable<Array<{region: string, teams: string[], averageScore: number}>> {
    return new Observable(observer => {
      setTimeout(() => {
        const championship = this.csvDataService.parseChampionshipData();
        const regionPerformance = new Map<string, {scores: number[], teams: Set<string>}>();

        // Analyser chaque équipe
        [
          { team: championship.marcel, name: 'Marcel' },
          { team: championship.teddy, name: 'Teddy' },
          { team: championship.thibaut, name: 'Thibaut' }
        ].forEach(({ team, name }) => {
          team.players.forEach(player => {
            if (!regionPerformance.has(player.region)) {
              regionPerformance.set(player.region, { scores: [], teams: new Set() });
            }
            
            const regionData = regionPerformance.get(player.region)!;
            regionData.scores.push(player.score);
            regionData.teams.add(name);
          });
        });

        // Calculer les statistiques moyennes par région
        const stats = Array.from(regionPerformance.entries()).map(([region, data]) => ({
          region,
          teams: Array.from(data.teams),
          averageScore: Math.round(data.scores.reduce((sum, score) => sum + score, 0) / data.scores.length)
        })).sort((a, b) => b.averageScore - a.averageScore);

        observer.next(stats);
        observer.complete();
      }, 100);
    });
  }

  /**
   * Valide l'intégrité des données et retourne un rapport
   */
  validateChampionshipData(): Observable<{isValid: boolean, errors: string[], summary: any}> {
    return new Observable(observer => {
      setTimeout(() => {
        const championship = this.csvDataService.parseChampionshipData();
        const validation = this.csvDataService.validateData(championship);
        
        const summary = {
          totalPlayers: championship.totalPlayers,
          teams: {
            marcel: {
              players: championship.marcel.playerCount,
              score: championship.marcel.totalScore,
              avgScore: Math.round(championship.marcel.totalScore / championship.marcel.playerCount)
            },
            teddy: {
              players: championship.teddy.playerCount,
              score: championship.teddy.totalScore,
              avgScore: Math.round(championship.teddy.totalScore / championship.teddy.playerCount)
            },
            thibaut: {
              players: championship.thibaut.playerCount,
              score: championship.thibaut.totalScore,
              avgScore: Math.round(championship.thibaut.totalScore / championship.thibaut.playerCount)
            }
          },
          lastUpdated: championship.lastUpdated
        };

        observer.next({
          ...validation,
          summary
        });
        observer.complete();
      }, 100);
    });
  }
}