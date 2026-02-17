import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { RenameGameDialogComponent } from './rename-game-dialog.component';
import { TranslationService } from '../../../../core/services/translation.service';

describe('RenameGameDialogComponent', () => {
  let component: RenameGameDialogComponent;
  let fixture: ComponentFixture<RenameGameDialogComponent>;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<RenameGameDialogComponent>>;

  beforeEach(async () => {
    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      imports: [RenameGameDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: { currentName: 'My Game' } },
        {
          provide: TranslationService,
          useValue: { t: (key: string) => key }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RenameGameDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with current game name', () => {
    expect(component.nameControl.value).toBe('My Game');
    expect(component.isUnchanged).toBeTrue();
  });

  it('should reject blank names with explicit validation', () => {
    component.nameControl.setValue('   ');
    component.confirm();

    expect(component.nameControl.hasError('blank')).toBeTrue();
    expect(dialogRefSpy.close).not.toHaveBeenCalled();
  });

  it('should reject unchanged names', () => {
    component.nameControl.setValue('My Game');
    component.confirm();

    expect(component.isUnchanged).toBeTrue();
    expect(dialogRefSpy.close).not.toHaveBeenCalled();
  });

  it('should close with trimmed name when valid', () => {
    component.nameControl.setValue('  New Name  ');
    component.confirm();

    expect(dialogRefSpy.close).toHaveBeenCalledWith('New Name');
  });
});
