import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { throwError } from 'rxjs';

import { TeamDetailComponent } from './team-detail.component';
import { UserContextService } from '../../../core/services/user-context.service';
import { TeamService } from '../../../core/services/team.service';
import { LeaderboardService } from '../../../core/services/leaderboard.service';

describe('TeamDetailComponent', () => {
  let fixture: ComponentFixture<TeamDetailComponent>;
  let component: TeamDetailComponent;
  let userContextSpy: jasmine.SpyObj<UserContextService>;
  let teamServiceSpy: jasmine.SpyObj<TeamService>;
  let leaderboardServiceSpy: jasmine.SpyObj<LeaderboardService>;
  let routeStub: { snapshot: { paramMap: ReturnType<typeof convertToParamMap> } };

  beforeEach(async () => {
    userContextSpy = jasmine.createSpyObj<UserContextService>('UserContextService', ['getCurrentUser']);
    teamServiceSpy = jasmine.createSpyObj<TeamService>('TeamService', ['getAllTeamsForSeason']);
    leaderboardServiceSpy = jasmine.createSpyObj<LeaderboardService>('LeaderboardService', ['getTeamLeaderboard']);
    routeStub = { snapshot: { paramMap: convertToParamMap({}) } };

    userContextSpy.getCurrentUser.and.returnValue({ username: 'Thibaut' } as any);

    await TestBed.configureTestingModule({
      imports: [TeamDetailComponent, HttpClientTestingModule],
      providers: [
        { provide: UserContextService, useValue: userContextSpy },
        { provide: TeamService, useValue: teamServiceSpy },
        { provide: LeaderboardService, useValue: leaderboardServiceSpy },
        { provide: ActivatedRoute, useValue: routeStub }
      ]
    }).compileComponents();
  });

  it('expose une erreur si le chargement des equipes echoue', () => {
    leaderboardServiceSpy.getTeamLeaderboard.and.returnValue(throwError(() => new Error('fail')));

    fixture = TestBed.createComponent(TeamDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.error).toBe('Donn\u00e9es indisponibles (CSV non charg\u00e9)');
    expect(component.team).toBeNull();
    expect(component.stats).toBeNull();
    expect(component.allTeams.length).toBe(0);
    expect(component.loading).toBe(false);
  });
});
