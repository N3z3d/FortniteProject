import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { DiagnosticComponent } from './diagnostic.component';
import { environment } from '../../../environments/environment';

describe('DiagnosticComponent', () => {
  let component: DiagnosticComponent;
  let fixture: ComponentFixture<DiagnosticComponent>;
  let router: jasmine.SpyObj<Router>;
  let http: jasmine.SpyObj<HttpClient>;

  beforeEach(async () => {
    router = jasmine.createSpyObj('Router', ['navigate']);
    http = jasmine.createSpyObj('HttpClient', ['get']);

    await TestBed.configureTestingModule({
      imports: [DiagnosticComponent],
      providers: [
        { provide: Router, useValue: router },
        { provide: HttpClient, useValue: http }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DiagnosticComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with empty diagnostics', () => {
    expect(component.diagnostics).toEqual([]);
    expect(component.isRunning).toBeFalse();
  });

  it('should run diagnostic on init', fakeAsync(() => {
    spyOn(component, 'runDiagnostic');

    fixture.detectChanges();

    expect(component.runDiagnostic).toHaveBeenCalled();
  }));

  it('should set isRunning to true when diagnostic starts', fakeAsync(() => {
    http.get.and.returnValue(of({ status: 'UP' }));

    component.runDiagnostic();

    expect(component.isRunning).toBeTrue();

    tick();

    expect(component.isRunning).toBeFalse();
  }));

  it('should test frontend environment', fakeAsync(() => {
    const originalProduction = environment.production;
    environment.production = false;
    http.get.and.returnValue(of({ status: 'UP' }));

    component.runDiagnostic();
    tick();

    const envTest = component.diagnostics.find(d => d.label === 'Environment Frontend');
    expect(envTest).toBeDefined();
    expect(envTest?.value).toBe('Développement');
    expect(envTest?.status).toBe('success');

    environment.production = originalProduction;
  }));

  it('should test API base URL configured', fakeAsync(() => {
    const originalApiUrl = environment.apiBaseUrl;
    environment.apiBaseUrl = 'http://localhost:8080';
    http.get.and.returnValue(of({ status: 'UP' }));

    component.runDiagnostic();
    tick();

    const apiTest = component.diagnostics.find(d => d.label === 'URL API configurée');
    expect(apiTest).toBeDefined();
    expect(apiTest?.value).toBe('http://localhost:8080');
    expect(apiTest?.status).toBe('success');

    environment.apiBaseUrl = originalApiUrl;
  }));

  it('should test local storage availability', fakeAsync(() => {
    http.get.and.returnValue(of({ status: 'UP' }));

    component.runDiagnostic();
    tick();

    const storageTest = component.diagnostics.find(d => d.label === 'Local Storage');
    expect(storageTest).toBeDefined();
    expect(storageTest?.value).toBe('Fonctionnel');
    expect(storageTest?.status).toBe('success');
  }));

  it('should test session storage availability', fakeAsync(() => {
    http.get.and.returnValue(of({ status: 'UP' }));

    component.runDiagnostic();
    tick();

    const sessionTest = component.diagnostics.find(d => d.label === 'Session Storage');
    expect(sessionTest).toBeDefined();
    expect(sessionTest?.value).toBe('Fonctionnel');
    expect(sessionTest?.status).toBe('success');
  }));

  it('should test backend connectivity success', fakeAsync(() => {
    const originalApiUrl = environment.apiBaseUrl;
    environment.apiBaseUrl = 'http://localhost:8080';
    http.get.and.returnValue(of({ status: 'UP' }));

    component.runDiagnostic();
    tick();

    const backendTest = component.diagnostics.find(d => d.label === 'Connectivité Backend');
    expect(backendTest).toBeDefined();
    expect(backendTest?.value).toBe('Connecté');
    expect(backendTest?.status).toBe('success');

    environment.apiBaseUrl = originalApiUrl;
  }));

  it('should test backend connectivity failure', fakeAsync(() => {
    const originalApiUrl = environment.apiBaseUrl;
    environment.apiBaseUrl = 'http://localhost:8080';
    http.get.and.returnValue(throwError(() => new Error('Connection refused')));

    component.runDiagnostic();
    tick();

    const backendTest = component.diagnostics.find(d => d.label === 'Connectivité Backend');
    expect(backendTest).toBeDefined();
    expect(backendTest?.value).toBe('Non disponible');
    expect(backendTest?.status).toBe('warning');

    environment.apiBaseUrl = originalApiUrl;
  }));

  it('should test browser features', fakeAsync(() => {
    http.get.and.returnValue(of({ status: 'UP' }));

    component.runDiagnostic();
    tick();

    const browserTest = component.diagnostics.find(d => d.label === 'Fonctionnalités Navigateur');
    expect(browserTest).toBeDefined();
    expect(browserTest?.status).toBe('success');
    expect(browserTest?.details).toContain('Fetch API');
  }));

  it('should return correct icon for success status', () => {
    expect(component.getIcon('success')).toBe('check_circle');
  });

  it('should return correct icon for warning status', () => {
    expect(component.getIcon('warning')).toBe('warning');
  });

  it('should return correct icon for error status', () => {
    expect(component.getIcon('error')).toBe('error');
  });

  it('should return help icon for unknown status', () => {
    expect(component.getIcon('unknown')).toBe('help');
  });

  it('should return icon class matching status', () => {
    expect(component.getIconClass('success')).toBe('success');
    expect(component.getIconClass('warning')).toBe('warning');
    expect(component.getIconClass('error')).toBe('error');
  });

  it('should count success diagnostics', fakeAsync(() => {
    http.get.and.returnValue(of({ status: 'UP' }));

    component.runDiagnostic();
    tick();

    const count = component.getSuccessCount();
    expect(count).toBeGreaterThan(0);
  }));

  it('should count warning diagnostics', fakeAsync(() => {
    const originalApiUrl = environment.apiBaseUrl;
    environment.apiBaseUrl = 'http://localhost:8080';
    http.get.and.returnValue(throwError(() => new Error('Connection refused')));

    component.runDiagnostic();
    tick();

    const count = component.getWarningCount();
    expect(count).toBeGreaterThanOrEqual(1);

    environment.apiBaseUrl = originalApiUrl;
  }));

  it('should count error diagnostics', () => {
    component.diagnostics = [
      { label: 'Test', value: 'Error', status: 'error' }
    ];

    const count = component.getErrorCount();

    expect(count).toBe(1);
  });

  it('should navigate back to games', () => {
    component.goBack();

    expect(router.navigate).toHaveBeenCalledWith(['/games']);
  });

  it('should handle unexpected errors during diagnostic', fakeAsync(() => {
    spyOn(component as any, 'testLocalStorage').and.throwError('Unexpected error');

    component.runDiagnostic();
    tick();

    const errorTest = component.diagnostics.find(d => d.label === 'Erreur Diagnostic');
    expect(errorTest).toBeDefined();
    expect(errorTest?.status).toBe('error');
    expect(component.isRunning).toBeFalse();
  }));
});
