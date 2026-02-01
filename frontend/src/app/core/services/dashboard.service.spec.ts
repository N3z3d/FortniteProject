import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { DashboardService, DashboardStats } from './dashboard.service';
import { environment } from '../../../environments/environment';

describe('DashboardService', () => {
  let service: DashboardService;
  let httpMock: HttpTestingController;

  const buildStats = (): DashboardStats => ({
    totalPoints: 1200,
    pointsByRegion: {
      EU: 100,
      NAC: 200,
      NAW: 150,
      BR: 120,
      ASIA: 180,
      OCE: 90,
      ME: 60
    },
    remainingTrades: 3,
    recentMovements: [
      {
        id: 'movement-1',
        type: 'TRADE',
        description: 'Trade executed',
        timestamp: '2025-01-01T00:00:00Z'
      }
    ]
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [DashboardService]
    });

    service = TestBed.inject(DashboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('fetches dashboard stats', () => {
    const mockStats = buildStats();

    service.getStats().subscribe((result) => {
      expect(result).toEqual(mockStats);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/dashboard/stats`);
    expect(req.request.method).toBe('GET');
    req.flush(mockStats);
  });

  it('fetches recent movements', () => {
    const mockMovements: DashboardStats['recentMovements'] = [
      {
        id: 'movement-2',
        type: 'TEAM_UPDATE',
        description: 'Team updated',
        timestamp: '2025-01-02T00:00:00Z'
      }
    ];

    service.getRecentMovements().subscribe((result) => {
      expect(result).toEqual(mockMovements);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/dashboard/movements`);
    expect(req.request.method).toBe('GET');
    req.flush(mockMovements);
  });
});
