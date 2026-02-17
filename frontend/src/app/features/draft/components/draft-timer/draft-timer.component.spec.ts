import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DraftTimerComponent } from './draft-timer.component';
import { TranslationService } from '../../../../core/services/translation.service';
import { GameParticipant } from '../../models/draft.interface';

class MockTranslationService {
  t(key: string): string {
    return key;
  }
}

describe('DraftTimerComponent', () => {
  let component: DraftTimerComponent;
  let fixture: ComponentFixture<DraftTimerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DraftTimerComponent],
      providers: [{ provide: TranslationService, useClass: MockTranslationService }]
    }).compileComponents();

    fixture = TestBed.createComponent(DraftTimerComponent);
    component = fixture.componentInstance;
  });

  it('renders current player username', () => {
    const player: GameParticipant = {
      id: '1',
      username: 'Tester',
      timeRemaining: 30
    };

    component.currentPlayer = player;
    component.formattedTime = '00:30';
    fixture.detectChanges();

    const title = fixture.nativeElement.querySelector('h2');
    expect(title.textContent).toContain('Tester');
  });

  it('shows timer only when timeRemaining exists', () => {
    const playerWithTime: GameParticipant = {
      id: '1',
      username: 'Tester',
      timeRemaining: 30
    };

    component.currentPlayer = playerWithTime;
    component.formattedTime = '00:30';
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.timer-value').textContent).toContain('00:30');

    component.currentPlayer = { id: '2', username: 'NoTimer' };
    component.formattedTime = null;
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.timer-value')).toBeNull();
  });
});
