import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { LeaderboardFacade } from './leaderboard.facade';
import { HttpLeaderboardRepository, MockLeaderboardRepository } from '../repositories/leaderboard.repository';
import { DataSourceStrategy } from '../strategies/data-source.strategy';
import { LeaderboardFilters } from '../services/leaderboard.service';

describe('LeaderboardFacade', () => {
  let facade: LeaderboardFacade;
  let httpRepo: jasmine.SpyObj<HttpLeaderboardRepository>;
  let mockRepo: jasmine.SpyObj<MockLeaderboardRepository>;
  let strategy: jasmine.SpyObj<DataSourceStrategy>;

  beforeEach(() => {
    httpRepo = jasmine.createSpyObj('HttpLeaderboardRepository', [
      'getLeaderboard',
      'getPronostiqueurLeaderboard',
      'getPlayerLeaderboard',
      'getTeamLeaderboard',
      'getStats',
      'getRegionDistribution'
    ]);
    mockRepo = jasmine.createSpyObj('MockLeaderboardRepository', [
      'getLeaderboard',
      'getPronostiqueurLeaderboard',
      'getPlayerLeaderboard',
      'getTeamLeaderboard',
      'getStats',
      'getRegionDistribution'
    ]);
    strategy = jasmine.createSpyObj('DataSourceStrategy', ['executeWithFallback', 'isDatabaseAvailable'], {
      currentSource$: of(null)
    });

    TestBed.configureTestingModule({
      providers: [
        LeaderboardFacade,
        { provide: HttpLeaderboardRepository, useValue: httpRepo },
        { provide: MockLeaderboardRepository, useValue: mockRepo },
        { provide: DataSourceStrategy, useValue: strategy }
      ]
    });

    facade = TestBed.inject(LeaderboardFacade);
  });

  it('should create', () => {
    expect(facade).toBeTruthy();
  });

  it('should get leaderboard with fallback', (done) => {
    const filters: LeaderboardFilters = { season: 2025 };
    const mockData = [{
      rank: 1,
      userId: 'user1',
      username: 'test',
      isSpecial: false,
      totalPoints: 100,
      pointsByRegion: { EU: 50, NAW: 50 },
      regionsWon: 2,
      firstPlacePlayers: 1,
      worldChampions: 0,
      team: {
        id: 't1',
        name: 'Team A',
        season: 2025,
        tradesRemaining: 3,
        players: []
      },
      recentMovements: []
    }];
    const httpObs = of(mockData);
    const mockObs = of(mockData);

    httpRepo.getLeaderboard.and.returnValue(httpObs);
    mockRepo.getLeaderboard.and.returnValue(mockObs);
    strategy.executeWithFallback.and.returnValue(of(mockData));

    facade.getLeaderboard(filters).subscribe(result => {
      expect(result).toEqual(mockData);
      expect(strategy.executeWithFallback).toHaveBeenCalledWith(
        httpObs,
        mockObs,
        'Leaderboard',
        jasmine.objectContaining({ allowFallback: jasmine.any(Boolean) })
      );
      done();
    });
  });

  it('should get pronostiqueur leaderboard with fallback', (done) => {
    const season = 2025;
    const mockData = [{
      userId: 'user1',
      username: 'test',
      email: 'test@test.com',
      rank: 1,
      totalPoints: 100,
      totalTeams: 2,
      avgPointsPerTeam: 50,
      bestTeamPoints: 80,
      bestTeamName: 'Team A',
      victories: 1,
      winRate: 0.5
    }];
    const httpObs = of(mockData);
    const mockObs = of(mockData);

    httpRepo.getPronostiqueurLeaderboard.and.returnValue(httpObs);
    mockRepo.getPronostiqueurLeaderboard.and.returnValue(mockObs);
    strategy.executeWithFallback.and.returnValue(of(mockData));

    facade.getPronostiqueurLeaderboard(season).subscribe(result => {
      expect(result).toEqual(mockData);
      expect(httpRepo.getPronostiqueurLeaderboard).toHaveBeenCalledWith(season);
      expect(mockRepo.getPronostiqueurLeaderboard).toHaveBeenCalledWith(season);
      done();
    });
  });

  it('should get player leaderboard with fallback', (done) => {
    const season = 2025;
    const region = 'EU';
    const mockData = [{
      playerId: 'p1',
      nickname: 'test',
      username: 'user1',
      region: 'EU',
      tranche: '1',
      rank: 1,
      totalPoints: 100,
      avgPointsPerGame: 25,
      bestScore: 50,
      teamsCount: 2
    }];
    const httpObs = of(mockData);
    const mockObs = of(mockData);

    httpRepo.getPlayerLeaderboard.and.returnValue(httpObs);
    mockRepo.getPlayerLeaderboard.and.returnValue(mockObs);
    strategy.executeWithFallback.and.returnValue(of(mockData));

    facade.getPlayerLeaderboard(season, region).subscribe(result => {
      expect(result).toEqual(mockData);
      expect(httpRepo.getPlayerLeaderboard).toHaveBeenCalledWith(season, region);
      expect(mockRepo.getPlayerLeaderboard).toHaveBeenCalledWith(season, region);
      done();
    });
  });

  it('should get player leaderboard without region', (done) => {
    const season = 2025;
    const mockData = [{
      playerId: 'p1',
      nickname: 'test',
      username: 'user1',
      region: 'EU',
      tranche: '1',
      rank: 1,
      totalPoints: 100,
      avgPointsPerGame: 25,
      bestScore: 50,
      teamsCount: 2
    }];
    const httpObs = of(mockData);
    const mockObs = of(mockData);

    httpRepo.getPlayerLeaderboard.and.returnValue(httpObs);
    mockRepo.getPlayerLeaderboard.and.returnValue(mockObs);
    strategy.executeWithFallback.and.returnValue(of(mockData));

    facade.getPlayerLeaderboard(season).subscribe(result => {
      expect(result).toEqual(mockData);
      expect(httpRepo.getPlayerLeaderboard).toHaveBeenCalledWith(season, undefined);
      done();
    });
  });

  it('should get team leaderboard with fallback', (done) => {
    const mockData = [{ rank: 1, teamName: 'test', points: 500 }];
    const httpObs = of(mockData);
    const mockObs = of(mockData);

    httpRepo.getTeamLeaderboard.and.returnValue(httpObs);
    mockRepo.getTeamLeaderboard.and.returnValue(mockObs);
    strategy.executeWithFallback.and.returnValue(of(mockData));

    facade.getTeamLeaderboard().subscribe(result => {
      expect(result).toEqual(mockData);
      expect(httpRepo.getTeamLeaderboard).toHaveBeenCalled();
      expect(mockRepo.getTeamLeaderboard).toHaveBeenCalled();
      done();
    });
  });

  it('should get stats with fallback', (done) => {
    const season = 2025;
    const mockData = { totalPlayers: 100, totalTeams: 20 };
    const httpObs = of(mockData);
    const mockObs = of(mockData);

    httpRepo.getStats.and.returnValue(httpObs);
    mockRepo.getStats.and.returnValue(mockObs);
    strategy.executeWithFallback.and.returnValue(of(mockData));

    facade.getStats(season).subscribe(result => {
      expect(result).toEqual(mockData);
      expect(httpRepo.getStats).toHaveBeenCalledWith(season);
      expect(mockRepo.getStats).toHaveBeenCalledWith(season);
      done();
    });
  });

  it('should get region distribution with fallback', (done) => {
    const mockData = { EU: 50, NAW: 30, NAC: 20 };
    const httpObs = of(mockData);
    const mockObs = of(mockData);

    httpRepo.getRegionDistribution.and.returnValue(httpObs);
    mockRepo.getRegionDistribution.and.returnValue(mockObs);
    strategy.executeWithFallback.and.returnValue(of(mockData));

    facade.getRegionDistribution().subscribe(result => {
      expect(result).toEqual(mockData);
      expect(httpRepo.getRegionDistribution).toHaveBeenCalled();
      expect(mockRepo.getRegionDistribution).toHaveBeenCalled();
      done();
    });
  });

  it('should check if database is available', () => {
    strategy.isDatabaseAvailable.and.returnValue(true);

    const result = facade.isDatabaseAvailable();

    expect(result).toBeTrue();
    expect(strategy.isDatabaseAvailable).toHaveBeenCalled();
  });

  it('should return false when database is not available', () => {
    strategy.isDatabaseAvailable.and.returnValue(false);

    const result = facade.isDatabaseAvailable();

    expect(result).toBeFalse();
  });

  it('should get data source status observable', (done) => {
    facade.getDataSourceStatus$().subscribe(status => {
      expect(status).toBeNull();
      done();
    });
  });
});
