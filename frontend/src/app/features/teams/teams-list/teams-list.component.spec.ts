import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { TeamsListComponent } from './teams-list.component';
import { TeamDto, TeamService } from '../../../core/services/team.service';
import { GameSelectionService } from '../../../core/services/game-selection.service';
import { LoggerService } from '../../../core/services/logger.service';

describe('TeamsListComponent', () => {
  let fixture: ComponentFixture<TeamsListComponent>;
  let component: TeamsListComponent;
  let teamServiceSpy: jasmine.SpyObj<TeamService>;
  let loggerSpy: jasmine.SpyObj<LoggerService>;
  let gameSelectionService: GameSelectionService;

  beforeEach(async () => {
    teamServiceSpy = jasmine.createSpyObj<TeamService>('TeamService', ['getTeamsByGame', 'getUserTeams']);
    loggerSpy = jasmine.createSpyObj<LoggerService>('LoggerService', ['debug', 'warn', 'error']);

    await TestBed.configureTestingModule({
      imports: [TeamsListComponent, RouterTestingModule, NoopAnimationsModule],
      providers: [
        { provide: TeamService, useValue: teamServiceSpy },
        { provide: LoggerService, useValue: loggerSpy }
      ]
    }).compileComponents();

    gameSelectionService = TestBed.inject(GameSelectionService);
  });

  it('mappe ownerUsername, totalScore et nickname depuis TeamDto', () => {
    const apiTeams: TeamDto[] = [
      {
        id: 'team-1',
        name: 'Équipe Test',
        season: 2025,
        totalScore: 1234,
        ownerUsername: 'Thibaut',
        players: [{ playerId: 'p1', nickname: 'Mero', region: 'EU', tranche: 'T1' }],
        createdAt: '2025-01-01T00:00:00Z',
        updatedAt: '2025-01-02T00:00:00Z'
      }
    ];

    teamServiceSpy.getUserTeams.and.returnValue(of(apiTeams));

    fixture = TestBed.createComponent(TeamsListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.teams.length).toBe(1);
    expect(component.teams[0].ownerName).toBe('Thibaut');
    expect(component.teams[0].totalPoints).toBe(1234);
    expect(component.teams[0].players[0].gamertag).toBe('Mero');
  });

  it('utilise la game selectionnee comme gameId', () => {
    gameSelectionService.setSelectedGame({ id: 'game-123' } as any);
    teamServiceSpy.getTeamsByGame.and.returnValue(of([]));
    teamServiceSpy.getUserTeams.and.returnValue(of([]));

    fixture = TestBed.createComponent(TeamsListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.gameId).toBe('game-123');
    expect(teamServiceSpy.getTeamsByGame).toHaveBeenCalledWith('game-123');
  });
  it('n utilise pas de fallback mock quand le service echoue', () => {
    teamServiceSpy.getUserTeams.and.returnValue(throwError(() => new Error('fail')));

    fixture = TestBed.createComponent(TeamsListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(loggerSpy.error).toHaveBeenCalled();
    expect(component.error).toBe(component.t.t('teams.list.errors.loadFailed'));
    expect(component.teams.length).toBe(0);
    expect(component.loading).toBe(false);
  });
});
