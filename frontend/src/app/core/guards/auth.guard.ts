import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { UserContextService } from '../services/user-context.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  
  constructor(
    private userContextService: UserContextService,
    private router: Router
  ) {}

  canActivate(): boolean {
    const isLoggedIn = this.userContextService.isLoggedIn();
    
    if (!isLoggedIn) {
      this.router.navigate(['/login']);
      return false;
    }
    
    return true;
  }
} 