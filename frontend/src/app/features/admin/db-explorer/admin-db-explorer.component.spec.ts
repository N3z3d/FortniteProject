import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AdminDbExplorerComponent } from './admin-db-explorer.component';
import { AdminService } from '../services/admin.service';
import { DbTableInfo, SqlQueryResult } from '../models/admin.models';

const MOCK_TABLES: DbTableInfo[] = [
  { tableName: 'games', entityName: 'Game', rowCount: 100, sizeDescription: '16 KB' },
  { tableName: 'users', entityName: 'User', rowCount: 50, sizeDescription: '8 KB' }
];

const MOCK_QUERY_RESULT: SqlQueryResult = {
  columns: ['id', 'name'],
  rows: [{ id: 1, name: 'Alice' }],
  totalRows: 1,
  truncated: false
};

describe('AdminDbExplorerComponent', () => {
  let component: AdminDbExplorerComponent;
  let fixture: ComponentFixture<AdminDbExplorerComponent>;
  let adminSpy: jasmine.SpyObj<AdminService>;

  beforeEach(async () => {
    adminSpy = jasmine.createSpyObj('AdminService', ['getDatabaseTables', 'executeSqlQuery']);
    adminSpy.getDatabaseTables.and.returnValue(of(MOCK_TABLES));

    await TestBed.configureTestingModule({
      imports: [AdminDbExplorerComponent, NoopAnimationsModule],
      providers: [{ provide: AdminService, useValue: adminSpy }]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminDbExplorerComponent);
    component = fixture.componentInstance;
  });

  it('calls getDatabaseTables on init', () => {
    fixture.detectChanges();
    expect(adminSpy.getDatabaseTables).toHaveBeenCalledTimes(1);
  });

  it('populates tables signal after successful load', () => {
    fixture.detectChanges();
    expect(component.tables()).toEqual(MOCK_TABLES);
    expect(component.loading()).toBeFalse();
    expect(component.loadError).toBeFalse();
  });

  it('renders a row for each table entry', () => {
    fixture.detectChanges();
    const rows = fixture.nativeElement.querySelectorAll('mat-row, tr[mat-row]');
    expect(rows.length).toBeGreaterThanOrEqual(2);
  });

  it('sets loadError true and loading false on error', () => {
    adminSpy.getDatabaseTables.and.returnValue(throwError(() => new Error('network')));
    fixture.detectChanges();
    expect(component.loadError).toBeTrue();
    expect(component.loading()).toBeFalse();
  });

  it('displays error state element on load failure', () => {
    adminSpy.getDatabaseTables.and.returnValue(throwError(() => new Error('network')));
    fixture.detectChanges();
    const error = fixture.nativeElement.querySelector('.db-explorer__error');
    expect(error).toBeTruthy();
  });

  it('displays empty state when no tables returned', () => {
    adminSpy.getDatabaseTables.and.returnValue(of([]));
    fixture.detectChanges();
    const empty = fixture.nativeElement.querySelector('.db-explorer__empty');
    expect(empty).toBeTruthy();
  });

  it('calls loadTables again when retry button clicked', () => {
    adminSpy.getDatabaseTables.and.returnValue(throwError(() => new Error('network')));
    fixture.detectChanges();
    const retryBtn = fixture.nativeElement.querySelector('.db-explorer__error button');
    expect(retryBtn).toBeTruthy();
    adminSpy.getDatabaseTables.and.returnValue(of(MOCK_TABLES));
    retryBtn.click();
    fixture.detectChanges();
    expect(adminSpy.getDatabaseTables).toHaveBeenCalledTimes(2);
    expect(component.loadError).toBeFalse();
    expect(component.tables().length).toBe(2);
  });

  describe('SQL Query Section', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('renders query section with textarea', () => {
      const textarea = fixture.nativeElement.querySelector('.db-explorer__query-input');
      expect(textarea).toBeTruthy();
    });

    it('executeQuery does nothing when sqlQuery is empty', () => {
      component.sqlQuery = '';
      component.executeQuery();
      expect(adminSpy.executeSqlQuery).not.toHaveBeenCalled();
    });

    it('executeQuery calls service with trimmed query', () => {
      adminSpy.executeSqlQuery.and.returnValue(of(MOCK_QUERY_RESULT));
      component.sqlQuery = 'SELECT * FROM games';
      component.executeQuery();
      expect(adminSpy.executeSqlQuery).toHaveBeenCalledWith('SELECT * FROM games');
    });

    it('sets queryResult signal on success', () => {
      adminSpy.executeSqlQuery.and.returnValue(of(MOCK_QUERY_RESULT));
      component.sqlQuery = 'SELECT id, name FROM users';
      component.executeQuery();
      expect(component.queryResult()).toEqual(MOCK_QUERY_RESULT);
      expect(component.queryLoading()).toBeFalse();
      expect(component.queryError).toBeNull();
    });

    it('sets queryError on query failure', () => {
      adminSpy.executeSqlQuery.and.returnValue(
        throwError(() => ({ message: 'Only SELECT queries are allowed' }))
      );
      component.sqlQuery = 'DELETE FROM users';
      component.executeQuery();
      expect(component.queryError).toContain('Only SELECT queries are allowed');
      expect(component.queryLoading()).toBeFalse();
      expect(component.queryResult()).toBeNull();
    });

    it('getCellValue returns string for known column', () => {
      const row: Record<string, unknown> = { id: 42, name: 'Bob' };
      expect(component.getCellValue(row, 'id')).toBe('42');
      expect(component.getCellValue(row, 'name')).toBe('Bob');
    });

    it('getCellValue returns dash for null value', () => {
      const row: Record<string, unknown> = { id: null };
      expect(component.getCellValue(row, 'id')).toBe('—');
    });

    it('getCellValue returns dash for missing column', () => {
      const row: Record<string, unknown> = { id: 1 };
      expect(component.getCellValue(row, 'nonexistent')).toBe('—');
    });
  });
});
