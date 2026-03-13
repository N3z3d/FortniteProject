import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminLogsComponent } from './admin-logs.component';
import { AdminService } from '../services/admin.service';
import { of, throwError } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('AdminLogsComponent', () => {
  let fixture: ComponentFixture<AdminLogsComponent>;
  let component: AdminLogsComponent;
  let adminServiceSpy: jasmine.SpyObj<AdminService>;

  const scrapeLog = {
    id: 'log-1', source: 'EU', startedAt: '2026-01-01T10:00:00',
    finishedAt: '2026-01-01T10:30:00', status: 'SUCCESS',
    totalRowsWritten: 42, errorMessage: null
  };

  const auditEntry = {
    id: 'audit-1', actor: 'admin', action: 'CORRECT_METADATA',
    entityType: 'PLAYER_IDENTITY', entityId: 'pid-1',
    details: 'username=Foo', timestamp: '2026-01-01T11:00:00'
  };

  beforeEach(async () => {
    adminServiceSpy = jasmine.createSpyObj('AdminService', ['getScrapingLogs', 'getAuditLog']);
    adminServiceSpy.getScrapingLogs.and.returnValue(of([scrapeLog]));
    adminServiceSpy.getAuditLog.and.returnValue(of([auditEntry]));

    await TestBed.configureTestingModule({
      imports: [AdminLogsComponent, NoopAnimationsModule],
      providers: [{ provide: AdminService, useValue: adminServiceSpy }]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminLogsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call both getScrapingLogs and getAuditLog on init', () => {
    expect(adminServiceSpy.getScrapingLogs).toHaveBeenCalledWith(50);
    expect(adminServiceSpy.getAuditLog).toHaveBeenCalledWith(50);
  });

  it('should populate scrapeLogs after successful load', () => {
    expect(component.scrapeLogs).toEqual([scrapeLog]);
  });

  it('should populate auditEntries after successful load', () => {
    expect(component.auditEntries).toEqual([auditEntry]);
  });

  it('should set loading to false after load completes', () => {
    expect(component.loading).toBeFalse();
  });

  it('should set error message on load failure', () => {
    adminServiceSpy.getScrapingLogs.and.returnValue(throwError(() => new Error('net')));
    component.loadData();
    expect(component.error).toBeTruthy();
    expect(component.loading).toBeFalse();
  });

  describe('scrapeStatusClass', () => {
    it('returns status-success for SUCCESS', () => {
      expect(component.scrapeStatusClass('SUCCESS')).toBe('status-success');
    });

    it('returns status-partial for PARTIAL', () => {
      expect(component.scrapeStatusClass('PARTIAL')).toBe('status-partial');
    });

    it('returns status-error for ERROR', () => {
      expect(component.scrapeStatusClass('ERROR')).toBe('status-error');
    });

    it('returns status-running for RUNNING', () => {
      expect(component.scrapeStatusClass('RUNNING')).toBe('status-running');
    });

    it('returns empty string for unknown status', () => {
      expect(component.scrapeStatusClass('UNKNOWN')).toBe('');
    });
  });

  describe('loadData', () => {
    it('resets error and loading before fetching', () => {
      component.error = 'old error';
      adminServiceSpy.getScrapingLogs.and.returnValue(of([]));
      adminServiceSpy.getAuditLog.and.returnValue(of([]));
      component.loadData();
      expect(component.error).toBeNull();
    });
  });
});
