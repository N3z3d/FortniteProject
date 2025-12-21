import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { TeamsListComponent } from './teams-list.component';
import { TeamDto, TeamService } from '../../../core/services/team.service';
import { LoggerService } from '../../../core/services/logger.service';

describe('TeamsListComponent', () => {
  let fixture: ComponentFixture<TeamsListComponent>;
  let component: TeamsListComponent;
  let teamServiceSpy: jasmine.SpyObj<TeamService>;
  let loggerSpy: jasmine.SpyObj<LoggerService>;
  let activatedRouteStub: { params: unknown; parent?: { params: unknown } | null };

  beforeEach(async () => {
    teamServiceSpy = jasmine.createSpyObj<TeamService>('TeamService', ['getTeamsByGame', 'getUserTeams']);
    loggerSpy = jasmine.createSpyObj<LoggerService>('LoggerService', ['warn', 'error']);
    activatedRouteStub = { params: of({}), parent: null };

    await TestBed.configureTestingModule({
      imports: [TeamsListComponent, RouterTestingModule, NoopAnimationsModule],
      providers: [
        { provide: TeamService, useValue: teamServiceSpy },
        { provide: LoggerService, useValue: loggerSpy },
        { provide: ActivatedRoute, useValue: activatedRouteStub }
      ]
    }).compileComponents();
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

  it("utilise l'id du parent route comme gameId", () => {
    activatedRouteStub.parent = { params: of({ id: 'game-123' }) };
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
    expect(component.error).toBe('Donn\u00e9es indisponibles (CSV non charg\u00e9)');
    expect(component.teams.length).toBe(0);
    expect(component.loading).toBe(false);
  });
});
