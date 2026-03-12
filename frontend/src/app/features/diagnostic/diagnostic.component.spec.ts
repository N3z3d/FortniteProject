import { ComponentFixture, TestBed } from '@angular/core/testing';
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
    http.get.and.returnValue(of({ status: 'UP' }));

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

  it('should run diagnostic on init', () => {
    spyOn(component, 'runDiagnostic').and.returnValue(Promise.resolve());

    fixture.detectChanges();

    expect(component.runDiagnostic).toHaveBeenCalled();
  });

  it('should set isRunning to true when diagnostic starts', async () => {
    http.get.and.returnValue(of({ status: 'UP' }));

    const promise = component.runDiagnostic();

    expect(component.isRunning).toBeTrue();

    await promise;

    expect(component.isRunning).toBeFalse();
  });

  it('should test frontend environment', async () => {
    const originalProduction = environment.production;
    environment.production = false;
    http.get.and.returnValue(of({ status: 'UP' }));

    await component.runDiagnostic();

    const envTest = component.diagnostics.find(d => d.label === 'Environment Frontend');
    expect(envTest).toBeDefined();
    expect(envTest?.value).toBe('Développement');
    expect(envTest?.status).toBe('success');

    environment.production = originalProduction;
  });

  it('should test API base URL configured', async () => {
    const originalApiUrl = environment.apiBaseUrl;
    environment.apiBaseUrl = 'http://localhost:8080';
    http.get.and.returnValue(of({ status: 'UP' }));

    await component.runDiagnostic();

    const apiTest = component.diagnostics.find(d => d.label === 'URL API configurée');
    expect(apiTest).toBeDefined();
    expect(apiTest?.value).toBe('http://localhost:8080');
    expect(apiTest?.status).toBe('success');

    environment.apiBaseUrl = originalApiUrl;
  });

  it('should test local storage availability', async () => {
    http.get.and.returnValue(of({ status: 'UP' }));

    await component.runDiagnostic();

    const storageTest = component.diagnostics.find(d => d.label === 'Local Storage');
    expect(storageTest).toBeDefined();
    expect(storageTest?.value).toBe('Fonctionnel');
    expect(storageTest?.status).toBe('success');
  });

  it('should test session storage availability', async () => {
    http.get.and.returnValue(of({ status: 'UP' }));

    await component.runDiagnostic();

    const sessionTest = component.diagnostics.find(d => d.label === 'Session Storage');
    expect(sessionTest).toBeDefined();
    expect(sessionTest?.value).toBe('Fonctionnel');
    expect(sessionTest?.status).toBe('success');
  });

  it('should test backend connectivity success', async () => {
    const originalApiUrl = environment.apiBaseUrl;
    environment.apiBaseUrl = 'http://localhost:8080';
    http.get.and.returnValue(of({ status: 'UP' }));

    await component.runDiagnostic();

    const backendTest = component.diagnostics.find(d => d.label === 'Connectivité Backend');
    expect(backendTest).toBeDefined();
    expect(backendTest?.value).toBe('Connecté');
    expect(backendTest?.status).toBe('success');

    environment.apiBaseUrl = originalApiUrl;
  });

  it('should test backend connectivity failure', async () => {
    const originalApiUrl = environment.apiBaseUrl;
    environment.apiBaseUrl = 'http://localhost:8080';
    http.get.and.returnValue(throwError(() => new Error('Connection refused')));

    await component.runDiagnostic();

    const backendTest = component.diagnostics.find(d => d.label === 'Connectivité Backend');
    expect(backendTest).toBeDefined();
    expect(backendTest?.value).toBe('Non disponible');
    expect(backendTest?.status).toBe('warning');

    environment.apiBaseUrl = originalApiUrl;
  });

  it('should test browser features', async () => {
    http.get.and.returnValue(of({ status: 'UP' }));

    await component.runDiagnostic();

    const browserTest = component.diagnostics.find(d => d.label === 'Fonctionnalités Navigateur');
    expect(browserTest).toBeDefined();
    expect(browserTest?.status).toBe('success');
    expect(browserTest?.details).toContain('Fetch API');
  });

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

  it('should count success diagnostics', async () => {
    http.get.and.returnValue(of({ status: 'UP' }));

    await component.runDiagnostic();

    const count = component.getSuccessCount();
    expect(count).toBeGreaterThan(0);
  });

  it('should count warning diagnostics', async () => {
    const originalApiUrl = environment.apiBaseUrl;
    environment.apiBaseUrl = 'http://localhost:8080';
    http.get.and.returnValue(throwError(() => new Error('Connection refused')));

    await component.runDiagnostic();

    const count = component.getWarningCount();
    expect(count).toBeGreaterThanOrEqual(1);

    environment.apiBaseUrl = originalApiUrl;
  });

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

  it('should handle unexpected errors during diagnostic', async () => {
    spyOn(component as any, 'testLocalStorage').and.throwError('Unexpected error');

    await component.runDiagnostic();

    const errorTest = component.diagnostics.find(d => d.label === 'Erreur Diagnostic');
    expect(errorTest).toBeDefined();
    expect(errorTest?.status).toBe('error');
    expect(component.isRunning).toBeFalse();
  });
});
