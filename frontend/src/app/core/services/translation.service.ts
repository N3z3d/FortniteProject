import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export type SupportedLanguage = 'fr' | 'en';

export interface Translations {
  [key: string]: string | Translations;
}

@Injectable({
  providedIn: 'root'
})
export class TranslationService {
  private currentLang$ = new BehaviorSubject<SupportedLanguage>('fr');
  private translations: Record<SupportedLanguage, Translations> = {
    fr: {},
    en: {}
  };

  constructor() {
    this.loadTranslations();
    this.restoreLanguagePreference();
  }

  get language$(): Observable<SupportedLanguage> {
    return this.currentLang$.asObservable();
  }

  get currentLanguage(): SupportedLanguage {
    return this.currentLang$.value;
  }

  setLanguage(lang: SupportedLanguage): void {
    this.currentLang$.next(lang);
    localStorage.setItem('app_language', lang);
  }

  translate(key: string, fallback?: string): string {
    const keys = key.split('.');
    let result: string | Translations = this.translations[this.currentLang$.value];

    for (const k of keys) {
      if (result && typeof result === 'object' && k in result) {
        result = result[k];
      } else {
        return fallback ?? key; // Return key if translation not found
      }
    }

    return typeof result === 'string' ? result : (fallback ?? key);
  }

  t(key: string, fallback?: string): string {
    return this.translate(key, fallback);
  }

  private restoreLanguagePreference(): void {
    const savedLang = localStorage.getItem('app_language') as SupportedLanguage;
    if (savedLang && (savedLang === 'fr' || savedLang === 'en')) {
      this.currentLang$.next(savedLang);
    }
  }

  private loadTranslations(): void {
    this.translations = {
      fr: {
        common: {
          save: 'Enregistrer',
          saving: 'Enregistrement...',
          and: 'et',
          cancel: 'Annuler',
          edit: 'Modifier',
          editing: 'Modification...',
          delete: 'Supprimer',
          confirm: 'Confirmer',
          loading: 'Chargement...',
          error: 'Erreur',
          success: 'Succès',
          yes: 'Oui',
          no: 'Non',
          back: 'Retour',
          next: 'Suivant',
          close: 'Fermer',
          search: 'Rechercher',
          noData: 'Aucune donnée',
          actions: 'Actions',
          details: 'Détails',
          players: 'Joueurs',
          player: 'Joueur'
        },
        layout: {
          welcome: 'Bienvenue'
        },
        auth: {
          login: 'Connexion',
          logout: 'Déconnexion',
          username: 'Nom d\'utilisateur',
          password: 'Mot de passe',
          email: 'Email',
          register: 'S\'inscrire',
          forgotPassword: 'Mot de passe oublié ?',
          rememberMe: 'Se souvenir de moi'
        },
        profile: {
          title: 'Profil',
          profileInformation: 'Informations du profil',
          accountActions: 'Actions du compte',
          username: 'Nom d\'utilisateur',
          role: 'Rôle',
          memberSince: 'Membre depuis',
          recentlyJoined: 'Récemment inscrit',
          editProfile: 'Modifier le profil',
          changePassword: 'Changer le mot de passe',
          viewStatistics: 'Voir les statistiques',
          noEmailProvided: 'Aucun email renseigné'
        },
        settings: {
          title: 'Paramètres',
          language: 'Langue',
          theme: 'Thème',
          themeDesc: 'Choisissez votre thème préféré',
          darkMode: 'Mode sombre',
          lightMode: 'Mode clair',
          systemDefault: 'Défaut système',
          notifications: 'Notifications',
          emailNotifications: 'Notifications par email',
          emailNotificationsDesc: 'Recevoir les mises à jour par email',
          pushNotifications: 'Notifications push',
          pushNotificationsDesc: 'Notifications push du navigateur',
          gameReminders: 'Rappels de parties',
          gameRemindersDesc: 'Me rappeler les parties à venir',
          draftAlerts: 'Alertes de draft',
          draftAlertsDesc: 'Me notifier quand c\'est mon tour de drafter',
          tradeNotifications: 'Notifications d\'échanges',
          tradeNotificationsDesc: 'M\'alerter des offres d\'échange',
          privacy: 'Confidentialité',
          privacyData: 'Confidentialité et données',
          account: 'Compte',
          general: 'Général',
          appearance: 'Apparence',
          selectLanguage: 'Sélectionner la langue',
          french: 'Français',
          english: 'Anglais',
          gamePreferences: 'Préférences de jeu',
          autoJoinDraft: 'Rejoindre le draft automatiquement',
          autoJoinDraftDesc: 'Rejoindre automatiquement le draft quand il commence',
          showOnlineStatus: 'Afficher le statut en ligne',
          showOnlineStatusDesc: 'Permettre aux autres de voir quand je suis en ligne',
          exportData: 'Exporter mes données',
          deleteAccount: 'Supprimer le compte',
          resetDefaults: 'Réinitialiser',
          settingsSaved: 'Paramètres enregistrés !',
          settingsReset: 'Paramètres réinitialisés'
        },
        navigation: {
          home: 'Accueil',
          dashboard: 'Tableau de bord',
          games: 'Parties',
          teams: 'Équipes',
          leaderboard: 'Classement',
          draft: 'Draft',
          trades: 'Échanges',
          profile: 'Profil',
          settings: 'Paramètres'
        },
        dashboard: {
          loading: 'Chargement du tableau de bord...',
          errorTitle: 'Erreur de chargement',
          retry: 'Réessayer',
          headings: {
            generalStatistics: 'Statistiques générales',
            chartsAndAnalytics: 'Graphiques et analyses',
            leaderboardPreview: 'Top 5 équipes - classement',
            centralNavigation: 'Navigation centrale',
            navigationHint: 'Accès rapide aux fonctionnalités'
          },
          labels: {
            team: 'Équipe',
            teams: 'Équipes',
            activeTeams: 'Équipes actives',
            players: 'Joueurs',
            proPlayers: 'Joueurs pro',
            points: 'Points',
            totalPoints: 'Points totaux',
            total: 'Totaux',
            season: 'Saison',
            progress2025: 'Progression 2025',
            currentRanking: 'Classement actuel',
            noTeamsFound: 'Aucune équipe trouvée',
            viewFullLeaderboard: 'Voir le classement complet',
            lastUpdated: 'Dernière mise à jour :',
            myTeams: 'Mes Équipes',
            participants: 'Participants',
            currentTop5: 'Top 5 actuel',
            rank: 'Rang'
          },
          tooltips: {
            proPlayers: 'Nombre de joueurs professionnels',
            totalPoints: 'Points totaux accumulés',
            seasonProgress: 'Progression de la saison'
          },
          noGame: {
            title: 'Commencer',
            subtitle: 'Vous n\'avez pas encore de partie active'
          },
          charts: {
            regionTitle: 'Répartition par région',
            regionDesc: 'Distribution des joueurs professionnels',
            top10Title: 'Top 10 Équipes',
            performanceByPoints: 'Performance par points',
            pointsAxis: 'Points'
          },
          aria: {
            activeTeamsSuffix: 'équipes actives',
            proPlayersSuffix: 'joueurs professionnels',
            totalPointsSuffix: 'points totaux',
            seasonProgressSuffix: " % d'avancement pour la saison 2025",
            seasonProgressBarPrefix: 'Barre de progression de la saison, ',
            seasonProgressBarSuffix: '% terminée',
            pieChart: 'Diagramme circulaire montrant la répartition des joueurs par région',
            pieChartDescription: 'Diagramme circulaire montrant la répartition des joueurs professionnels par région géographique',
            barChart: 'Histogramme montrant la performance des 10 meilleures équipes',
            barChartDescription: 'Histogramme comparant les scores des 10 équipes les mieux classées',
            top5TeamsRanking: 'Classement des 5 meilleures équipes',
            navigateToFullLeaderboard: 'Aller au classement complet',
            navigationGroup: "Navigation principale de l'application",
            position: 'Position',
            with: 'avec',
            points: 'points'
          },
          navCards: {
            gamesDesc: 'Arène de combat',
            gamesTooltip: 'Gérer vos parties et rejoindre de nouveaux matchs',
            gamesAria: 'Aller à la gestion des parties',
            gamesDescription: 'Gérez vos parties fantasy, créez de nouveaux matchs et rejoignez des compétitions existantes',
            leaderboardDesc: 'Victoire Royale',
            leaderboardTooltip: 'Consulter les classements',
            leaderboardAria: 'Aller au classement et aux statistiques',
            leaderboardDescription: 'Voir le classement des joueurs, le classement des équipes et les statistiques de compétition',
            teamsDesc: "Créateur d'escouade",
            teamsTooltip: 'Gérer vos équipes et joueurs',
            teamsAria: 'Aller à la gestion des équipes',
            teamsDescription: 'Construire et gérer vos équipes fantasy, recruter des joueurs et optimiser votre escouade',
            tradesDesc: "Boutique d'objets",
            tradesTooltip: 'Échanger des joueurs',
            tradesAria: 'Aller aux échanges',
            tradesDescription: "Échanger des joueurs avec d'autres équipes, négocier et améliorer votre effectif",
            draftDesc: 'Passe de combat',
            draftTooltip: 'Participer au draft',
            draftAria: 'Aller au draft',
            draftDescription: 'Participer au draft, sélectionner vos joueurs et viser les meilleurs picks',
            newGameDesc: 'Zone de largage',
            newGameTooltip: 'Créer une nouvelle partie',
            newGameAria: 'Créer une nouvelle partie fantasy',
            newGameDescription: 'Créer et configurer une nouvelle partie fantasy avec des règles et paramètres personnalisés'
          },
          live: {
            noGameSelected: 'Aucune partie sélectionnée pour le tableau de bord',
            updatedPrefix: 'Tableau de bord mis à jour avec',
            teams: 'équipes',
            players: 'joueurs',
            errorLoading: 'Erreur lors du chargement du tableau de bord. Veuillez réessayer.',
            navigatingTo: 'Navigation vers'
          }
        },
        games: {
          myGames: 'Mes parties',
          createGame: 'Créer une partie',
          joinGame: 'Rejoindre une partie',
          joinWithCode: 'Rejoindre avec un code',
          join: 'Rejoindre',
          enterCode: 'Entrez le code',
          noGames: 'Aucune partie',
          players: 'joueurs',
          manage: 'Gérer',
          viewDetails: 'Voir les détails',
          status: {
            creating: 'Création',
            drafting: 'Draft',
            active: 'Active',
            completed: 'Terminée'
          }
        },
        trades: {
          title: 'Centre d\'Échanges',
          subtitle: 'Gérez vos échanges de joueurs et négociations',
          createTrade: 'Créer un échange',
          refreshData: 'Actualiser les données',
          searchPlaceholder: 'Rechercher par joueur ou utilisateur...',
          loading: 'Chargement de vos échanges...',
          errorTitle: 'Une erreur est survenue',
          tryAgain: 'Réessayer',
          filters: {
            all: 'Tous les échanges',
            completed: 'Terminés'
          },
          stats: {
            totalTrades: 'Total des échanges',
            pendingOffers: 'Offres en attente',
            successful: 'Réussis',
            avgValue: 'Valeur moyenne'
          },
          tabs: {
            pending: 'En attente',
            sent: 'Envoyés',
            received: 'Reçus',
            history: 'Historique'
          },
          empty: {
            pending: 'Aucun échange en attente',
            pendingDesc: 'Vous n\'avez aucune offre d\'échange en attente pour le moment.',
            sent: 'Aucun échange envoyé',
            sentDesc: 'Vous n\'avez pas encore envoyé d\'offres d\'échange.',
            received: 'Aucun échange reçu',
            receivedDesc: 'Vous n\'avez pas encore reçu d\'offres d\'échange.',
            history: 'Aucun historique',
            historyDesc: 'Vos échanges terminés apparaîtront ici.'
          },
          labels: {
            offering: 'Propose',
            receiving: 'Reçoit',
            offeredPlayers: 'Joueurs proposés',
            requestedPlayers: 'Joueurs demandés',
            offered: 'proposés',
            requested: 'demandés',
            wanted: 'demandés',
            more: 'de plus',
            expiresSoon: 'Expire bientôt',
            youGetMoreValue: 'Vous gagnez en valeur',
            youGiveMoreValue: 'Vous perdez en valeur',
            balancedTrade: 'Échange équilibré'
          },
          actions: {
            accept: 'Accepter',
            reject: 'Refuser',
            withdraw: 'Retirer',
            viewDetails: 'Voir les détails'
          },
          status: {
            pending: 'En attente',
            accepted: 'Accepté',
            rejected: 'Refusé',
            withdrawn: 'Retiré',
            expired: 'Expiré'
          }
        },
        leaderboard: {
          title: 'Classement des Joueurs - Saison 2025',
          loading: 'Chargement des données du classement en cours',
          errorTitle: 'Erreur de chargement',
          retry: 'Réessayer',
          noData: 'Aucune donnée de classement',
          noDataDesc: 'Aucune équipe ou joueur n\'est disponible pour le moment. Vérifiez que le backend est démarré ou réessayez plus tard.',
          reload: 'Recharger',
          skipToContent: 'Aller au contenu principal',
          filtersHeading: 'Filtres et recherche',
          searchPlaceholder: 'Rechercher un joueur...',
          searchLabel: 'Rechercher un joueur par nom ou pseudo',
          allRegions: 'Toutes les régions',
          resetFilters: 'Réinitialiser les filtres',
          players: 'Joueurs',
          totalPoints: 'Points Total',
          podiumHeading: 'Podium des 3 meilleurs joueurs',
          firstPlace: 'Première place',
          secondPlace: 'Deuxième place',
          thirdPlace: 'Troisième place',
          rank: 'Rang',
          player: 'Joueur',
          region: 'Région',
          points: 'Points',
          noPlayersFound: 'Aucun joueur trouvé',
          noPlayersFoundDesc: 'Essayez de modifier vos critères de recherche',
          previous: 'Précédent',
          next: 'Suivant',
          pageOf: 'Page {current} sur {total}',
          regions: {
            EU: 'Europe',
            NAW: 'NAW (Amérique du Nord Ouest)',
            ASIA: 'Asie',
            BR: 'Brésil',
            NAC: 'NAC (Amérique du Nord Centre)',
            OCE: 'Océanie',
            ME: 'Moyen-Orient'
          }
        },
        footer: {
          legalNotice: 'Mentions légales',
          privacyPolicy: 'Politique de confidentialité',
          contact: 'Contact',
          copyright: '© 2025 Fortnite Fantasy. Tous droits réservés.',
          disclaimer: 'Ce site n\'est pas affilié à Epic Games.'
        },
        errors: {
          generic: 'Une erreur est survenue',
          network: 'Erreur de connexion au serveur',
          unauthorized: 'Non autorisé',
          notFound: 'Non trouvé',
          validation: 'Erreur de validation'
        },
        notifications: {
          close: 'Fermer',
          loadingDefault: 'Chargement en cours...',
          apiLoading: '{operation} en cours...',
          apiSuccess: '{operation} réalisé avec succès',
          apiError: 'Erreur lors de {operation}',
          apiErrorWithDetails: 'Erreur lors de {operation}: {error}',
          game: {
            created: 'Jeu "{name}" créé avec succès',
            joined: 'Vous avez rejoint "{name}"',
            left: 'Vous avez quitté "{name}"',
            draftStarted: 'La draft a commencé pour "{name}"',
            yourTurn: 'C\'est votre tour de drafter !',
            viewAction: 'Voir',
            viewTeamAction: 'Voir équipe',
            participateAction: 'Participer',
            gotoDraftAction: 'Aller à la draft'
          },
          team: {
            playerAdded: '{player} ajouté à votre équipe',
            playerRemoved: '{player} retiré de votre équipe',
            tradeCompleted: 'Trade effectué: {out} ↔ {in}'
          },
          auth: {
            loginSuccess: 'Connexion réussie en tant que {username}',
            logoutSuccess: 'Déconnexion réussie',
            sessionExpired: 'Session expirée, veuillez vous reconnecter',
            profileSwitched: 'Profil changé vers {username}',
            connectAction: 'Se connecter'
          }
        },
        draft: {
          filters: {
            allRegions: 'Toutes les régions',
            naeRegion: 'Nord-Amérique Est',
            nawRegion: 'Nord-Amérique Ouest',
            euRegion: 'Europe',
            asiaRegion: 'Asie',
            oceRegion: 'Océanie',
            brazilRegion: 'Brésil',
            menaRegion: 'Moyen-Orient/Afrique',
            allLevels: 'Tous les niveaux',
            beginner: 'Débutant',
            intermediate: 'Intermédiaire',
            advanced: 'Avancé',
            expert: 'Expert'
          },
          sort: {
            pointsDesc: 'Points (décroissant)',
            pointsAsc: 'Points (croissant)',
            nameAsc: 'Nom (A-Z)',
            nameDesc: 'Nom (Z-A)',
            regionAsc: 'Région (A-Z)',
            kdDesc: 'K/D (décroissant)',
            winrateDesc: 'Winrate (décroissant)'
          },
          performance: {
            excellent: 'Excellent',
            good: 'Bon',
            average: 'Moyen',
            poor: 'Faible',
            unknown: 'Inconnu'
          },
          status: {
            notStarted: 'Non démarré',
            inProgress: 'En cours',
            paused: 'En pause',
            completed: 'Terminé',
            cancelled: 'Annulé',
            error: 'Erreur',
            created: 'Créé',
            active: 'En cours'
          },
          errors: {
            playerAlreadySelected: 'Ce joueur a déjà été sélectionné',
            notYourTurn: 'Ce n\'est pas votre tour de drafter',
            regionLimitExceeded: 'Limite de joueurs par région atteinte',
            teamFull: 'Votre équipe est complète',
            timeExpired: 'Le temps de sélection a expiré',
            invalidSelection: 'Sélection invalide',
            draftNotActive: 'Le draft n\'est pas actif',
            connectionError: 'Erreur de connexion au serveur',
            unauthorized: 'Vous n\'êtes pas autorisé à participer à ce draft',
            gameNotFound: 'Jeu non trouvé',
            playerNotFound: 'Joueur non trouvé',
            invalidRound: 'Round de draft invalide',
            draftCompleted: 'Le draft est déjà terminé',
            serverError: 'Erreur serveur, veuillez réessayer'
          },
          success: {
            playerSelected: 'Joueur sélectionné avec succès',
            draftStarted: 'Le draft a commencé',
            draftCompleted: 'Le draft est terminé',
            turnUpdated: 'C\'est votre tour de drafter',
            teamUpdated: 'Équipe mise à jour',
            draftPaused: 'Le draft a été mis en pause',
            draftResumed: 'Le draft a repris'
          },
          accessibility: {
            playerCard: 'Carte joueur',
            selectPlayer: 'Sélectionner ce joueur',
            teamOverview: 'Vue d\'ensemble de l\'équipe',
            draftTimer: 'Temps restant pour sélectionner',
            currentTurn: 'Tour actuel',
            draftStatus: 'Statut du draft',
            playerAvailable: 'Joueur disponible pour sélection',
            playerSelected: 'Joueur déjà sélectionné',
            yourTurn: 'C\'est votre tour de sélectionner un joueur',
            waitingTurn: 'En attente de votre tour',
            draftCompletedMsg: 'Draft terminé, toutes les équipes sont complètes'
          }
        },
        home: {
          welcome: 'Bienvenue dans Fortnite Pro League !',
          regionSelected: 'Région {name} sélectionnée !',
          creatingTeam: 'Création d\'équipe en cours...',
          loadingRules: 'Chargement des règles...',
          regions: {
            eu: {
              name: 'Europe',
              description: 'Le cœur stratégique du competitive. Des équipes disciplinées et une mécanique impeccable.'
            },
            nac: {
              name: 'North America Central',
              description: 'L\'épicentre du gaming créatif. Innovation et spectacle garantis.'
            },
            naw: {
              name: 'North America West',
              description: 'Terre des pionniers et des game-changers. Où naissent les méta.'
            },
            br: {
              name: 'Brazil',
              description: 'La passion à l\'état pur. Un style unique et une technique impressionnante.'
            },
            oce: {
              name: 'Oceania',
              description: 'Les guerriers des antipodes. Redoutables et imprévisibles.'
            },
            me: {
              name: 'Middle East',
              description: 'Les diamants du désert. Talent émergent et ambition infinie.'
            },
            asia: {
              name: 'Asia',
              description: 'La région de l\'innovation technologique. Précision et excellence.'
            }
          }
        }
      },
      en: {
        common: {
          save: 'Save',
          saving: 'Saving...',
          and: 'and',
          cancel: 'Cancel',
          edit: 'Edit',
          editing: 'Editing...',
          delete: 'Delete',
          confirm: 'Confirm',
          loading: 'Loading...',
          error: 'Error',
          success: 'Success',
          yes: 'Yes',
          no: 'No',
          back: 'Back',
          next: 'Next',
          close: 'Close',
          search: 'Search',
          noData: 'No data',
          actions: 'Actions',
          details: 'Details',
          players: 'Players',
          player: 'Player'
        },
        layout: {
          welcome: 'Welcome'
        },
        auth: {
          login: 'Login',
          logout: 'Logout',
          username: 'Username',
          password: 'Password',
          email: 'Email',
          register: 'Register',
          forgotPassword: 'Forgot password?',
          rememberMe: 'Remember me'
        },
        profile: {
          title: 'Profile',
          profileInformation: 'Profile Information',
          accountActions: 'Account Actions',
          username: 'Username',
          role: 'Role',
          memberSince: 'Member Since',
          recentlyJoined: 'Recently joined',
          editProfile: 'Edit Profile',
          changePassword: 'Change Password',
          viewStatistics: 'View Statistics',
          noEmailProvided: 'No email provided'
        },
        settings: {
          title: 'Settings',
          language: 'Language',
          theme: 'Theme',
          themeDesc: 'Choose your preferred theme',
          darkMode: 'Dark Mode',
          lightMode: 'Light Mode',
          systemDefault: 'System Default',
          notifications: 'Notifications',
          emailNotifications: 'Email Notifications',
          emailNotificationsDesc: 'Receive updates via email',
          pushNotifications: 'Push Notifications',
          pushNotificationsDesc: 'Browser push notifications',
          gameReminders: 'Game Reminders',
          gameRemindersDesc: 'Remind me about upcoming games',
          draftAlerts: 'Draft Alerts',
          draftAlertsDesc: 'Notify when it\'s my turn to draft',
          tradeNotifications: 'Trade Notifications',
          tradeNotificationsDesc: 'Alert me about trade offers',
          privacy: 'Privacy',
          privacyData: 'Privacy & Data',
          account: 'Account',
          general: 'General',
          appearance: 'Display',
          selectLanguage: 'Select display language',
          french: 'French',
          english: 'English',
          gamePreferences: 'Game Preferences',
          autoJoinDraft: 'Auto-Join Draft',
          autoJoinDraftDesc: 'Automatically join draft when it starts',
          showOnlineStatus: 'Show Online Status',
          showOnlineStatusDesc: 'Let others see when you\'re online',
          exportData: 'Export My Data',
          deleteAccount: 'Delete Account',
          resetDefaults: 'Reset to Defaults',
          settingsSaved: 'Settings saved successfully!',
          settingsReset: 'Settings reset to defaults'
        },
        navigation: {
          home: 'Home',
          dashboard: 'Dashboard',
          games: 'Games',
          teams: 'Teams',
          leaderboard: 'Leaderboard',
          draft: 'Draft',
          trades: 'Trades',
          profile: 'Profile',
          settings: 'Settings'
        },
        dashboard: {
          loading: 'Loading dashboard...',
          errorTitle: 'Loading error',
          retry: 'Retry',
          headings: {
            generalStatistics: 'General Statistics',
            chartsAndAnalytics: 'Charts and Analytics',
            leaderboardPreview: 'Top 5 Teams - Leaderboard',
            centralNavigation: 'Central Navigation',
            navigationHint: 'Quick access to main features'
          },
          labels: {
            team: 'Team',
            teams: 'Teams',
            activeTeams: 'Active teams',
            players: 'Players',
            proPlayers: 'Pro players',
            points: 'Points',
            totalPoints: 'Total points',
            total: 'Total',
            season: 'Season',
            progress2025: 'Progress 2025',
            currentRanking: 'Current ranking',
            noTeamsFound: 'No teams found',
            viewFullLeaderboard: 'View full leaderboard',
            lastUpdated: 'Last updated:',
            myTeams: 'My Teams',
            participants: 'Participants',
            currentTop5: 'Current Top 5',
            rank: 'Rank'
          },
          tooltips: {
            proPlayers: 'Number of professional players',
            totalPoints: 'Total accumulated points',
            seasonProgress: 'Season progress'
          },
          noGame: {
            title: 'Get Started',
            subtitle: 'You don\'t have an active game yet'
          },
          charts: {
            regionTitle: 'Distribution by Region',
            regionDesc: 'Professional players distribution',
            top10Title: 'Top 10 Teams',
            performanceByPoints: 'Performance by points',
            pointsAxis: 'Points'
          },
          aria: {
            activeTeamsSuffix: 'active teams',
            proPlayersSuffix: 'professional players',
            totalPointsSuffix: 'total points',
            seasonProgressSuffix: ' percent progress for season 2025',
            seasonProgressBarPrefix: 'Season progress bar, ',
            seasonProgressBarSuffix: '% complete',
            pieChart: 'Pie chart showing player distribution by region',
            pieChartDescription: 'Pie chart showing the distribution of professional players by geographic region',
            barChart: 'Bar chart showing performance of top 10 teams',
            barChartDescription: 'Bar chart comparing scores of the top 10 ranked teams',
            top5TeamsRanking: 'Top 5 teams ranking',
            navigateToFullLeaderboard: 'Navigate to full leaderboard',
            navigationGroup: 'Main application navigation',
            position: 'Position',
            with: 'with',
            points: 'points'
          },
          navCards: {
            gamesDesc: 'Battle Arena',
            gamesTooltip: 'Manage your games and join new matches',
            gamesAria: 'Navigate to games management section',
            gamesDescription: 'Manage your fantasy games, create new matches, and join existing competitions',
            leaderboardDesc: 'Victory Royale',
            leaderboardTooltip: 'Check player rankings',
            leaderboardAria: 'Navigate to leaderboard and rankings',
            leaderboardDescription: 'View player rankings, team standings, and competition statistics',
            teamsDesc: 'Squad Builder',
            teamsTooltip: 'Manage your teams and players',
            teamsAria: 'Navigate to team management',
            teamsDescription: 'Build and manage your fantasy teams, recruit players, and optimize your squad',
            tradesDesc: 'Item Shop',
            tradesTooltip: 'Trade players',
            tradesAria: 'Navigate to player trading',
            tradesDescription: 'Trade players with other teams, negotiate deals, and improve your roster',
            draftDesc: 'Battle Pass',
            draftTooltip: 'Participate in player draft',
            draftAria: 'Navigate to player draft',
            draftDescription: 'Participate in player drafts, select your team members, and compete for top picks',
            newGameDesc: 'Drop Zone',
            newGameTooltip: 'Create a new game',
            newGameAria: 'Create a new fantasy game',
            newGameDescription: 'Create and configure a new fantasy league game with custom rules and settings'
          },
          live: {
            noGameSelected: 'No game selected for dashboard',
            updatedPrefix: 'Dashboard updated with',
            teams: 'teams',
            players: 'players',
            errorLoading: 'Error loading dashboard data. Please try again.',
            navigatingTo: 'Navigating to'
          }
        },
        games: {
          myGames: 'My Games',
          createGame: 'Create Game',
          joinGame: 'Join Game',
          joinWithCode: 'Join with code',
          join: 'Join',
          enterCode: 'Enter code',
          noGames: 'No games',
          players: 'players',
          manage: 'Manage',
          viewDetails: 'View details',
          status: {
            creating: 'Creating',
            drafting: 'Drafting',
            active: 'Active',
            completed: 'Completed'
          }
        },
        trades: {
          title: 'Trading Hub',
          subtitle: 'Manage your player trades and negotiations',
          createTrade: 'Create New Trade',
          refreshData: 'Refresh Data',
          searchPlaceholder: 'Search by player or user...',
          loading: 'Loading your trades...',
          errorTitle: 'Something went wrong',
          tryAgain: 'Try Again',
          filters: {
            all: 'All Trades',
            completed: 'Completed'
          },
          stats: {
            totalTrades: 'Total Trades',
            pendingOffers: 'Pending Offers',
            successful: 'Successful',
            avgValue: 'Avg Value'
          },
          tabs: {
            pending: 'Pending',
            sent: 'Sent',
            received: 'Received',
            history: 'History'
          },
          empty: {
            pending: 'No Pending Trades',
            pendingDesc: 'You don\'t have any pending trade offers at the moment.',
            sent: 'No Sent Trades',
            sentDesc: 'You haven\'t sent any trade offers yet.',
            received: 'No Received Trades',
            receivedDesc: 'You haven\'t received any trade offers yet.',
            history: 'No Trade History',
            historyDesc: 'Your completed trades will appear here.'
          },
          labels: {
            offering: 'Offering',
            receiving: 'Receiving',
            offeredPlayers: 'Offered Players',
            requestedPlayers: 'Requested Players',
            offered: 'offered',
            requested: 'requested',
            wanted: 'wanted',
            more: 'more',
            expiresSoon: 'Expires soon',
            youGetMoreValue: 'You get more value',
            youGiveMoreValue: 'You give more value',
            balancedTrade: 'Balanced trade'
          },
          actions: {
            accept: 'Accept',
            reject: 'Reject',
            withdraw: 'Withdraw',
            viewDetails: 'View Details'
          },
          status: {
            pending: 'Pending',
            accepted: 'Accepted',
            rejected: 'Rejected',
            withdrawn: 'Withdrawn',
            expired: 'Expired'
          }
        },
        leaderboard: {
          title: 'Player Leaderboard - Season 2025',
          loading: 'Loading leaderboard data',
          errorTitle: 'Loading error',
          retry: 'Retry',
          noData: 'No leaderboard data',
          noDataDesc: 'No team or player is available at the moment. Check that the backend is running or try again later.',
          reload: 'Reload',
          skipToContent: 'Skip to main content',
          filtersHeading: 'Filters and search',
          searchPlaceholder: 'Search for a player...',
          searchLabel: 'Search for a player by name or username',
          allRegions: 'All regions',
          resetFilters: 'Reset filters',
          players: 'Players',
          totalPoints: 'Total Points',
          podiumHeading: 'Top 3 players podium',
          firstPlace: 'First place',
          secondPlace: 'Second place',
          thirdPlace: 'Third place',
          rank: 'Rank',
          player: 'Player',
          region: 'Region',
          points: 'Points',
          noPlayersFound: 'No players found',
          noPlayersFoundDesc: 'Try modifying your search criteria',
          previous: 'Previous',
          next: 'Next',
          pageOf: 'Page {current} of {total}',
          regions: {
            EU: 'Europe',
            NAW: 'NAW (North America West)',
            ASIA: 'Asia',
            BR: 'Brazil',
            NAC: 'NAC (North America Central)',
            OCE: 'Oceania',
            ME: 'Middle East'
          }
        },
        footer: {
          legalNotice: 'Legal Notice',
          privacyPolicy: 'Privacy Policy',
          contact: 'Contact',
          copyright: '© 2025 Fortnite Fantasy. All rights reserved.',
          disclaimer: 'This site is not affiliated with Epic Games.'
        },
        errors: {
          generic: 'An error occurred',
          network: 'Server connection error',
          unauthorized: 'Unauthorized',
          notFound: 'Not found',
          validation: 'Validation error'
        },
        notifications: {
          close: 'Close',
          loadingDefault: 'Loading...',
          apiLoading: '{operation} in progress...',
          apiSuccess: '{operation} completed successfully',
          apiError: 'Error during {operation}',
          apiErrorWithDetails: 'Error during {operation}: {error}',
          game: {
            created: 'Game "{name}" created successfully',
            joined: 'You joined "{name}"',
            left: 'You left "{name}"',
            draftStarted: 'Draft started for "{name}"',
            yourTurn: 'It\'s your turn to draft!',
            viewAction: 'View',
            viewTeamAction: 'View team',
            participateAction: 'Participate',
            gotoDraftAction: 'Go to draft'
          },
          team: {
            playerAdded: '{player} added to your team',
            playerRemoved: '{player} removed from your team',
            tradeCompleted: 'Trade completed: {out} ↔ {in}'
          },
          auth: {
            loginSuccess: 'Successfully logged in as {username}',
            logoutSuccess: 'Successfully logged out',
            sessionExpired: 'Session expired, please log in again',
            profileSwitched: 'Profile switched to {username}',
            connectAction: 'Log in'
          }
        },
        draft: {
          filters: {
            allRegions: 'All regions',
            naeRegion: 'North America East',
            nawRegion: 'North America West',
            euRegion: 'Europe',
            asiaRegion: 'Asia',
            oceRegion: 'Oceania',
            brazilRegion: 'Brazil',
            menaRegion: 'Middle East/Africa',
            allLevels: 'All levels',
            beginner: 'Beginner',
            intermediate: 'Intermediate',
            advanced: 'Advanced',
            expert: 'Expert'
          },
          sort: {
            pointsDesc: 'Points (descending)',
            pointsAsc: 'Points (ascending)',
            nameAsc: 'Name (A-Z)',
            nameDesc: 'Name (Z-A)',
            regionAsc: 'Region (A-Z)',
            kdDesc: 'K/D (descending)',
            winrateDesc: 'Winrate (descending)'
          },
          performance: {
            excellent: 'Excellent',
            good: 'Good',
            average: 'Average',
            poor: 'Poor',
            unknown: 'Unknown'
          },
          status: {
            notStarted: 'Not started',
            inProgress: 'In progress',
            paused: 'Paused',
            completed: 'Completed',
            cancelled: 'Cancelled',
            error: 'Error',
            created: 'Created',
            active: 'Active'
          },
          errors: {
            playerAlreadySelected: 'This player has already been selected',
            notYourTurn: 'It\'s not your turn to draft',
            regionLimitExceeded: 'Region player limit reached',
            teamFull: 'Your team is full',
            timeExpired: 'Selection time expired',
            invalidSelection: 'Invalid selection',
            draftNotActive: 'Draft is not active',
            connectionError: 'Server connection error',
            unauthorized: 'You are not authorized to participate in this draft',
            gameNotFound: 'Game not found',
            playerNotFound: 'Player not found',
            invalidRound: 'Invalid draft round',
            draftCompleted: 'Draft is already completed',
            serverError: 'Server error, please try again'
          },
          success: {
            playerSelected: 'Player selected successfully',
            draftStarted: 'Draft started',
            draftCompleted: 'Draft completed',
            turnUpdated: 'It\'s your turn to draft',
            teamUpdated: 'Team updated',
            draftPaused: 'Draft paused',
            draftResumed: 'Draft resumed'
          },
          accessibility: {
            playerCard: 'Player card',
            selectPlayer: 'Select this player',
            teamOverview: 'Team overview',
            draftTimer: 'Time remaining to select',
            currentTurn: 'Current turn',
            draftStatus: 'Draft status',
            playerAvailable: 'Player available for selection',
            playerSelected: 'Player already selected',
            yourTurn: 'It\'s your turn to select a player',
            waitingTurn: 'Waiting for your turn',
            draftCompletedMsg: 'Draft completed, all teams are full'
          }
        },
        home: {
          welcome: 'Welcome to Fortnite Pro League!',
          regionSelected: 'Region {name} selected!',
          creatingTeam: 'Creating team...',
          loadingRules: 'Loading rules...',
          regions: {
            eu: {
              name: 'Europe',
              description: 'The strategic heart of competitive. Disciplined teams and impeccable mechanics.'
            },
            nac: {
              name: 'North America Central',
              description: 'The epicenter of creative gaming. Innovation and spectacle guaranteed.'
            },
            naw: {
              name: 'North America West',
              description: 'Land of pioneers and game-changers. Where metas are born.'
            },
            br: {
              name: 'Brazil',
              description: 'Pure passion. A unique style and impressive technique.'
            },
            oce: {
              name: 'Oceania',
              description: 'The warriors from down under. Formidable and unpredictable.'
            },
            me: {
              name: 'Middle East',
              description: 'Desert diamonds. Emerging talent and infinite ambition.'
            },
            asia: {
              name: 'Asia',
              description: 'The region of technological innovation. Precision and excellence.'
            }
          }
        }
      }
    };
  }
}
