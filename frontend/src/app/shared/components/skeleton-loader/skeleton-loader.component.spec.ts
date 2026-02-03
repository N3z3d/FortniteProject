import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { SkeletonLoaderComponent, SkeletonType } from './skeleton-loader.component';

describe('SkeletonLoaderComponent', () => {
  let component: SkeletonLoaderComponent;
  let fixture: ComponentFixture<SkeletonLoaderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SkeletonLoaderComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(SkeletonLoaderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('default values', () => {
    it('should have default type as text', () => {
      expect(component.type).toBe('text');
    });

    it('should have default count as 1', () => {
      expect(component.count).toBe(1);
    });

    it('should have default animated as true', () => {
      expect(component.animated).toBe(true);
    });

    it('should have default size as 40', () => {
      expect(component.size).toBe(40);
    });

    it('should have default inline as false', () => {
      expect(component.inline).toBe(false);
    });
  });

  describe('text skeleton', () => {
    beforeEach(() => {
      component.type = 'text';
      component.count = 3;
      fixture.detectChanges();
    });

    it('should render correct number of text lines', () => {
      const textElements = fixture.debugElement.queryAll(By.css('.skeleton-text'));
      expect(textElements.length).toBe(3);
    });

    it('should apply animated class when animated is true', () => {
      const textElements = fixture.debugElement.queryAll(By.css('.skeleton-text.animated'));
      expect(textElements.length).toBe(3);
    });

    it('should not apply animated class when animated is false', () => {
      component.animated = false;
      fixture.detectChanges();
      const animatedElements = fixture.debugElement.queryAll(By.css('.skeleton-text.animated'));
      expect(animatedElements.length).toBe(0);
    });
  });

  describe('title skeleton', () => {
    beforeEach(() => {
      component.type = 'title';
      fixture.detectChanges();
    });

    it('should render title skeleton', () => {
      const titleElement = fixture.debugElement.query(By.css('.skeleton-title'));
      expect(titleElement).toBeTruthy();
    });
  });

  describe('avatar skeleton', () => {
    beforeEach(() => {
      component.type = 'avatar';
      component.size = 60;
      fixture.detectChanges();
    });

    it('should render avatar skeleton', () => {
      const avatarElement = fixture.debugElement.query(By.css('.skeleton-avatar'));
      expect(avatarElement).toBeTruthy();
    });

    it('should apply custom size', () => {
      const avatarElement = fixture.debugElement.query(By.css('.skeleton-avatar'));
      expect(avatarElement.styles['width']).toBe('60px');
      expect(avatarElement.styles['height']).toBe('60px');
    });
  });

  describe('thumbnail skeleton', () => {
    beforeEach(() => {
      component.type = 'thumbnail';
      component.width = 300;
      component.height = 200;
      fixture.detectChanges();
    });

    it('should render thumbnail skeleton', () => {
      const thumbnailElement = fixture.debugElement.query(By.css('.skeleton-thumbnail'));
      expect(thumbnailElement).toBeTruthy();
    });

    it('should apply custom dimensions', () => {
      const thumbnailElement = fixture.debugElement.query(By.css('.skeleton-thumbnail'));
      expect(thumbnailElement.styles['width']).toBe('300px');
      expect(thumbnailElement.styles['height']).toBe('200px');
    });

    it('should use default dimensions when not specified', () => {
      component.width = undefined;
      component.height = undefined;
      fixture.detectChanges();
      const thumbnailElement = fixture.debugElement.query(By.css('.skeleton-thumbnail'));
      expect(thumbnailElement.styles['width']).toBe('200px');
      expect(thumbnailElement.styles['height']).toBe('120px');
    });
  });

  describe('card skeleton', () => {
    beforeEach(() => {
      component.type = 'card';
      fixture.detectChanges();
    });

    it('should render card skeleton', () => {
      const cardElement = fixture.debugElement.query(By.css('.skeleton-card'));
      expect(cardElement).toBeTruthy();
    });

    it('should contain header and body', () => {
      const headerElement = fixture.debugElement.query(By.css('.skeleton-card-header'));
      const bodyElement = fixture.debugElement.query(By.css('.skeleton-card-body'));
      expect(headerElement).toBeTruthy();
      expect(bodyElement).toBeTruthy();
    });

    it('should contain text lines in body', () => {
      const textElements = fixture.debugElement.queryAll(By.css('.skeleton-card-body .skeleton-text'));
      expect(textElements.length).toBe(3);
    });
  });

  describe('button skeleton', () => {
    beforeEach(() => {
      component.type = 'button';
      fixture.detectChanges();
    });

    it('should render button skeleton', () => {
      const buttonElement = fixture.debugElement.query(By.css('.skeleton-button'));
      expect(buttonElement).toBeTruthy();
    });

    it('should apply custom width', () => {
      component.width = 200;
      fixture.detectChanges();
      const buttonElement = fixture.debugElement.query(By.css('.skeleton-button'));
      expect(buttonElement.styles['width']).toBe('200px');
    });

    it('should use default width when not specified', () => {
      component.width = undefined;
      fixture.detectChanges();
      const buttonElement = fixture.debugElement.query(By.css('.skeleton-button'));
      expect(buttonElement.styles['width']).toBe('120px');
    });
  });

  describe('chip skeleton', () => {
    beforeEach(() => {
      component.type = 'chip';
      fixture.detectChanges();
    });

    it('should render chip skeleton', () => {
      const chipElement = fixture.debugElement.query(By.css('.skeleton-chip'));
      expect(chipElement).toBeTruthy();
    });
  });

  describe('lines getter', () => {
    it('should return array with correct length', () => {
      component.count = 5;
      expect(component.lines.length).toBe(5);
    });

    it('should return array with indices', () => {
      component.count = 3;
      expect(component.lines).toEqual([0, 1, 2]);
    });
  });

  describe('containerClass getter', () => {
    it('should return empty string when inline is false', () => {
      component.inline = false;
      expect(component.containerClass).toBe('');
    });

    it('should return inline when inline is true', () => {
      component.inline = true;
      expect(component.containerClass).toBe('inline');
    });
  });

  describe('getLineWidth', () => {
    it('should return varying widths based on index', () => {
      expect(component.getLineWidth(0)).toBe('100%');
      expect(component.getLineWidth(1)).toBe('85%');
      expect(component.getLineWidth(2)).toBe('70%');
      expect(component.getLineWidth(3)).toBe('90%');
      expect(component.getLineWidth(4)).toBe('60%');
    });

    it('should cycle through widths for larger indices', () => {
      expect(component.getLineWidth(5)).toBe('100%');
      expect(component.getLineWidth(6)).toBe('85%');
    });
  });

  describe('inline container', () => {
    it('should apply inline class when inline is true', () => {
      component.inline = true;
      fixture.detectChanges();
      const container = fixture.debugElement.query(By.css('.skeleton-container.inline'));
      expect(container).toBeTruthy();
    });

    it('should not apply inline class when inline is false', () => {
      component.inline = false;
      fixture.detectChanges();
      const container = fixture.debugElement.query(By.css('.skeleton-container:not(.inline)'));
      expect(container).toBeTruthy();
    });
  });
});
