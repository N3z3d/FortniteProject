import { of, throwError } from 'rxjs';
import { PipelineService } from '../../services/pipeline.service';
import {
  PlayerIdentityEntry,
  PipelineCount,
  PipelineRegionalStats,
  ScrapeLogEntry,
  PipelineAlertStatus
} from '../../models/admin.models';

export const UNRESOLVED: PlayerIdentityEntry = {
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

export const RESOLVED: PlayerIdentityEntry = {
  id: 'e2',
  playerId: 'p2',
  playerUsername: 'Alpha',
  playerRegion: 'NA',
  epicId: 'alpha_fn',
  status: 'RESOLVED',
  confidenceScore: 90,
  resolvedBy: 'admin',
  resolvedAt: '2026-01-02T00:00:00',
  rejectedAt: null,
  rejectionReason: null,
  createdAt: '2026-01-01T00:00:00',
  correctedUsername: null,
  correctedRegion: null,
  correctedBy: null,
  correctedAt: null
};

export const COUNT: PipelineCount = { unresolvedCount: 1, resolvedCount: 1 };

export const NONE_ALERT: PipelineAlertStatus = {
  level: 'NONE',
  unresolvedCount: 0,
  oldestUnresolvedAt: null,
  elapsedHours: 0,
  checkedAt: '2026-03-01T12:00:00Z'
};

export const CRITICAL_ALERT: PipelineAlertStatus = {
  level: 'CRITICAL',
  unresolvedCount: 5,
  oldestUnresolvedAt: '2026-02-26T10:00:00Z',
  elapsedHours: 72,
  checkedAt: '2026-03-01T12:00:00Z'
};

export const WARNING_ALERT: PipelineAlertStatus = {
  level: 'WARNING',
  unresolvedCount: 2,
  oldestUnresolvedAt: '2026-02-28T10:00:00Z',
  elapsedHours: 26,
  checkedAt: '2026-03-01T12:00:00Z'
};

export const SAMPLE_LOG: ScrapeLogEntry = {
  id: 'log1',
  source: 'EU',
  startedAt: '2026-03-01T05:00:00Z',
  finishedAt: '2026-03-01T06:00:00Z',
  status: 'SUCCESS',
  totalRowsWritten: 100,
  errorMessage: null
};

export function makePipelineServiceSpy(
  overrides: Partial<{
    unresolved: PlayerIdentityEntry[];
    resolved: PlayerIdentityEntry[];
    count: PipelineCount;
    resolveResult: PlayerIdentityEntry | null;
    rejectResult: PlayerIdentityEntry | null;
    correctResult: PlayerIdentityEntry | null;
    failLoad: boolean;
    regional: PipelineRegionalStats[];
    failRegional: boolean;
    scrapeLog: ScrapeLogEntry[];
    pipelineAlert: PipelineAlertStatus | null;
    failAlert: boolean;
  }> = {}
): jasmine.SpyObj<PipelineService> {
  const spy = jasmine.createSpyObj<PipelineService>('PipelineService', [
    'getUnresolved',
    'getResolved',
    'getCount',
    'resolvePlayer',
    'rejectPlayer',
    'getRegionalStatus',
    'correctMetadata',
    'getScrapeLog',
    'getUnresolvedAlertStatus',
    'getAvailableRegions',
    'getSuggestedEpicId'
  ]);
  if (overrides.failLoad) {
    spy.getUnresolved.and.returnValue(throwError(() => new Error('fail')));
    spy.getResolved.and.returnValue(throwError(() => new Error('fail')));
    spy.getCount.and.returnValue(throwError(() => new Error('fail')));
    spy.getScrapeLog.and.returnValue(throwError(() => new Error('fail')));
    spy.getUnresolvedAlertStatus.and.returnValue(throwError(() => new Error('fail')));
  } else {
    spy.getUnresolved.and.returnValue(of(overrides.unresolved ?? [UNRESOLVED]));
    spy.getResolved.and.returnValue(of(overrides.resolved ?? [RESOLVED]));
    spy.getCount.and.returnValue(of(overrides.count ?? COUNT));
    spy.getScrapeLog.and.returnValue(of(overrides.scrapeLog ?? []));
    if (overrides.failAlert) {
      spy.getUnresolvedAlertStatus.and.returnValue(throwError(() => new Error('alert fail')));
    } else {
      const alertResult: PipelineAlertStatus | null =
        'pipelineAlert' in overrides ? (overrides.pipelineAlert as PipelineAlertStatus | null) : NONE_ALERT;
      spy.getUnresolvedAlertStatus.and.returnValue(of(alertResult));
    }
  }
  if (overrides.failRegional) {
    spy.getRegionalStatus.and.returnValue(throwError(() => new Error('regional fail')));
  } else {
    spy.getRegionalStatus.and.returnValue(of(overrides.regional ?? []));
  }
  const resolveResult = 'resolveResult' in overrides ? overrides.resolveResult! : RESOLVED;
  const rejectResult =
    'rejectResult' in overrides
      ? overrides.rejectResult!
      : ({ ...UNRESOLVED, status: 'REJECTED' } as PlayerIdentityEntry);
  const correctResult = 'correctResult' in overrides ? overrides.correctResult! : RESOLVED;
  spy.resolvePlayer.and.returnValue(of(resolveResult));
  spy.rejectPlayer.and.returnValue(of(rejectResult));
  spy.correctMetadata.and.returnValue(of(correctResult));
  spy.getAvailableRegions.and.returnValue(of([]));
  spy.getSuggestedEpicId.and.returnValue(of(null));
  return spy;
}
