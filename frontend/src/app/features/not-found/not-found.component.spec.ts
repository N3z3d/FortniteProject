import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Location } from '@angular/common';
import { NotFoundComponent } from './not-found.component';

describe('NotFoundComponent', () => {
  let component: NotFoundComponent;
  let fixture: ComponentFixture<NotFoundComponent>;
  let router: jasmine.SpyObj<Router>;
  let location: jasmine.SpyObj<Location>;

  beforeEach(async () => {
    router = jasmine.createSpyObj('Router', ['navigate']);
    location = jasmine.createSpyObj('Location', ['back']);

    await TestBed.configureTestingModule({
      imports: [NotFoundComponent],
      providers: [
        { provide: Router, useValue: router },
        { provide: Location, useValue: location }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(NotFoundComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display 404 error number', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const errorNumber = compiled.querySelector('.error-number');

    expect(errorNumber).toBeTruthy();
    expect(errorNumber?.textContent).toBe('404');
  });

  it('should display error message', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const title = compiled.querySelector('mat-card-title');

    expect(title).toBeTruthy();
    expect(title?.textContent).toContain('Page introuvable');
  });

  it('should display helpful links section', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const helpfulLinks = compiled.querySelector('.helpful-links');

    expect(helpfulLinks).toBeTruthy();
    expect(helpfulLinks?.textContent).toContain('Que souhaitez-vous faire');
  });

  it('should have home button', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const buttons = compiled.querySelectorAll('button');
    const homeButton = Array.from(buttons).find(b => b.textContent?.includes('Accueil'));

    expect(homeButton).toBeTruthy();
  });

  it('should have games button', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const buttons = compiled.querySelectorAll('button');
    const gamesButton = Array.from(buttons).find(b => b.textContent?.includes('Mes Jeux'));

    expect(gamesButton).toBeTruthy();
  });

  it('should have back button', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const buttons = compiled.querySelectorAll('button');
    const backButton = Array.from(buttons).find(b => b.textContent?.includes('Page précédente'));

    expect(backButton).toBeTruthy();
  });

  it('should display fun stats section', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const funStats = compiled.querySelector('.fun-stats');

    expect(funStats).toBeTruthy();
  });

  it('should display player stats', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const statItems = compiled.querySelectorAll('.stat-item');

    expect(statItems.length).toBe(3);
  });

  it('should navigate to home on goHome', () => {
    component.goHome();

    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });

  it('should navigate to games on goToGames', () => {
    component.goToGames();

    expect(router.navigate).toHaveBeenCalledWith(['/games']);
  });

  it('should go back in history when available', () => {
    const originalLength = window.history.length;
    (window.history as any).length = 5;

    component.goBack();

    expect(location.back).toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();

    (window.history as any).length = originalLength;
  });

  it('should navigate to games when no history', () => {
    const originalLength = window.history.length;
    (window.history as any).length = 1;

    component.goBack();

    expect(router.navigate).toHaveBeenCalledWith(['/games']);
    expect(location.back).not.toHaveBeenCalled();

    (window.history as any).length = originalLength;
  });

  it('should have gradient background', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const container = compiled.querySelector('.not-found-container') as HTMLElement;

    expect(container).toBeTruthy();
    const background = getComputedStyle(container).background;
    expect(background).toBeDefined();
  });

  it('should display all error reasons', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const list = compiled.querySelector('.error-details ul');
    const items = list?.querySelectorAll('li');

    expect(items?.length).toBe(4);
  });

  it('should have responsive design classes', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const container = compiled.querySelector('.not-found-container');
    const content = compiled.querySelector('.not-found-content');

    expect(container).toBeTruthy();
    expect(content).toBeTruthy();
  });
});
