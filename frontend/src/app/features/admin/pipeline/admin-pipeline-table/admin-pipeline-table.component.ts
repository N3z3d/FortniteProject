import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnChanges,
  SimpleChanges,
  ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl, Validators } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatInputModule } from '@angular/material/input';
import { MatIconButton } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatBadgeModule } from '@angular/material/badge';
import { PlayerIdentityEntry } from '../../models/admin.models';

export interface ResolvedEvent {
  playerId: string;
  epicId: string;
}

export interface RejectedEvent {
  playerId: string;
  reason?: string;
}

@Component({
  selector: 'app-admin-pipeline-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTableModule,
    MatInputModule,
    MatIconButton,
    MatIconModule,
    MatTooltipModule,
    MatBadgeModule
  ],
  templateUrl: './admin-pipeline-table.component.html',
  styleUrls: ['./admin-pipeline-table.component.scss']
})
export class AdminPipelineTableComponent implements OnChanges {
  @Input() entries: PlayerIdentityEntry[] = [];
  @Input() mode: 'unresolved' | 'resolved' = 'unresolved';
  @Output() resolved = new EventEmitter<ResolvedEvent>();
  @Output() rejected = new EventEmitter<RejectedEvent>();

  readonly unresolvedColumns = ['playerUsername', 'playerRegion', 'epicId', 'actions'];
  readonly resolvedColumns = [
    'playerUsername',
    'playerRegion',
    'epicId',
    'confidenceScore',
    'resolvedBy',
    'status'
  ];

  epicIdControls: Map<string, FormControl<string>> = new Map();

  get displayedColumns(): string[] {
    return this.mode === 'unresolved' ? this.unresolvedColumns : this.resolvedColumns;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['entries'] && this.mode === 'unresolved') {
      this.rebuildControls();
    }
  }

  onConfirm(entry: PlayerIdentityEntry): void {
    const control = this.epicIdControls.get(entry.playerId);
    if (!control || control.invalid) return;
    this.resolved.emit({ playerId: entry.playerId, epicId: control.value.trim() });
  }

  onReject(entry: PlayerIdentityEntry): void {
    this.rejected.emit({ playerId: entry.playerId });
  }

  onKeydown(event: KeyboardEvent, entry: PlayerIdentityEntry): void {
    if (event.key === 'Enter') {
      event.preventDefault();
      this.onConfirm(entry);
    } else if (event.key === 'Escape') {
      this.epicIdControls.get(entry.playerId)?.reset('');
    }
  }

  isConfirmDisabled(entry: PlayerIdentityEntry): boolean {
    return this.epicIdControls.get(entry.playerId)?.invalid ?? true;
  }

  getControl(playerId: string): FormControl<string> {
    return this.epicIdControls.get(playerId) ?? new FormControl('', { nonNullable: true });
  }

  statusClass(status: string): string {
    return `status--${status.toLowerCase()}`;
  }

  scoreClass(score: number | null): string {
    if (score === null) return 'score--unknown';
    if (score >= 80) return 'score--high';
    if (score >= 50) return 'score--medium';
    return 'score--low';
  }

  private rebuildControls(): void {
    const existingIds = new Set(this.epicIdControls.keys());
    const currentIds = new Set(this.entries.map(e => e.playerId));

    // Remove stale controls
    existingIds.forEach(id => {
      if (!currentIds.has(id)) this.epicIdControls.delete(id);
    });

    // Add missing controls
    this.entries.forEach(entry => {
      if (!this.epicIdControls.has(entry.playerId)) {
        this.epicIdControls.set(
          entry.playerId,
          new FormControl('', { nonNullable: true, validators: [Validators.required] })
        );
      }
    });
  }
}
