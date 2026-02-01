import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class BrowserNavigationService {
  reload(): void {
    window.location.reload();
  }

  navigateHome(): void {
    window.location.assign('/');
  }
}
