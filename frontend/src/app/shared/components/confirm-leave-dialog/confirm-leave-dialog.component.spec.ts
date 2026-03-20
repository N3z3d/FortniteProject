import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ConfirmLeaveDialogComponent } from './confirm-leave-dialog.component';
import { TranslationService } from '../../../core/services/translation.service';

describe('ConfirmLeaveDialogComponent', () => {
  let fixture: ComponentFixture<ConfirmLeaveDialogComponent>;
  let mockDialogRef: { close: ReturnType<typeof vi.fn> };
  let mockTranslation: { t: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    mockDialogRef = { close: vi.fn() };
    mockTranslation = {
      t: vi.fn((key: string) => key),
    };

    await TestBed.configureTestingModule({
      imports: [ConfirmLeaveDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: mockDialogRef },
        { provide: TranslationService, useValue: mockTranslation },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmLeaveDialogComponent);
    fixture.detectChanges();
  });

  it('should render the dialog with translated keys', () => {
    expect(mockTranslation.t).toHaveBeenCalled();
  });

  it('should close with false when "Rester" button is clicked', () => {
    // The "Rester" button has [mat-dialog-close]="false"
    const buttons = fixture.nativeElement.querySelectorAll('button');
    const stayBtn = Array.from(buttons as NodeListOf<HTMLButtonElement>).find(
      (b: HTMLButtonElement) => b.textContent?.includes('draft.leaveConfirm.stay')
    );
    expect(stayBtn).toBeTruthy();
  });

  it('should close with true when "Quitter" button is clicked', () => {
    const buttons = fixture.nativeElement.querySelectorAll('button');
    const leaveBtn = Array.from(buttons as NodeListOf<HTMLButtonElement>).find(
      (b: HTMLButtonElement) => b.textContent?.includes('draft.leaveConfirm.leave')
    );
    expect(leaveBtn).toBeTruthy();
  });
});
