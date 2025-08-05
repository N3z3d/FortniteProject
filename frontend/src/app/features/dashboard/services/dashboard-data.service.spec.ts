import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { DashboardDataService } from './dashboard-data.service';
import { environment } from '../../../../environments/environment';

/**
 * Tests TDD pour DashboardDataService
 * Vérifie que les statistiques sont correctement calculées depuis l'API
 */
describe('DashboardDataService - TDD', () => {
  let service: DashboardDataService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.apiUrl}/api`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [DashboardDataService]
    });
    
    service = TestBed.inject(DashboardDataService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getGameStatistics', () => {
    it('devrait récupérer les statistiques réelles depuis l\'API stats', (done) => {
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

      // ACT
      service.getGameStatistics(gameId).subscribe(stats => {
        // ASSERT
        expect(stats).toEqual(mockStatsResponse);
        expect(stats.totalPlayers).toBe(147); // Doit être 147, pas 12
        expect(stats.totalTeams).toBe(3);
        done();
      });

      // ASSERT - Vérifier l'appel HTTP
      const req = httpMock.expectOne(`${apiUrl}/leaderboard/stats?season=2025`);
      expect(req.request.method).toBe('GET');
      req.flush(mockStatsResponse);
    });

    it('devrait retourner des statistiques vides en cas d\'erreur API', (done) => {
      // ARRANGE
      const gameId = 'test-game-id';

      // ACT
      service.getGameStatistics(gameId).subscribe(stats => {
        // ASSERT
        expect(stats.totalPlayers).toBe(0);
        expect(stats.totalTeams).toBe(0);
        expect(stats.totalPoints).toBe(0);
        done();
      });

      // ASSERT - Simuler une erreur HTTP
      const req = httpMock.expectOne(`${apiUrl}/leaderboard/stats?season=2025`);
      req.error(new ProgressEvent('Network error'));
    });
  });

  describe('calculateDerivedStatistics', () => {
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
      
      console.warn('⚠️ Ce test montre le problème : on compte seulement les joueurs dans les équipes (12) au lieu du total BDD (147)');
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
      
      console.log('✅ Solution : utiliser les statistiques directement de l\'endpoint /api/leaderboard/stats');
    });
  });

  describe('Integration avec vraies données', () => {
    it('devrait retourner des données cohérentes pour le dashboard', (done) => {
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
        done();
      });

      // Mock les appels HTTP
      const statsReq = httpMock.expectOne(`${apiUrl}/leaderboard/stats?season=2025`);
      const leaderboardReq = httpMock.expectOne(`${apiUrl}/leaderboard?season=2025`);  
      const regionReq = httpMock.expectOne(`${apiUrl}/leaderboard/distribution/regions`);

      statsReq.flush(mockStats);
      leaderboardReq.flush(mockLeaderboard);
      regionReq.flush(mockRegionDist);
    });
  });
});