import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { SnakeOrderBarComponent, SnakeParticipant } from './snake-order-bar.component';
import { ResponsiveService } from '../../../core/services/responsive.service';

const SMALL: SnakeParticipant[] = [
  { id: '1', username: 'KARIM' },
  { id: '2', username: 'THOMAS' },
  { id: '3', username: 'LUCAS' },
];

const LARGE: SnakeParticipant[] = Array.from({ length: 8 }, (_, i) => ({
  id: `${i + 1}`,
  username: `PLAYER${i + 1}`,
}));

describe('SnakeOrderBarComponent', () => {
  let component: SnakeOrderBarComponent;
  let fixture: ComponentFixture<SnakeOrderBarComponent>;
  let isMobileSignal: ReturnType<typeof signal<boolean>>;

  beforeEach(async () => {
    isMobileSignal = signal(false);

    await TestBed.configureTestingModule({
      imports: [SnakeOrderBarComponent, NoopAnimationsModule],
      providers: [
        {
          provide: ResponsiveService,
          useValue: { isMobile: isMobileSignal },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SnakeOrderBarComponent);
    component = fixture.componentInstance;
    component.participants = SMALL;
    component.currentIndex = 0;
    fixture.detectChanges();
  });

  // ===== AVATAR MODE =====

  it('should show avatar mode when ≤6 participants and not mobile', () => {
    expect(fixture.nativeElement.querySelector('.avatar-list')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.text-mode')).toBeNull();
  });

  it('should render one avatar per participant', () => {
    const avatars = fixture.nativeElement.querySelectorAll('.avatar-item');
    expect(avatars.length).toBe(3);
  });

  it('should display initials in each avatar', () => {
    const avatars = fixture.nativeElement.querySelectorAll('.avatar-item');
    expect(avatars[0].textContent.trim()).toBe('KA');
    expect(avatars[1].textContent.trim()).toBe('TH');
  });

  it('should apply active class to current participant', () => {
    const avatars = fixture.nativeElement.querySelectorAll('.avatar-item');
    expect(avatars[0].classList).toContain('avatar-item--active');
    expect(avatars[1].classList).not.toContain('avatar-item--active');
  });

  it('should update active class when currentIndex changes', () => {
    component.currentIndex = 1;
    fixture.detectChanges();

    const avatars = fixture.nativeElement.querySelectorAll('.avatar-item');
    expect(avatars[0].classList).not.toContain('avatar-item--active');
    expect(avatars[1].classList).toContain('avatar-item--active');
  });

  // ===== TEXT MODE — > 6 =====

  it('should show text mode when >6 participants', () => {
    component.participants = LARGE;
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.text-mode')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.avatar-list')).toBeNull();
  });

  it('should display current participant name in text mode', () => {
    component.participants = LARGE;
    component.currentIndex = 2;
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.text-mode').textContent).toContain('PLAYER3');
  });

  it('should display position and total in text mode', () => {
    component.participants = LARGE;
    component.currentIndex = 2;
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.text-mode').textContent).toContain('3/8');
  });

  // ===== TEXT MODE — mobile =====

  it('should show text mode when isMobile is true regardless of participant count', () => {
    isMobileSignal.set(true);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.text-mode')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.avatar-list')).toBeNull();
  });

  it('should revert to avatar mode when isMobile becomes false', () => {
    isMobileSignal.set(true);
    fixture.detectChanges();
    isMobileSignal.set(false);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.avatar-list')).not.toBeNull();
  });

  // ===== REGION LABEL =====

  it('should show region label when regionLabel is provided', () => {
    component.regionLabel = 'EU';
    fixture.detectChanges();

    const label = fixture.nativeElement.querySelector('.region-label');
    expect(label).not.toBeNull();
    expect(label.textContent.trim()).toBe('EU');
  });

  it('should not show region label when regionLabel is not provided', () => {
    expect(fixture.nativeElement.querySelector('.region-label')).toBeNull();
  });

  // ===== ARIA =====

  it('should have correct aria-label', () => {
    const bar = fixture.nativeElement.querySelector('.snake-bar');
    expect(bar.getAttribute('aria-label')).toBe('Tour de KARIM, position 1 sur 3');
  });

  it('should update aria-label when currentIndex changes', () => {
    component.currentIndex = 2;
    fixture.detectChanges();

    const bar = fixture.nativeElement.querySelector('.snake-bar');
    expect(bar.getAttribute('aria-label')).toBe('Tour de LUCAS, position 3 sur 3');
  });

  // ===== CURSOR OFFSET =====

  it('should compute cursor offset for index 0', () => {
    expect(component.cursorOffset).toBe('0px');
  });

  it('should compute cursor offset for index 2', () => {
    component.currentIndex = 2;
    expect(component.cursorOffset).toBe('80px');
  });
});
