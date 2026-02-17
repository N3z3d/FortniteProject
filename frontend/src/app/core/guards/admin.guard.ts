import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { UserContextService } from '../services/user-context.service';

@Injectable({
  providedIn: 'root'
})
export class AdminGuard implements CanActivate {

  constructor(
    private readonly userContextService: UserContextService,
    private readonly router: Router
  ) {}

  canActivate(): boolean {
    const user = this.userContextService.getCurrentUser();

    if (user?.role === 'Administrateur') {
      return true;
    }

    this.router.navigate(['/games']);
    return false;
  }
}
