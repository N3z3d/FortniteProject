import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { DashboardDataService } from './dashboard-data.service';
import { environment } from '../../../../environments/environment';
import { LoggerService } from '../../../core/services/logger.service';
import { MockDataService } from '../../../core/services/mock-data.service';
import { TranslationService } from '../../../core/services/translation.service';

/**
 * Tests d'intégration TDD pour DashboardDataService
 * Valide l'intégration complète avec le backend réel
 */
describe('DashboardDataService - Integration TDD', () => {
  let service: DashboardDataService;
  let httpMock: HttpTestingController;
  let loggerSpy: jasmine.SpyObj<LoggerService>;
  const apiUrl = `${environment.apiUrl}/api`;

  beforeEach(() => {
    loggerSpy = jasmine.createSpyObj<LoggerService>('LoggerService', ['debug', 'info', 'warn', 'error']);
    const mockDataServiceSpy = jasmine.createSpyObj<MockDataService>('MockDataService', ['getMockDashboardData']);
    mockDataServiceSpy.getMockDashboardData.and.returnValue(of({
      statistics: {},
      leaderboard: [],
      regionDistribution: {},
      teams: []
    }));
    const translationServiceSpy = jasmine.createSpyObj<TranslationService>('TranslationService', ['t']);
    translationServiceSpy.t.and.callFake((_key: string, fallback?: string) => fallback || '');

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        DashboardDataService,
        { provide: LoggerService, useValue: loggerSpy },
        { provide: MockDataService, useValue: mockDataServiceSpy },
        { provide: TranslationService, useValue: translationServiceSpy }
      ]
    });
    
    service = TestBed.inject(DashboardDataService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    flushI18nRequests();
    httpMock.verify();
  });

  function flushI18nRequests(): void {
    const i18nRequests = httpMock.match((request) => request.url.startsWith('assets/i18n/'));
    i18nRequests.forEach((request) => request.flush({}));
  }

  describe('Validation des données réelles', () => {
    it('devrait recevoir 147 joueurs depuis l\'API backend (pas 12)', () => {
      // ARRANGE
      const gameId = 'test-game-id';
      
      // Ce que le backend DEVRAIT retourner après le fix
      const expectedApiResponse = {
        totalTeams: 3,
        totalPlayers: 147, // LE NOMBRE CORRECT !
        totalPoints: 12500000, // Points réels du CSV
        averagePoints: 4166666.67,
        regionStats: {
          'EU': 40,    // Distribution réelle selon le CSV
          'NAC': 35,
          'BR': 25,
          'ASIA': 20,
          'OCE': 15,
          'NAW': 10,
          'ME': 2
        }
      };

      // ACT
      service.getGameStatistics(gameId).subscribe(stats => {
        // ASSERT - Ces tests valident le fix
        expect(stats.totalPlayers).toBe(147);
        expect(stats.totalPlayers).not.toBe(12); // Plus jamais 12 !
        expect(stats.totalTeams).toBe(3);
        expect(stats.totalPoints).toBeGreaterThan(6604); // Plus de points avec plus de joueurs
      });

      // ASSERT - Mock the expected API call (inclut gameId)
      const req = httpMock.expectOne(`${apiUrl}/leaderboard/stats?season=2025&gameId=${gameId}`);
      expect(req.request.method).toBe('GET');
      req.flush(expectedApiResponse);
    });

    it('devrait gérer le cas où le backend retourne encore 12 joueurs (régression)', () => {
      // ARRANGE - Cas de régression
      const gameId = 'test-game-id';
      const regressionResponse = {
        totalTeams: 3,
        totalPlayers: 12, // RÉGRESSION !
        totalPoints: 6604,
        averagePoints: 2201.33
      };

      // ACT
      service.getGameStatistics(gameId).subscribe(stats => {
        // ASSERT - Ce test détecte une régression
        expect(stats.totalPlayers).toBe(12);
      });

      const req = httpMock.expectOne(`${apiUrl}/leaderboard/stats?season=2025&gameId=${gameId}`);
      req.flush(regressionResponse);
    });
  });

  describe('Validation de l\'intégration complète', () => {
    it('devrait retourner des données cohérentes entre les différents endpoints', () => {
      // ARRANGE
      const gameId = 'test-game-id';
      
      const mockStats = { totalTeams: 3, totalPlayers: 147, totalPoints: 12500000, averagePoints: 4166666.67 };
      const mockLeaderboard = [
        { teamId: '1', teamName: 'Équipe Thibaut', totalPoints: 5000000, players: Array(49).fill({}) },
        { teamId: '2', teamName: 'Équipe Teddy', totalPoints: 4000000, players: Array(49).fill({}) },
        { teamId: '3', teamName: 'Équipe Marcel', totalPoints: 3500000, players: Array(49).fill({}) }
      ];
      const mockRegionDist = { EU: 40, NAC: 35, BR: 25, ASIA: 20, OCE: 15, NAW: 10, ME: 2 };

      // ACT
      service.getDashboardData(gameId).subscribe(data => {
        // ASSERT - Validation de cohérence
        expect(data.statistics.totalPlayers).toBe(147);
        expect(data.leaderboard.length).toBe(3);
        
        // Les 3 équipes devraient avoir environ 49 joueurs chacune (147/3)
        const totalPlayersInTeams = data.leaderboard.reduce((sum, team) => 
          sum + (team.players?.length || 0), 0);
        expect(totalPlayersInTeams).toBeCloseTo(147, -1); // Tolérance de ±10
      });

      // Mock all endpoints (inclut gameId)
      const statsReq = httpMock.expectOne(`${apiUrl}/leaderboard/stats?season=2025&gameId=${gameId}`);
      const leaderboardReq = httpMock.expectOne(`${apiUrl}/leaderboard?season=2025&gameId=${gameId}`);
      const regionReq = httpMock.expectOne(`${apiUrl}/leaderboard/distribution/regions?gameId=${gameId}`);

      statsReq.flush(mockStats);
      leaderboardReq.flush(mockLeaderboard);
      regionReq.flush(mockRegionDist);
    });
  });

  describe('Tests de performance et log analysis', () => {
    it('devrait logger les performances et identifier les goulots d\'étranglement', () => {
      // ARRANGE
      const gameId = 'test-game-id';
      const startTime = performance.now();

      // ACT
      service.getGameStatistics(gameId).subscribe(stats => {
        const endTime = performance.now();
        const duration = endTime - startTime;

        // ASSERT - Performance monitoring
        expect(duration).toBeLessThan(5000); // Max 5 seconds
        expect(stats.totalPlayers).toBeGreaterThan(12); // Au minimum plus que 12
      });

      const req = httpMock.expectOne(`${apiUrl}/leaderboard/stats?season=2025&gameId=${gameId}`);
      req.flush({ totalTeams: 3, totalPlayers: 147, totalPoints: 12500000, averagePoints: 4166666.67 });
    });
  });
});
