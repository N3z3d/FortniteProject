import { TestBed } from '@angular/core/testing';
import { CsvDataService } from './csv-data.service';

describe('CsvDataService', () => {
  let service: CsvDataService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CsvDataService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should parse championship data correctly', () => {
    const championship = service.parseChampionshipData();
    
    expect(championship).toBeTruthy();
    expect(championship.totalPlayers).toBe(147);
    expect(championship.marcel).toBeTruthy();
    expect(championship.teddy).toBeTruthy();
    expect(championship.thibaut).toBeTruthy();
  });

  it('should have correct player counts per team', () => {
    const championship = service.parseChampionshipData();
    
    // Vérifier les comptes exacts
    expect(championship.marcel.playerCount).toBe(49);
    expect(championship.teddy.playerCount).toBe(49);
    expect(championship.thibaut.playerCount).toBe(49);
    
    // Vérifier le total
    const totalPlayers = championship.marcel.playerCount + 
                        championship.teddy.playerCount + 
                        championship.thibaut.playerCount;
    expect(totalPlayers).toBe(147);
  });

  it('should calculate correct total scores', () => {
    const championship = service.parseChampionshipData();
    
    // Vérifier que les scores sont positifs et réalistes
    expect(championship.marcel.totalScore).toBeGreaterThan(0);
    expect(championship.teddy.totalScore).toBeGreaterThan(0);
    expect(championship.thibaut.totalScore).toBeGreaterThan(0);
    
    // Les scores devraient être dans une fourchette réaliste (environ 50k-200k par joueur)
    expect(championship.marcel.totalScore).toBeGreaterThan(500000);
    expect(championship.teddy.totalScore).toBeGreaterThan(500000);
    expect(championship.thibaut.totalScore).toBeGreaterThan(500000);
  });

  it('should have proper region distribution', () => {
    const championship = service.parseChampionshipData();
    
    // Vérifier que chaque équipe a des joueurs de différentes régions
    expect(Object.keys(championship.marcel.regionDistribution).length).toBeGreaterThan(1);
    expect(Object.keys(championship.teddy.regionDistribution).length).toBeGreaterThan(1);
    expect(Object.keys(championship.thibaut.regionDistribution).length).toBeGreaterThan(1);
  });

  it('should calculate region stats correctly', () => {
    const championship = service.parseChampionshipData();
    const regionStats = service.calculateRegionStats(championship);
    
    expect(regionStats).toBeTruthy();
    expect(regionStats.length).toBeGreaterThan(0);
    
    // Vérifier que le total des joueurs par région correspond au total
    const totalPlayersByRegion = regionStats.reduce((sum, stat) => sum + stat.playerCount, 0);
    expect(totalPlayersByRegion).toBe(147);
  });

  it('should validate data correctly', () => {
    const championship = service.parseChampionshipData();
    const validation = service.validateData(championship);
    
    expect(validation.isValid).toBe(true);
    expect(validation.errors.length).toBe(0);
  });

  it('should get detailed team stats', () => {
    const championship = service.parseChampionshipData();
    const marcelStats = service.getTeamDetailedStats(championship.marcel);
    
    expect(marcelStats.topPlayers).toBeTruthy();
    expect(marcelStats.topPlayers.length).toBe(5);
    expect(marcelStats.averageScore).toBeGreaterThan(0);
    expect(marcelStats.regionStats).toBeTruthy();
    expect(marcelStats.totalScore).toBe(championship.marcel.totalScore);
    expect(marcelStats.playerCount).toBe(championship.marcel.playerCount);
  });
});
