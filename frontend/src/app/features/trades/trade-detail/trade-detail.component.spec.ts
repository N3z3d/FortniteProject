import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { TradeDetailComponent } from './trade-detail.component';
import { LoggerService } from '../../../core/services/logger.service';
import { TranslationService } from '../../../core/services/translation.service';

describe('TradeDetailComponent', () => {
  let component: TradeDetailComponent;
  let fixture: ComponentFixture<TradeDetailComponent>;
  let router: jasmine.SpyObj<Router>;
  let logger: jasmine.SpyObj<LoggerService>;
  let translationService: jasmine.SpyObj<TranslationService>;
  let activatedRoute: any;

  beforeEach(async () => {
    router = jasmine.createSpyObj('Router', ['navigate']);
    logger = jasmine.createSpyObj('LoggerService', ['debug']);
    translationService = jasmine.createSpyObj('TranslationService', ['t']);
    translationService.t.and.callFake((key: string, fallback?: string) => fallback || key);

    activatedRoute = {
      snapshot: {
        paramMap: {
          get: jasmine.createSpy('get').and.returnValue('1')
        }
      }
    };

    await TestBed.configureTestingModule({
      imports: [TradeDetailComponent],
      providers: [
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: activatedRoute },
        { provide: LoggerService, useValue: logger },
        { provide: TranslationService, useValue: translationService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TradeDetailComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with loading state', () => {
    expect(component.isLoading).toBeTrue();
    expect(component.trade).toBeNull();
  });

  it('should get trade id from route params on init', () => {
    fixture.detectChanges();

    expect(activatedRoute.snapshot.paramMap.get).toHaveBeenCalledWith('id');
    expect(component.tradeId).toBe('1');
  });

  it('should handle missing trade id', () => {
    activatedRoute.snapshot.paramMap.get.and.returnValue(null);

    fixture.detectChanges();

    expect(component.tradeId).toBe('');
  });

  it('should load trade detail after timeout', fakeAsync(() => {
    fixture.detectChanges();

    expect(component.isLoading).toBeTrue();
    expect(component.trade).toBeNull();

    tick(1000);

    expect(component.isLoading).toBeFalse();
    expect(component.trade).not.toBeNull();
  }));

  it('should load valid mock trade for id 1', fakeAsync(() => {
    fixture.detectChanges();
    tick(1000);

    const trade = component.trade!;
    expect(trade.id).toBe('1');
    expect(trade.playerOut.username).toBe('Ninja');
    expect(trade.playerIn.username).toBe('Tfue');
    expect(trade.team.name).toBe('Team Alpha');
    expect(trade.status).toBe('PENDING');
  }));

  it('should return null for invalid trade id', fakeAsync(() => {
    activatedRoute.snapshot.paramMap.get.and.returnValue('invalid');

    fixture.detectChanges();
    tick(1000);

    expect(component.trade).toBeNull();
  }));

  it('should complete trade and log action', () => {
    component.tradeId = '1';

    component.completeTrade();

    expect(logger.debug).toHaveBeenCalledWith('TradeDetail: completing trade', { tradeId: '1' });
  });

  it('should cancel trade and log action', () => {
    component.tradeId = '1';

    component.cancelTrade();

    expect(logger.debug).toHaveBeenCalledWith('TradeDetail: cancelling trade', { tradeId: '1' });
  });

  it('should navigate back to trades list', () => {
    component.goBack();

    expect(router.navigate).toHaveBeenCalledWith(['/trades']);
  });

  it('should return accent color for PENDING status', () => {
    expect(component.getStatusColor('PENDING')).toBe('accent');
  });

  it('should return primary color for COMPLETED status', () => {
    expect(component.getStatusColor('COMPLETED')).toBe('primary');
  });

  it('should return warn color for CANCELLED status', () => {
    expect(component.getStatusColor('CANCELLED')).toBe('warn');
  });

  it('should return empty string for unknown status color', () => {
    expect(component.getStatusColor('UNKNOWN')).toBe('');
  });

  it('should get status label from translation', () => {
    component.getStatusLabel('PENDING');

    expect(translationService.t).toHaveBeenCalledWith('trades.status.pending', 'PENDING');
  });

  it('should get status label for COMPLETED', () => {
    component.getStatusLabel('COMPLETED');

    expect(translationService.t).toHaveBeenCalledWith('trades.status.completed', 'COMPLETED');
  });

  it('should get status label for CANCELLED', () => {
    component.getStatusLabel('CANCELLED');

    expect(translationService.t).toHaveBeenCalledWith('trades.status.cancelled', 'CANCELLED');
  });

  it('should use fallback for unknown status label', () => {
    const result = component.getStatusLabel('UNKNOWN');

    expect(translationService.t).toHaveBeenCalledWith('trades.status.unknown', 'UNKNOWN');
  });

  it('should get timeline label for PENDING', () => {
    component.getTimelineLabel('PENDING');

    expect(translationService.t).toHaveBeenCalledWith('trades.detail.timelinePending');
  });

  it('should get timeline label for COMPLETED', () => {
    component.getTimelineLabel('COMPLETED');

    expect(translationService.t).toHaveBeenCalledWith('trades.detail.timelineCompleted');
  });

  it('should get timeline label for CANCELLED', () => {
    component.getTimelineLabel('CANCELLED');

    expect(translationService.t).toHaveBeenCalledWith('trades.detail.timelineCancelled');
  });

  it('should return status string as fallback for unknown timeline', () => {
    const result = component.getTimelineLabel('UNKNOWN');

    expect(result).toBe('UNKNOWN');
  });
});
