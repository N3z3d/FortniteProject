import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, Subject } from 'rxjs';
import { AdminPipelineTableComponent } from './admin-pipeline-table.component';
import { EpicIdSuggestion, PlayerIdentityEntry } from '../../models/admin.models';
import { PipelineService } from '../../services/pipeline.service';

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
  createdAt: '2026-01-01T00:00:00',
  correctedUsername: null,
  correctedRegion: null,
  correctedBy: null,
  correctedAt: null
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
  createdAt: '2026-01-01T00:00:00',
  correctedUsername: null,
  correctedRegion: null,
  correctedBy: null,
  correctedAt: null
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
  createdAt: '2026-01-01T00:00:00',
  correctedUsername: null,
  correctedRegion: null,
  correctedBy: null,
  correctedAt: null
};

describe('AdminPipelineTableComponent', () => {
  let fixture: ComponentFixture<AdminPipelineTableComponent>;
  let component: AdminPipelineTableComponent;
  let pipelineServiceSpy: jasmine.SpyObj<PipelineService>;

  function setup(
    entries: PlayerIdentityEntry[] = [],
    mode: 'unresolved' | 'resolved' = 'unresolved'
  ): void {
    fixture.componentRef.setInput('entries', entries);
    fixture.componentRef.setInput('mode', mode);
    fixture.detectChanges();
  }

  beforeEach(async () => {
    pipelineServiceSpy = jasmine.createSpyObj('PipelineService', ['getSuggestedEpicId']);
    pipelineServiceSpy.getSuggestedEpicId.and.returnValue(of(null));

    await TestBed.configureTestingModule({
      imports: [AdminPipelineTableComponent, NoopAnimationsModule],
      providers: [{ provide: PipelineService, useValue: pipelineServiceSpy }]
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

    it('exposes playerId on the rendered row for stable targeting', () => {
      const row = fixture.debugElement.query(By.css('.pipeline-row'));
      expect(row.nativeElement.getAttribute('data-player-id')).toBe('p1');
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

    it('emits correctRequested event with full entry on correct click', () => {
      const spy = jasmine.createSpy('correctRequested');
      component.correctRequested.subscribe(spy);
      fixture.debugElement.query(By.css('.btn-correct')).nativeElement.click();
      expect(spy).toHaveBeenCalledWith(ENTRY_UNRESOLVED);
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

    it('displays resolved columns: username, region, epicId, score, resolvedBy, status, actions', () => {
      expect(component.displayedColumns).toEqual([
        'playerUsername',
        'playerRegion',
        'epicId',
        'confidenceScore',
        'resolvedBy',
        'status',
        'actions'
      ]);
    });

    it('hides confirm and reject buttons in resolved mode', () => {
      expect(fixture.debugElement.query(By.css('.btn-confirm'))).toBeNull();
      expect(fixture.debugElement.query(By.css('.btn-reject'))).toBeNull();
    });

    it('shows correct button in resolved mode', () => {
      const correctBtns = fixture.debugElement.queryAll(By.css('.btn-correct'));
      expect(correctBtns.length).toBe(2);
    });

    it('emits correctRequested event with entry on correct click in resolved mode', () => {
      const spy = jasmine.createSpy('correctRequested');
      component.correctRequested.subscribe(spy);
      fixture.debugElement.queryAll(By.css('.btn-correct'))[0].nativeElement.click();
      expect(spy).toHaveBeenCalledWith(ENTRY_RESOLVED);
    });
  });

  describe('Auto-suggest on load', () => {
    it('auto-suggests for each entry when mode is unresolved', () => {
      const suggestion: EpicIdSuggestion = {
        suggestedEpicId: 'auto-epic-id',
        displayName: 'Bughaboo',
        confidenceScore: 85,
        found: true
      };
      pipelineServiceSpy.getSuggestedEpicId.and.returnValue(of(suggestion));

      setup([ENTRY_UNRESOLVED], 'unresolved');

      expect(pipelineServiceSpy.getSuggestedEpicId).toHaveBeenCalledWith(
        'p1',
        jasmine.objectContaining({ onRetry: jasmine.any(Function) })
      );
      expect(component.getControl('p1').value).toBe('auto-epic-id');
    });

    it('does not auto-suggest in resolved mode', () => {
      setup([ENTRY_RESOLVED], 'resolved');

      expect(pipelineServiceSpy.getSuggestedEpicId).not.toHaveBeenCalled();
    });

    it('does not re-suggest if suggestion already present', () => {
      const suggestion: EpicIdSuggestion = {
        suggestedEpicId: 'first-epic',
        displayName: 'Bughaboo',
        confidenceScore: 80,
        found: true
      };
      pipelineServiceSpy.getSuggestedEpicId.and.returnValue(of(suggestion));

      // Premier chargement
      setup([ENTRY_UNRESOLVED], 'unresolved');
      expect(pipelineServiceSpy.getSuggestedEpicId).toHaveBeenCalledTimes(1);

      // Deuxième chargement avec les mêmes entrées
      fixture.componentRef.setInput('entries', [ENTRY_UNRESOLVED]);
      fixture.detectChanges();

      // Pas de nouvel appel car la suggestion est déjà présente
      expect(pipelineServiceSpy.getSuggestedEpicId).toHaveBeenCalledTimes(1);
    });
  });

  describe('Suggest Epic ID', () => {
    beforeEach(() => setup([ENTRY_UNRESOLVED], 'unresolved'));

    it('shows suggest button in unresolved mode', () => {
      const btn = fixture.debugElement.query(By.css('.btn-suggest'));
      expect(btn).toBeTruthy();
    });

    it('pre-fills epic ID input when suggestion is found', () => {
      const suggestion: EpicIdSuggestion = {
        suggestedEpicId: 'bugha_epic_id',
        displayName: 'Bugha',
        confidenceScore: 90,
        found: true
      };
      pipelineServiceSpy.getSuggestedEpicId.and.returnValue(of(suggestion));

      component.onSuggest(ENTRY_UNRESOLVED);
      fixture.detectChanges();

      const control = component.getControl(ENTRY_UNRESOLVED.playerId);
      expect(control.value).toBe('bugha_epic_id');
    });

    it('shows confidence badge after suggestion is found', () => {
      const suggestion: EpicIdSuggestion = {
        suggestedEpicId: 'bugha_epic_id',
        displayName: 'Bugha',
        confidenceScore: 90,
        found: true
      };
      pipelineServiceSpy.getSuggestedEpicId.and.returnValue(of(suggestion));

      component.onSuggest(ENTRY_UNRESOLVED);
      fixture.detectChanges();

      const badge = fixture.debugElement.query(By.css('.confidence-badge'));
      expect(badge).toBeTruthy();
      expect(badge.nativeElement.textContent).toContain('90%');
    });

    it('does not pre-fill input when suggestion not found', () => {
      const notFound: EpicIdSuggestion = {
        suggestedEpicId: null,
        displayName: null,
        confidenceScore: 0,
        found: false
      };
      pipelineServiceSpy.getSuggestedEpicId.and.returnValue(of(notFound));

      component.onSuggest(ENTRY_UNRESOLVED);
      fixture.detectChanges();

      const control = component.getControl(ENTRY_UNRESOLVED.playerId);
      expect(control.value).toBe('');
      expect(fixture.debugElement.query(By.css('.confidence-badge'))).toBeNull();
    });
  });

  describe('Rate limit retry', () => {
    beforeEach(() => setup([ENTRY_UNRESOLVED], 'unresolved'));

    it('isRateLimitLoading is false initially', () => {
      expect(component.isRateLimitLoading(ENTRY_UNRESOLVED.playerId)).toBeFalse();
    });

    it('isRateLimitLoading becomes true when onRetry callback is invoked', () => {
      const subject = new Subject<EpicIdSuggestion | null>();
      pipelineServiceSpy.getSuggestedEpicId.and.callFake(
        (_id: string, callbacks?: { onRetry?: () => void }) => {
          callbacks?.onRetry?.();
          return subject.asObservable();
        }
      );

      component.onSuggest(ENTRY_UNRESOLVED);
      fixture.detectChanges();

      expect(component.isRateLimitLoading(ENTRY_UNRESOLVED.playerId)).toBeTrue();
    });

    it('shows rate-limit-hint span when isRateLimitLoading is true', () => {
      const subject = new Subject<EpicIdSuggestion | null>();
      pipelineServiceSpy.getSuggestedEpicId.and.callFake(
        (_id: string, callbacks?: { onRetry?: () => void }) => {
          callbacks?.onRetry?.();
          return subject.asObservable();
        }
      );

      component.onSuggest(ENTRY_UNRESOLVED);
      fixture.detectChanges();

      const hint = fixture.debugElement.query(By.css('.rate-limit-hint'));
      expect(hint).toBeTruthy();
      expect(hint.nativeElement.textContent).toContain('Rate limit');
    });

    it('isRateLimitLoading resets to false after suggest resolves', () => {
      const subject = new Subject<EpicIdSuggestion | null>();
      pipelineServiceSpy.getSuggestedEpicId.and.callFake(
        (_id: string, callbacks?: { onRetry?: () => void }) => {
          callbacks?.onRetry?.();
          return subject.asObservable();
        }
      );

      component.onSuggest(ENTRY_UNRESOLVED);
      subject.next(null);
      subject.complete();
      fixture.detectChanges();

      expect(component.isRateLimitLoading(ENTRY_UNRESOLVED.playerId)).toBeFalse();
      const hint = fixture.debugElement.query(By.css('.rate-limit-hint'));
      expect(hint).toBeNull();
    });

    it('suggestLoading stays true while subject is pending (retrying)', () => {
      const subject = new Subject<EpicIdSuggestion | null>();
      pipelineServiceSpy.getSuggestedEpicId.and.callFake(
        (_id: string, callbacks?: { onRetry?: () => void }) => {
          callbacks?.onRetry?.();
          return subject.asObservable();
        }
      );

      component.onSuggest(ENTRY_UNRESOLVED);
      fixture.detectChanges();

      expect(component.isSuggestLoading(ENTRY_UNRESOLVED.playerId)).toBeTrue();
    });

    it('emits rateLimitExhausted when the service reports exhausted retries', () => {
      const exhaustedSpy = jasmine.createSpy('rateLimitExhausted');
      component.rateLimitExhausted.subscribe(exhaustedSpy);
      pipelineServiceSpy.getSuggestedEpicId.and.callFake(
        (_id: string, callbacks?: { onRateLimitExhausted?: () => void }) => {
          callbacks?.onRateLimitExhausted?.();
          return of(null);
        }
      );

      component.onSuggest(ENTRY_UNRESOLVED);

      expect(exhaustedSpy).toHaveBeenCalledTimes(1);
    });

    it('does not store a not-found sentinel after exhausted rate limit', () => {
      component.suggestions.delete(ENTRY_UNRESOLVED.playerId);
      pipelineServiceSpy.getSuggestedEpicId.and.callFake(
        (_id: string, callbacks?: { onRateLimitExhausted?: () => void }) => {
          callbacks?.onRateLimitExhausted?.();
          return of(null);
        }
      );

      component.onSuggest(ENTRY_UNRESOLVED);

      expect(component.getSuggestion(ENTRY_UNRESOLVED.playerId)).toBeUndefined();
    });

    it('emits resolutionUnavailable and does not store a not-found sentinel after 503', () => {
      const unavailableSpy = jasmine.createSpy('resolutionUnavailable');
      component.resolutionUnavailable.subscribe(unavailableSpy);
      component.suggestions.delete(ENTRY_UNRESOLVED.playerId);
      pipelineServiceSpy.getSuggestedEpicId.and.callFake(
        (_id: string, callbacks?: { onResolutionUnavailable?: () => void }) => {
          callbacks?.onResolutionUnavailable?.();
          return of(null);
        }
      );

      component.onSuggest(ENTRY_UNRESOLVED);

      expect(unavailableSpy).toHaveBeenCalledTimes(1);
      expect(component.getSuggestion(ENTRY_UNRESOLVED.playerId)).toBeUndefined();
      expect(component.isSuggestLoading(ENTRY_UNRESOLVED.playerId)).toBeFalse();
      expect(component.isRateLimitLoading(ENTRY_UNRESOLVED.playerId)).toBeFalse();
    });

    it('resets suggest and rate-limit loading after exhausted rate limit', () => {
      pipelineServiceSpy.getSuggestedEpicId.and.callFake(
        (_id: string, callbacks?: { onRetry?: () => void; onRateLimitExhausted?: () => void }) => {
          callbacks?.onRetry?.();
          callbacks?.onRateLimitExhausted?.();
          return of(null);
        }
      );

      component.onSuggest(ENTRY_UNRESOLVED);

      expect(component.isSuggestLoading(ENTRY_UNRESOLVED.playerId)).toBeFalse();
      expect(component.isRateLimitLoading(ENTRY_UNRESOLVED.playerId)).toBeFalse();
    });
  });

  describe('Stale suggestion invalidation', () => {
    it('purges cached suggestion and auto-filled value when username changes for same playerId', () => {
      const suggestion: EpicIdSuggestion = {
        suggestedEpicId: 'first-epic',
        displayName: 'Bughaboo',
        confidenceScore: 80,
        found: true
      };
      const pending = new Subject<EpicIdSuggestion | null>();
      pipelineServiceSpy.getSuggestedEpicId.and.returnValues(of(suggestion), pending.asObservable());

      setup([ENTRY_UNRESOLVED], 'unresolved');
      expect(component.getControl('p1').value).toBe('first-epic');

      fixture.componentRef.setInput('entries', [
        { ...ENTRY_UNRESOLVED, playerUsername: 'BughabooFixed' }
      ]);
      fixture.detectChanges();

      expect(component.getSuggestion('p1')).toBeUndefined();
      expect(component.getControl('p1').value).toBe('');
      expect(pipelineServiceSpy.getSuggestedEpicId).toHaveBeenCalledTimes(2);
    });

    it('keeps dirty admin input when region changes for same playerId', () => {
      const suggestion: EpicIdSuggestion = {
        suggestedEpicId: 'first-epic',
        displayName: 'Bughaboo',
        confidenceScore: 80,
        found: true
      };
      const pending = new Subject<EpicIdSuggestion | null>();
      pipelineServiceSpy.getSuggestedEpicId.and.returnValues(of(suggestion), pending.asObservable());

      setup([ENTRY_UNRESOLVED], 'unresolved');
      const control = component.getControl('p1');
      control.setValue('manual-admin-value');
      control.markAsDirty();

      fixture.componentRef.setInput('entries', [{ ...ENTRY_UNRESOLVED, playerRegion: 'NAW' }]);
      fixture.detectChanges();

      expect(component.getSuggestion('p1')).toBeUndefined();
      expect(control.value).toBe('manual-admin-value');
      expect(pipelineServiceSpy.getSuggestedEpicId).toHaveBeenCalledTimes(2);
    });

    it('ignores stale async suggestion response after username changes for same playerId', () => {
      const staleRequest = new Subject<EpicIdSuggestion | null>();
      const currentRequest = new Subject<EpicIdSuggestion | null>();
      const staleSuggestion: EpicIdSuggestion = {
        suggestedEpicId: 'stale-epic',
        displayName: 'Bughaboo',
        confidenceScore: 80,
        found: true
      };
      const currentSuggestion: EpicIdSuggestion = {
        suggestedEpicId: 'current-epic',
        displayName: 'BughabooFixed',
        confidenceScore: 90,
        found: true
      };
      pipelineServiceSpy.getSuggestedEpicId.and.returnValues(
        staleRequest.asObservable(),
        currentRequest.asObservable()
      );

      setup([ENTRY_UNRESOLVED], 'unresolved');

      fixture.componentRef.setInput('entries', [
        { ...ENTRY_UNRESOLVED, playerUsername: 'BughabooFixed' }
      ]);
      fixture.detectChanges();

      staleRequest.next(staleSuggestion);
      staleRequest.complete();
      fixture.detectChanges();

      expect(component.getSuggestion('p1')).toBeUndefined();
      expect(component.getControl('p1').value).toBe('');
      expect(component.isSuggestLoading('p1')).toBeTrue();

      currentRequest.next(currentSuggestion);
      currentRequest.complete();
      fixture.detectChanges();

      expect(component.getSuggestion('p1')).toEqual(currentSuggestion);
      expect(component.getControl('p1').value).toBe('current-epic');
      expect(component.isSuggestLoading('p1')).toBeFalse();
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
