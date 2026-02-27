import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnInit,
  OnDestroy,
  OnChanges,
  inject,
  DestroyRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

export interface PlayerFilter {
  searchTerm: string;
  region: string | null;
  tranche: string | null;
  hideUnavailable: boolean;
  hideTaken: boolean;
}

@Component({
  selector: 'app-player-search-filter',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatIconModule,
  ],
  templateUrl: './player-search-filter.component.html',
  styleUrls: ['./player-search-filter.component.scss'],
})
export class PlayerSearchFilterComponent implements OnInit, OnChanges, OnDestroy {
  private readonly destroyRef = inject(DestroyRef);

  @Input() mode: 'draft' | 'browse' = 'draft';
  @Input() availableRegions: string[] = [];
  @Input() availableTranches: string[] = [];
  @Input() currentRegion: string | null = null;
  @Input() currentTranche: string | null = null;
  @Input() totalResults = 0;

  @Output() readonly filterChanged = new EventEmitter<PlayerFilter>();

  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly regionControl = new FormControl<string | null>(null);
  readonly trancheControl = new FormControl<string | null>(null);

  hideUnavailable = true;
  hideTaken = true;

  get regionLocked(): boolean {
    return this.mode === 'draft' && this.currentRegion !== null;
  }

  get trancheLocked(): boolean {
    return this.mode === 'draft' && this.currentTranche !== null;
  }

  ngOnChanges(): void {
    if (this.regionLocked) {
      this.regionControl.disable({ emitEvent: false });
    } else {
      this.regionControl.enable({ emitEvent: false });
    }
    if (this.trancheLocked) {
      this.trancheControl.disable({ emitEvent: false });
    } else {
      this.trancheControl.enable({ emitEvent: false });
    }
  }

  ngOnInit(): void {
    this.searchControl.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => this.emitFilter());
  }

  ngOnDestroy(): void {
    // takeUntilDestroyed handles cleanup via DestroyRef
  }

  onRegionChange(): void {
    this.emitFilter();
  }

  onTrancheChange(): void {
    this.emitFilter();
  }

  onToggleChange(field: 'hideTaken' | 'hideUnavailable', value: boolean): void {
    this[field] = value;
    this.emitFilter();
  }

  private emitFilter(): void {
    const region = this.regionLocked
      ? this.currentRegion
      : (this.regionControl.value ?? null);

    const tranche = this.trancheLocked
      ? this.currentTranche
      : (this.trancheControl.value ?? null);

    this.filterChanged.emit({
      searchTerm: this.normalizeText(this.searchControl.value),
      region,
      tranche,
      hideUnavailable: this.hideUnavailable,
      hideTaken: this.hideTaken,
    });
  }

  private normalizeText(text: string): string {
    return text
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase();
  }
}
