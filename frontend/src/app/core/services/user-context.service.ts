import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, map } from 'rxjs';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

export interface UserProfile {
  id: string;
  username: string;
  email: string;
  role?: string;
  lastLoginDate?: Date;
  browserFingerprint?: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserContextService {
  private readonly STORAGE_KEY = 'currentUser';
  private readonly LAST_USER_KEY = 'lastUser';
  private readonly AUTO_LOGIN_KEY = 'autoLogin';
  private readonly userChangedSubject = new BehaviorSubject<UserProfile | null>(null);

  public readonly userChanged$: Observable<UserProfile | null> =
    this.userChangedSubject.asObservable();

  constructor(private readonly authService: AuthService) {
    this.initializeFromStorage();
  }

  getAvailableProfiles(): UserProfile[] {
    return [
      {
        id: '1',
        username: 'admin',
        email: 'admin@fortnite-pronos.com',
        role: 'Administrateur'
      },
      {
        id: '2',
        username: 'thibaut',
        email: 'thibaut@fortnite-pronos.com',
        role: 'Joueur'
      },
      {
        id: '3',
        username: 'marcel',
        email: 'marcel@fortnite-pronos.com',
        role: 'Joueur'
      },
      {
        id: '4',
        username: 'teddy',
        email: 'teddy@fortnite-pronos.com',
        role: 'Joueur'
      }
    ];
  }

  getCurrentUser(): UserProfile | null {
    const storedUser = sessionStorage.getItem(this.STORAGE_KEY);
    return storedUser ? JSON.parse(storedUser) : null;
  }

  login(user: UserProfile): Observable<void> {
    const password = environment.devUserPassword ?? '';
    return this.authService.login(user.username, password).pipe(
      map(() => {
        const enrichedUser = {
          ...user,
          lastLoginDate: new Date(),
          browserFingerprint: this.generateBrowserFingerprint()
        };
        sessionStorage.setItem(this.STORAGE_KEY, JSON.stringify(enrichedUser));
        localStorage.setItem(this.LAST_USER_KEY, JSON.stringify(enrichedUser));
        localStorage.setItem(this.AUTO_LOGIN_KEY, 'true');
        this.userChangedSubject.next(enrichedUser);
      })
    );
  }

  logout(): void {
    this.authService.clearToken();
    this.clearAllStorage();
    this.userChangedSubject.next(null);
  }

  private clearAllStorage(): void {
    sessionStorage.removeItem(this.STORAGE_KEY);
    localStorage.removeItem(this.LAST_USER_KEY);
    localStorage.removeItem(this.AUTO_LOGIN_KEY);
  }

  isLoggedIn(): boolean {
    return this.authService.getToken() !== null;
  }

  isAdmin(): boolean {
    const jwtUser = this.authService.getStoredUser();
    if (jwtUser) {
      return jwtUser.role === 'ADMIN';
    }
    // Fallback to profile role for sessions started before JWT migration
    return this.getCurrentUser()?.role === 'Administrateur';
  }

  getUserById(id: string): UserProfile | undefined {
    return this.getAvailableProfiles().find(profile => profile.id === id);
  }

  getUserByUsername(username: string): UserProfile | undefined {
    return this.getAvailableProfiles().find(profile => profile.username === username);
  }

  private initializeFromStorage(): void {
    const currentUser = this.getCurrentUser();
    if (currentUser) {
      this.userChangedSubject.next(currentUser);
    }
  }

  isAutoLoginEnabled(): boolean {
    return localStorage.getItem(this.AUTO_LOGIN_KEY) === 'true';
  }

  getLastUser(): UserProfile | null {
    const lastUser = localStorage.getItem(this.LAST_USER_KEY);
    return lastUser ? JSON.parse(lastUser) : null;
  }

  attemptAutoLogin(): UserProfile | null {
    if (!this.isAutoLoginEnabled()) {
      return null;
    }

    const lastUser = this.getLastUser();
    if (!lastUser) {
      return null;
    }

    const currentFingerprint = this.generateBrowserFingerprint();
    if (lastUser.browserFingerprint === currentFingerprint) {
      // Subscribe to trigger the HTTP call (fire-and-forget — caller navigates optimistically)
      this.login(lastUser).subscribe({ error: () => {} });
      return lastUser;
    }

    return null;
  }

  private generateBrowserFingerprint(): string {
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');
    if (ctx) {
      ctx.textBaseline = 'top';
      ctx.font = '14px Arial';
      ctx.fillText('Browser fingerprint', 2, 2);
    }

    const fingerprint = [
      navigator.userAgent,
      navigator.language,
      screen.width + 'x' + screen.height,
      screen.colorDepth,
      new Date().getTimezoneOffset(),
      canvas.toDataURL()
    ].join('|');

    let hash = 0;
    for (let i = 0; i < fingerprint.length; i++) {
      const char = fingerprint.charCodeAt(i);
      hash = (hash << 5) - hash + char;
      hash &= hash;
    }

    return hash.toString(36);
  }
}
