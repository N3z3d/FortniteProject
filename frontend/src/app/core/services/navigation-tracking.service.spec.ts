import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { NavigationTrackingService } from './navigation-tracking.service';

describe('NavigationTrackingService', () => {
  let service: NavigationTrackingService;
  let httpMock: HttpTestingController;
  const endpointUrl = `${environment.apiUrl}/api/analytics/navigation`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [NavigationTrackingService]
    });

    service = TestBed.inject(NavigationTrackingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should post normalized path when url contains query and hash', () => {
    service.trackNavigation('/games/abc?created=true#info').subscribe();

    const request = httpMock.expectOne(endpointUrl);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ path: '/games/abc' });
    request.flush({});
  });

  it('should skip request for invalid url', () => {
    let completed = false;

    service.trackNavigation('games/abc').subscribe(() => {
      completed = true;
    });

    httpMock.expectNone(endpointUrl);
    expect(completed).toBeTrue();
  });
});
