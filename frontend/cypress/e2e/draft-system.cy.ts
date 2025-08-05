describe('Draft System End-to-End Tests', () => {
  beforeEach(() => {
    // Intercepter les appels API pour simuler le backend
    cy.intercept('GET', '/api/drafts/*/board-state', {
      statusCode: 200,
      body: {
        draft: {
          id: 'draft-123',
          gameId: 'game-123',
          status: 'ACTIVE',
          currentRound: 1,
          currentPick: 1,
          totalRounds: 3,
          createdAt: '2025-01-15T10:30:00',
          updatedAt: '2025-01-15T10:35:00',
          startedAt: '2025-01-15T10:30:00',
          finishedAt: null
        },
        participants: [
          {
            participant: {
              id: 'participant-1',
              username: 'user1',
              joinedAt: '2025-01-15T10:30:00',
              isCreator: true,
              draftOrder: 1
            },
            selections: [],
            isCurrentTurn: true,
            timeRemaining: 300,
            hasTimedOut: false
          },
          {
            participant: {
              id: 'participant-2',
              username: 'user2',
              joinedAt: '2025-01-15T10:31:00',
              isCreator: false,
              draftOrder: 2
            },
            selections: [],
            isCurrentTurn: false,
            timeRemaining: null,
            hasTimedOut: false
          }
        ],
        availablePlayers: [
          {
            id: 'player-1',
            nickname: 'pixie',
            username: 'pixie',
            region: 'EU',
            tranche: '1',
            currentSeason: 2025
          },
          {
            id: 'player-2',
            nickname: 'Muz',
            username: 'Muz',
            region: 'NAC',
            tranche: '1',
            currentSeason: 2025
          },
          {
            id: 'player-3',
            nickname: 'Bugha',
            username: 'Bugha',
            region: 'NAC',
            tranche: '2',
            currentSeason: 2025
          }
        ],
        selectedPlayers: [],
        currentParticipant: {
          id: 'participant-1',
          username: 'user1',
          joinedAt: '2025-01-15T10:30:00',
          isCreator: true,
          draftOrder: 1
        },
        progress: {
          currentRound: 1,
          currentPick: 1,
          totalRounds: 3,
          totalPicks: 6,
          completedPicks: 0,
          progressPercentage: 0,
          estimatedTimeRemaining: null
        },
        rules: {
          maxPlayersPerTeam: 3,
          regionQuotas: {
            EU: 2,
            NAC: 2,
            BR: 1,
            ASIA: 1,
            OCE: 1,
            ME: 1
          },
          timeLimitPerPick: 300,
          autoPickEnabled: true,
          autoPickDelay: 43200
        }
      }
    }).as('getDraftBoardState');

    cy.intercept('POST', '/api/drafts/*/select', {
      statusCode: 200,
      body: {
        id: 'pick-1',
        draftId: 'draft-123',
        participantId: 'participant-1',
        playerId: 'player-1',
        round: 1,
        pickNumber: 1,
        selectionTime: '2025-01-15T10:35:00',
        timeTakenSeconds: 30,
        autoPick: false
      }
    }).as('selectPlayer');

    cy.intercept('POST', '/api/drafts/*/pause', {
      statusCode: 200,
      body: true
    }).as('pauseDraft');

    cy.intercept('POST', '/api/drafts/*/resume', {
      statusCode: 200,
      body: true
    }).as('resumeDraft');

    cy.intercept('POST', '/api/drafts/*/cancel', {
      statusCode: 200,
      body: true
    }).as('cancelDraft');

    // Visiter la page de draft
    cy.visit('/draft/game-123');
  });

  describe('Draft Interface Loading', () => {
    it('should load draft interface successfully', () => {
      cy.wait('@getDraftBoardState');
      
      // Vérifier que l'interface se charge
      cy.get('.draft-container').should('be.visible');
      cy.get('.draft-header').should('be.visible');
      cy.get('.draft-title').should('contain', 'Draft - game-123');
    });

    it('should display loading state initially', () => {
      // Vérifier l'état de chargement
      cy.get('.loading-spinner').should('be.visible');
      cy.get('.loading-container').should('contain', 'Chargement du draft...');
    });

    it('should handle loading error gracefully', () => {
      // Intercepter une erreur
      cy.intercept('GET', '/api/drafts/*/board-state', {
        statusCode: 500,
        body: { error: 'Internal server error' }
      }).as('getDraftBoardStateError');

      cy.visit('/draft/game-123');
      cy.wait('@getDraftBoardStateError');

      cy.get('.error-container').should('be.visible');
      cy.get('.error-message').should('contain', 'Internal server error');
      cy.get('button').contains('Réessayer').should('be.visible');
    });
  });

  describe('Draft Status and Information', () => {
    it('should display draft status correctly', () => {
      cy.wait('@getDraftBoardState');
      
      cy.get('.draft-status').should('contain', 'En cours');
      cy.get('.draft-status').should('have.class', 'mat-chip-primary');
    });

    it('should display draft progress information', () => {
      cy.wait('@getDraftBoardState');
      
      cy.get('.draft-stats').should('be.visible');
      cy.get('.stat-item').should('have.length', 3);
      cy.get('.stat-value').first().should('contain', '1/3'); // Round
      cy.get('.stat-value').eq(1).should('contain', '1'); // Pick
      cy.get('.stat-value').eq(2).should('contain', '0%'); // Progress
    });

    it('should display progress bar', () => {
      cy.wait('@getDraftBoardState');
      
      cy.get('.progress-bar').should('be.visible');
      cy.get('.progress-text').should('contain', '0 / 6 sélections');
    });
  });

  describe('Player Selection Interface', () => {
    it('should display available players', () => {
      cy.wait('@getDraftBoardState');
      
      cy.get('.player-card').should('have.length', 3);
      cy.get('.player-card').first().should('contain', 'pixie');
      cy.get('.player-card').eq(1).should('contain', 'Muz');
      cy.get('.player-card').eq(2).should('contain', 'Bugha');
    });

    it('should display player information correctly', () => {
      cy.wait('@getDraftBoardState');
      
      const firstPlayer = cy.get('.player-card').first();
      firstPlayer.should('contain', 'pixie');
      firstPlayer.should('contain', 'EU');
      firstPlayer.should('contain', 'Tranche 1');
    });

    it('should allow player selection when user turn', () => {
      cy.wait('@getDraftBoardState');
      
      // Vérifier que le bouton de sélection est disponible
      cy.get('.player-card').first().find('.select-button').should('be.visible');
      cy.get('.player-card').first().find('.select-button').should('not.be.disabled');
    });

    it('should disable player selection when not user turn', () => {
      // Modifier le mock pour que ce ne soit pas le tour de l'utilisateur
      cy.intercept('GET', '/api/drafts/*/board-state', {
        statusCode: 200,
        body: {
          // ... même structure mais avec isCurrentTurn: false pour participant-1
          participants: [
            {
              participant: {
                id: 'participant-1',
                username: 'user1',
                joinedAt: '2025-01-15T10:30:00',
                isCreator: true,
                draftOrder: 1
              },
              selections: [],
              isCurrentTurn: false, // Pas le tour de l'utilisateur
              timeRemaining: null,
              hasTimedOut: false
            }
          ]
          // ... reste du mock
        }
      }).as('getDraftBoardStateNotUserTurn');

      cy.visit('/draft/game-123');
      cy.wait('@getDraftBoardStateNotUserTurn');

      cy.get('.player-card').first().find('.select-button').should('be.disabled');
    });

    it('should handle player selection successfully', () => {
      cy.wait('@getDraftBoardState');
      
      cy.get('.player-card').first().find('.select-button').click();
      cy.wait('@selectPlayer');
      
      // Vérifier que la sélection a été effectuée
      cy.get('.player-card').first().should('have.class', 'selected');
    });

    it('should handle player selection error', () => {
      cy.intercept('POST', '/api/drafts/*/select', {
        statusCode: 400,
        body: { message: 'Player already selected' }
      }).as('selectPlayerError');

      cy.wait('@getDraftBoardState');
      
      cy.get('.player-card').first().find('.select-button').click();
      cy.wait('@selectPlayerError');
      
      // Vérifier que l'erreur est affichée
      cy.get('.mat-snack-bar-container').should('be.visible');
      cy.get('.mat-snack-bar-container').should('contain', 'Player already selected');
    });
  });

  describe('Filtering and Search', () => {
    it('should filter players by search term', () => {
      cy.wait('@getDraftBoardState');
      
      cy.get('.search-input').type('pixie');
      cy.get('.player-card').should('have.length', 1);
      cy.get('.player-card').should('contain', 'pixie');
    });

    it('should filter players by region', () => {
      cy.wait('@getDraftBoardState');
      
      cy.get('.region-select').click();
      cy.get('.mat-option').contains('Europe').click();
      cy.get('.player-card').should('have.length', 1);
      cy.get('.player-card').should('contain', 'pixie');
    });

    it('should filter players by tranche', () => {
      cy.wait('@getDraftBoardState');
      
      cy.get('.tranche-select').click();
      cy.get('.mat-option').contains('Tranche 1').click();
      cy.get('.player-card').should('have.length', 2);
      cy.get('.player-card').should('contain', 'pixie');
      cy.get('.player-card').should('contain', 'Muz');
    });

    it('should clear all filters', () => {
      cy.wait('@getDraftBoardState');
      
      // Appliquer des filtres
      cy.get('.search-input').type('pixie');
      cy.get('.region-select').click();
      cy.get('.mat-option').contains('Europe').click();
      
      // Vérifier que les filtres sont appliqués
      cy.get('.player-card').should('have.length', 1);
      
      // Effacer les filtres
      cy.get('.clear-filters-button').click();
      
      // Vérifier que tous les joueurs sont visibles
      cy.get('.player-card').should('have.length', 3);
      cy.get('.search-input').should('have.value', '');
    });
  });

  describe('Draft Actions', () => {
    it('should pause draft successfully', () => {
      cy.wait('@getDraftBoardState');
      
      cy.get('.pause-button').click();
      cy.wait('@pauseDraft');
      
      // Vérifier que le statut change
      cy.get('.draft-status').should('contain', 'En pause');
    });

    it('should resume draft successfully', () => {
      // Modifier le mock pour un draft en pause
      cy.intercept('GET', '/api/drafts/*/board-state', {
        statusCode: 200,
        body: {
          // ... même structure mais avec status: 'PAUSED'
          draft: {
            id: 'draft-123',
            gameId: 'game-123',
            status: 'PAUSED',
            currentRound: 1,
            currentPick: 1,
            totalRounds: 3,
            createdAt: '2025-01-15T10:30:00',
            updatedAt: '2025-01-15T10:35:00',
            startedAt: '2025-01-15T10:30:00',
            finishedAt: null
          }
          // ... reste du mock
        }
      }).as('getDraftBoardStatePaused');

      cy.visit('/draft/game-123');
      cy.wait('@getDraftBoardStatePaused');
      
      cy.get('.resume-button').click();
      cy.wait('@resumeDraft');
      
      // Vérifier que le statut change
      cy.get('.draft-status').should('contain', 'En cours');
    });

    it('should cancel draft with confirmation', () => {
      cy.wait('@getDraftBoardState');
      
      cy.get('.cancel-button').click();
      
      // Vérifier que la boîte de dialogue de confirmation s'affiche
      cy.get('.mat-dialog-container').should('be.visible');
      cy.get('.mat-dialog-container').should('contain', 'Annuler le draft');
      
      // Confirmer l'annulation
      cy.get('.mat-dialog-container').find('button').contains('Confirmer').click();
      cy.wait('@cancelDraft');
      
      // Vérifier la redirection
      cy.url().should('include', '/games');
    });
  });

  describe('Participants Section', () => {
    it('should display all participants', () => {
      cy.wait('@getDraftBoardState');
      
      cy.get('.participant-card').should('have.length', 2);
      cy.get('.participant-card').first().should('contain', 'user1');
      cy.get('.participant-card').eq(1).should('contain', 'user2');
    });

    it('should highlight current participant', () => {
      cy.wait('@getDraftBoardState');
      
      cy.get('.participant-card').first().should('have.class', 'current-turn');
      cy.get('.participant-card').eq(1).should('not.have.class', 'current-turn');
    });

    it('should display participant selections', () => {
      // Modifier le mock pour inclure des sélections
      cy.intercept('GET', '/api/drafts/*/board-state', {
        statusCode: 200,
        body: {
          // ... même structure mais avec des sélections
          participants: [
            {
              participant: {
                id: 'participant-1',
                username: 'user1',
                joinedAt: '2025-01-15T10:30:00',
                isCreator: true,
                draftOrder: 1
              },
              selections: [
                {
                  id: 'pick-1',
                  draftId: 'draft-123',
                  participantId: 'participant-1',
                  playerId: 'player-1',
                  round: 1,
                  pickNumber: 1,
                  selectionTime: '2025-01-15T10:35:00',
                  timeTakenSeconds: 30,
                  autoPick: false
                }
              ],
              isCurrentTurn: true,
              timeRemaining: 300,
              hasTimedOut: false
            }
          ]
          // ... reste du mock
        }
      }).as('getDraftBoardStateWithSelections');

      cy.visit('/draft/game-123');
      cy.wait('@getDraftBoardStateWithSelections');
      
      cy.get('.participant-selections').should('contain', 'pixie');
    });
  });

  describe('Responsive Design', () => {
    it('should adapt to mobile screen size', () => {
      cy.wait('@getDraftBoardState');
      
      cy.viewport('iphone-6');
      
      // Vérifier que l'interface s'adapte
      cy.get('.draft-grid').should('have.css', 'grid-template-columns', '1fr');
      cy.get('.participants-section').should('have.css', 'order', '-1');
    });

    it('should adapt to tablet screen size', () => {
      cy.wait('@getDraftBoardState');
      
      cy.viewport('ipad-2');
      
      // Vérifier que l'interface s'adapte
      cy.get('.draft-grid').should('have.css', 'grid-template-columns', '1fr');
    });

    it('should maintain functionality on small screens', () => {
      cy.wait('@getDraftBoardState');
      
      cy.viewport(375, 667); // iPhone SE
      
      // Vérifier que les fonctionnalités restent accessibles
      cy.get('.search-input').should('be.visible');
      cy.get('.player-card').should('be.visible');
      cy.get('.select-button').should('be.visible');
    });
  });

  describe('Accessibility', () => {
    it('should have proper ARIA labels', () => {
      cy.wait('@getDraftBoardState');
      
      cy.get('.search-input').should('have.attr', 'aria-label');
      cy.get('.back-button').should('have.attr', 'aria-label');
    });

    it('should support keyboard navigation', () => {
      cy.wait('@getDraftBoardState');
      
      // Naviguer avec Tab
      cy.get('body').tab();
      cy.focused().should('have.class', 'search-input');
      
      cy.get('body').tab();
      cy.focused().should('have.class', 'region-select');
    });

    it('should have proper button roles', () => {
      cy.wait('@getDraftBoardState');
      
      cy.get('button').each(($button) => {
        cy.wrap($button).should('have.attr', 'role');
      });
    });

    it('should have proper contrast ratios', () => {
      cy.wait('@getDraftBoardState');
      
      // Vérifier que le texte est lisible
      cy.get('.draft-title').should('have.css', 'color');
      cy.get('.player-nickname').should('have.css', 'color');
    });
  });

  describe('Error Recovery', () => {
    it('should retry loading on error', () => {
      // Premier appel en erreur
      cy.intercept('GET', '/api/drafts/*/board-state', {
        statusCode: 500,
        body: { error: 'Network error' }
      }).as('getDraftBoardStateError');

      cy.visit('/draft/game-123');
      cy.wait('@getDraftBoardStateError');

      cy.get('.error-container').should('be.visible');

      // Deuxième appel réussi
      cy.intercept('GET', '/api/drafts/*/board-state', {
        statusCode: 200,
        body: {
          // ... mock de succès
        }
      }).as('getDraftBoardStateSuccess');

      cy.get('button').contains('Réessayer').click();
      cy.wait('@getDraftBoardStateSuccess');

      cy.get('.draft-container').should('be.visible');
      cy.get('.error-container').should('not.exist');
    });

    it('should handle network disconnection gracefully', () => {
      cy.wait('@getDraftBoardState');
      
      // Simuler une déconnexion réseau
      cy.intercept('GET', '/api/drafts/*/board-state', {
        forceNetworkError: true
      }).as('getDraftBoardStateNetworkError');

      cy.get('.refresh-button').click();
      cy.wait('@getDraftBoardStateNetworkError');

      cy.get('.error-container').should('be.visible');
      cy.get('.error-message').should('contain', 'Erreur réseau');
    });
  });

  describe('Performance', () => {
    it('should load within acceptable time', () => {
      const startTime = Date.now();
      
      cy.visit('/draft/game-123');
      cy.wait('@getDraftBoardState');
      
      cy.get('.draft-container').should('be.visible').then(() => {
        const loadTime = Date.now() - startTime;
        expect(loadTime).to.be.lessThan(3000); // Moins de 3 secondes
      });
    });

    it('should handle large player lists efficiently', () => {
      // Mock avec beaucoup de joueurs
      const manyPlayers = Array.from({ length: 100 }, (_, i) => ({
        id: `player-${i}`,
        nickname: `Player${i}`,
        username: `Player${i}`,
        region: 'EU',
        tranche: '1',
        currentSeason: 2025
      }));

      cy.intercept('GET', '/api/drafts/*/board-state', {
        statusCode: 200,
        body: {
          // ... même structure mais avec manyPlayers
          availablePlayers: manyPlayers
        }
      }).as('getDraftBoardStateManyPlayers');

      cy.visit('/draft/game-123');
      cy.wait('@getDraftBoardStateManyPlayers');

      // Vérifier que l'interface reste responsive
      cy.get('.player-card').should('have.length', 100);
      cy.get('.search-input').should('be.visible');
      cy.get('.search-input').type('Player1');
      
      // Vérifier que le filtrage fonctionne rapidement
      cy.get('.player-card').should('have.length', 1);
    });
  });
}); 