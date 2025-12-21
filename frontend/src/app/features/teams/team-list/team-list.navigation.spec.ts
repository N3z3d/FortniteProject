import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { TeamList } from './team-list';
import { TeamService, TeamDto } from '../../../core/services/team.service';
import { LoggerService } from '../../../core/services/logger.service';

describe('TeamList - Navigation (JIRA-7A Fix)', () => {
  let component: TeamList;
  let fixture: ComponentFixture<TeamList>;
  let mockRouter: jasmine.SpyObj<Router>;
  let mockTeamService: jasmine.SpyObj<TeamService>;
  let mockLogger: jasmine.SpyObj<LoggerService>;

  const mockTeam: TeamDto = {
    id: 'team-123',
    name: 'Test Team',
    season: 2025,
    ownerUsername: 'testuser',
    totalScore: 1000,
    players: [],
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };

  beforeEach(async () => {
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);
    mockTeamService = jasmine.createSpyObj('TeamService', ['getAllTeamsForSeason']);
    mockLogger = jasmine.createSpyObj('LoggerService', ['debug', 'info', 'error']);

    await TestBed.configureTestingModule({
      imports: [TeamList],
      providers: [
        { provide: Router, useValue: mockRouter },
        { provide: TeamService, useValue: mockTeamService },
        { provide: LoggerService, useValue: mockLogger }
      ]
    }).compileComponents();

    mockTeamService.getAllTeamsForSeason.and.returnValue(of([mockTeam]));
    fixture = TestBed.createComponent(TeamList);
    component = fixture.componentInstance;
  });

  describe('viewTeamDetails', () => {
    it('should navigate to team detail page with correct ID', () => {
      // Arrange
      const teamId = 'team-123';

      // Act
      component.viewTeamDetails(teamId);

      // Assert
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/teams/detail', teamId]);
    });

    it('should log navigation attempt', () => {
      // Arrange
      const teamId = 'team-456';

      // Act
      component.viewTeamDetails(teamId);

      // Assert
      expect(mockLogger.info).toHaveBeenCalledWith(
        'TeamList: navigating to team details',
        { teamId }
      );
    });

    it('should handle navigation with different team IDs', () => {
      // Arrange
      const teamIds = ['team-1', 'team-2', 'team-3'];

      // Act & Assert
      teamIds.forEach(id => {
        component.viewTeamDetails(id);
        expect(mockRouter.navigate).toHaveBeenCalledWith(['/teams/detail', id]);
      });

      expect(mockRouter.navigate).toHaveBeenCalledTimes(teamIds.length);
    });
  });

  describe('manageTeam', () => {
    it('should navigate to team edit page with correct ID', () => {
      // Arrange
      const teamId = 'team-789';

      // Act
      component.manageTeam(teamId);

      // Assert
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/teams/edit', teamId]);
    });

    it('should log edit navigation attempt', () => {
      // Arrange
      const teamId = 'team-999';

      // Act
      component.manageTeam(teamId);

      // Assert
      expect(mockLogger.info).toHaveBeenCalledWith(
        'TeamList: navigating to team edit',
        { teamId }
      );
    });
  });

  describe('Button Integration', () => {
    it('should have viewTeamDetails method accessible from template', () => {
      expect(component.viewTeamDetails).toBeDefined();
      expect(typeof component.viewTeamDetails).toBe('function');
    });

    it('should have manageTeam method accessible from template', () => {
      expect(component.manageTeam).toBeDefined();
      expect(typeof component.manageTeam).toBe('function');
    });
  });

  describe('Edge Cases', () => {
    it('should handle empty team ID gracefully', () => {
      // Act
      component.viewTeamDetails('');

      // Assert
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/teams/detail', '']);
      expect(mockLogger.info).toHaveBeenCalled();
    });

    it('should handle special characters in team ID', () => {
      // Arrange
      const specialId = 'team-@#$%';

      // Act
      component.viewTeamDetails(specialId);

      // Assert
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/teams/detail', specialId]);
    });
  });
});
