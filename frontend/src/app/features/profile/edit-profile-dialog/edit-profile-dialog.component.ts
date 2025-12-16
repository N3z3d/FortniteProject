import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { UserProfile } from '../../../core/services/user-context.service';

@Component({
  selector: 'app-edit-profile-dialog',
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
  templateUrl: './edit-profile-dialog.component.html',
  styleUrls: ['./edit-profile-dialog.component.scss']
})
export class EditProfileDialogComponent {
  profileForm: FormGroup;
  saving = false;

  constructor(
    private readonly dialogRef: MatDialogRef<EditProfileDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { user: UserProfile },
    private readonly fb: FormBuilder,
    private readonly snackBar: MatSnackBar
  ) {
    this.profileForm = this.fb.group({
      username: [data.user.username, [Validators.required, Validators.minLength(3)]],
      email: [data.user.email, [Validators.required, Validators.email]]
    });
  }

  onSave(): void {
    if (this.profileForm.invalid) {
      this.snackBar.open('Veuillez corriger les erreurs du formulaire', 'Fermer', {
        duration: 3000
      });
      return;
    }

    this.saving = true;

    // Simuler un appel API
    setTimeout(() => {
      const updatedUser: UserProfile = {
        ...this.data.user,
        username: this.profileForm.value.username,
        email: this.profileForm.value.email
      };

      this.snackBar.open('Profil mis à jour avec succès !', 'Fermer', {
        duration: 3000
      });

      this.saving = false;
      this.dialogRef.close(updatedUser);
    }, 1000);
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  get usernameError(): string {
    const control = this.profileForm.get('username');
    if (control?.hasError('required')) {
      return 'Le nom d\'utilisateur est requis';
    }
    if (control?.hasError('minlength')) {
      return 'Minimum 3 caractères';
    }
    return '';
  }

  get emailError(): string {
    const control = this.profileForm.get('email');
    if (control?.hasError('required')) {
      return 'L\'email est requis';
    }
    if (control?.hasError('email')) {
      return 'Email invalide';
    }
    return '';
  }
}
