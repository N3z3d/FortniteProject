import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { FormControl } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { ChangePasswordDialogComponent } from './change-password-dialog.component';
import { TranslationService } from '../../../core/services/translation.service';
import { UiErrorFeedbackService } from '../../../core/services/ui-error-feedback.service';

describe('ChangePasswordDialogComponent', () => {
  let component: ChangePasswordDialogComponent;
  let fixture: ComponentFixture<ChangePasswordDialogComponent>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ChangePasswordDialogComponent>>;
  let uiFeedback: jasmine.SpyObj<UiErrorFeedbackService>;
  let translationService: jasmine.SpyObj<TranslationService>;

  beforeEach(async () => {
    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);
    uiFeedback = jasmine.createSpyObj('UiErrorFeedbackService', ['showSuccessFromKey', 'showError']);
    translationService = jasmine.createSpyObj('TranslationService', ['t', 'translate']);
    translationService.t.and.callFake((key: string) => key);
    translationService.translate.and.callFake((key: string) => key);

    TestBed.configureTestingModule({
      imports: [ChangePasswordDialogComponent]
    });
    TestBed.overrideComponent(ChangePasswordDialogComponent, {
      set: {
        providers: [
          { provide: MatDialogRef, useValue: dialogRef },
          { provide: UiErrorFeedbackService, useValue: uiFeedback },
          { provide: TranslationService, useValue: translationService }
        ]
      }
    });

    await TestBed.compileComponents();

    fixture = TestBed.createComponent(ChangePasswordDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('marks weak password as invalid', () => {
    const control = new FormControl('password');
    const result = component.passwordStrengthValidator(control);

    expect(result).toEqual({ weakPassword: true });
  });

  it('accepts strong password', () => {
    const control = new FormControl('StrongPass1');
    const result = component.passwordStrengthValidator(control);

    expect(result).toBeNull();
  });

  it('returns mismatch error when passwords differ', () => {
    component.passwordForm.get('newPassword')?.setValue('StrongPass1');
    component.passwordForm.get('confirmPassword')?.setValue('StrongPass2');

    expect(component.passwordForm.hasError('passwordMismatch')).toBeTrue();
    expect(component.confirmPasswordError).toBe('profile.changePasswordDialog.errors.passwordMismatch');
  });

  it('shows form invalid message when saving invalid form', () => {
    component.passwordForm.get('currentPassword')?.setValue('');

    component.onSave();

    expect(uiFeedback.showError).toHaveBeenCalledWith(
      null,
      'profile.changePasswordDialog.errors.formInvalid',
      { duration: 3000 }
    );
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('saves when form is valid', fakeAsync(() => {
    component.passwordForm.get('currentPassword')?.setValue('Oldpass1');
    component.passwordForm.get('newPassword')?.setValue('StrongPass1');
    component.passwordForm.get('confirmPassword')?.setValue('StrongPass1');

    component.onSave();

    expect(component.saving).toBeTrue();
    tick(1500);

    expect(uiFeedback.showSuccessFromKey).toHaveBeenCalledWith('profile.changePasswordDialog.success', 3000);
    expect(component.saving).toBeFalse();
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  }));

  it('closes dialog when cancel is clicked', () => {
    component.onCancel();

    expect(dialogRef.close).toHaveBeenCalledWith(false);
  });

  it('returns password strength labels', () => {
    component.passwordForm.get('newPassword')?.setValue('');
    expect(component.getPasswordStrength()).toEqual({ label: '', class: '', percent: 0 });

    component.passwordForm.get('newPassword')?.setValue('abc');
    expect(component.getPasswordStrength()).toEqual({
      label: 'profile.changePasswordDialog.strength.weak',
      class: 'weak',
      percent: 33
    });

    component.passwordForm.get('newPassword')?.setValue('Abcdef12');
    expect(component.getPasswordStrength()).toEqual({
      label: 'profile.changePasswordDialog.strength.medium',
      class: 'medium',
      percent: 66
    });

    component.passwordForm.get('newPassword')?.setValue('Abcdef12!345');
    expect(component.getPasswordStrength()).toEqual({
      label: 'profile.changePasswordDialog.strength.strong',
      class: 'strong',
      percent: 100
    });
  });
});
