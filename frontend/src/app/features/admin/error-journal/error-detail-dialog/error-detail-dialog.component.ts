import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslationService } from '../../../../core/services/translation.service';
import { ErrorEntry } from '../../models/error-journal.models';

@Component({
  selector: 'app-error-detail-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <h2 mat-dialog-title class="dialog-title">
      <mat-icon>bug_report</mat-icon>
      {{ t.t('admin.errors.detail.title') }}
    </h2>
    <mat-dialog-content class="dialog-content">
      <div class="detail-grid">
        <div class="detail-row">
          <span class="detail-label">{{ t.t('admin.errors.table.type') }}</span>
          <span class="detail-value type-value">{{ data.exceptionType }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">{{ t.t('admin.errors.table.status') }}</span>
          <span class="detail-value status-badge" [class]="getStatusClass(data.statusCode)">{{ data.statusCode }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">{{ t.t('admin.errors.table.message') }}</span>
          <span class="detail-value">{{ data.message }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">{{ t.t('admin.errors.table.path') }}</span>
          <span class="detail-value code-value">{{ data.path }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">{{ t.t('admin.errors.detail.errorCode') }}</span>
          <span class="detail-value code-value">{{ data.errorCode }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">{{ t.t('admin.errors.table.timestamp') }}</span>
          <span class="detail-value">{{ data.timestamp }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">ID</span>
          <span class="detail-value code-value">{{ data.id }}</span>
        </div>
      </div>
      <div class="stack-trace-section" *ngIf="data.stackTrace">
        <h3 class="stack-trace-title">{{ t.t('admin.errors.detail.stackTrace') }}</h3>
        <pre class="stack-trace">{{ data.stackTrace }}</pre>
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>{{ t.t('admin.errors.detail.close') }}</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-title {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #ff6b6b;
    }
    .dialog-content {
      min-width: 500px;
      max-width: 700px;
    }
    .detail-grid {
      display: grid;
      gap: 12px;
    }
    .detail-row {
      display: flex;
      gap: 12px;
      align-items: baseline;
    }
    .detail-label {
      min-width: 120px;
      font-weight: 600;
      color: rgba(255, 255, 255, 0.7);
      font-size: 0.85rem;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .detail-value {
      word-break: break-word;
    }
    .type-value {
      color: #ff6b6b;
      font-weight: 600;
    }
    .code-value {
      font-family: monospace;
      font-size: 0.9rem;
      color: #ffd700;
    }
    .status-badge {
      display: inline-block;
      padding: 2px 10px;
      border-radius: 12px;
      font-weight: 700;
      font-size: 0.85rem;
    }
    .status-4xx {
      background: rgba(255, 152, 0, 0.2);
      color: #ff9800;
    }
    .status-5xx {
      background: rgba(244, 67, 54, 0.2);
      color: #f44336;
    }
    .status-other {
      background: rgba(158, 158, 158, 0.2);
      color: #9e9e9e;
    }
    .stack-trace-section {
      margin-top: 16px;
    }
    .stack-trace-title {
      font-size: 0.9rem;
      color: rgba(255, 255, 255, 0.7);
      margin-bottom: 8px;
    }
    .stack-trace {
      background: rgba(0, 0, 0, 0.3);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 8px;
      padding: 12px;
      font-size: 0.8rem;
      color: rgba(255, 255, 255, 0.8);
      max-height: 300px;
      overflow-y: auto;
      white-space: pre-wrap;
      word-break: break-all;
    }
  `]
})
export class ErrorDetailDialogComponent {
  public readonly t = inject(TranslationService);
  public readonly data: ErrorEntry = inject(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<ErrorDetailDialogComponent>);

  getStatusClass(statusCode: number): string {
    if (statusCode >= 500) return 'status-5xx';
    if (statusCode >= 400) return 'status-4xx';
    return 'status-other';
  }
}
