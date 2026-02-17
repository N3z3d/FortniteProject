import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import {
  InvitationCodeDurationDialogComponent,
  InvitationCodeDurationDialogData
} from './invitation-code-duration-dialog.component';
import { TranslationService } from '../../../../core/services/translation.service';

describe('InvitationCodeDurationDialogComponent', () => {
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<InvitationCodeDurationDialogComponent>>;
  let dialogData: InvitationCodeDurationDialogData;

  const translationMap: Record<string, string> = {
    'games.detail.regenerateDialog.title': 'Regenerate invitation code',
    'games.detail.regenerateDialog.subtitle': 'Choose duration',
    'games.detail.regenerateDialog.confirm': 'Regenerate',
    'games.detail.regenerateDialog.cancel': 'Cancel'
  };

  const translationStub = {
    t: (key: string, fallback?: string) => translationMap[key] ?? fallback ?? key
  };

  beforeEach(async () => {
    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);
    dialogData = { defaultDuration: '48h' };

    await TestBed.configureTestingModule({
      imports: [InvitationCodeDurationDialogComponent],
      providers: [
        {
          provide: MAT_DIALOG_DATA,
          useFactory: () => dialogData
        },
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: TranslationService, useValue: translationStub }
      ]
    }).compileComponents();
  });

  it('initializes with provided default duration', () => {
    const fixture = TestBed.createComponent(InvitationCodeDurationDialogComponent);
    const component = fixture.componentInstance;

    expect(component.selectedDuration).toBe('48h');
  });

  it('updates selection when user selects another duration', () => {
    const fixture = TestBed.createComponent(InvitationCodeDurationDialogComponent);
    const component = fixture.componentInstance;

    component.selectDuration('7d');

    expect(component.selectedDuration).toBe('7d');
  });

  it('closes dialog with selected duration on confirm', () => {
    const fixture = TestBed.createComponent(InvitationCodeDurationDialogComponent);
    const component = fixture.componentInstance;
    component.selectedDuration = '24h';

    component.onConfirm();

    expect(dialogRefSpy.close).toHaveBeenCalledWith('24h');
  });

  it('closes dialog without value on cancel', () => {
    const fixture = TestBed.createComponent(InvitationCodeDurationDialogComponent);
    const component = fixture.componentInstance;

    component.onCancel();

    expect(dialogRefSpy.close).toHaveBeenCalledWith(undefined);
  });

  it('falls back to regenerate wording when generate keys are missing', () => {
    dialogData = { defaultDuration: '24h', mode: 'generate' };
    const fixture = TestBed.createComponent(InvitationCodeDurationDialogComponent);
    const component = fixture.componentInstance;

    expect(component.getTitleText()).toBe('Regenerate invitation code');
    expect(component.getSubtitleText()).toBe('Choose duration');
    expect(component.getConfirmText()).toBe('Regenerate');
  });

  describe('DOM rendering', () => {
    it('renders a radiogroup with 4 duration options', () => {
      const fixture = TestBed.createComponent(InvitationCodeDurationDialogComponent);
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;

      const radioGroup = el.querySelector('[role="radiogroup"]');
      expect(radioGroup).toBeTruthy();

      const cards = el.querySelectorAll('.duration-card');
      expect(cards.length).toBe(4);
    });

    it('marks selected card with aria-checked=true', () => {
      const fixture = TestBed.createComponent(InvitationCodeDurationDialogComponent);
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;

      const cards = el.querySelectorAll('.duration-card');
      const checkedCards = Array.from(cards).filter(
        c => c.getAttribute('aria-checked') === 'true'
      );
      expect(checkedCards.length).toBe(1);
    });

    it('updates aria-checked when a different card is clicked', () => {
      const fixture = TestBed.createComponent(InvitationCodeDurationDialogComponent);
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;

      const firstCard = el.querySelector('.duration-card') as HTMLButtonElement;
      firstCard.click();
      fixture.detectChanges();

      expect(firstCard.getAttribute('aria-checked')).toBe('true');
    });

    it('renders header icon and title', () => {
      const fixture = TestBed.createComponent(InvitationCodeDurationDialogComponent);
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;

      const icon = el.querySelector('.header-icon');
      expect(icon).toBeTruthy();
      expect(icon!.getAttribute('aria-hidden')).toBe('true');

      const title = el.querySelector('.dialog-title');
      expect(title).toBeTruthy();
      expect(title!.textContent!.trim()).toBe('Regenerate invitation code');
    });

    it('renders cancel and confirm buttons', () => {
      const fixture = TestBed.createComponent(InvitationCodeDurationDialogComponent);
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;

      const cancelBtn = el.querySelector('.cancel-btn');
      const confirmBtn = el.querySelector('.confirm-btn');
      expect(cancelBtn).toBeTruthy();
      expect(confirmBtn).toBeTruthy();
    });

    it('shows selected class only on the active card', () => {
      const fixture = TestBed.createComponent(InvitationCodeDurationDialogComponent);
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;

      const selectedCards = el.querySelectorAll('.duration-card.selected');
      expect(selectedCards.length).toBe(1);
    });
  });
});
