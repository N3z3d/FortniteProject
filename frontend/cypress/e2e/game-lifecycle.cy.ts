describe('Game Lifecycle End-to-End Tests', () => {
  beforeEach(() => {
    // Visiter la page d'accueil
    cy.visit('/');
    
    // Attendre que l'application soit chargée
    cy.get('app-root').should('be.visible');
  });

  it('should complete full game lifecycle: create, join, start draft', () => {
    // 1. Naviguer vers la section games
    cy.get('[data-cy="nav-games"]').click();
    cy.url().should('include', '/games');

    // 2. Créer une nouvelle game
    cy.get('[data-cy="create-game-btn"]').click();
    cy.url().should('include', '/games/create');

    // Remplir le formulaire de création
    cy.get('[data-cy="game-name-input"]').type('Test Game E2E');
    cy.get('[data-cy="max-participants-input"]').clear().type('8');
    
    // Ajouter des règles de région
    cy.get('[data-cy="add-region-rule-btn"]').click();
    cy.get('[data-cy="region-select"]').first().select('EU');
    cy.get('[data-cy="region-quota-input"]').first().type('2');

    // Soumettre le formulaire
    cy.get('[data-cy="submit-game-btn"]').click();

    // 3. Vérifier que la game a été créée
    cy.url().should('include', '/games');
    cy.get('[data-cy="game-card"]').should('contain', 'Test Game E2E');
    cy.get('[data-cy="game-status"]').should('contain', 'En création');

    // 4. Voir les détails de la game
    cy.get('[data-cy="game-card"]').first().click();
    cy.url().should('include', '/games/');

    // Vérifier les détails
    cy.get('[data-cy="game-title"]').should('contain', 'Test Game E2E');
    cy.get('[data-cy="participant-count"]').should('contain', '1/8');
    cy.get('[data-cy="game-status-chip"]').should('contain', 'En création');

    // 5. Rejoindre la game (simuler un autre utilisateur)
    cy.get('[data-cy="join-game-btn"]').should('be.visible').click();
    
    // Vérifier le message de succès
    cy.get('[data-cy="snackbar"]').should('contain', 'Game rejoint avec succès');
    
    // Vérifier que le nombre de participants a augmenté
    cy.get('[data-cy="participant-count"]').should('contain', '2/8');

    // 6. Vérifier la liste des participants
    cy.get('[data-cy="participants-section"]').should('be.visible');
    cy.get('[data-cy="creator-item"]').should('be.visible');
    cy.get('[data-cy="participant-item"]').should('be.visible');

    // 7. Démarrer le draft
    cy.get('[data-cy="start-draft-btn"]').should('be.visible').click();
    
    // Confirmer l'action
    cy.on('window:confirm', () => true);
    
    // Vérifier le message de succès
    cy.get('[data-cy="snackbar"]').should('contain', 'Draft démarré avec succès');
    
    // Vérifier que le statut a changé
    cy.get('[data-cy="game-status-chip"]').should('contain', 'En draft');

    // 8. Vérifier que la section draft est visible
    cy.get('[data-cy="draft-section"]').should('be.visible');
    cy.get('[data-cy="draft-card"]').should('contain', 'Phase de Draft');
  });

  it('should handle game creation validation errors', () => {
    // Naviguer vers la création de game
    cy.visit('/games/create');

    // Tenter de soumettre un formulaire vide
    cy.get('[data-cy="submit-game-btn"]').click();

    // Vérifier les messages d'erreur
    cy.get('[data-cy="game-name-error"]').should('contain', 'Le nom est requis');
    cy.get('[data-cy="max-participants-error"]').should('contain', 'Le nombre de participants est requis');

    // Tenter de créer avec des valeurs invalides
    cy.get('[data-cy="game-name-input"]').type('A'); // Nom trop court
    cy.get('[data-cy="max-participants-input"]').type('1'); // Trop peu de participants

    cy.get('[data-cy="submit-game-btn"]').click();

    // Vérifier les messages d'erreur
    cy.get('[data-cy="game-name-error"]').should('contain', 'Le nom doit contenir au moins 3 caractères');
    cy.get('[data-cy="max-participants-error"]').should('contain', 'Le nombre minimum de participants est 2');
  });

  it('should handle joining a full game', () => {
    // Créer une game avec 2 participants maximum
    cy.visit('/games/create');
    cy.get('[data-cy="game-name-input"]').type('Small Game');
    cy.get('[data-cy="max-participants-input"]').clear().type('2');
    cy.get('[data-cy="submit-game-btn"]').click();

    // Voir les détails de la game
    cy.get('[data-cy="game-card"]').first().click();

    // Rejoindre la game
    cy.get('[data-cy="join-game-btn"]').click();
    cy.get('[data-cy="snackbar"]').should('contain', 'Game rejoint avec succès');

    // Vérifier que la game est maintenant complète
    cy.get('[data-cy="participant-count"]').should('contain', '2/2');
    cy.get('[data-cy="join-game-btn"]').should('not.exist');
    cy.get('[data-cy="game-full-message"]').should('contain', 'Game complète');
  });

  it('should handle game deletion', () => {
    // Créer une game
    cy.visit('/games/create');
    cy.get('[data-cy="game-name-input"]').type('Game to Delete');
    cy.get('[data-cy="max-participants-input"]').clear().type('8');
    cy.get('[data-cy="submit-game-btn"]').click();

    // Voir les détails de la game
    cy.get('[data-cy="game-card"]').first().click();

    // Supprimer la game
    cy.get('[data-cy="delete-game-btn"]').click();
    
    // Confirmer la suppression
    cy.on('window:confirm', () => true);
    
    // Vérifier le message de succès
    cy.get('[data-cy="snackbar"]').should('contain', 'Game supprimée avec succès');
    
    // Vérifier qu'on est redirigé vers la liste des games
    cy.url().should('include', '/games');
    
    // Vérifier que la game n'apparaît plus
    cy.get('[data-cy="game-card"]').should('not.contain', 'Game to Delete');
  });

  it('should handle search and filtering games', () => {
    // Créer plusieurs games avec des noms différents
    const gameNames = ['Alpha Game', 'Beta Game', 'Gamma Game'];
    
    gameNames.forEach((name, index) => {
      cy.visit('/games/create');
      cy.get('[data-cy="game-name-input"]').type(name);
      cy.get('[data-cy="max-participants-input"]').clear().type('8');
      cy.get('[data-cy="submit-game-btn"]').click();
    });

    // Naviguer vers la liste des games
    cy.visit('/games');

    // Tester la recherche
    cy.get('[data-cy="search-input"]').type('Alpha');
    cy.get('[data-cy="game-card"]').should('contain', 'Alpha Game');
    cy.get('[data-cy="game-card"]').should('not.contain', 'Beta Game');

    // Effacer la recherche
    cy.get('[data-cy="clear-filters-btn"]').click();
    cy.get('[data-cy="game-card"]').should('contain', 'Alpha Game');
    cy.get('[data-cy="game-card"]').should('contain', 'Beta Game');

    // Tester le filtrage par statut
    cy.get('[data-cy="status-filter"]').select('CREATING');
    cy.get('[data-cy="game-card"]').each(($card) => {
      cy.wrap($card).find('[data-cy="game-status"]').should('contain', 'En création');
    });
  });

  it('should handle responsive design on mobile', () => {
    // Définir la taille d'écran mobile
    cy.viewport('iphone-x');

    // Naviguer vers la création de game
    cy.visit('/games/create');

    // Vérifier que l'interface s'adapte
    cy.get('[data-cy="game-form"]').should('be.visible');
    cy.get('[data-cy="submit-game-btn"]').should('be.visible');

    // Créer une game
    cy.get('[data-cy="game-name-input"]').type('Mobile Test Game');
    cy.get('[data-cy="max-participants-input"]').clear().type('8');
    cy.get('[data-cy="submit-game-btn"]').click();

    // Vérifier que la liste s'affiche correctement
    cy.get('[data-cy="game-card"]').should('be.visible');
    
    // Vérifier que les boutons sont empilés verticalement
    cy.get('[data-cy="game-card"]').first().click();
    cy.get('[data-cy="action-buttons"]').should('have.css', 'flex-direction', 'column');
  });

  it('should handle network errors gracefully', () => {
    // Intercepter les requêtes API et simuler des erreurs
    cy.intercept('POST', '/api/games', { statusCode: 500, body: { error: 'Server error' } }).as('createGameError');
    cy.intercept('GET', '/api/games/user', { statusCode: 500, body: { error: 'Server error' } }).as('getGamesError');

    // Tenter de créer une game
    cy.visit('/games/create');
    cy.get('[data-cy="game-name-input"]').type('Error Test Game');
    cy.get('[data-cy="max-participants-input"]').clear().type('8');
    cy.get('[data-cy="submit-game-btn"]').click();

    // Vérifier que l'erreur est affichée
    cy.get('[data-cy="error-message"]').should('contain', 'Erreur');
    cy.get('[data-cy="retry-btn"]').should('be.visible');

    // Tenter de charger la liste des games
    cy.visit('/games');
    cy.get('[data-cy="error-message"]').should('contain', 'Erreur');
  });

  it('should handle loading states correctly', () => {
    // Intercepter les requêtes pour simuler des délais
    cy.intercept('GET', '/api/games/user', (req) => {
      req.reply((res) => {
        setTimeout(() => {
          res.send({ body: [] });
        }, 1000);
      });
    }).as('slowRequest');

    // Naviguer vers la liste des games
    cy.visit('/games');

    // Vérifier que le spinner de chargement est affiché
    cy.get('[data-cy="loading-spinner"]').should('be.visible');
    cy.get('[data-cy="loading-text"]').should('contain', 'Chargement');

    // Attendre que le chargement soit terminé
    cy.wait('@slowRequest');
    cy.get('[data-cy="loading-spinner"]').should('not.exist');
  });

  it('should maintain data consistency across page refreshes', () => {
    // Créer une game
    cy.visit('/games/create');
    cy.get('[data-cy="game-name-input"]').type('Consistency Test Game');
    cy.get('[data-cy="max-participants-input"]').clear().type('8');
    cy.get('[data-cy="submit-game-btn"]').click();

    // Vérifier que la game est créée
    cy.get('[data-cy="game-card"]').should('contain', 'Consistency Test Game');

    // Recharger la page
    cy.reload();

    // Vérifier que la game est toujours là
    cy.get('[data-cy="game-card"]').should('contain', 'Consistency Test Game');

    // Voir les détails et recharger
    cy.get('[data-cy="game-card"]').first().click();
    cy.get('[data-cy="game-title"]').should('contain', 'Consistency Test Game');
    
    cy.reload();
    cy.get('[data-cy="game-title"]').should('contain', 'Consistency Test Game');
  });

  it('should handle keyboard navigation and accessibility', () => {
    // Naviguer vers la liste des games
    cy.visit('/games');

    // Tester la navigation au clavier
    cy.get('body').tab();
    cy.focused().should('have.attr', 'data-cy', 'create-game-btn');

    // Naviguer vers la création de game
    cy.get('[data-cy="create-game-btn"]').click();

    // Tester la navigation dans le formulaire
    cy.get('[data-cy="game-name-input"]').should('be.focused');
    cy.get('[data-cy="game-name-input"]').type('Accessibility Test');
    cy.tab();
    cy.focused().should('have.attr', 'data-cy', 'max-participants-input');

    // Vérifier les attributs d'accessibilité
    cy.get('[data-cy="game-name-input"]').should('have.attr', 'aria-label');
    cy.get('[data-cy="submit-game-btn"]').should('have.attr', 'aria-label');
  });
}); 