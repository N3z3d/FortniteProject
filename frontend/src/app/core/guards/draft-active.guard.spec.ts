import { TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';
import { ComponentWithDraftState, canDeactivateDraftGuard } from './draft-active.guard';
import { ConfirmLeaveDialogComponent } from '../../shared/components/confirm-leave-dialog/confirm-leave-dialog.component';

describe('canDeactivateDraftGuard', () => {
  let mockDialog: { open: ReturnType<typeof vi.fn> };

  const makeComponent = (active: boolean): ComponentWithDraftState => ({
    isDraftActive: () => active,
  });

  beforeEach(() => {
    mockDialog = { open: vi.fn() };

    TestBed.configureTestingModule({
      providers: [{ provide: MatDialog, useValue: mockDialog }],
    });
  });

  it('should return true immediately when draft is NOT active', () => {
    const component = makeComponent(false);
    const result = TestBed.runInInjectionContext(() =>
      canDeactivateDraftGuard(component, null as never, null as never, null as never)
    );
    expect(result).toBe(true);
    expect(mockDialog.open).not.toHaveBeenCalled();
  });

  it('should return false (via dialog) when draft IS active and user cancels', async () => {
    const component = makeComponent(true);
    const fakeDialogRef = {
      afterClosed: () => of(false),
    } as unknown as MatDialogRef<ConfirmLeaveDialogComponent>;
    mockDialog.open.mockReturnValue(fakeDialogRef);

    const result$ = TestBed.runInInjectionContext(() =>
      canDeactivateDraftGuard(component, null as never, null as never, null as never)
    ) as ReturnType<typeof of>;

    const value = await new Promise<boolean>(resolve => {
      (result$ as ReturnType<typeof of>).subscribe((v: boolean) => resolve(v));
    });

    expect(mockDialog.open).toHaveBeenCalledWith(ConfirmLeaveDialogComponent, {
      width: '400px',
      disableClose: true,
    });
    expect(value).toBe(false);
  });

  it('should return true (via dialog) when draft IS active and user confirms leave', async () => {
    const component = makeComponent(true);
    const fakeDialogRef = {
      afterClosed: () => of(true),
    } as unknown as MatDialogRef<ConfirmLeaveDialogComponent>;
    mockDialog.open.mockReturnValue(fakeDialogRef);

    const result$ = TestBed.runInInjectionContext(() =>
      canDeactivateDraftGuard(component, null as never, null as never, null as never)
    ) as ReturnType<typeof of>;

    const value = await new Promise<boolean>(resolve => {
      (result$ as ReturnType<typeof of>).subscribe((v: boolean) => resolve(v));
    });

    expect(value).toBe(true);
  });
});
