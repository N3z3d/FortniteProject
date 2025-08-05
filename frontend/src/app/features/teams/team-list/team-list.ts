import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TeamService, TeamDto, TeamPlayerDto } from '../../../core/services/team.service';

@Component({
  selector: 'app-team-list',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './team-list.html',
  styleUrl: './team-list.scss'
})
export class TeamList implements OnInit {
  teams: TeamDto[] = [];
  loading = true;
  error: string | null = null;

  constructor(private teamService: TeamService) {}

  ngOnInit() {
    this.loadTeams();
  }

  loadTeams() {
    this.loading = true;
    this.error = null;
    
    this.teamService.getAllTeamsForSeason(2025).subscribe({
      next: (teams) => {
        this.teams = teams;
        this.loading = false;
        console.log('Équipes chargées:', teams);
      },
      error: (error) => {
        console.error('Erreur lors du chargement des équipes:', error);
        this.error = 'Erreur lors du chargement des équipes';
        this.loading = false;
      }
    });
  }

  getRegionColor(region: string): string {
    const colors: { [key: string]: string } = {
      'NAC': '#3f51b5',
      'NAW': '#e91e63',
      'EU': '#4caf50',
      'ASIA': '#ff9800',
      'OCE': '#9c27b0',
      'BR': '#f44336',
      'ME': '#607d8b'
    };
    return colors[region] || '#757575';
  }

  getUserRoleLabel(role: string): string {
    const labels: { [key: string]: string } = {
      'PARTICIPANT': 'Participant',
      'MARCEL': 'Marcel',
      'ADMIN': 'Admin'
    };
    return labels[role] || role;
  }

  getUserRoleColor(role: string): string {
    const colors: { [key: string]: string } = {
      'PARTICIPANT': 'primary',
      'MARCEL': 'accent',
      'ADMIN': 'warn'
    };
    return colors[role] || 'primary';
  }

  trackByTeam(index: number, team: TeamDto): string {
    return team.id;
  }

  trackByPlayer(index: number, player: TeamPlayerDto): string {
    return player.playerId;
  }
}
