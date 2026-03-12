import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { DraftAuditPageComponent } from './draft-audit-page.component';
import { DraftAuditService, DraftAuditEntry } from '../../services/draft-audit.service';

const GAME_ID = 'game-123';

const SWAP_ENTRY: DraftAuditEntry = {
  id: 'audit-1',
  type: 'SWAP_SOLO',
  occurredAt: '2026-03-01T10:00:00',
  participantId: 'part-1',
  proposerParticipantId: null,
  targetParticipantId: null,
  playerOutId: 'player-out-1',
  playerInId: 'player-in-1'
};

const TRADE_ACCEPTED_ENTRY: DraftAuditEntry = {
  id: 'audit-2',
  type: 'TRADE_ACCEPTED',
  occurredAt: '2026-03-01T11:00:00',
  participantId: null,
  proposerParticipantId: 'part-1',
  targetParticipantId: 'part-2',
  playerOutId: 'player-out-2',
  playerInId: 'player-in-2'
};

describe('DraftAuditPageComponent', () => {
  let fixture: ComponentFixture<DraftAuditPageComponent>;
  let component: DraftAuditPageComponent;
  let auditSpy: jasmine.SpyObj<DraftAuditService>;

  async function setupComponent(
    entries: DraftAuditEntry[] = [SWAP_ENTRY],
    fail = false
  ): Promise<void> {
    auditSpy = jasmine.createSpyObj<DraftAuditService>('DraftAuditService', ['getAudit']);
    auditSpy.getAudit.and.returnValue(
      fail ? throwError(() => new Error('fail')) : of(entries)
    );

    await TestBed.configureTestingModule({
      imports: [DraftAuditPageComponent, NoopAnimationsModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => GAME_ID } } } }
      ]
    })
      .overrideProvider(DraftAuditService, { useValue: auditSpy })
      .compileComponents();

    fixture = TestBed.createComponent(DraftAuditPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  describe('Initialization', () => {
    it('calls getAudit with gameId on init', async () => {
      await setupComponent();
      expect(auditSpy.getAudit).toHaveBeenCalledWith(GAME_ID);
    });

    it('sets gameId from route param', async () => {
      await setupComponent();
      expect(component.gameId).toBe(GAME_ID);
    });

    it('loading is false after data loads', async () => {
      await setupComponent();
      expect(component.loading()).toBeFalse();
    });

    it('entries are populated after load', async () => {
      await setupComponent([SWAP_ENTRY, TRADE_ACCEPTED_ENTRY]);
      expect(component.entries()).toEqual([SWAP_ENTRY, TRADE_ACCEPTED_ENTRY]);
    });
  });

  describe('Error state', () => {
    it('sets loadError when service fails', async () => {
      await setupComponent([], true);
      expect(component.loadError).toBeTrue();
    });

    it('shows error element when loadError is true', async () => {
      await setupComponent([], true);
      fixture.detectChanges();
      const el = fixture.debugElement.query(By.css('.audit-error'));
      expect(el).toBeTruthy();
    });
  });

  describe('Empty state', () => {
    it('shows empty state when no entries', async () => {
      await setupComponent([]);
      fixture.detectChanges();
      const el = fixture.debugElement.query(By.css('.audit-empty'));
      expect(el).toBeTruthy();
    });

    it('does not show table when entries are empty', async () => {
      await setupComponent([]);
      fixture.detectChanges();
      const table = fixture.debugElement.query(By.css('.audit-table'));
      expect(table).toBeNull();
    });
  });

  describe('Table rendering', () => {
    it('renders audit table when entries exist', async () => {
      await setupComponent([SWAP_ENTRY]);
      fixture.detectChanges();
      const table = fixture.debugElement.query(By.css('.audit-table'));
      expect(table).toBeTruthy();
    });

    it('renders one row per entry', async () => {
      await setupComponent([SWAP_ENTRY, TRADE_ACCEPTED_ENTRY]);
      fixture.detectChanges();
      const rows = fixture.debugElement.queryAll(By.css('tbody tr'));
      expect(rows.length).toBe(2);
    });

    it('applies swap chip class for SWAP_SOLO type', async () => {
      await setupComponent([SWAP_ENTRY]);
      fixture.detectChanges();
      const chip = fixture.debugElement.query(By.css('.type-chip--swap'));
      expect(chip).toBeTruthy();
    });

    it('applies trade-accepted chip class for TRADE_ACCEPTED type', async () => {
      await setupComponent([TRADE_ACCEPTED_ENTRY]);
      fixture.detectChanges();
      const chip = fixture.debugElement.query(By.css('.type-chip--trade-accepted'));
      expect(chip).toBeTruthy();
    });
  });

  describe('typeLabel', () => {
    it('returns Échange solo for SWAP_SOLO', async () => {
      await setupComponent();
      expect(component.typeLabel('SWAP_SOLO')).toBe('Échange solo');
    });

    it('returns Trade proposé for TRADE_PROPOSED', async () => {
      await setupComponent();
      expect(component.typeLabel('TRADE_PROPOSED')).toBe('Trade proposé');
    });

    it('returns Trade accepté for TRADE_ACCEPTED', async () => {
      await setupComponent();
      expect(component.typeLabel('TRADE_ACCEPTED')).toBe('Trade accepté');
    });

    it('returns Trade refusé for TRADE_REJECTED', async () => {
      await setupComponent();
      expect(component.typeLabel('TRADE_REJECTED')).toBe('Trade refusé');
    });
  });

  describe('Reload', () => {
    it('reloads audit data on reload call', async () => {
      await setupComponent([SWAP_ENTRY]);
      const callsBefore = auditSpy.getAudit.calls.count();
      component.reload();
      expect(auditSpy.getAudit.calls.count()).toBeGreaterThan(callsBefore);
    });
  });
});
