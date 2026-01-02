import { DashboardChartService } from './dashboard-chart.service';
import { LoggerService } from '../../../core/services/logger.service';

describe('DashboardChartService', () => {
  let service: DashboardChartService;
  let loggerSpy: jasmine.SpyObj<LoggerService>;

  beforeEach(() => {
    loggerSpy = jasmine.createSpyObj<LoggerService>('LoggerService', ['debug', 'warn']);
    service = new DashboardChartService(loggerSpy);
  });

  it('normalise les regions dans un ordre stable', () => {
    const result = service.getRegionChartData({ EU: '2', NAC: 1 } as any);

    expect(result.labels).toEqual(['EU', 'NAC', 'NAW', 'BR', 'ASIA', 'OCE', 'ME']);
    expect(result.data).toEqual([2, 1, 0, 0, 0, 0, 0]);
  });

  it('detecte correctement les donnees de regions vides', () => {
    expect(service.hasRegionData({ EU: 0, NAC: 0 } as any)).toBeFalse();
    expect(service.hasRegionData({ EU: 1, NAC: 0 } as any)).toBeTrue();
  });

  it('trie les points et nettoie les labels des equipes', () => {
    const entries = [
      { teamName: 'Equipe de Marcel', totalPoints: 120 },
      { ownerName: 'Thibaut', totalPoints: 200 },
      { teamName: '', ownerName: 'Teddy', totalPoints: 50 }
    ];

    const result = service.getPointsChartData(entries, 10);

    expect(result.data).toEqual([200, 120, 50]);
    expect(result.labels).toEqual(['Thibaut', 'Marcel', 'Teddy']);
  });

  it('ignore les equipes avec 0 points', () => {
    const entries = [
      { teamName: 'Equipe de Marcel', totalPoints: 0 },
      { ownerName: 'Thibaut', totalPoints: 200 }
    ];

    const result = service.getPointsChartData(entries, 10);

    expect(result.data).toEqual([200]);
    expect(result.labels).toEqual(['Thibaut']);
  });

  it('detecte correctement les donnees de points vides', () => {
    expect(service.hasPointsData([{ totalPoints: 0 }])).toBeFalse();
    expect(service.hasPointsData([{ totalPoints: 5 }])).toBeTrue();
  });
});
