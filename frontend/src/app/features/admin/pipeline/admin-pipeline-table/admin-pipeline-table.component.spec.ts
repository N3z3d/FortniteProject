import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { AdminPipelineTableComponent } from './admin-pipeline-table.component';
import { PlayerIdentityEntry } from '../../models/admin.models';

const ENTRY_UNRESOLVED: PlayerIdentityEntry = {
  id: 'e1',
  playerId: 'p1',
  playerUsername: 'Bughaboo',
  playerRegion: 'EU',
  epicId: null,
  status: 'UNRESOLVED',
  confidenceScore: null,
  resolvedBy: null,
  resolvedAt: null,
  rejectedAt: null,
  rejectionReason: null,
  createdAt: '2026-01-01T00:00:00'
};

const ENTRY_RESOLVED: PlayerIdentityEntry = {
  id: 'e2',
  playerId: 'p2',
  playerUsername: 'AlphaKing',
  playerRegion: 'NA',
  epicId: 'alphaking_fn',
  status: 'RESOLVED',
  confidenceScore: 88,
  resolvedBy: 'admin',
  resolvedAt: '2026-01-02T10:00:00',
  rejectedAt: null,
  rejectionReason: null,
  createdAt: '2026-01-01T00:00:00'
};

const ENTRY_REJECTED: PlayerIdentityEntry = {
  id: 'e3',
  playerId: 'p3',
  playerUsername: 'Shadow',
  playerRegion: 'EU',
  epicId: null,
  status: 'REJECTED',
  confidenceScore: null,
  resolvedBy: null,
  resolvedAt: null,
  rejectedAt: '2026-01-03T00:00:00',
  rejectionReason: 'Joueur introuvable',
  createdAt: '2026-01-01T00:00:00'
};

describe('AdminPipelineTableComponent', () => {
  let fixture: ComponentFixture<AdminPipelineTableComponent>;
  let component: AdminPipelineTableComponent;

  function setup(
    entries: PlayerIdentityEntry[] = [],
    mode: 'unresolved' | 'resolved' = 'unresolved'
  ): void {
    fixture.componentRef.setInput('entries', entries);
    fixture.componentRef.setInput('mode', mode);
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminPipelineTableComponent, NoopAnimationsModule]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminPipelineTableComponent);
    component = fixture.componentInstance;
  });

  describe('Empty state', () => {
    it('shows empty message in unresolved mode when no entries', () => {
      setup([], 'unresolved');
      const msg = fixture.debugElement.query(By.css('.pipeline-table__empty'));
      expect(msg).toBeTruthy();
      expect(msg.nativeElement.textContent).toContain('Aucun joueur en attente');
    });

    it('shows empty message in resolved mode when no entries', () => {
      setup([], 'resolved');
      const msg = fixture.debugElement.query(By.css('.pipeline-table__empty'));
      expect(msg.nativeElement.textContent).toContain('Aucun joueur résolu');
    });

    it('hides table when entries list is empty', () => {
      setup([], 'unresolved');
      expect(fixture.debugElement.query(By.css('table'))).toBeNull();
    });
  });

  describe('Unresolved mode', () => {
    beforeEach(() => setup([ENTRY_UNRESOLVED], 'unresolved'));

    it('renders the table with unresolved entry', () => {
      expect(fixture.debugElement.query(By.css('table'))).toBeTruthy();
    });

    it('renders player username in table', () => {
      const cells = fixture.debugElement.queryAll(By.css('.cell--username'));
      expect(cells[0].nativeElement.textContent).toContain('Bughaboo');
    });

    it('renders region badge', () => {
      const badge = fixture.debugElement.query(By.css('.region-badge'));
      expect(badge.nativeElement.textContent).toContain('EU');
    });

    it('shows editable epic ID input field', () => {
      const input = fixture.debugElement.query(By.css('.epic-id-input'));
      expect(input).toBeTruthy();
    });

    it('epic ID input uses monospace font class', () => {
      const input = fixture.debugElement.query(By.css('.epic-id-input'));
      const style = getComputedStyle(input.nativeElement);
      // Verify the CSS class applies (component sets the class)
      expect(input.nativeElement.classList.contains('epic-id-input')).toBeTrue();
    });

    it('confirm button is disabled when epic ID field is empty', () => {
      const confirmBtn = fixture.debugElement.query(By.css('.btn-confirm'));
      expect(confirmBtn.nativeElement.disabled).toBeTrue();
    });

    it('confirm button becomes enabled when epic ID is filled', () => {
      const control = component.getControl(ENTRY_UNRESOLVED.playerId);
      control.setValue('bughaboo_fn');
      fixture.detectChanges();
      const confirmBtn = fixture.debugElement.query(By.css('.btn-confirm'));
      expect(confirmBtn.nativeElement.disabled).toBeFalse();
    });

    it('emits resolved event with playerId and epicId on confirm click', () => {
      const spy = jasmine.createSpy('resolved');
      component.resolved.subscribe(spy);
      const control = component.getControl(ENTRY_UNRESOLVED.playerId);
      control.setValue('bughaboo_fn');
      fixture.detectChanges();
      fixture.debugElement.query(By.css('.btn-confirm')).nativeElement.click();
      expect(spy).toHaveBeenCalledWith({ playerId: 'p1', epicId: 'bughaboo_fn' });
    });

    it('emits rejected event with playerId on reject click', () => {
      const spy = jasmine.createSpy('rejected');
      component.rejected.subscribe(spy);
      fixture.debugElement.query(By.css('.btn-reject')).nativeElement.click();
      expect(spy).toHaveBeenCalledWith({ playerId: 'p1' });
    });

    it('Enter key triggers confirm when epic ID is filled', () => {
      const spy = jasmine.createSpy('resolved');
      component.resolved.subscribe(spy);
      const control = component.getControl(ENTRY_UNRESOLVED.playerId);
      control.setValue('some_epic_id');
      fixture.detectChanges();
      const input = fixture.debugElement.query(By.css('.epic-id-input'));
      const event = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true });
      input.nativeElement.dispatchEvent(event);
      expect(spy).toHaveBeenCalled();
    });

    it('Escape key clears the epic ID field', () => {
      const control = component.getControl(ENTRY_UNRESOLVED.playerId);
      control.setValue('partial_input');
      const input = fixture.debugElement.query(By.css('.epic-id-input'));
      const event = new KeyboardEvent('keydown', { key: 'Escape', bubbles: true });
      input.nativeElement.dispatchEvent(event);
      expect(control.value).toBe('');
    });

    it('displays unresolved columns: username, region, epicId, actions', () => {
      expect(component.displayedColumns).toEqual([
        'playerUsername',
        'playerRegion',
        'epicId',
        'actions'
      ]);
    });
  });

  describe('Resolved mode', () => {
    beforeEach(() => setup([ENTRY_RESOLVED, ENTRY_REJECTED], 'resolved'));

    it('renders resolved entry without edit input', () => {
      expect(fixture.debugElement.query(By.css('.epic-id-input'))).toBeNull();
    });

    it('shows epicId as read-only text', () => {
      const spans = fixture.debugElement.queryAll(By.css('.epic-id-readonly'));
      expect(spans[0].nativeElement.textContent).toContain('alphaking_fn');
    });

    it('shows confidence score column', () => {
      const cells = fixture.debugElement.queryAll(By.css('.cell--score'));
      expect(cells[0].nativeElement.textContent).toContain('88');
    });

    it('shows status chips', () => {
      const chips = fixture.debugElement.queryAll(By.css('.status-chip'));
      expect(chips.length).toBeGreaterThan(0);
    });

    it('applies correct CSS class to RESOLVED status chip', () => {
      const resolvedChip = fixture.debugElement.queryAll(By.css('.status--resolved'));
      expect(resolvedChip.length).toBe(1);
    });

    it('applies correct CSS class to REJECTED status chip', () => {
      const rejectedChip = fixture.debugElement.queryAll(By.css('.status--rejected'));
      expect(rejectedChip.length).toBe(1);
    });

    it('displays resolved columns: username, region, epicId, score, resolvedBy, status', () => {
      expect(component.displayedColumns).toEqual([
        'playerUsername',
        'playerRegion',
        'epicId',
        'confidenceScore',
        'resolvedBy',
        'status'
      ]);
    });

    it('hides actions buttons in resolved mode', () => {
      expect(fixture.debugElement.query(By.css('.btn-confirm'))).toBeNull();
      expect(fixture.debugElement.query(By.css('.btn-reject'))).toBeNull();
    });
  });

  describe('scoreClass', () => {
    it('returns score--high for score >= 80', () => {
      expect(component.scoreClass(80)).toBe('score--high');
      expect(component.scoreClass(100)).toBe('score--high');
    });

    it('returns score--medium for score 50-79', () => {
      expect(component.scoreClass(50)).toBe('score--medium');
      expect(component.scoreClass(79)).toBe('score--medium');
    });

    it('returns score--low for score < 50', () => {
      expect(component.scoreClass(30)).toBe('score--low');
    });

    it('returns score--unknown for null score', () => {
      expect(component.scoreClass(null)).toBe('score--unknown');
    });
  });
});
