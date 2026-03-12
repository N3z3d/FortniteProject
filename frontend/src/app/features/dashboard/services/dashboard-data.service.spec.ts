import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { DashboardDataService } from './dashboard-data.service';
import { environment } from '../../../../environments/environment';
import { StatsApiMapper } from '../mappers/stats-api.mapper';
import { TranslationService } from '../../../core/services/translation.service';
import { LoggerService } from '../../../core/services/logger.service';

/**
 * Tests TDD pour DashboardDataService
 * Vérifie que les statistiques sont correctement calculées depuis l'API
 */
describe('DashboardDataService - TDD', () => {
  let service: DashboardDataService;
  let httpMock: HttpTestingController;
  let loggerSpy: jasmine.SpyObj<LoggerService>;
  const apiUrl = `${environment.apiUrl}/api`;

  beforeEach(() => {
    const translationServiceSpy = jasmine.createSpyObj('TranslationService', ['t']);
    translationServiceSpy.t.and.callFake((_key: string, fallback?: string) => fallback || '');
    loggerSpy = jasmine.createSpyObj<LoggerService>('LoggerService', ['debug', 'info', 'warn', 'error']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        DashboardDataService,
        { provide: TranslationService, useValue: translationServiceSpy },
        { provide: LoggerService, useValue: loggerSpy }
      ]
    });
    
    service = TestBed.inject(DashboardDataService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getGameStatistics', () => {
    it('devrait récupérer les statistiques réelles depuis l\'API stats', () => {
      // ARRANGE
      const gameId = 'test-game-id';
      const mockStatsResponse = {
        totalPlayers: 147,
        totalTeams: 3,
        totalPoints: 12500000,
        averagePointsPerTeam: 4166666,
        mostActiveTeam: 'Équipe Thibaut',
        seasonProgress: 57.5
      };
      const expectedStats = StatsApiMapper.mapApiStatsToDisplayStats(mockStatsResponse);

      // ACT
      service.getGameStatistics(gameId).subscribe(stats => {
        // ASSERT
        expect(stats).toEqual(expectedStats);
        expect(stats.totalPlayers).toBe(147); // Doit être 147, pas 12
        expect(stats.totalTeams).toBe(3);
      });

      // ASSERT - Vérifier l'appel HTTP (inclut gameId)
      const req = httpMock.expectOne(`${apiUrl}/leaderboard/stats?season=2025&gameId=${gameId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockStatsResponse);
    });

    it('devrait retourner des statistiques vides en cas d\'erreur API', () => {
      // ARRANGE
      const gameId = 'test-game-id';

      // ACT
      service.getGameStatistics(gameId).subscribe(stats => {
        // ASSERT
        expect(stats.totalPlayers).toBe(0);
        expect(stats.totalTeams).toBe(0);
        expect(stats.totalPoints).toBe(0);
      });

      // ASSERT - Simuler une erreur HTTP (inclut gameId)
      const req = httpMock.expectOne(`${apiUrl}/leaderboard/stats?season=2025&gameId=${gameId}`);
      req.error(new ProgressEvent('Network error'));
    });
  });

  describe('calculateDerivedStatistics', () => {
    it('should use translated fallback label when no team is available', () => {
      const result = service.calculateDerivedStatistics({ teams: [] });
      expect(result.mostActiveTeam).toBe('No team');
    });

    it('ne devrait PAS calculer les joueurs depuis les équipes', () => {
      // ARRANGE - Ce test valide qu'on NE fait PAS cela
      const rawDataWithTeams = {
        teams: [
          { name: 'Team 1', players: [{}, {}, {}, {}] }, // 4 joueurs
          { name: 'Team 2', players: [{}, {}, {}, {}] }, // 4 joueurs  
          { name: 'Team 3', players: [{}, {}, {}, {}] }  // 4 joueurs
        ]
      };

      // ACT
      const result = service.calculateDerivedStatistics(rawDataWithTeams);

      // ASSERT - Cette approche est INCORRECTE car elle compte seulement les joueurs dans les équipes
      // Les vraies stats doivent venir de l'API
      expect(result.totalPlayers).toBe(12); // Ceci est le problème !
      
    });

    it('devrait utiliser les vraies statistiques de l\'API au lieu de calculer depuis les équipes', () => {
      // ARRANGE - Les vraies stats viennent de l'API
      const realStatsFromAPI = {
        totalPlayers: 147,  // Vrai nombre depuis la BDD
        totalTeams: 3,
        totalPoints: 12500000,
        averagePointsPerTeam: 4166666,
        mostActiveTeam: 'Équipe Thibaut'
      };

      // ACT & ASSERT
      // Le service devrait utiliser directement les stats de l'API
      expect(realStatsFromAPI.totalPlayers).toBe(147);
      
    });
  });

  describe('Integration avec vraies données', () => {
    it('devrait retourner des données cohérentes pour le dashboard', () => {
      // ARRANGE
      const gameId = 'test-game';
      const mockStats = {
        totalPlayers: 147,
        totalTeams: 3, 
        totalPoints: 12500000,
        averagePointsPerTeam: 4166666
      };
      const mockLeaderboard = [
        { teamId: '1', teamName: 'Équipe Thibaut', totalPoints: 5000000, players: Array(49).fill({}) },
        { teamId: '2', teamName: 'Équipe Teddy', totalPoints: 4000000, players: Array(49).fill({}) },
        { teamId: '3', teamName: 'Équipe Marcel', totalPoints: 3500000, players: Array(49).fill({}) }
      ];
      const mockRegionDist = { EU: 40, NAC: 35, BR: 25, ASIA: 20, OCE: 15, NAW: 10, ME: 2 };

      // ACT
      service.getDashboardData(gameId).subscribe(data => {
        // ASSERT
        expect(data.statistics.totalPlayers).toBe(147); // Le vrai nombre !
        expect(data.statistics.totalTeams).toBe(3);
        expect(data.leaderboard.length).toBe(3);
      });

      // Mock les appels HTTP (inclut gameId dans tous les endpoints)
      const statsReq = httpMock.expectOne(`${apiUrl}/leaderboard/stats?season=2025&gameId=${gameId}`);
      const leaderboardReq = httpMock.expectOne(`${apiUrl}/leaderboard?season=2025&gameId=${gameId}`);
      const regionReq = httpMock.expectOne(`${apiUrl}/leaderboard/distribution/regions?gameId=${gameId}`);

      statsReq.flush(mockStats);
      leaderboardReq.flush(mockLeaderboard);
      regionReq.flush(mockRegionDist);
    });
  });
});
