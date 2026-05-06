import {
  Component,
  ChangeDetectorRef,
  DestroyRef,
  Input,
  Output,
  EventEmitter,
  OnChanges,
  SimpleChanges,
  ChangeDetectionStrategy,
  inject
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { MatTableModule } from '@angular/material/table';
import { MatInputModule } from '@angular/material/input';
import { MatIconButton } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatBadgeModule } from '@angular/material/badge';
import { EpicIdSuggestion, PlayerIdentityEntry } from '../../models/admin.models';
import { PipelineService } from '../../services/pipeline.service';

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
  private readonly pipelineService = inject(PipelineService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly cdr = inject(ChangeDetectorRef);

  @Input() entries: PlayerIdentityEntry[] = [];
  @Input() mode: 'unresolved' | 'resolved' = 'unresolved';
  @Output() resolved = new EventEmitter<ResolvedEvent>();
  @Output() rejected = new EventEmitter<RejectedEvent>();
  @Output() correctRequested = new EventEmitter<PlayerIdentityEntry>();
  @Output() rateLimitExhausted = new EventEmitter<void>();
  @Output() resolutionUnavailable = new EventEmitter<void>();

  readonly unresolvedColumns = ['playerUsername', 'playerRegion', 'epicId', 'actions'];
  readonly resolvedColumns = [
    'playerUsername',
    'playerRegion',
    'epicId',
    'confidenceScore',
    'resolvedBy',
    'status',
    'actions'
  ];

  epicIdControls: Map<string, FormControl<string>> = new Map();
  suggestions: Map<string, EpicIdSuggestion> = new Map();
  suggestLoading: Map<string, boolean> = new Map();
  rateLimitLoading: Map<string, boolean> = new Map();
  private entrySignatures: Map<string, string> = new Map();

  get displayedColumns(): string[] {
    return this.mode === 'unresolved' ? this.unresolvedColumns : this.resolvedColumns;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['entries'] && this.mode === 'unresolved') {
      this.rebuildControls();
      this.autoSuggestNewEntries();
    } else if (changes['mode'] && this.mode === 'unresolved' && this.entries.length > 0) {
      this.rebuildControls();
      this.autoSuggestNewEntries();
    }
  }

  private autoSuggestNewEntries(): void {
    this.entries
      .filter(e => !this.suggestions.has(e.playerId) && !this.suggestLoading.get(e.playerId))
      .forEach(e => this.onSuggest(e));
  }

  onConfirm(entry: PlayerIdentityEntry): void {
    const control = this.epicIdControls.get(entry.playerId);
    if (!control || control.invalid) return;
    this.resolved.emit({ playerId: entry.playerId, epicId: control.value.trim() });
  }

  onReject(entry: PlayerIdentityEntry): void {
    this.rejected.emit({ playerId: entry.playerId });
  }

  onCorrect(entry: PlayerIdentityEntry): void {
    this.correctRequested.emit(entry);
  }

  onSuggest(entry: PlayerIdentityEntry): void {
    if (this.suggestLoading.get(entry.playerId)) return;

    const requestSignature = this.registerSuggestionRequest(entry);
    const isCurrentRequest = () =>
      this.isCurrentSuggestionRequest(entry.playerId, requestSignature);
    let rateLimitExhausted = false;
    let resolutionUnavailable = false;

    this.startSuggestionLoading(entry.playerId);
    this.pipelineService
      .getSuggestedEpicId(entry.playerId, {
        onRetry: () => {
          this.handleSuggestionRetry(entry.playerId, isCurrentRequest);
        },
        onRateLimitExhausted: () => {
          if (!isCurrentRequest()) return;
          rateLimitExhausted = true;
          this.rateLimitExhausted.emit();
        },
        onResolutionUnavailable: () => {
          if (!isCurrentRequest()) return;
          resolutionUnavailable = true;
          this.resolutionUnavailable.emit();
        }
      })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.finishSuggestionLoading(entry.playerId, isCurrentRequest);
        })
      )
      .subscribe({
        next: suggestion => {
          if (!isCurrentRequest()) return;
          this.applySuggestionResult(
            entry.playerId,
            suggestion,
            rateLimitExhausted || resolutionUnavailable
          );
        }
      });
  }

  isRateLimitLoading(playerId: string): boolean {
    return this.rateLimitLoading.get(playerId) ?? false;
  }

  getSuggestion(playerId: string): EpicIdSuggestion | undefined {
    return this.suggestions.get(playerId);
  }

  isSuggestLoading(playerId: string): boolean {
    return this.suggestLoading.get(playerId) ?? false;
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

    // Remove stale controls and clear stale map state
    existingIds.forEach(id => {
      if (!currentIds.has(id)) {
        this.epicIdControls.delete(id);
        this.suggestions.delete(id);
        this.suggestLoading.delete(id);
        this.rateLimitLoading.delete(id);
        this.entrySignatures.delete(id);
      }
    });

    // Add missing controls
    this.entries.forEach(entry => {
      const signature = this.entrySignature(entry);
      const previousSignature = this.entrySignatures.get(entry.playerId);
      if (previousSignature !== undefined && previousSignature !== signature) {
        this.clearSuggestionState(entry.playerId);
      }
      this.entrySignatures.set(entry.playerId, signature);
      if (!this.epicIdControls.has(entry.playerId)) {
        this.epicIdControls.set(
          entry.playerId,
          new FormControl('', { nonNullable: true, validators: [Validators.required] })
        );
      }
    });
  }

  private clearSuggestionState(playerId: string): void {
    this.suggestions.delete(playerId);
    this.suggestLoading.delete(playerId);
    this.rateLimitLoading.delete(playerId);
    const control = this.epicIdControls.get(playerId);
    if (control && !control.dirty) {
      control.reset('', { emitEvent: false });
    }
  }

  private entrySignature(entry: PlayerIdentityEntry): string {
    return `${entry.playerUsername}::${entry.playerRegion}`;
  }

  private registerSuggestionRequest(entry: PlayerIdentityEntry): string {
    const requestSignature = this.entrySignature(entry);
    this.entrySignatures.set(entry.playerId, requestSignature);
    return requestSignature;
  }

  private isCurrentSuggestionRequest(playerId: string, requestSignature: string): boolean {
    return this.entrySignatures.get(playerId) === requestSignature;
  }

  private startSuggestionLoading(playerId: string): void {
    this.suggestLoading.set(playerId, true);
    this.rateLimitLoading.set(playerId, false);
    this.cdr.markForCheck();
  }

  private handleSuggestionRetry(playerId: string, isCurrentRequest: () => boolean): void {
    if (!isCurrentRequest()) return;
    this.rateLimitLoading.set(playerId, true);
    this.cdr.markForCheck();
  }

  private finishSuggestionLoading(playerId: string, isCurrentRequest: () => boolean): void {
    if (!isCurrentRequest()) return;
    this.suggestLoading.set(playerId, false);
    this.rateLimitLoading.set(playerId, false);
    this.cdr.markForCheck();
  }

  private applySuggestionResult(
    playerId: string,
    suggestion: EpicIdSuggestion | null,
    transientFailure: boolean
  ): void {
    if (suggestion?.found && suggestion.suggestedEpicId) {
      this.applyFoundSuggestion(playerId, suggestion);
    } else if (!transientFailure) {
      this.markSuggestionNotFound(playerId);
    }
  }

  private applyFoundSuggestion(playerId: string, suggestion: EpicIdSuggestion): void {
    this.suggestions.set(playerId, suggestion);
    const control = this.epicIdControls.get(playerId);
    if (control && !control.dirty) {
      control.setValue(suggestion.suggestedEpicId ?? '');
    }
  }

  private markSuggestionNotFound(playerId: string): void {
    // Store not-found sentinel to prevent infinite retry on next ngOnChanges.
    this.suggestions.set(playerId, {
      found: false,
      suggestedEpicId: null,
      displayName: null,
      confidenceScore: 0
    });
  }
}
