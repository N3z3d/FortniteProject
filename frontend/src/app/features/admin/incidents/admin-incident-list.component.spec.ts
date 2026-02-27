import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { AdminIncidentListComponent } from './admin-incident-list.component';
import { AdminService } from '../services/admin.service';
import { TranslationService } from '../../../core/services/translation.service';
import { IncidentEntry } from '../models/admin.models';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

const mockIncidents: IncidentEntry[] = [
  {
    id: 'id-1',
    gameId: 'game-1',
    gameName: 'Game Alpha',
    reporterId: 'r-1',
    reporterUsername: 'player1',
    incidentType: 'CHEATING',
    description: 'Suspected aimbot usage',
    timestamp: '2026-02-27T09:00:00'
  },
  {
    id: 'id-2',
    gameId: 'game-1',
    gameName: 'Game Alpha',
    reporterId: 'r-2',
    reporterUsername: 'player2',
    incidentType: 'ABUSE',
    description: 'Verbal abuse in chat',
    timestamp: '2026-02-27T10:00:00'
  }
];

describe('AdminIncidentListComponent', () => {
  let component: AdminIncidentListComponent;
  let fixture: ComponentFixture<AdminIncidentListComponent>;
  let adminServiceSpy: jasmine.SpyObj<AdminService>;
  let translationServiceSpy: jasmine.SpyObj<TranslationService>;

  beforeEach(async () => {
    adminServiceSpy = jasmine.createSpyObj('AdminService', ['getIncidents']);
    translationServiceSpy = jasmine.createSpyObj('TranslationService', ['t']);
    translationServiceSpy.t.and.callFake((key: string) => key);

    adminServiceSpy.getIncidents.and.returnValue(of(mockIncidents));

    await TestBed.configureTestingModule({
      imports: [AdminIncidentListComponent, NoopAnimationsModule],
      providers: [
        { provide: AdminService, useValue: adminServiceSpy },
        { provide: TranslationService, useValue: translationServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminIncidentListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load incidents on init', () => {
    expect(adminServiceSpy.getIncidents).toHaveBeenCalledWith(50);
    expect(component.incidents.length).toBe(2);
    expect(component.loading).toBeFalse();
    expect(component.error).toBeFalse();
  });

  it('should display loading state while fetching', () => {
    component.loading = true;
    fixture.detectChanges();
    const spinner = fixture.nativeElement.querySelector('mat-spinner');
    expect(spinner).toBeTruthy();
  });

  it('should display error state on load failure', () => {
    adminServiceSpy.getIncidents.and.returnValue(throwError(() => new Error('HTTP error')));
    component.loadData();
    fixture.detectChanges();

    expect(component.error).toBeTrue();
    expect(component.loading).toBeFalse();
  });

  it('should filter incidents by type when typeFilter is set', () => {
    adminServiceSpy.getIncidents.and.returnValue(of(mockIncidents));
    component.typeFilter = 'CHEATING';
    component.loadData();

    expect(component.incidents.length).toBe(1);
    expect(component.incidents[0].incidentType).toBe('CHEATING');
  });

  it('should clear filters and reload', () => {
    component.typeFilter = 'ABUSE';
    adminServiceSpy.getIncidents.and.returnValue(of(mockIncidents));
    component.clearFilters();

    expect(component.typeFilter).toBeNull();
    expect(component.incidents.length).toBe(2);
  });

  it('should truncate long descriptions', () => {
    const longText = 'a'.repeat(120);
    const result = component.truncate(longText, 80);
    expect(result.length).toBe(83);
    expect(result.endsWith('...')).toBeTrue();
  });

  it('should return text unchanged if within limit', () => {
    const shortText = 'short description';
    expect(component.truncate(shortText, 80)).toBe(shortText);
  });

  it('should format timestamp as locale string', () => {
    const ts = '2026-02-27T09:00:00';
    const result = component.formatTimestamp(ts);
    expect(result).toBeTruthy();
    expect(result.length).toBeGreaterThan(0);
  });

  it('should return empty string for empty timestamp', () => {
    expect(component.formatTimestamp('')).toBe('');
  });

  it('should unsubscribe on destroy', () => {
    const nextSpy = spyOn(component['destroy$'], 'next').and.callThrough();
    component.ngOnDestroy();
    expect(nextSpy).toHaveBeenCalled();
  });
});
