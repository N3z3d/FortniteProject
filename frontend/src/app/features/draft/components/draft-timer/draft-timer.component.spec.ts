import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { SimpleChange } from '@angular/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { DraftTimerComponent } from './draft-timer.component';
import { TranslationService } from '../../../../core/services/translation.service';
import { GameParticipant } from '../../models/draft.interface';

class MockTranslationService {
  t(key: string): string {
    return key;
  }
}

function triggerDurationChange(
  component: DraftTimerComponent,
  seconds: number
): void {
  component.durationSeconds = seconds;
  component.ngOnChanges({
    durationSeconds: new SimpleChange(0, seconds, false),
  });
}

describe('DraftTimerComponent', () => {
  let component: DraftTimerComponent;
  let fixture: ComponentFixture<DraftTimerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DraftTimerComponent, NoopAnimationsModule],
      providers: [{ provide: TranslationService, useClass: MockTranslationService }],
    }).compileComponents();

    fixture = TestBed.createComponent(DraftTimerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    component.ngOnDestroy();
  });

  // ===== LEGACY COMPATIBILITY =====

  it('should render currentPlayer username in h2', () => {
    const player: GameParticipant = { id: '1', username: 'Tester', timeRemaining: 30 };
    component.currentPlayer = player;
    component.formattedTime = '00:30';
    fixture.detectChanges();

    const h2 = fixture.nativeElement.querySelector('h2');
    expect(h2.textContent).toContain('Tester');
  });

  it('should display formattedTime when durationSeconds is 0', () => {
    const player: GameParticipant = { id: '1', username: 'A', timeRemaining: 30 };
    component.currentPlayer = player;
    component.formattedTime = '00:30';
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.timer-digits').textContent).toContain('00:30');
  });

  // ===== TIMER STATES =====

  it('should be in warmup state when warmup input is true', fakeAsync(() => {
    component.warmup = true;
    triggerDurationChange(component, 60);
    tick(1000);
    fixture.detectChanges();

    expect(component.timerState).toBe('warmup');
    const wrapper = fixture.nativeElement.querySelector('.timer-wrapper');
    expect(wrapper.getAttribute('data-state')).toBe('warmup');
  }));

  it('should be in active state when secondsLeft > 15', fakeAsync(() => {
    triggerDurationChange(component, 60);
    tick(1000); // 59s remaining
    fixture.detectChanges();

    expect(component.timerState).toBe('active');
    expect(component.secondsLeft).toBe(59);
  }));

  it('should be in warning state when secondsLeft <= 15 and > 5', fakeAsync(() => {
    triggerDurationChange(component, 60);
    tick(46000); // 14s remaining
    fixture.detectChanges();

    expect(component.timerState).toBe('warning');
    expect(component.secondsLeft).toBe(14);
  }));

  it('should be in urgent state when secondsLeft <= 5', fakeAsync(() => {
    triggerDurationChange(component, 60);
    tick(56000); // 4s remaining
    fixture.detectChanges();

    expect(component.timerState).toBe('urgent');
    expect(component.secondsLeft).toBe(4);
  }));

  it('should emit expired and enter expired state when countdown reaches zero', fakeAsync(() => {
    const expiredSpy = jasmine.createSpy('expired');
    component.expired.subscribe(expiredSpy);

    triggerDurationChange(component, 10);
    tick(10000);
    fixture.detectChanges();

    expect(expiredSpy).toHaveBeenCalledTimes(1);
    expect(component.timerState).toBe('expired');
  }));

  // ===== AUTOPICK TOAST =====

  it('should show autopick toast after expiry when autoPickPlayer is set', fakeAsync(() => {
    component.autoPickPlayer = 'Sniper99';
    triggerDurationChange(component, 5);
    tick(5000);
    fixture.detectChanges();

    expect(component.showAutopickToast).toBe(true);
    const toast = fixture.nativeElement.querySelector('.autopick-toast');
    expect(toast).not.toBeNull();
    expect(toast.textContent).toContain('Sniper99');
  }));

  it('should not show autopick toast when autoPickPlayer is null', fakeAsync(() => {
    component.autoPickPlayer = null;
    triggerDurationChange(component, 5);
    tick(5000);
    fixture.detectChanges();

    expect(component.showAutopickToast).toBe(false);
  }));

  it('should hide toast automatically after 5s cancel window', fakeAsync(() => {
    component.autoPickPlayer = 'Sniper99';
    triggerDurationChange(component, 5);
    tick(5000); // timer expires + toast appears
    fixture.detectChanges();

    expect(component.showAutopickToast).toBe(true);

    tick(5000); // cancel window elapses
    fixture.detectChanges();

    expect(component.showAutopickToast).toBe(false);
  }));

  it('should emit cancelled and hide toast when cancel button is clicked', fakeAsync(() => {
    const cancelledSpy = jasmine.createSpy('cancelled');
    component.cancelled.subscribe(cancelledSpy);
    component.autoPickPlayer = 'Sniper99';

    triggerDurationChange(component, 5);
    tick(5000);
    fixture.detectChanges();

    component.cancelAutopick();
    fixture.detectChanges();

    expect(cancelledSpy).toHaveBeenCalledTimes(1);
    expect(component.showAutopickToast).toBe(false);
  }));

  // ===== ARIA =====

  it('should update aria-label with secondsLeft during countdown', fakeAsync(() => {
    triggerDurationChange(component, 30);
    tick(1000);
    fixture.detectChanges();

    const wrapper = fixture.nativeElement.querySelector('.timer-wrapper');
    expect(wrapper.getAttribute('aria-label')).toBe('29 secondes restantes');
  }));

  it('should set criticalAnnouncement at 15s warning threshold', fakeAsync(() => {
    triggerDurationChange(component, 20);
    tick(5000); // 15s remaining
    fixture.detectChanges();

    expect(component.criticalAnnouncement).toContain('15');
  }));

  it('should set criticalAnnouncement at 5s urgent threshold', fakeAsync(() => {
    triggerDurationChange(component, 10);
    tick(5000); // 5s remaining
    fixture.detectChanges();

    expect(component.criticalAnnouncement).toContain('5');
  }));

  it('should have aria-live="off" on the countdown display element', () => {
    const player: GameParticipant = { id: '1', username: 'A', timeRemaining: 30 };
    component.currentPlayer = player;
    component.formattedTime = '00:30';
    fixture.detectChanges();

    const display = fixture.nativeElement.querySelector('.timer-display');
    expect(display?.getAttribute('aria-live')).toBe('off');
  });

  // ===== CLEANUP =====

  it('should cancel active subscriptions on destroy', fakeAsync(() => {
    triggerDurationChange(component, 60);
    tick(1000);

    component.ngOnDestroy();

    const secondsBefore = component.secondsLeft;
    tick(5000);
    // secondsLeft should not change after destroy
    expect(component.secondsLeft).toBe(secondsBefore);
  }));

  it('should reset countdown when durationSeconds changes', fakeAsync(() => {
    triggerDurationChange(component, 30);
    tick(10000); // 20s remaining

    triggerDurationChange(component, 60); // reset with new duration
    fixture.detectChanges();

    expect(component.secondsLeft).toBe(60);
  }));
});
