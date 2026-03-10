import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface LoginApiResponse {
  token: string;
  refreshToken?: string;
  user: {
    id: string;
    email: string;
    role: string;
  };
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private static readonly AUTH_TOKEN_KEY = 'jwt_token';
  private static readonly AUTH_USER_KEY = 'jwt_user';
  private readonly loginUrl = `${environment.apiUrl}/api/auth/login`;

  constructor(private readonly http: HttpClient) {}

  login(username: string, password: string): Observable<LoginApiResponse> {
    return this.http
      .post<LoginApiResponse>(this.loginUrl, { username, password })
      .pipe(tap(response => this.storeToken(response.token, response.user)));
  }

  storeToken(token: string, user: LoginApiResponse['user']): void {
    sessionStorage.setItem(AuthService.AUTH_TOKEN_KEY, token);
    sessionStorage.setItem(AuthService.AUTH_USER_KEY, JSON.stringify(user));
  }

  clearToken(): void {
    sessionStorage.removeItem(AuthService.AUTH_TOKEN_KEY);
    sessionStorage.removeItem(AuthService.AUTH_USER_KEY);
  }

  getToken(): string | null {
    return sessionStorage.getItem(AuthService.AUTH_TOKEN_KEY);
  }

  getStoredUser(): LoginApiResponse['user'] | null {
    const raw = sessionStorage.getItem(AuthService.AUTH_USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as LoginApiResponse['user'];
    } catch {
      return null;
    }
  }
}
