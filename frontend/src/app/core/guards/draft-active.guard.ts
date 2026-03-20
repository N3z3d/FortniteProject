import { CanDeactivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { map } from 'rxjs/operators';

import { ConfirmLeaveDialogComponent } from '../../shared/components/confirm-leave-dialog/confirm-leave-dialog.component';

export interface ComponentWithDraftState {
  isDraftActive(): boolean;
}

export const canDeactivateDraftGuard: CanDeactivateFn<ComponentWithDraftState> = component => {
  if (!component.isDraftActive()) return true;

  const dialog = inject(MatDialog);
  const dialogRef = dialog.open(ConfirmLeaveDialogComponent, {
    width: '400px',
    disableClose: true,
  });
  return dialogRef.afterClosed().pipe(map(result => result === true));
};
