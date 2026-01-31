import { TestBed } from '@angular/core/testing';
import { DraftStateHelperService } from './draft-state-helper.service';

describe('DraftStateHelperService', () => {
  let service: DraftStateHelperService;
  let mockTranslate: jasmine.Spy;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DraftStateHelperService]
    });
    service = TestBed.inject(DraftStateHelperService);
    mockTranslate = jasmine.createSpy('t').and.returnValue('translated');
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getStatusColor', () => {
    it('should return accent for ACTIVE status', () => {
      expect(service.getStatusColor('ACTIVE')).toBe('accent');
      expect(service.getStatusColor('IN_PROGRESS')).toBe('accent');
    });

    it('should return warn for PAUSED/CANCELLED/ERROR status', () => {
      expect(service.getStatusColor('PAUSED')).toBe('warn');
      expect(service.getStatusColor('CANCELLED')).toBe('warn');
      expect(service.getStatusColor('ERROR')).toBe('warn');
    });

    it('should return primary for unknown status', () => {
      expect(service.getStatusColor('UNKNOWN')).toBe('primary');
      expect(service.getStatusColor('PENDING')).toBe('primary');
    });
  });

  describe('getStatusLabelKey', () => {
    it('should return i18n key for known status', () => {
      const key = service.getStatusLabelKey('ACTIVE');
      expect(key).toBeTruthy();
    });

    it('should return status itself for unknown status', () => {
      const key = service.getStatusLabelKey('UNKNOWN_STATUS');
      expect(key).toBe('UNKNOWN_STATUS');
    });
  });

  describe('getRegionLabelKey', () => {
    it('should return i18n key for known region', () => {
      const key = service.getRegionLabelKey('EUROPE');
      expect(key).toBeTruthy();
    });

    it('should return region itself for unknown region', () => {
      const key = service.getRegionLabelKey('UNKNOWN_REGION');
      expect(key).toBe('UNKNOWN_REGION');
    });
  });

  describe('getTrancheLabel', () => {
    it('should format tranche T1 correctly', () => {
      mockTranslate.and.returnValue('Tranche {value}');
      const result = service.getTrancheLabel('T1', mockTranslate);
      expect(result).toBe('Tranche 1');
    });

    it('should format tranche T5 correctly', () => {
      mockTranslate.and.returnValue('Tranche {value}');
      const result = service.getTrancheLabel('T5', mockTranslate);
      expect(result).toBe('Tranche 5');
    });

    it('should return original value for non-standard format', () => {
      const result = service.getTrancheLabel('CUSTOM', mockTranslate);
      expect(result).toBe('CUSTOM');
    });
  });

  describe('formatTemplate', () => {
    it('should replace single placeholder', () => {
      const result = service.formatTemplate('Hello {name}', { name: 'Alice' });
      expect(result).toBe('Hello Alice');
    });

    it('should replace multiple placeholders', () => {
      const result = service.formatTemplate('{greeting} {name}, you have {count} messages', {
        greeting: 'Hello',
        name: 'Bob',
        count: 5
      });
      expect(result).toBe('Hello Bob, you have 5 messages');
    });

    it('should handle numeric values', () => {
      const result = service.formatTemplate('Count: {count}', { count: 42 });
      expect(result).toBe('Count: 42');
    });

    it('should return template unchanged if no placeholders', () => {
      const result = service.formatTemplate('No placeholders', {});
      expect(result).toBe('No placeholders');
    });
  });

  describe('getSearchResultsTitle', () => {
    it('should call formatTemplate with count', () => {
      mockTranslate.and.returnValue('{count} results');
      const result = service.getSearchResultsTitle(5, mockTranslate);
      expect(result).toBe('5 results');
    });
  });

  describe('getShowAllResultsLabel', () => {
    it('should call formatTemplate with count', () => {
      mockTranslate.and.returnValue('Show all {count}');
      const result = service.getShowAllResultsLabel(10, mockTranslate);
      expect(result).toBe('Show all 10');
    });
  });

  describe('getSuggestionRankLabel', () => {
    it('should call formatTemplate with rank', () => {
      mockTranslate.and.returnValue('Rank #{rank}');
      const result = service.getSuggestionRankLabel(3, mockTranslate);
      expect(result).toBe('Rank #3');
    });
  });

  describe('getSlotsRemainingLabel', () => {
    it('should use singular key for count=1', () => {
      mockTranslate.and.callFake((key: string) => {
        if (key === 'draft.ui.slotsRemainingSingle') return '{count} slot remaining';
        return '';
      });

      const result = service.getSlotsRemainingLabel(1, mockTranslate);
      expect(result).toBe('1 slot remaining');
    });

    it('should use plural key for count>1', () => {
      mockTranslate.and.callFake((key: string) => {
        if (key === 'draft.ui.slotsRemainingMultiple') return '{count} slots remaining';
        return '';
      });

      const result = service.getSlotsRemainingLabel(3, mockTranslate);
      expect(result).toBe('3 slots remaining');
    });
  });

  describe('getWaitingMessage', () => {
    it('should return default message when username is null', () => {
      mockTranslate.and.returnValue('Waiting for next turn');
      const result = service.getWaitingMessage(null, mockTranslate);
      expect(result).toBe('Waiting for next turn');
    });

    it('should format message with username', () => {
      mockTranslate.and.returnValue('Waiting for {username}');
      const result = service.getWaitingMessage('Alice', mockTranslate);
      expect(result).toBe('Waiting for Alice');
    });
  });
});
