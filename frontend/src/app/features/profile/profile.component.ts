import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatListModule } from '@angular/material/list';
import { UserContextService, UserProfile } from '../../core/services/user-context.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
    MatListModule
  ],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {
  currentUser: UserProfile | null = null;

  constructor(
    private userContextService: UserContextService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.currentUser = this.userContextService.getCurrentUser();
    if (!this.currentUser) {
      this.router.navigate(['/login']);
    }
  }

  editProfile(): void {
    // Navigate to edit profile page (future implementation)
    console.log('Edit profile clicked');
  }

  changePassword(): void {
    // Navigate to change password page (future implementation)
    console.log('Change password clicked');
  }

  viewStatistics(): void {
    // Navigate to statistics page (future implementation)
    console.log('View statistics clicked');
  }

  logout(): void {
    this.userContextService.logout();
    this.router.navigate(['/login']);
  }
}