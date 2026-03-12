import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
import { AdminPipelinePageComponent } from './admin-pipeline-page.component';
import { PipelineService } from '../../services/pipeline.service';
import { PipelineRegionalStats } from '../../models/admin.models';
import {
  UNRESOLVED,
  RESOLVED,
  CRITICAL_ALERT,
  WARNING_ALERT,
  NONE_ALERT,
  SAMPLE_LOG,
  makePipelineServiceSpy
} from './admin-pipeline-page.fixtures';

describe('AdminPipelinePageComponent', () => {
  let fixture: ComponentFixture<AdminPipelinePageComponent>;
  let component: AdminPipelinePageComponent;
  let pipelineSpy: jasmine.SpyObj<PipelineService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;

  async function setupComponent(
    serviceOverrides: Parameters<typeof makePipelineServiceSpy>[0] = {}
  ): Promise<void> {
    pipelineSpy = makePipelineServiceSpy(serviceOverrides);
    snackBarSpy = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    dialogSpy = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialogSpy.open.and.returnValue({ afterClosed: () => of(undefined) } as any);

    await TestBed.configureTestingModule({
      imports: [AdminPipelinePageComponent, NoopAnimationsModule],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .overrideProvider(PipelineService, { useValue: pipelineSpy })
      .overrideProvider(MatSnackBar, { useValue: snackBarSpy })
      .overrideProvider(MatDialog, { useValue: dialogSpy })
      .compileComponents();

    fixture = TestBed.createComponent(AdminPipelinePageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  describe('Initial load', () => {
    it('calls getUnresolved, getResolved, getCount on init', async () => {
      await setupComponent();
      fixture.detectChanges();
      expect(pipelineSpy.getUnresolved).toHaveBeenCalledTimes(1);
      expect(pipelineSpy.getResolved).toHaveBeenCalledTimes(1);
      expect(pipelineSpy.getCount).toHaveBeenCalledTimes(1);
    });

    it('populates unresolvedEntries after load', async () => {
      await setupComponent();
      fixture.detectChanges();
      expect(component.unresolvedEntries).toEqual([UNRESOLVED]);
    });

    it('populates resolvedEntries after load', async () => {
      await setupComponent();
      fixture.detectChanges();
      expect(component.resolvedEntries).toEqual([RESOLVED]);
    });

    it('loading signal is false after data loads', async () => {
      await setupComponent();
      fixture.detectChanges();
      expect(component.loading()).toBeFalse();
    });

    it('sets dataLoadError when loading fails', async () => {
      await setupComponent({ failLoad: true });
      fixture.detectChanges();
      expect(component.loading()).toBeFalse();
      expect(component.dataLoadError).toBeTrue();
    });

    it('shows partial error banner when dataLoadError is true', async () => {
      await setupComponent({ failLoad: true });
      fixture.detectChanges();
      const banner = fixture.debugElement.query(By.css('.pipeline-error--partial'));
      expect(banner).toBeTruthy();
    });
  });

  describe('Inbox-zero', () => {
    it('isInboxZero is true when unresolved is empty after loading', async () => {
      await setupComponent({ unresolved: [] });
      fixture.detectChanges();
      expect(component.isInboxZero).toBeTrue();
    });

    it('isInboxZero is false when unresolved has entries', async () => {
      await setupComponent();
      fixture.detectChanges();
      expect(component.isInboxZero).toBeFalse();
    });

    it('renders inbox-zero DOM element when queue is empty', async () => {
      await setupComponent({ unresolved: [] });
      fixture.detectChanges();
      const el = fixture.debugElement.query(By.css('.pipeline-inbox-zero'));
      expect(el).toBeTruthy();
    });
  });

  describe('Resolve action', () => {
    it('calls resolvePlayer service on onResolved event', async () => {
      await setupComponent();
      fixture.detectChanges();
      component.onResolved({ playerId: 'p1', epicId: 'bughaboo_fn' });
      expect(pipelineSpy.resolvePlayer).toHaveBeenCalledWith({
        playerId: 'p1',
        epicId: 'bughaboo_fn'
      });
    });

    it('shows success snackbar after resolve', async () => {
      await setupComponent();
      fixture.detectChanges();
      component.onResolved({ playerId: 'p1', epicId: 'bughaboo_fn' });
      expect(snackBarSpy.open).toHaveBeenCalledWith(
        jasmine.stringContaining('bughaboo_fn'),
        'Fermer',
        jasmine.any(Object)
      );
    });

    it('reloads data after successful resolve', async () => {
      await setupComponent();
      fixture.detectChanges();
      const callsBefore = pipelineSpy.getUnresolved.calls.count();
      component.onResolved({ playerId: 'p1', epicId: 'x' });
      expect(pipelineSpy.getUnresolved.calls.count()).toBeGreaterThan(callsBefore);
    });

    it('shows error snackbar when resolve returns null', async () => {
      await setupComponent({ resolveResult: null });
      fixture.detectChanges();
      component.onResolved({ playerId: 'p1', epicId: 'bad' });
      expect(snackBarSpy.open).toHaveBeenCalledWith(
        jasmine.stringContaining('Erreur'),
        'Fermer',
        jasmine.any(Object)
      );
    });
  });

  describe('Reject action', () => {
    it('calls rejectPlayer service on onRejected event', async () => {
      await setupComponent();
      fixture.detectChanges();
      component.onRejected({ playerId: 'p1' });
      expect(pipelineSpy.rejectPlayer).toHaveBeenCalledWith({
        playerId: 'p1',
        reason: undefined
      });
    });

    it('shows snackbar after reject', async () => {
      await setupComponent();
      fixture.detectChanges();
      component.onRejected({ playerId: 'p1' });
      expect(snackBarSpy.open).toHaveBeenCalledWith(
        '✗ Joueur rejeté',
        'Fermer',
        jasmine.any(Object)
      );
    });
  });

  describe('Correct metadata action', () => {
    it('opens correct metadata dialog on onCorrectRequested', async () => {
      await setupComponent();
      fixture.detectChanges();
      component.onCorrectRequested(UNRESOLVED);
      expect(dialogSpy.open).toHaveBeenCalled();
    });

    it('calls correctMetadata service when dialog closes with result', async () => {
      await setupComponent();
      fixture.detectChanges();
      dialogSpy.open.and.returnValue({ afterClosed: () => of({ newRegion: 'NAW' }) } as any);
      component.onCorrectRequested(UNRESOLVED);
      expect(pipelineSpy.correctMetadata).toHaveBeenCalledWith('p1', { newRegion: 'NAW' });
    });

    it('shows success snackbar after correction', async () => {
      await setupComponent();
      fixture.detectChanges();
      dialogSpy.open.and.returnValue({ afterClosed: () => of({ newUsername: 'Fixed' }) } as any);
      component.onCorrectRequested(UNRESOLVED);
      expect(snackBarSpy.open).toHaveBeenCalledWith(
        jasmine.stringContaining('corrigée'),
        'Fermer',
        jasmine.any(Object)
      );
    });

    it('does not call correctMetadata when dialog is dismissed', async () => {
      await setupComponent();
      fixture.detectChanges();
      component.onCorrectRequested(UNRESOLVED);
      expect(pipelineSpy.correctMetadata).not.toHaveBeenCalled();
    });
  });

  describe('Refresh', () => {
    it('reloads data on refresh call', async () => {
      await setupComponent();
      fixture.detectChanges();
      const countBefore = pipelineSpy.getUnresolved.calls.count();
      component.refresh();
      expect(pipelineSpy.getUnresolved.calls.count()).toBeGreaterThan(countBefore);
    });
  });

  describe('Header', () => {
    it('renders MODE ADMINISTRATION heading', async () => {
      await setupComponent();
      fixture.detectChanges();
      const heading = fixture.debugElement.query(By.css('.pipeline-header__heading'));
      expect(heading.nativeElement.textContent).toContain('MODE ADMINISTRATION');
    });
  });

  describe('Scrape log', () => {
    it('populates scrapeLog after load', async () => {
      await setupComponent({ scrapeLog: [SAMPLE_LOG] });
      fixture.detectChanges();
      expect(component.scrapeLog).toEqual([SAMPLE_LOG]);
    });

    it('scrapeLog is empty array when service returns empty', async () => {
      await setupComponent({ scrapeLog: [] });
      fixture.detectChanges();
      expect(component.scrapeLog).toEqual([]);
    });
  });

  describe('Pipeline alert banner', () => {
    it('shows alert banner when level is CRITICAL', async () => {
      await setupComponent({ pipelineAlert: CRITICAL_ALERT });
      fixture.detectChanges();
      const banner = fixture.debugElement.query(By.css('.pipeline-alert-banner'));
      expect(banner).toBeTruthy();
    });

    it('shows alert banner when level is WARNING', async () => {
      await setupComponent({ pipelineAlert: WARNING_ALERT });
      fixture.detectChanges();
      const banner = fixture.debugElement.query(By.css('.pipeline-alert-banner'));
      expect(banner).toBeTruthy();
    });

    it('does not show alert banner when level is NONE', async () => {
      await setupComponent({ pipelineAlert: NONE_ALERT });
      fixture.detectChanges();
      const banner = fixture.debugElement.query(By.css('.pipeline-alert-banner'));
      expect(banner).toBeNull();
    });

    it('alert banner shows elapsed hours from pipelineAlert', async () => {
      await setupComponent({ pipelineAlert: CRITICAL_ALERT });
      fixture.detectChanges();
      const banner = fixture.debugElement.query(By.css('.pipeline-alert-banner'));
      expect(banner.nativeElement.textContent).toContain('72');
    });

    it('alertLabel returns CRITIQUE for CRITICAL level', async () => {
      await setupComponent();
      expect((component as any).alertLabel('CRITICAL')).toContain('CRITIQUE');
    });

    it('alertLabel returns ATTENTION for WARNING level', async () => {
      await setupComponent();
      expect((component as any).alertLabel('WARNING')).toContain('ATTENTION');
    });
  });

  describe('Alert error', () => {
    it('sets alertError when getUnresolvedAlertStatus fails', async () => {
      await setupComponent({ failAlert: true });
      fixture.detectChanges();
      expect(component.alertError).toBeTrue();
    });

    it('shows alert error indicator when alertError is true', async () => {
      await setupComponent({ failAlert: true });
      fixture.detectChanges();
      const el = fixture.debugElement.query(By.css('.pipeline-alert-error'));
      expect(el).toBeTruthy();
    });
  });

  describe('Regional stats', () => {
    const EU_REGION: PipelineRegionalStats = {
      region: 'EU',
      unresolvedCount: 3,
      resolvedCount: 10,
      rejectedCount: 1,
      totalCount: 14,
      lastIngestedAt: '2026-02-01T10:00:00'
    };

    it('calls getRegionalStatus on init', async () => {
      await setupComponent({ regional: [EU_REGION] });
      fixture.detectChanges();
      expect(pipelineSpy.getRegionalStatus).toHaveBeenCalledTimes(1);
    });

    it('populates regionalStats after successful load', async () => {
      await setupComponent({ regional: [EU_REGION] });
      fixture.detectChanges();
      expect(component.regionalStats).toEqual([EU_REGION]);
    });

    it('sets regionalError true when regional load fails', async () => {
      await setupComponent({ failRegional: true });
      fixture.detectChanges();
      expect(component.regionalError).toBeTrue();
    });

    it('regional stats tab is visible even when main data load fails', async () => {
      await setupComponent({ failLoad: true });
      fixture.detectChanges();
      expect(component.loading()).toBeFalse();
      expect(component.dataLoadError).toBeTrue();
      expect(component.regionalStats).toBeDefined();
    });
  });
});
