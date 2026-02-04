import { TestBed } from '@angular/core/testing';
import { BrowserNavigationService } from './browser-navigation.service';

describe('BrowserNavigationService', () => {
  let service: BrowserNavigationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [BrowserNavigationService]
    });
    service = TestBed.inject(BrowserNavigationService);
  });

  describe('service creation', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });

    it('should be a singleton', () => {
      const service2 = TestBed.inject(BrowserNavigationService);
      expect(service).toBe(service2);
    });
  });

  describe('reload', () => {
    it('should have reload method', () => {
      expect(service.reload).toBeDefined();
      expect(typeof service.reload).toBe('function');
    });
  });

  describe('navigateHome', () => {
    it('should have navigateHome method', () => {
      expect(service.navigateHome).toBeDefined();
      expect(typeof service.navigateHome).toBe('function');
    });
  });
});
