import { TestBed } from '@angular/core/testing';
import { DraftPlayerFilterService } from './draft-player-filter.service';
import { Player } from './draft.service';
import { PlayerRegion } from '../models/draft.interface';

describe('DraftPlayerFilterService', () => {
  let service: DraftPlayerFilterService;

  const createMockPlayer = (overrides: Partial<Player> = {}): Player => ({
    id: 'player-1',
    nickname: 'TestPlayer',
    username: 'testuser',
    region: 'EUROPE',
    tranche: 'T1',
    epicAccountId: 'epic-123',
    isSelected: false,
    ...overrides
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DraftPlayerFilterService]
    });
    service = TestBed.inject(DraftPlayerFilterService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('filterPlayers', () => {
    it('should return all players when no filters applied', () => {
      const players: Player[] = [
        createMockPlayer({ id: '1', region: 'EUROPE' }),
        createMockPlayer({ id: '2', region: 'NORTH_AMERICA' })
      ];

      const result = service.filterPlayers(players, {
        selectedRegion: 'ALL',
        selectedTranche: 'ALL',
        searchTerm: ''
      });

      expect(result.length).toBe(2);
    });

    it('should filter by region', () => {
      const players: Player[] = [
        createMockPlayer({ id: '1', region: 'EUROPE' }),
        createMockPlayer({ id: '2', region: 'NORTH_AMERICA' })
      ];

      const result = service.filterPlayers(players, {
        selectedRegion: 'EUROPE',
        selectedTranche: 'ALL',
        searchTerm: ''
      });

      expect(result.length).toBe(1);
      expect(result[0].region).toBe('EUROPE');
    });

    it('should filter by tranche', () => {
      const players: Player[] = [
        createMockPlayer({ id: '1', tranche: 'T1' }),
        createMockPlayer({ id: '2', tranche: 'T2' })
      ];

      const result = service.filterPlayers(players, {
        selectedRegion: 'ALL',
        selectedTranche: 'T1',
        searchTerm: ''
      });

      expect(result.length).toBe(1);
      expect(result[0].tranche).toBe('T1');
    });

    it('should filter by search term (nickname)', () => {
      const players: Player[] = [
        createMockPlayer({ id: '1', nickname: 'Alice' }),
        createMockPlayer({ id: '2', nickname: 'Bob' })
      ];

      const result = service.filterPlayers(players, {
        selectedRegion: 'ALL',
        selectedTranche: 'ALL',
        searchTerm: 'ali'
      });

      expect(result.length).toBe(1);
      expect(result[0].nickname).toBe('Alice');
    });

    it('should filter by search term (username)', () => {
      const players: Player[] = [
        createMockPlayer({ id: '1', username: 'alice123' }),
        createMockPlayer({ id: '2', username: 'bob456' })
      ];

      const result = service.filterPlayers(players, {
        selectedRegion: 'ALL',
        selectedTranche: 'ALL',
        searchTerm: '123'
      });

      expect(result.length).toBe(1);
      expect(result[0].username).toBe('alice123');
    });

    it('should apply multiple filters', () => {
      const players: Player[] = [
        createMockPlayer({ id: '1', region: 'EUROPE', tranche: 'T1', nickname: 'Alice' }),
        createMockPlayer({ id: '2', region: 'EUROPE', tranche: 'T2', nickname: 'Bob' }),
        createMockPlayer({ id: '3', region: 'NORTH_AMERICA', tranche: 'T1', nickname: 'Alice' })
      ];

      const result = service.filterPlayers(players, {
        selectedRegion: 'EUROPE',
        selectedTranche: 'T1',
        searchTerm: 'alice'
      });

      expect(result.length).toBe(1);
      expect(result[0].id).toBe('1');
    });
  });

  describe('extractUniqueRegions', () => {
    it('should extract unique regions', () => {
      const players: Player[] = [
        createMockPlayer({ region: 'EUROPE' }),
        createMockPlayer({ region: 'EUROPE' }),
        createMockPlayer({ region: 'NORTH_AMERICA' })
      ];

      const result = service.extractUniqueRegions(players);

      expect(result.length).toBe(2);
      expect(result).toContain('EUROPE');
      expect(result).toContain('NORTH_AMERICA');
    });

    it('should return empty array for empty input', () => {
      const result = service.extractUniqueRegions([]);
      expect(result).toEqual([]);
    });
  });

  describe('extractUniqueTranches', () => {
    it('should extract unique tranches', () => {
      const players: Player[] = [
        createMockPlayer({ tranche: 'T1' }),
        createMockPlayer({ tranche: 'T1' }),
        createMockPlayer({ tranche: 'T2' })
      ];

      const result = service.extractUniqueTranches(players);

      expect(result.length).toBe(2);
      expect(result).toContain('T1');
      expect(result).toContain('T2');
    });

    it('should return empty array for empty input', () => {
      const result = service.extractUniqueTranches([]);
      expect(result).toEqual([]);
    });
  });

  describe('getSmartSuggestions', () => {
    it('should return top 3 players by default', () => {
      const players: Player[] = [
        createMockPlayer({ id: '1' }),
        createMockPlayer({ id: '2' }),
        createMockPlayer({ id: '3' }),
        createMockPlayer({ id: '4' })
      ];

      const result = service.getSmartSuggestions(players);

      expect(result.length).toBe(3);
      expect(result[0].rank).toBe(1);
      expect(result[1].rank).toBe(2);
      expect(result[2].rank).toBe(3);
    });

    it('should respect custom limit', () => {
      const players: Player[] = [
        createMockPlayer({ id: '1' }),
        createMockPlayer({ id: '2' }),
        createMockPlayer({ id: '3' })
      ];

      const result = service.getSmartSuggestions(players, 2);

      expect(result.length).toBe(2);
    });

    it('should include score for each suggestion', () => {
      const players: Player[] = [createMockPlayer()];

      const result = service.getSmartSuggestions(players);

      expect(result[0].score).toBeGreaterThan(0);
    });
  });
});
