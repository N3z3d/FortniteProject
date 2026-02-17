import { TestBed } from '@angular/core/testing';
import { TradeDetailService } from './trade-detail.service';
import { TranslationService } from '../../../core/services/translation.service';

describe('TradeDetailService', () => {
  let service: TradeDetailService;
  let translationService: jasmine.SpyObj<TranslationService>;

  beforeEach(() => {
    translationService = jasmine.createSpyObj('TranslationService', ['t']);
    translationService.t.and.callFake((key: string, fallback?: string) => fallback || key);

    TestBed.configureTestingModule({
      providers: [
        TradeDetailService,
        { provide: TranslationService, useValue: translationService }
      ]
    });

    service = TestBed.inject(TradeDetailService);
  });

  it('getStatusColor returns expected colors', () => {
    expect(service.getStatusColor('PENDING')).toBe('accent');
    expect(service.getStatusColor('COMPLETED')).toBe('primary');
    expect(service.getStatusColor('CANCELLED')).toBe('warn');
    expect(service.getStatusColor('UNKNOWN')).toBe('');
  });

  it('getStatusLabel uses translation keys', () => {
    service.getStatusLabel('PENDING');
    service.getStatusLabel('COMPLETED');
    service.getStatusLabel('CANCELLED');

    expect(translationService.t).toHaveBeenCalledWith('trades.status.pending', 'PENDING');
    expect(translationService.t).toHaveBeenCalledWith('trades.status.completed', 'COMPLETED');
    expect(translationService.t).toHaveBeenCalledWith('trades.status.cancelled', 'CANCELLED');
  });

  it('getStatusLabel uses fallback for unknown status', () => {
    service.getStatusLabel('UNKNOWN');

    expect(translationService.t).toHaveBeenCalledWith('trades.status.unknown', 'UNKNOWN');
  });

  it('getTimelineLabel uses translation keys', () => {
    service.getTimelineLabel('PENDING');
    service.getTimelineLabel('COMPLETED');
    service.getTimelineLabel('CANCELLED');

    expect(translationService.t).toHaveBeenCalledWith('trades.detail.timelinePending');
    expect(translationService.t).toHaveBeenCalledWith('trades.detail.timelineCompleted');
    expect(translationService.t).toHaveBeenCalledWith('trades.detail.timelineCancelled');
  });

  it('getTimelineLabel falls back to status for unknown values', () => {
    expect(service.getTimelineLabel('UNKNOWN')).toBe('UNKNOWN');
  });

  it('getTradeDetail returns mock trade for known id', () => {
    const trade = service.getTradeDetail('1');

    expect(trade).not.toBeNull();
    expect(trade?.id).toBe('1');
    expect(trade?.playerOut.username).toBe('Ninja');
    expect(trade?.playerIn.username).toBe('Tfue');
    expect(trade?.team.name).toBe('Team Alpha');
  });

  it('getTradeDetail returns null for unknown id', () => {
    expect(service.getTradeDetail('unknown')).toBeNull();
  });
});
