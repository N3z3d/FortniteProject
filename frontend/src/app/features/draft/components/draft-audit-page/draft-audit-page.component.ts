import { Component, OnInit, inject, ChangeDetectionStrategy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DraftAuditService, DraftAuditEntry } from '../../services/draft-audit.service';

@Component({
  selector: 'app-draft-audit-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './draft-audit-page.component.html',
  styleUrls: ['./draft-audit-page.component.scss']
})
export class DraftAuditPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly auditService = inject(DraftAuditService);

  readonly loading = signal(true);
  readonly entries = signal<DraftAuditEntry[]>([]);
  loadError = false;
  gameId = '';

  ngOnInit(): void {
    this.gameId = this.route.snapshot.paramMap.get('id') ?? '';
    this.loadAudit();
  }

  reload(): void {
    this.loadAudit();
  }

  typeLabel(type: DraftAuditEntry['type']): string {
    switch (type) {
      case 'SWAP_SOLO': return 'Échange solo';
      case 'TRADE_PROPOSED': return 'Trade proposé';
      case 'TRADE_ACCEPTED': return 'Trade accepté';
      case 'TRADE_REJECTED': return 'Trade refusé';
    }
  }

  private loadAudit(): void {
    this.loading.set(true);
    this.loadError = false;
    this.auditService.getAudit(this.gameId).subscribe({
      next: data => {
        this.entries.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.loadError = true;
        this.loading.set(false);
      }
    });
  }
}
