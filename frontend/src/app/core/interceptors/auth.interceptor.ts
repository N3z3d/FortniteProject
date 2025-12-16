import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { UserContextService } from '../services/user-context.service';

const isApiRequest = (url: string): boolean => {
  if (url.startsWith('/api')) {
    return true;
  }

  const normalizedApi = (environment.apiUrl || '').replace(/\/+$/, '');
  return normalizedApi !== '' && url.startsWith(`${normalizedApi}/api`);
};

export const AuthInterceptor: HttpInterceptorFn = (request, next) => {
  const userContextService = inject(UserContextService);
  const currentUser = userContextService.getCurrentUser() || userContextService.getLastUser();
  const fallbackDevUser = !environment.production && environment.enableFallbackData
    ? environment.defaultDevUser
    : null;
  const username = (currentUser?.username || fallbackDevUser || '').trim();

  // Leave non-API calls or anonymous users untouched
  if (!username || !isApiRequest(request.url)) {
    return next(request);
  }

  const params = request.params.has('user')
    ? request.params
    : request.params.set('user', username);

  const modifiedRequest = request.clone({
    params,
    setHeaders: {
      'X-Test-User': username
    }
  });

  return next(modifiedRequest);
};
