import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';

import { TranslationService } from '../../../core/services/translation.service';

@Component({
  selector: 'app-confirm-leave-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ t.t('draft.leaveConfirm.title') }}</h2>
    <mat-dialog-content>{{ t.t('draft.leaveConfirm.message') }}</mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false">{{ t.t('draft.leaveConfirm.stay') }}</button>
      <button mat-raised-button color="warn" [mat-dialog-close]="true">
        {{ t.t('draft.leaveConfirm.leave') }}
      </button>
    </mat-dialog-actions>
  `,
})
export class ConfirmLeaveDialogComponent {
  public readonly t = inject(TranslationService);
}
