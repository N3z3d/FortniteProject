import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { DataSourceStrategy, DataSourceType } from './data-source.strategy';
import { LoggerService } from '../services/logger.service';

describe('DataSourceStrategy', () => {
  let strategy: DataSourceStrategy;
  let loggerService: jasmine.SpyObj<LoggerService>;

  beforeEach(() => {
    const loggerSpy = jasmine.createSpyObj('LoggerService', ['debug', 'error', 'warn']);

    TestBed.configureTestingModule({
      providers: [
        DataSourceStrategy,
        { provide: LoggerService, useValue: loggerSpy }
      ]
    });

    strategy = TestBed.inject(DataSourceStrategy);
    loggerService = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;
  });

  it('should use primary source when available', (done) => {
    const primaryData = { value: 'from-db' };
    const primary$ = of(primaryData);
    const fallback$ = of({ value: 'from-mock' });

    strategy.executeWithFallback(primary$, fallback$, 'test-operation')
      .subscribe(data => {
        expect(data).toEqual(primaryData);
        expect(strategy.isDatabaseAvailable()).toBe(true);
        expect(strategy.getCurrentStatus().type).toBe(DataSourceType.DATABASE);
        done();
      });
  });

  it('should fallback to secondary source when primary fails', (done) => {
    const fallbackData = { value: 'from-mock' };
    const primary$ = throwError(() => new Error('DB connection failed'));
    const fallback$ = of(fallbackData);

    strategy.executeWithFallback(primary$, fallback$, 'test-operation', { allowFallback: true })
      .subscribe(data => {
        expect(data).toEqual(fallbackData);
        expect(strategy.isDatabaseAvailable()).toBe(false);
        expect(strategy.getCurrentStatus().type).toBe(DataSourceType.MOCK);
        expect(loggerService.warn).toHaveBeenCalled();
        done();
      });
  });

  it('should surface error when fallback is disabled', (done) => {
    const primary$ = throwError(() => new Error('DB connection failed'));
    const fallback$ = of({ value: 'from-mock' });

    strategy.executeWithFallback(primary$, fallback$, 'test-operation', { allowFallback: false })
      .subscribe({
        next: () => fail('Should have thrown error'),
        error: () => {
          expect(strategy.getCurrentStatus().type).toBe(DataSourceType.ERROR);
          expect(loggerService.error).toHaveBeenCalled();
          done();
        }
      });
  });

  it('should emit status updates via observable', (done) => {
    const primary$ = of({ value: 'test' });
    const fallback$ = of({ value: 'fallback' });

    let statusUpdateCount = 0;
    strategy.currentSource$.subscribe(status => {
      statusUpdateCount++;
      if (statusUpdateCount === 2) { // Initial + after primary success
        expect(status.type).toBe(DataSourceType.DATABASE);
        expect(status.isAvailable).toBe(true);
        done();
      }
    });

    strategy.executeWithFallback(primary$, fallback$, 'test').subscribe();
  });

  it('should fail when both sources fail', (done) => {
    const primary$ = throwError(() => new Error('Primary failed'));
    const fallback$ = throwError(() => new Error('Fallback failed'));

    strategy.executeWithFallback(primary$, fallback$, 'test-operation', { allowFallback: true })
      .subscribe({
        next: () => fail('Should have thrown error'),
        error: (error) => {
          expect(error.message).toContain('All data sources failed');
          expect(loggerService.error).toHaveBeenCalled();
          done();
        }
      });
  });
});
