import { Component, Inject, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormControl, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';

import { TranslationService } from '../../../../core/services/translation.service';

export interface RenameGameDialogData {
  currentName: string;
}

@Component({
  selector: 'app-rename-game-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule
  ],
  templateUrl: './rename-game-dialog.component.html',
  styleUrls: ['./rename-game-dialog.component.scss']
})
export class RenameGameDialogComponent {
  public readonly t = inject(TranslationService);

  readonly minLength = 3;
  readonly maxLength = 50;

  readonly nameControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.minLength(this.minLength), Validators.maxLength(this.maxLength), this.notBlank]
  });

  constructor(
    private readonly dialogRef: MatDialogRef<RenameGameDialogComponent, string | undefined>,
    @Inject(MAT_DIALOG_DATA) readonly data: RenameGameDialogData
  ) {
    this.nameControl.setValue(data.currentName);
  }

  get isUnchanged(): boolean {
    return this.nameControl.value.trim() === this.data.currentName.trim();
  }

  cancel(): void {
    this.dialogRef.close(undefined);
  }

  confirm(): void {
    if (this.nameControl.invalid || this.isUnchanged) {
      this.nameControl.markAsTouched();
      return;
    }

    this.dialogRef.close(this.nameControl.value.trim());
  }

  private notBlank(control: AbstractControl): ValidationErrors | null {
    if (typeof control.value !== 'string') {
      return { blank: true };
    }

    return control.value.trim().length === 0 ? { blank: true } : null;
  }
}
