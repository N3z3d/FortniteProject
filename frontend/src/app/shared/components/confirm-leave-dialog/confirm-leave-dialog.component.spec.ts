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

  it('should call dialogRef.close(false) when "Rester" button is clicked', () => {
    const buttons = fixture.nativeElement.querySelectorAll('button');
    const stayBtn = Array.from(buttons as NodeListOf<HTMLButtonElement>).find(
      (b: HTMLButtonElement) => b.textContent?.includes('draft.leaveConfirm.stay')
    );
    expect(stayBtn).toBeTruthy();
    stayBtn!.click();
    fixture.detectChanges();
    expect(mockDialogRef.close).toHaveBeenCalledWith(false);
  });

  it('should call dialogRef.close(true) when "Quitter" button is clicked', () => {
    const buttons = fixture.nativeElement.querySelectorAll('button');
    const leaveBtn = Array.from(buttons as NodeListOf<HTMLButtonElement>).find(
      (b: HTMLButtonElement) => b.textContent?.includes('draft.leaveConfirm.leave')
    );
    expect(leaveBtn).toBeTruthy();
    leaveBtn!.click();
    fixture.detectChanges();
    expect(mockDialogRef.close).toHaveBeenCalledWith(true);
  });
});
