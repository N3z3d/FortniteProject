import { Component, inject, OnInit, ChangeDetectionStrategy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { PlayerIdentityEntry, CorrectMetadataRequest } from '../../models/admin.models';
import { PipelineService } from '../../services/pipeline.service';

export interface CorrectMetadataDialogData {
  entry: PlayerIdentityEntry;
}

@Component({
  selector: 'app-correct-metadata-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule
  ],
  templateUrl: './correct-metadata-dialog.component.html'
})
export class CorrectMetadataDialogComponent implements OnInit {
  private readonly dialogRef = inject(MatDialogRef<CorrectMetadataDialogComponent>);
  private readonly pipelineService = inject(PipelineService);
  readonly data: CorrectMetadataDialogData = inject(MAT_DIALOG_DATA);

  readonly availableRegions = signal<string[]>([]);

  readonly form: FormGroup = inject(FormBuilder).group({
    newUsername: [
      this.data.entry.correctedUsername ?? this.data.entry.playerUsername ?? null,
      [Validators.maxLength(50)]
    ],
    newRegion: [this.data.entry.correctedRegion ?? this.data.entry.playerRegion ?? null]
  });

  ngOnInit(): void {
    this.pipelineService.getAvailableRegions().subscribe(regions => {
      this.availableRegions.set(regions);
    });
  }

  submit(): void {
    if (this.form.valid) {
      const result: CorrectMetadataRequest = {};
      const { newUsername, newRegion } = this.form.value;
      if (newUsername?.trim()) result.newUsername = newUsername.trim();
      if (newRegion?.trim()) result.newRegion = newRegion.trim();
      this.dialogRef.close(result);
    }
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
