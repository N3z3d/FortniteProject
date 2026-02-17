import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { ErrorJournalComponent } from './error-journal.component';
import { AdminService } from '../services/admin.service';
import { TranslationService } from '../../../core/services/translation.service';
import { ErrorEntry, ErrorStatistics } from '../models/error-journal.models';
import { ErrorDetailDialogComponent } from './error-detail-dialog/error-detail-dialog.component';

describe('ErrorJournalComponent', () => {
  let component: ErrorJournalComponent;
  let fixture: ComponentFixture<ErrorJournalComponent>;
  let adminServiceSpy: jasmine.SpyObj<AdminService>;

  const mockErrors: ErrorEntry[] = [
    {
      id: 'uuid-1',
      timestamp: '2026-02-17T10:00:00',
      exceptionType: 'GameNotFoundException',
      message: 'Game not found',
      statusCode: 404,
      errorCode: 'GAME_NOT_FOUND',
      path: '/api/games/123',
      stackTrace: 'stack trace here'
    },
    {
      id: 'uuid-2',
      timestamp: '2026-02-17T09:30:00',
      exceptionType: 'BusinessException',
      message: 'Business rule violated',
      statusCode: 400,
      errorCode: 'BUSINESS_RULE_VIOLATION',
      path: '/api/trades',
      stackTrace: 'another stack trace'
    }
  ];

  const mockStats: ErrorStatistics = {
    totalErrors: 15,
    errorsByType: { GameNotFoundException: 10, BusinessException: 5 },
    errorsByStatusCode: { 404: 10, 400: 5 },
    topErrors: [
      { type: 'GameNotFoundException', message: 'not found', count: 10, lastOccurrence: '2026-02-17T10:00:00' }
    ]
  };

  beforeEach(async () => {
    adminServiceSpy = jasmine.createSpyObj('AdminService', ['getErrors', 'getErrorStatistics', 'getErrorDetail']);

    adminServiceSpy.getErrors.and.returnValue(of(mockErrors));
    adminServiceSpy.getErrorStatistics.and.returnValue(of(mockStats));

    await TestBed.configureTestingModule({
      imports: [ErrorJournalComponent, NoopAnimationsModule],
      providers: [
        { provide: AdminService, useValue: adminServiceSpy },
        TranslationService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ErrorJournalComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('initialization', () => {
    it('should load errors and statistics on init', fakeAsync(() => {
      fixture.detectChanges();
      tick();

      expect(component.errors).toEqual(mockErrors);
      expect(component.statistics).toEqual(mockStats);
      expect(component.loading).toBeFalse();
      expect(component.error).toBeFalse();
    }));

    it('should set error flag on load failure', fakeAsync(() => {
      adminServiceSpy.getErrors.and.returnValue(throwError(() => new Error('Network error')));

      fixture.detectChanges();
      tick();

      expect(component.error).toBeTrue();
      expect(component.loading).toBeFalse();
    }));

    it('should show loading initially', () => {
      expect(component.loading).toBeTrue();
    });
  });

  describe('filtering', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      tick();
    }));

    it('should reload data when filters applied', fakeAsync(() => {
      component.statusFilter = 400;
      component.typeFilter = 'Business';
      adminServiceSpy.getErrors.calls.reset();

      component.applyFilters();
      tick();

      expect(adminServiceSpy.getErrors).toHaveBeenCalledWith(50, 400, 'Business');
    }));

    it('should clear filters and reload', fakeAsync(() => {
      component.statusFilter = 500;
      component.typeFilter = 'Game';
      adminServiceSpy.getErrors.calls.reset();

      component.clearFilters();
      tick();

      expect(component.statusFilter).toBeNull();
      expect(component.typeFilter).toBe('');
      expect(adminServiceSpy.getErrors).toHaveBeenCalledWith(50, undefined, undefined);
    }));
  });

  describe('detail dialog', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      tick();
    }));

    it('should open detail dialog with error entry', () => {
      const openSpy = spyOn((component as any).dialog, 'open');

      component.openDetail(mockErrors[0]);

      expect(openSpy).toHaveBeenCalledWith(
        ErrorDetailDialogComponent,
        jasmine.objectContaining({
          data: mockErrors[0],
          width: '700px'
        })
      );
    });

    it('should open detail dialog from keyboard on Enter', () => {
      const openSpy = spyOn(component, 'openDetail');
      const event = new KeyboardEvent('keydown', { key: 'Enter' });

      component.onErrorRowKeydown(event, mockErrors[0]);

      expect(openSpy).toHaveBeenCalledWith(mockErrors[0]);
    });

    it('should ignore unrelated keys for row keyboard handler', () => {
      const openSpy = spyOn(component, 'openDetail');
      const event = new KeyboardEvent('keydown', { key: 'Escape' });

      component.onErrorRowKeydown(event, mockErrors[0]);

      expect(openSpy).not.toHaveBeenCalled();
    });
  });

  describe('accessibility', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      tick();
      fixture.detectChanges();
    }));

    it('should render each error row as keyboard-focusable button', () => {
      const firstRow = fixture.nativeElement.querySelector('tr.error-row') as HTMLElement;

      expect(firstRow).toBeTruthy();
      expect(firstRow.getAttribute('role')).toBe('button');
      expect(firstRow.getAttribute('tabindex')).toBe('0');
    });

    it('should expose explicit aria-label for detail icon button', () => {
      const detailButton = fixture.nativeElement.querySelector('button[mat-icon-button]') as HTMLButtonElement;

      expect(detailButton).toBeTruthy();
      expect(detailButton.getAttribute('aria-label')).toBe('admin.errors.table.viewDetail');
    });
  });

  describe('helpers', () => {
    it('should return correct status class for 5xx', () => {
      expect(component.getStatusClass(500)).toBe('status-5xx');
      expect(component.getStatusClass(503)).toBe('status-5xx');
    });

    it('should return correct status class for 4xx', () => {
      expect(component.getStatusClass(400)).toBe('status-4xx');
      expect(component.getStatusClass(404)).toBe('status-4xx');
    });

    it('should return other class for non-4xx/5xx', () => {
      expect(component.getStatusClass(200)).toBe('status-other');
    });

    it('should truncate long messages', () => {
      const longMessage = 'A'.repeat(100);
      expect(component.truncateMessage(longMessage, 80).length).toBe(83);
      expect(component.truncateMessage(longMessage, 80)).toContain('...');
    });

    it('should not truncate short messages', () => {
      expect(component.truncateMessage('short')).toBe('short');
    });

    it('should handle empty message', () => {
      expect(component.truncateMessage('')).toBe('');
    });

    it('should return status entries from statistics', fakeAsync(() => {
      fixture.detectChanges();
      tick();

      const entries = component.getStatusEntries();
      expect(entries.length).toBe(2);
    }));

    it('should return type entries from statistics', fakeAsync(() => {
      fixture.detectChanges();
      tick();

      const entries = component.getTypeEntries();
      expect(entries.length).toBe(2);
    }));

    it('should return empty arrays when no statistics', () => {
      component.statistics = null;
      expect(component.getStatusEntries()).toEqual([]);
      expect(component.getTypeEntries()).toEqual([]);
    });
  });

  describe('pagination', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      tick();
    }));

    it('should update pageSize on page change', fakeAsync(() => {
      adminServiceSpy.getErrors.calls.reset();

      component.onPageChange({ pageIndex: 0, pageSize: 100, length: 50 });
      tick();

      expect(component.pageSize).toBe(100);
      expect(adminServiceSpy.getErrors).toHaveBeenCalledWith(100, undefined, undefined);
    }));
  });

  describe('refresh', () => {
    it('should reload data on refresh', fakeAsync(() => {
      fixture.detectChanges();
      tick();
      adminServiceSpy.getErrors.calls.reset();

      component.loadData();
      tick();

      expect(adminServiceSpy.getErrors).toHaveBeenCalled();
      expect(adminServiceSpy.getErrorStatistics).toHaveBeenCalled();
    }));
  });
});
