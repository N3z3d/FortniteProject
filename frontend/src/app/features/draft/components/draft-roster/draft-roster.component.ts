import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatChipsModule } from '@angular/material/chips';

import { Player } from '../../models/draft.interface';
import { TranslationService } from '../../../../core/services/translation.service';
import { DraftStateHelperService } from '../../services/draft-state-helper.service';

@Component({
  selector: 'app-draft-roster',
  standalone: true,
  imports: [CommonModule, MatChipsModule],
  templateUrl: './draft-roster.component.html',
  styleUrls: ['./draft-roster.component.scss']
})
export class DraftRosterComponent {
  public readonly t = inject(TranslationService);
  private readonly helperService = inject(DraftStateHelperService);

  @Input() team: Player[] = [];
  @Input() remainingSlots = 0;

  getRegionLabel(region: string | undefined): string {
    if (!region) return '';
    const key = this.helperService.getRegionLabelKey(region);
    return this.t.t(key, region);
  }

  getSlotsRemainingLabel(): string {
    return this.helperService.getSlotsRemainingLabel(this.remainingSlots, (key) => this.t.t(key));
  }
}
