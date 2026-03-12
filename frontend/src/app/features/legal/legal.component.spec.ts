import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { LegalComponent } from './legal.component';
import { TranslationService } from '../../core/services/translation.service';

describe('LegalComponent', () => {
  let component: LegalComponent;
  let fixture: ComponentFixture<LegalComponent>;
  let translationService: jasmine.SpyObj<TranslationService>;
  let activatedRoute: ActivatedRoute;

  beforeEach(async () => {
    translationService = jasmine.createSpyObj('TranslationService', ['t']);
    translationService.t.and.callFake((key: string, fallback?: string) => fallback || key);

    await TestBed.configureTestingModule({
      imports: [LegalComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            data: of({ pageType: 'legal-notice' })
          }
        },
        { provide: TranslationService, useValue: translationService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LegalComponent);
    component = fixture.componentInstance;
    activatedRoute = TestBed.inject(ActivatedRoute);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with legal-notice from route data', () => {
    expect(component.pageType).toBe('legal-notice');
  });

  it('should get config for legal-notice', () => {
    component.pageType = 'legal-notice';
    const config = component.config;

    expect(config.icon).toBe('gavel');
    expect(config.titleKey).toBe('legal.legalNotice.title');
    expect(config.sections.length).toBe(3);
  });

  it('should get config for contact', () => {
    component.pageType = 'contact';
    const config = component.config;

    expect(config.icon).toBe('mail');
    expect(config.titleKey).toBe('legal.contact.title');
    expect(config.sections.length).toBe(2);
  });

  it('should get config for privacy', () => {
    component.pageType = 'privacy';
    const config = component.config;

    expect(config.icon).toBe('security');
    expect(config.titleKey).toBe('legal.privacy.title');
    expect(config.sections.length).toBe(3);
  });

  it('should get title with fallback for legal-notice', () => {
    component.pageType = 'legal-notice';
    const title = component.getTitle();

    expect(translationService.t).toHaveBeenCalledWith(
      'legal.legalNotice.title',
      'Mentions Legales'
    );
    expect(title).toBe('Mentions Legales');
  });

  it('should get title with fallback for contact', () => {
    component.pageType = 'contact';
    const title = component.getTitle();

    expect(translationService.t).toHaveBeenCalledWith(
      'legal.contact.title',
      'Contact'
    );
  });

  it('should get title with fallback for privacy', () => {
    component.pageType = 'privacy';
    const title = component.getTitle();

    expect(translationService.t).toHaveBeenCalledWith(
      'legal.privacy.title',
      'Politique de Confidentialite'
    );
  });

  it('should use default pageType when route data is missing', () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [LegalComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { data: of({}) }
        },
        { provide: TranslationService, useValue: translationService }
      ]
    }).compileComponents().then(() => {
      const newFixture = TestBed.createComponent(LegalComponent);
      const newComponent = newFixture.componentInstance;
      newFixture.detectChanges();

      expect(newComponent.pageType).toBe('legal-notice');
    });
  });

  it('should have lastUpdateDate defined', () => {
    expect(component.lastUpdateDate).toBe('2026-01-22');
  });
});
