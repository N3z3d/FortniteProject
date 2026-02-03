import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';

import { TeamDetailComponent } from './team-detail.component';
import { UserContextService } from '../../../core/services/user-context.service';
import { TeamDetailDataService, Team } from '../services/team-detail-data.service';
import { TeamDetailStatsService, TeamStats } from '../services/team-detail-stats.service';

describe('TeamDetailComponent', () => {
  let fixture: ComponentFixture<TeamDetailComponent>;
  let component: TeamDetailComponent;
  let userContextSpy: jasmine.SpyObj<UserContextService>;
  let dataServiceSpy: jasmine.SpyObj<TeamDetailDataService>;
  let statsServiceSpy: jasmine.SpyObj<TeamDetailStatsService>;
  let routeStub: { snapshot: { paramMap: ReturnType<typeof convertToParamMap> } };

  const mockTeam: Team = {
    name: 'Team Alpha',
    totalPoints: 1500,
    players: [
      { name: 'Player1', points: 800, region: 'NAE', rank: 1, tranche: 'TRANCHE_1' },
      { name: 'Player2', points: 400, region: 'NAW', rank: 2, tranche: 'TRANCHE_2' },
      { name: 'Player3', points: 300, region: 'EU', rank: 3, tranche: 'TRANCHE_3' }
    ]
  } as any;

  const mockStats: TeamStats = {
    totalPoints: 1500,
    averagePoints: 500,
    topPlayer: { name: 'Player1', points: 800 } as any,
    regionDistribution: { NAE: 800, NAW: 400, EU: 300 }
  } as any;

  beforeEach(async () => {
    userContextSpy = jasmine.createSpyObj<UserContextService>('UserContextService', ['getCurrentUser']);
    dataServiceSpy = jasmine.createSpyObj<TeamDetailDataService>('TeamDetailDataService', ['loadMyTeam', 'loadTeamById']);
    statsServiceSpy = jasmine.createSpyObj<TeamDetailStatsService>('TeamDetailStatsService', [
      'calculateStats',
      'getRegionColor',
      'getRegionFlag',
      'getTrancheColor',
      'getTrancheNumber',
      'getPlayerRank',
      'formatPoints',
      'getTopPlayers',
      'getSortedPlayers',
      'getProgressPercentage',
      'getRegionPercentage',
      'getRegionArcLength',
      'getRegionArcOffset',
      'getSortedRegionsByPoints',
      'getTop10PercentPlayersCount',
      'getRegionRatio',
      'getSortedRegionsByRatio',
      'getAverageRatio',
      'getRegionArcLengthByRatio',
      'getRegionArcOffsetByRatio'
    ]);
    routeStub = { snapshot: { paramMap: convertToParamMap({}) } };

    userContextSpy.getCurrentUser.and.returnValue({ username: 'Thibaut' } as any);
    statsServiceSpy.calculateStats.and.returnValue(mockStats);

    await TestBed.configureTestingModule({
      imports: [TeamDetailComponent, HttpClientTestingModule],
      providers: [
        { provide: UserContextService, useValue: userContextSpy },
        { provide: TeamDetailDataService, useValue: dataServiceSpy },
        { provide: TeamDetailStatsService, useValue: statsServiceSpy },
        { provide: ActivatedRoute, useValue: routeStub }
      ]
    }).compileComponents();
  });

  describe('ngOnInit', () => {
    it('should call loadTeamById when route has id param', () => {
      routeStub.snapshot.paramMap = convertToParamMap({ id: 'team-123' });
      dataServiceSpy.loadTeamById.and.returnValue(of({ team: mockTeam, allTeams: [mockTeam], error: null }));

      fixture = TestBed.createComponent(TeamDetailComponent);
      component = fixture.componentInstance;
      // Don't call detectChanges to avoid template rendering issues
      component.ngOnInit();

      expect(dataServiceSpy.loadTeamById).toHaveBeenCalledWith('team-123');
      expect(component.team).toEqual(mockTeam);
    });

    it('should call loadMyTeam when route has no id param', () => {
      dataServiceSpy.loadMyTeam.and.returnValue(of({ team: mockTeam, allTeams: [mockTeam], error: null }));

      fixture = TestBed.createComponent(TeamDetailComponent);
      component = fixture.componentInstance;
      // Don't call detectChanges to avoid template rendering issues
      component.ngOnInit();

      expect(dataServiceSpy.loadMyTeam).toHaveBeenCalledWith('Thibaut');
      expect(component.team).toEqual(mockTeam);
    });
  });

  describe('loadMyTeam', () => {
    beforeEach(() => {
      fixture = TestBed.createComponent(TeamDetailComponent);
      component = fixture.componentInstance;
    });

    it('should load team successfully', () => {
      dataServiceSpy.loadMyTeam.and.returnValue(of({ team: mockTeam, allTeams: [mockTeam], error: null }));

      component.loadMyTeam();

      expect(component.loading).toBe(false);
      expect(component.team).toEqual(mockTeam);
      expect(component.allTeams).toEqual([mockTeam]);
      expect(component.error).toBeNull();
    });

    it('should handle error when user not found', () => {
      userContextSpy.getCurrentUser.and.returnValue(null);

      component.loadMyTeam();

      expect(component.error).toBe('User not found');
      expect(component.team).toBeNull();
      expect(component.loading).toBe(false);
    });

    it('should calculate stats after successful load', () => {
      dataServiceSpy.loadMyTeam.and.returnValue(of({ team: mockTeam, allTeams: [], error: null }));

      component.loadMyTeam();

      expect(statsServiceSpy.calculateStats).toHaveBeenCalledWith(mockTeam);
      expect(component.stats).toEqual(mockStats);
    });
  });

  describe('loadTeamById', () => {
    beforeEach(() => {
      fixture = TestBed.createComponent(TeamDetailComponent);
      component = fixture.componentInstance;
    });

    it('should load team by id successfully', () => {
      dataServiceSpy.loadTeamById.and.returnValue(of({ team: mockTeam, allTeams: [mockTeam], error: null }));

      component.loadTeamById('team-123');

      expect(component.loading).toBe(false);
      expect(component.team).toEqual(mockTeam);
      expect(component.allTeams).toEqual([mockTeam]);
      expect(component.error).toBeNull();
    });

    it('should handle error from data service', () => {
      const errorMessage = 'Team not found';
      dataServiceSpy.loadTeamById.and.returnValue(of({ team: null, allTeams: [], error: errorMessage }));

      component.loadTeamById('team-123');

      expect(component.error).toBe(errorMessage);
      expect(component.team).toBeNull();
      expect(component.loading).toBe(false);
    });
  });

  describe('retryLoad', () => {
    beforeEach(() => {
      fixture = TestBed.createComponent(TeamDetailComponent);
      component = fixture.componentInstance;
    });

    it('should call loadTeamById when route has id', () => {
      routeStub.snapshot.paramMap = convertToParamMap({ id: 'team-456' });
      dataServiceSpy.loadTeamById.and.returnValue(of({ team: mockTeam, allTeams: [], error: null }));

      component.retryLoad();

      expect(dataServiceSpy.loadTeamById).toHaveBeenCalledWith('team-456');
    });

    it('should call loadMyTeam when route has no id', () => {
      dataServiceSpy.loadMyTeam.and.returnValue(of({ team: mockTeam, allTeams: [], error: null }));

      component.retryLoad();

      expect(dataServiceSpy.loadMyTeam).toHaveBeenCalledWith('Thibaut');
    });
  });

  describe('Helper methods', () => {
    beforeEach(() => {
      fixture = TestBed.createComponent(TeamDetailComponent);
      component = fixture.componentInstance;
      component.team = mockTeam;
      component.stats = mockStats;
    });

    it('should get region color', () => {
      statsServiceSpy.getRegionColor.and.returnValue('#ff0000');
      expect(component.getRegionColor('NAE')).toBe('#ff0000');
    });

    it('should get region flag', () => {
      statsServiceSpy.getRegionFlag.and.returnValue('🇺🇸');
      expect(component.getRegionFlag('NAE')).toBe('🇺🇸');
    });

    it('should get tranche color', () => {
      statsServiceSpy.getTrancheColor.and.returnValue('#gold');
      expect(component.getTrancheColor(1)).toBe('#gold');
    });

    it('should get tranche number', () => {
      statsServiceSpy.getTrancheNumber.and.returnValue(1);
      expect(component.getTrancheNumber('TRANCHE_1')).toBe(1);
    });

    it('should get player rank', () => {
      statsServiceSpy.getPlayerRank.and.returnValue(5);
      const player: any = mockTeam.players[0];
      expect(component.getPlayerRank(player)).toBe(5);
    });

    it('should format points', () => {
      statsServiceSpy.formatPoints.and.returnValue('1,500');
      expect(component.formatPoints(1500)).toBe('1,500');
    });

    it('should get top players', () => {
      const topPlayers = [mockTeam.players[0]];
      statsServiceSpy.getTopPlayers.and.returnValue(topPlayers);
      expect(component.getTopPlayers()).toEqual(topPlayers);
    });

    it('should return empty array when no team for getTopPlayers', () => {
      component.team = null;
      expect(component.getTopPlayers()).toEqual([]);
    });

    it('should get sorted players', () => {
      statsServiceSpy.getSortedPlayers.and.returnValue(mockTeam.players);
      expect(component.getSortedPlayers()).toEqual(mockTeam.players);
    });

    it('should return empty array when no team for getSortedPlayers', () => {
      component.team = null;
      expect(component.getSortedPlayers()).toEqual([]);
    });

    it('should get progress percentage', () => {
      statsServiceSpy.getProgressPercentage.and.returnValue(75);
      expect(component.getProgressPercentage(750)).toBe(75);
    });

    it('should get region percentage', () => {
      statsServiceSpy.getRegionPercentage.and.returnValue(53.3);
      expect(component.getRegionPercentage('NAE')).toBe(53.3);
    });

    it('should get region arc length', () => {
      statsServiceSpy.getRegionArcLength.and.returnValue(120);
      expect(component.getRegionArcLength('NAE')).toBe(120);
    });

    it('should get region arc offset', () => {
      statsServiceSpy.getRegionArcOffset.and.returnValue(45);
      expect(component.getRegionArcOffset('EU')).toBe(45);
    });

    it('should get sorted regions by points', () => {
      const sortedRegions = ['NAE', 'NAW', 'EU'];
      statsServiceSpy.getSortedRegionsByPoints.and.returnValue(sortedRegions);
      expect(component.getSortedRegionsByPoints()).toEqual(sortedRegions);
    });

    it('should get top 10% players count', () => {
      component.allTeams = [mockTeam];
      statsServiceSpy.getTop10PercentPlayersCount.and.returnValue(2);
      expect(component.getTop10PercentPlayersCount()).toBe(2);
    });

    it('should return 0 for top 10% when no team', () => {
      component.team = null;
      expect(component.getTop10PercentPlayersCount()).toBe(0);
    });

    it('should return 0 for top 10% when no allTeams', () => {
      component.allTeams = [];
      expect(component.getTop10PercentPlayersCount()).toBe(0);
    });

    it('should get region ratio', () => {
      statsServiceSpy.getRegionRatio.and.returnValue(1.25);
      expect(component.getRegionRatio('NAE')).toBe(1.25);
    });

    it('should get sorted regions by ratio', () => {
      const sortedRegions = ['NAE', 'EU', 'NAW'];
      statsServiceSpy.getSortedRegionsByRatio.and.returnValue(sortedRegions);
      expect(component.getSortedRegionsByRatio()).toEqual(sortedRegions);
    });

    it('should get average ratio', () => {
      statsServiceSpy.getAverageRatio.and.returnValue(1.0);
      expect(component.getAverageRatio()).toBe(1.0);
    });

    it('should get region arc length by ratio', () => {
      statsServiceSpy.getRegionArcLengthByRatio.and.returnValue(90);
      expect(component.getRegionArcLengthByRatio('NAE')).toBe(90);
    });

    it('should get region arc offset by ratio', () => {
      statsServiceSpy.getRegionArcOffsetByRatio.and.returnValue(180);
      expect(component.getRegionArcOffsetByRatio('EU')).toBe(180);
    });
  });
});
