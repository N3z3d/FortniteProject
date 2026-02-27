import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { PlayerCatalogueService } from './player-catalogue.service';
import { environment } from '../../../../environments/environment';

describe('PlayerCatalogueService', () => {
  let service: PlayerCatalogueService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), PlayerCatalogueService],
    });
    service = TestBed.inject(PlayerCatalogueService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should call /players/search with region param', () => {
    service.getPlayers({ region: 'EU' }).subscribe();

    const req = http.expectOne(r => r.url.includes('/players/search') && r.params.get('region') === 'EU');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should return empty array on HTTP error', (done) => {
    service.getPlayers({}).subscribe(result => {
      expect(result).toEqual([]);
      done();
    });

    const req = http.expectOne(r => r.url.includes('/players/search'));
    req.flush('error', { status: 500, statusText: 'Server Error' });
  });

  it('should call /players/{id}/sparkline with region and days', () => {
    service.getSparkline('abc', 'EU', 14).subscribe();

    const req = http.expectOne(r =>
      r.url.includes('/players/abc/sparkline') &&
      r.params.get('region') === 'EU' &&
      r.params.get('days') === '14'
    );
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should return empty array on sparkline HTTP error', (done) => {
    service.getSparkline('xyz').subscribe(result => {
      expect(result).toEqual([]);
      done();
    });

    const req = http.expectOne(r => r.url.includes('/players/xyz/sparkline'));
    req.flush('error', { status: 404, statusText: 'Not Found' });
  });
});
