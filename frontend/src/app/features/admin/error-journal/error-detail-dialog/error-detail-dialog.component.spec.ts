import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ErrorDetailDialogComponent } from './error-detail-dialog.component';
import { TranslationService } from '../../../../core/services/translation.service';
import { ErrorEntry } from '../../models/error-journal.models';

describe('ErrorDetailDialogComponent', () => {
  let component: ErrorDetailDialogComponent;
  let fixture: ComponentFixture<ErrorDetailDialogComponent>;

  const mockEntry: ErrorEntry = {
    id: 'uuid-1',
    timestamp: '2026-02-17T10:00:00',
    exceptionType: 'GameNotFoundException',
    message: 'Game abc-123 not found',
    statusCode: 404,
    errorCode: 'GAME_NOT_FOUND',
    path: '/api/games/abc-123',
    stackTrace: 'com.fortnite.pronos.exception.GameNotFoundException: Game abc-123 not found\n\tat GameService.findById(GameService.java:42)'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ErrorDetailDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: mockEntry },
        { provide: MatDialogRef, useValue: { close: jasmine.createSpy('close') } },
        TranslationService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ErrorDetailDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display error entry data', () => {
    expect(component.data).toEqual(mockEntry);
  });

  it('should return status-4xx for 404', () => {
    expect(component.getStatusClass(404)).toBe('status-4xx');
  });

  it('should return status-5xx for 500', () => {
    expect(component.getStatusClass(500)).toBe('status-5xx');
  });

  it('should return status-other for 200', () => {
    expect(component.getStatusClass(200)).toBe('status-other');
  });

  it('should render exception type in template', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('GameNotFoundException');
  });

  it('should render stack trace', () => {
    const pre = fixture.nativeElement.querySelector('pre.stack-trace');
    expect(pre).toBeTruthy();
    expect(pre.textContent).toContain('GameService.findById');
  });

  it('should render path', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('/api/games/abc-123');
  });
});
