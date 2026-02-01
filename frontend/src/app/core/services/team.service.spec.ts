import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TeamService, TeamDto } from './team.service';
import { UserContextService } from './user-context.service';
import { environment } from '../../../environments/environment';

describe('TeamService', () => {
  let service: TeamService;
  let httpMock: HttpTestingController;
  let userContext: jasmine.SpyObj<UserContextService>;

  const sampleTeam: TeamDto = {
    id: 'team-1',
    name: 'Team One',
    season: 2025,
    totalScore: 100,
    ownerUsername: 'Alice',
    players: [],
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-01T00:00:00Z'
  };

  beforeEach(() => {
    userContext = jasmine.createSpyObj('UserContextService', ['getCurrentUser']);
    userContext.getCurrentUser.and.returnValue({ username: 'CurrentUser' } as any);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        TeamService,
        { provide: UserContextService, useValue: userContext }
      ]
    });

    service = TestBed.inject(TeamService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('gets team for user with default season', () => {
    service.getTeamForUserAndSeason('user-1').subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/api/teams/user/user-1/season/2025`);
    expect(req.request.method).toBe('GET');
    req.flush(sampleTeam);
  });

  it('gets all teams for a season', () => {
    service.getAllTeamsForSeason(2024).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/api/teams/season/2024`);
    expect(req.request.method).toBe('GET');
    req.flush([sampleTeam]);
  });

  it('creates a team', () => {
    service.createTeam('user-1', 'Team One', 2024).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/api/teams`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ userId: 'user-1', name: 'Team One', season: 2024 });
    req.flush(sampleTeam);
  });

  it('updates a team', () => {
    service.updateTeam('team-1', { name: 'Updated' }).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/api/teams/team-1`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ name: 'Updated' });
    req.flush(sampleTeam);
  });

  it('deletes a team', () => {
    service.deleteTeam('team-1').subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/api/teams/team-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('adds a player to a team', () => {
    service.addPlayerToTeam('user-1', 2025, 'player-1', 2).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/api/teams/user/user-1/season/2025/players/add`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ playerId: 'player-1', position: 2 });
    req.flush(sampleTeam);
  });

  it('gets user teams with explicit user id', () => {
    service.getUserTeams('user-2', 2026).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/api/teams/user/user-2/season/2026`);
    expect(req.request.method).toBe('GET');
    req.flush([sampleTeam]);
  });

  it('gets user teams from context when no user id is provided', () => {
    service.getUserTeams(undefined, 2026).subscribe();

    const req = httpMock.expectOne((request) => request.url === `${environment.apiUrl}/api/teams/user`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('user')).toBe('CurrentUser');
    expect(req.request.params.get('year')).toBe('2026');
    req.flush([sampleTeam]);
  });

  it('gets teams by game or falls back to season list', () => {
    service.getTeamsByGame('').subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/api/teams/season/2025`);
    expect(req.request.method).toBe('GET');
    req.flush([sampleTeam]);
  });
});
