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

export const AuthInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  if (!token || !isApiRequest(request.url)) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    })
  );
};
