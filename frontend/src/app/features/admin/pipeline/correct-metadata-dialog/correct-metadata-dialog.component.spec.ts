import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';
import {
  CorrectMetadataDialogComponent,
  CorrectMetadataDialogData
} from './correct-metadata-dialog.component';
import { PlayerIdentityEntry } from '../../models/admin.models';
import { PipelineService } from '../../services/pipeline.service';

const ENTRY: PlayerIdentityEntry = {
  id: 'e1',
  playerId: 'p1',
  playerUsername: 'Bughaboo',
  playerRegion: 'EU',
  epicId: null,
  status: 'UNRESOLVED',
  confidenceScore: null,
  resolvedBy: null,
  resolvedAt: null,
  rejectedAt: null,
  rejectionReason: null,
  createdAt: '2026-01-01T00:00:00',
  correctedUsername: null,
  correctedRegion: null,
  correctedBy: null,
  correctedAt: null
};

const ENTRY_WITH_CORRECTIONS: PlayerIdentityEntry = {
  ...ENTRY,
  correctedUsername: 'BughabooFixed',
  correctedRegion: 'NAW'
};

const REGIONS = ['UNKNOWN', 'EU', 'NAW', 'BR', 'ASIA', 'OCE', 'NAC', 'ME', 'NA'];

describe('CorrectMetadataDialogComponent', () => {
  let fixture: ComponentFixture<CorrectMetadataDialogComponent>;
  let component: CorrectMetadataDialogComponent;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CorrectMetadataDialogComponent>>;
  let pipelineSpy: jasmine.SpyObj<PipelineService>;

  async function setupComponent(entry: PlayerIdentityEntry = ENTRY, regions = REGIONS): Promise<void> {
    dialogRef = jasmine.createSpyObj<MatDialogRef<CorrectMetadataDialogComponent>>('MatDialogRef', [
      'close'
    ]);
    pipelineSpy = jasmine.createSpyObj<PipelineService>('PipelineService', [
      'getAvailableRegions'
    ]);
    pipelineSpy.getAvailableRegions.and.returnValue(of(regions));

    const dialogData: CorrectMetadataDialogData = { entry };

    await TestBed.configureTestingModule({
      imports: [CorrectMetadataDialogComponent, NoopAnimationsModule]
    })
      .overrideProvider(MAT_DIALOG_DATA, { useValue: dialogData })
      .overrideProvider(MatDialogRef, { useValue: dialogRef })
      .overrideProvider(PipelineService, { useValue: pipelineSpy })
      .compileComponents();

    fixture = TestBed.createComponent(CorrectMetadataDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  describe('Region loading', () => {
    it('loads available regions from PipelineService on init', async () => {
      await setupComponent();
      expect(pipelineSpy.getAvailableRegions).toHaveBeenCalledTimes(1);
      expect(component.availableRegions()).toEqual(REGIONS);
    });

    it('availableRegions is empty when service returns empty', async () => {
      await setupComponent(ENTRY, []);
      expect(component.availableRegions()).toEqual([]);
    });
  });

  describe('Form initialization', () => {
    it('initializes form with player username and region when no existing correction', async () => {
      await setupComponent();
      expect(component.form.get('newUsername')?.value).toBe('Bughaboo');
      expect(component.form.get('newRegion')?.value).toBe('EU');
    });

    it('pre-populates form with existing corrected values', async () => {
      await setupComponent(ENTRY_WITH_CORRECTIONS);
      expect(component.form.get('newUsername')?.value).toBe('BughabooFixed');
      expect(component.form.get('newRegion')?.value).toBe('NAW');
    });
  });

  describe('submit', () => {
    it('closes dialog with trimmed form values', async () => {
      await setupComponent();
      component.form.setValue({ newUsername: '  NewName  ', newRegion: 'NAW' });
      component.submit();
      expect(dialogRef.close).toHaveBeenCalledWith({ newUsername: 'NewName', newRegion: 'NAW' });
    });

    it('excludes blank newUsername from result', async () => {
      await setupComponent();
      component.form.setValue({ newUsername: '   ', newRegion: 'EU' });
      component.submit();
      expect(dialogRef.close).toHaveBeenCalledWith({ newRegion: 'EU' });
    });

    it('returns empty object when both fields are blank', async () => {
      await setupComponent();
      component.form.setValue({ newUsername: '', newRegion: '' });
      component.submit();
      expect(dialogRef.close).toHaveBeenCalledWith({});
    });

    it('does not close when form is invalid', async () => {
      await setupComponent();
      component.form.get('newUsername')?.setValue('a'.repeat(51));
      component.submit();
      expect(dialogRef.close).not.toHaveBeenCalled();
    });
  });

  describe('cancel', () => {
    it('closes dialog without result', async () => {
      await setupComponent();
      component.cancel();
      expect(dialogRef.close).toHaveBeenCalledWith();
    });
  });

  describe('validation', () => {
    it('form is invalid when newUsername exceeds 50 characters', async () => {
      await setupComponent();
      component.form.get('newUsername')?.setValue('a'.repeat(51));
      expect(component.form.invalid).toBeTrue();
    });

    it('form is valid with exactly 50 characters for newUsername', async () => {
      await setupComponent();
      component.form.get('newUsername')?.setValue('a'.repeat(50));
      expect(component.form.valid).toBeTrue();
    });
  });
});
