import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { EditProfileDialogComponent } from './edit-profile-dialog.component';
import { TranslationService } from '../../../core/services/translation.service';
import { UiErrorFeedbackService } from '../../../core/services/ui-error-feedback.service';
import { UserProfile } from '../../../core/services/user-context.service';

describe('EditProfileDialogComponent', () => {
  let component: EditProfileDialogComponent;
  let fixture: ComponentFixture<EditProfileDialogComponent>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<EditProfileDialogComponent>>;
  let uiFeedback: jasmine.SpyObj<UiErrorFeedbackService>;
  let translationService: jasmine.SpyObj<TranslationService>;

  const user: UserProfile = {
    id: 'user-1',
    username: 'PlayerOne',
    email: 'player@game.test',
    role: 'Player'
  };

  beforeEach(async () => {
    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);
    uiFeedback = jasmine.createSpyObj('UiErrorFeedbackService', ['showSuccessFromKey', 'showError']);
    translationService = jasmine.createSpyObj('TranslationService', ['t', 'translate']);
    translationService.t.and.callFake((key: string) => key);
    translationService.translate.and.callFake((key: string) => key);

    TestBed.configureTestingModule({
      imports: [EditProfileDialogComponent]
    });
    TestBed.overrideComponent(EditProfileDialogComponent, {
      set: {
        providers: [
          { provide: MatDialogRef, useValue: dialogRef },
          { provide: UiErrorFeedbackService, useValue: uiFeedback },
          { provide: MAT_DIALOG_DATA, useValue: { user } },
          { provide: TranslationService, useValue: translationService }
        ]
      }
    });

    await TestBed.compileComponents();

    fixture = TestBed.createComponent(EditProfileDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(component.profileForm.value.username).toBe(user.username);
    expect(component.profileForm.value.email).toBe(user.email);
  });

  it('shows form invalid message when saving invalid form', () => {
    component.profileForm.get('username')?.setValue('');

    component.onSave();

    expect(uiFeedback.showError).toHaveBeenCalledWith(
      null,
      'profile.editDialog.formInvalid',
      { duration: 3000 }
    );
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('saves when form is valid', fakeAsync(() => {
    component.profileForm.get('username')?.setValue('NewName');
    component.profileForm.get('email')?.setValue('new@game.test');

    component.onSave();

    expect(component.saving).toBeTrue();
    tick(1000);

    expect(uiFeedback.showSuccessFromKey).toHaveBeenCalledWith('profile.editDialog.success', 3000);
    expect(component.saving).toBeFalse();
    expect(dialogRef.close).toHaveBeenCalledWith({
      ...user,
      username: 'NewName',
      email: 'new@game.test'
    });
  }));

  it('closes dialog without payload when cancel is clicked', () => {
    component.onCancel();

    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('returns username validation errors', () => {
    component.profileForm.get('username')?.setValue('');
    expect(component.usernameError).toBe('profile.editDialog.errors.usernameRequired');

    component.profileForm.get('username')?.setValue('ab');
    expect(component.usernameError).toBe('profile.editDialog.errors.usernameMinLength');
  });

  it('returns email validation errors', () => {
    component.profileForm.get('email')?.setValue('');
    expect(component.emailError).toBe('profile.editDialog.errors.emailRequired');

    component.profileForm.get('email')?.setValue('not-an-email');
    expect(component.emailError).toBe('profile.editDialog.errors.emailInvalid');
  });
});
