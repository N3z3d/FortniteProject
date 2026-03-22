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

  it('should call /players/catalogue with region param', () => {
    service.getPlayers({ region: 'EU' }).subscribe();

    const req = http.expectOne(
      r => r.url.includes('/players/catalogue') && r.params.get('region') === 'EU'
    );
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should call /players/catalogue/search when a search term is provided', () => {
    service.getPlayers({ search: 'bugha' }).subscribe();

    const req = http.expectOne(
      r =>
        r.url.includes('/players/catalogue/search') &&
        r.params.get('q') === 'bugha'
    );
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should map catalogue players and filter unavailable or wrong tranche locally', () => {
    service.getPlayers({ available: true, tranche: '2' }).subscribe(result => {
      expect(result).toEqual([
        jasmine.objectContaining({
          id: '2',
          username: 'Bravo',
          nickname: 'Bravo',
          region: 'NAC',
          tranche: '2',
          available: true,
          currentSeason: 2025,
        }),
      ]);
    });

    const req = http.expectOne(r => r.url.includes('/players/catalogue'));
    req.flush([
      {
        id: '1',
        nickname: 'Alpha',
        region: 'EU',
        tranche: '1',
        locked: false,
        currentSeason: 2024,
      },
      {
        id: '2',
        nickname: 'Bravo',
        region: 'NAC',
        tranche: '2',
        locked: false,
        currentSeason: 2025,
      },
      {
        id: '3',
        nickname: 'Charlie',
        region: 'EU',
        tranche: '2',
        locked: true,
        currentSeason: 2025,
      },
    ]);
  });

  it('should fallback to the local default season when the catalogue API omits it', () => {
    service.getPlayers({}).subscribe(result => {
      expect(result).toEqual([
        jasmine.objectContaining({
          id: '1',
          currentSeason: 2025,
        }),
      ]);
    });

    const req = http.expectOne(r => r.url.includes('/players/catalogue'));
    req.flush([
      {
        id: '1',
        nickname: 'Alpha',
        region: 'EU',
        tranche: '1',
        locked: false,
      },
    ]);
  });

  it('should propagate HTTP error to caller', () => {
    let errorCaught = false;
    service.getPlayers({}).subscribe({
      next: () => fail('should not emit'),
      error: () => { errorCaught = true; },
    });

    const req = http.expectOne(r => r.url.includes('/players/catalogue'));
    req.flush('error', { status: 500, statusText: 'Server Error' });

    expect(errorCaught).toBeTrue();
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

  it('should return empty array on sparkline HTTP error', () => {
    service.getSparkline('xyz').subscribe(result => {
      expect(result).toEqual([]);
    });

    const req = http.expectOne(r => r.url.includes('/players/xyz/sparkline'));
    req.flush('error', { status: 404, statusText: 'Not Found' });
  });
});
