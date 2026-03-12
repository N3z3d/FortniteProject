import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AdminGamesSupervisionComponent } from './admin-games-supervision.component';
import { AdminService } from '../../services/admin.service';
import { GameSupervisionEntry } from '../../models/admin.models';

const MOCK_GAMES: GameSupervisionEntry[] = [
  {
    gameId: 'g1',
    gameName: 'Alpha League',
    status: 'ACTIVE',
    draftMode: 'SNAKE',
    participantCount: 4,
    maxParticipants: 8,
    creatorUsername: 'thibaut',
    createdAt: '2026-03-01T10:00:00'
  },
  {
    gameId: 'g2',
    gameName: 'Beta Cup',
    status: 'DRAFTING',
    draftMode: 'SNAKE',
    participantCount: 2,
    maxParticipants: 6,
    creatorUsername: 'alice',
    createdAt: '2026-03-01T09:00:00'
  },
  {
    gameId: 'g3',
    gameName: 'Gamma Open',
    status: 'CREATING',
    draftMode: 'SNAKE',
    participantCount: 1,
    maxParticipants: 4,
    creatorUsername: 'bob',
    createdAt: '2026-03-01T08:00:00'
  }
];

describe('AdminGamesSupervisionComponent', () => {
  let component: AdminGamesSupervisionComponent;
  let fixture: ComponentFixture<AdminGamesSupervisionComponent>;
  let adminService: jasmine.SpyObj<AdminService>;

  beforeEach(async () => {
    adminService = jasmine.createSpyObj('AdminService', ['getGamesSupervision']);
    adminService.getGamesSupervision.and.returnValue(of(MOCK_GAMES));

    await TestBed.configureTestingModule({
      imports: [
        AdminGamesSupervisionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule
      ],
      providers: [{ provide: AdminService, useValue: adminService }]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminGamesSupervisionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('displays the game list after loading', () => {
    const rows = fixture.nativeElement.querySelectorAll('tr[mat-row]');
    expect(rows.length).toBe(3);
  });

  it('shows all games when filter is ALL', () => {
    component.selectStatus('ALL');
    fixture.detectChanges();
    expect(component.filteredGames().length).toBe(3);
  });

  it('filters to only DRAFTING games when DRAFTING tab selected', () => {
    component.selectStatus('DRAFTING');
    fixture.detectChanges();
    const drafting = component.filteredGames();
    expect(drafting.length).toBe(1);
    expect(drafting[0].gameName).toBe('Beta Cup');
  });

  it('filters to only ACTIVE games when ACTIVE tab selected', () => {
    component.selectStatus('ACTIVE');
    fixture.detectChanges();
    expect(component.filteredGames().length).toBe(1);
    expect(component.filteredGames()[0].status).toBe('ACTIVE');
  });

  it('shows empty state message when filteredGames is empty', () => {
    component.selectStatus('CREATING');
    fixture.detectChanges();
    // filter to CREATING: 1 game
    expect(component.filteredGames().length).toBe(1);
    expect(component.filteredGames()[0].status).toBe('CREATING');
    // now filter to a status with no games
    adminService.getGamesSupervision.and.returnValue(of([]));
    component['loadGames']();
    fixture.detectChanges();
    component.selectStatus('ACTIVE');
    fixture.detectChanges();
    expect(component.filteredGames().length).toBe(0);
    const emptyRow = fixture.nativeElement.querySelector('.supervision-table__empty');
    expect(emptyRow).toBeTruthy();
  });

  it('shows error banner when loadError is true', () => {
    component.loadError = true;
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.supervision-error');
    expect(banner).toBeTruthy();
  });

  it('sets loadError to true when service throws an error', () => {
    adminService.getGamesSupervision.and.returnValue(throwError(() => new Error('Network error')));
    component['loadGames']();
    fixture.detectChanges();
    expect(component.loadError).toBeTrue();
  });

  it('statusClass returns correct CSS class for ACTIVE', () => {
    expect(component['statusClass']('ACTIVE')).toBe('status-badge--active');
  });

  it('statusClass returns correct CSS class for CREATING', () => {
    expect(component['statusClass']('CREATING')).toBe('status-badge--creating');
  });

  it('statusClass returns correct CSS class for DRAFTING', () => {
    expect(component['statusClass']('DRAFTING')).toBe('status-badge--drafting');
  });

  it('does not show spinner after loading completes', fakeAsync(() => {
    component.loading.set(false);
    fixture.detectChanges();
    tick();
    const spinner = fixture.nativeElement.querySelector('mat-spinner');
    expect(spinner).toBeFalsy();
  }));
});
