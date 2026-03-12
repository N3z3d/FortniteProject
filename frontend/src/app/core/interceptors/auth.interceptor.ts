import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';

const trimTrailingSlashes = (value: string): string => {
  let normalizedValue = value;
  while (normalizedValue.endsWith('/')) {
    normalizedValue = normalizedValue.slice(0, -1);
  }
  return normalizedValue;
};

const isApiRequest = (url: string): boolean => {
  if (url.startsWith('/api')) {
    return true;
  }

  const normalizedApi = trimTrailingSlashes(environment.apiUrl || '');
  return normalizedApi !== '' && url.startsWith(`${normalizedApi}/api`);
};

const E2E_TOKEN_PREFIX = 'e2e.';

const getSeededUsername = (): string | null => {
  const rawUser = sessionStorage.getItem('currentUser');
  if (!rawUser) {
    return null;
  }

  try {
    const currentUser = JSON.parse(rawUser) as { username?: string };
    return currentUser.username?.trim() || null;
  } catch {
    return null;
  }
};

export const AuthInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  if (!token || !isApiRequest(request.url)) {
    return next(request);
  }

  if (!environment.production && token.startsWith(E2E_TOKEN_PREFIX)) {
    const seededUsername = getSeededUsername();
    if (!seededUsername) {
      return next(request);
    }

    return next(
      request.clone({
        setHeaders: { 'X-Test-User': seededUsername }
      })
    );
  }

  return next(
    request.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    })
  );
};
