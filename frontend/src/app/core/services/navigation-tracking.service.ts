import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class NavigationTrackingService {
  private readonly endpointUrl = `${environment.apiUrl}/api/analytics/navigation`;

  constructor(private readonly http: HttpClient) {}

  trackNavigation(url: string): Observable<void> {
    const normalizedPath = this.normalizePath(url);
    if (!normalizedPath) {
      return of(void 0);
    }
    return this.http
      .post(this.endpointUrl, { path: normalizedPath })
      .pipe(map(() => void 0));
  }

  private normalizePath(rawUrl: string): string | null {
    if (!rawUrl) {
      return null;
    }
    const trimmedUrl = rawUrl.trim();
    if (!trimmedUrl.startsWith('/')) {
      return null;
    }
    const withoutHash = trimmedUrl.split('#')[0];
    const withoutQuery = withoutHash.split('?')[0];
    return withoutQuery || null;
  }
}
