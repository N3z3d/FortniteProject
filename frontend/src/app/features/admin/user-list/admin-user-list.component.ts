import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { Subject, takeUntil } from 'rxjs';

import { AdminUserEntry, UserRole } from '../models/admin.models';
import { AdminService } from '../services/admin.service';

type RoleFilter = 'ALL' | UserRole;
type StatusFilter = 'ALL' | 'ACTIVE' | 'DELETED';

@Component({
  selector: 'app-admin-user-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule
  ],
  templateUrl: './admin-user-list.component.html',
  styleUrl: './admin-user-list.component.scss'
})
export class AdminUserListComponent implements OnInit, OnDestroy {
  private readonly adminService = inject(AdminService);
  private readonly destroy$ = new Subject<void>();

  readonly loading = signal(false);
  readonly allUsers = signal<AdminUserEntry[]>([]);
  loadError = false;

  readonly searchControl = new FormControl('');
  readonly roleFilter = new FormControl<RoleFilter>('ALL');
  readonly statusFilter = new FormControl<StatusFilter>('ALL');

  readonly displayedColumns = ['username', 'email', 'role', 'season', 'status'];

  get filteredUsers(): AdminUserEntry[] {
    const search = (this.searchControl.value ?? '').toLowerCase().trim();
    const role = this.roleFilter.value ?? 'ALL';
    const status = this.statusFilter.value ?? 'ALL';

    return this.allUsers().filter(u => {
      const matchesSearch =
        !search ||
        u.username.toLowerCase().includes(search) ||
        u.email.toLowerCase().includes(search);
      const matchesRole = role === 'ALL' || u.role === role;
      const matchesStatus =
        status === 'ALL' ||
        (status === 'ACTIVE' && !u.deleted) ||
        (status === 'DELETED' && u.deleted);
      return matchesSearch && matchesRole && matchesStatus;
    });
  }

  ngOnInit(): void {
    this.loadUsers();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadUsers(): void {
    this.loading.set(true);
    this.loadError = false;
    this.adminService
      .getUsers()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: data => {
          this.allUsers.set(data);
          this.loading.set(false);
        },
        error: () => {
          this.loadError = true;
          this.loading.set(false);
        }
      });
  }
}
