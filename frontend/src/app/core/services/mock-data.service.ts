import { Injectable } from '@angular/core';
import { Observable, of, delay } from 'rxjs';
import { LeaderboardEntryDTO } from '../models/leaderboard.model';
import { Game } from '../../features/game/models/game.interface';
import { MOCK_GAMES } from '../data/mock-game-data';
import { LoggerService } from './logger.service';

/**
 * Simplified Mock Data Service
 * Provides basic fallback data for development
 */
@Injectable({
  providedIn: 'root'
})
export class MockDataService {
  private isBackendAvailable = false;
  
  constructor(private logger: LoggerService) {
    this.logger.debug('🎮 MockDataService initialized - Basic fallback ready');
  }

  /**
   * Basic Mock Games Data
   */
  getMockGames(): Observable<Game[]> {
    return of(MOCK_GAMES).pipe(delay(800));
  }

  /**
   * Basic Mock Leaderboard Data
   */
  getMockLeaderboard(): Observable<LeaderboardEntryDTO[]> {
    const mockLeaderboard: LeaderboardEntryDTO[] = [
      {
        rank: 1,
        teamId: 'team-1',
        teamName: 'Alpha Squad',
        ownerName: 'ProGamer_Alpha',
        totalPoints: 2850,
        lastUpdate: '2025-01-25T10:00:00Z',
        players: [
          { id: 'p1', name: 'Aqua', region: 'EU', tranche: 'Elite', points: 580 },
          { id: 'p2', name: 'Bugha', region: 'NAE', tranche: 'Elite', points: 560 }
        ]
      },
      {
        rank: 2,
        teamId: 'team-2',
        teamName: 'Victory Legends',
        ownerName: 'FortniteKing_92',
        totalPoints: 2720,
        lastUpdate: '2025-01-25T10:00:00Z',
        players: [
          { id: 'p3', name: 'Zayt', region: 'NAE', tranche: 'Elite', points: 570 },
          { id: 'p4', name: 'Saf', region: 'NAE', tranche: 'Elite', points: 550 }
        ]
      }
    ];

    return of(mockLeaderboard).pipe(delay(800));
  }

  /**
   * Basic Mock Statistics
   */
  getMockStatistics(): Observable<any> {
    const mockStats = {
      totalTeams: 2,
      totalPlayers: 4,
      totalPoints: 5570,
      averagePointsPerTeam: 2785,
      seasonProgress: 75
    };

    return of(mockStats).pipe(delay(600));
  }

  /**
   * Basic Mock Region Distribution
   */
  getMockRegionDistribution(): Observable<{ [key: string]: number }> {
    const mockDistribution = {
      'EU': 2,
      'NAE': 2,
      'NAW': 0,
      'BR': 0,
      'ASIA': 0,
      'OCE': 0,
      'ME': 0
    };

    return of(mockDistribution).pipe(delay(400));
  }

  /**
   * Basic Mock Dashboard Data
   */
  getMockDashboardData(gameId: string): Observable<any> {
    this.logger.debug('🎮 Loading basic mock data for gameId:', gameId);
    
    const mockData = {
      statistics: {
        totalTeams: 2,
        totalPlayers: 4,
        totalPoints: 5570,
        averagePointsPerTeam: 2785,
        seasonProgress: 75
      },
      leaderboard: [
        {
          rank: 1,
          teamId: 'team-1',
          teamName: 'Alpha Squad',
          ownerName: 'ProGamer_Alpha',
          totalPoints: 2850,
          lastUpdate: '2025-01-25T10:00:00Z',
          players: []
        }
      ],
      regionDistribution: {
        'EU': 2,
        'NAE': 2
      },
      teams: []
    };

    return of(mockData).pipe(delay(800));
  }

  /**
   * Simulate Backend Availability Check
   */
  checkBackendAvailability(): Observable<boolean> {
    return of(false).pipe(delay(1000));
  }
}
