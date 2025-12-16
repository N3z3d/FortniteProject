import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

@Component({
  selector: 'app-change-password-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatSnackBarModule
  ],
  templateUrl: './change-password-dialog.component.html',
  styleUrls: ['./change-password-dialog.component.scss']
})
export class ChangePasswordDialogComponent {
  passwordForm: FormGroup;
  saving = false;
  hideCurrentPassword = true;
  hideNewPassword = true;
  hideConfirmPassword = true;

  constructor(
    private readonly dialogRef: MatDialogRef<ChangePasswordDialogComponent>,
    private readonly fb: FormBuilder,
    private readonly snackBar: MatSnackBar
  ) {
    this.passwordForm = this.fb.group({
      currentPassword: ['', [Validators.required, Validators.minLength(6)]],
      newPassword: ['', [Validators.required, Validators.minLength(8), this.passwordStrengthValidator]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordMatchValidator });
  }

  passwordStrengthValidator(control: AbstractControl): ValidationErrors | null {
    const value = control.value;
    if (!value) return null;

    const hasUpperCase = /[A-Z]/.test(value);
    const hasLowerCase = /[a-z]/.test(value);
    const hasNumeric = /[0-9]/.test(value);

    const valid = hasUpperCase && hasLowerCase && hasNumeric;
    return valid ? null : { weakPassword: true };
  }

  passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
    const newPassword = group.get('newPassword')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;

    if (newPassword && confirmPassword && newPassword !== confirmPassword) {
      return { passwordMismatch: true };
    }
    return null;
  }

  onSave(): void {
    if (this.passwordForm.invalid) {
      this.snackBar.open('Veuillez corriger les erreurs du formulaire', 'Fermer', {
        duration: 3000
      });
      return;
    }

    this.saving = true;

    // Simuler un appel API
    setTimeout(() => {
      this.snackBar.open('Mot de passe modifié avec succès !', 'Fermer', {
        duration: 3000
      });
      this.saving = false;
      this.dialogRef.close(true);
    }, 1500);
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }

  get currentPasswordError(): string {
    const control = this.passwordForm.get('currentPassword');
    if (control?.hasError('required')) {
      return 'Le mot de passe actuel est requis';
    }
    if (control?.hasError('minlength')) {
      return 'Minimum 6 caractères';
    }
    return '';
  }

  get newPasswordError(): string {
    const control = this.passwordForm.get('newPassword');
    if (control?.hasError('required')) {
      return 'Le nouveau mot de passe est requis';
    }
    if (control?.hasError('minlength')) {
      return 'Minimum 8 caractères';
    }
    if (control?.hasError('weakPassword')) {
      return 'Doit contenir majuscule, minuscule et chiffre';
    }
    return '';
  }

  get confirmPasswordError(): string {
    const control = this.passwordForm.get('confirmPassword');
    if (control?.hasError('required')) {
      return 'Confirmation requise';
    }
    if (this.passwordForm.hasError('passwordMismatch')) {
      return 'Les mots de passe ne correspondent pas';
    }
    return '';
  }

  getPasswordStrength(): { label: string; class: string; percent: number } {
    const password = this.passwordForm.get('newPassword')?.value || '';

    if (!password) {
      return { label: '', class: '', percent: 0 };
    }

    let strength = 0;
    if (password.length >= 8) strength++;
    if (password.length >= 12) strength++;
    if (/[A-Z]/.test(password)) strength++;
    if (/[a-z]/.test(password)) strength++;
    if (/[0-9]/.test(password)) strength++;
    if (/[^A-Za-z0-9]/.test(password)) strength++;

    if (strength <= 2) {
      return { label: 'Faible', class: 'weak', percent: 33 };
    } else if (strength <= 4) {
      return { label: 'Moyen', class: 'medium', percent: 66 };
    } else {
      return { label: 'Fort', class: 'strong', percent: 100 };
    }
  }
}
