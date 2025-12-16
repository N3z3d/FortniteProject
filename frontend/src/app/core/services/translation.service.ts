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

  translate(key: string): string {
    const keys = key.split('.');
    let result: string | Translations = this.translations[this.currentLang$.value];

    for (const k of keys) {
      if (result && typeof result === 'object' && k in result) {
        result = result[k];
      } else {
        return key; // Return key if translation not found
      }
    }

    return typeof result === 'string' ? result : key;
  }

  t(key: string): string {
    return this.translate(key);
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
          and: 'et',
          cancel: 'Annuler',
          edit: 'Modifier',
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
          actions: 'Actions'
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
          headings: {
            generalStatistics: 'Statistiques générales',
            chartsAndAnalytics: 'Graphiques et analyses',
            leaderboardPreview: 'Top 5 équipes - classement',
            centralNavigation: 'Navigation centrale',
            navigationHint: 'Accès rapide aux fonctionnalités principales'
          },
          labels: {
            team: 'Équipe',
            teams: 'Équipes',
            activeTeams: 'Équipes actives',
            players: 'Joueurs',
            proPlayers: 'Joueurs pro',
            points: 'Points',
            totalPoints: 'Points totaux',
            season: 'Saison',
            progress2025: 'Progression 2025',
            currentRanking: 'Classement actuel',
            noTeamsFound: 'Aucune équipe trouvée',
            viewFullLeaderboard: 'Voir le classement complet',
            lastUpdated: 'Dernière mise à jour :'
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
          },
          charts: {
            regionTitle: 'Répartition par région',
            top10Title: 'Top 10 équipes par points',
            pointsAxis: 'Points'
          }
        },
        games: {
          myGames: 'Mes Parties',
          createGame: 'Créer une partie',
          joinGame: 'Rejoindre une partie',
          joinWithCode: 'Rejoindre avec un code',
          noGames: 'Aucune partie',
          players: 'joueurs',
          status: {
            creating: 'Création',
            drafting: 'Draft',
            active: 'Active',
            completed: 'Terminée'
          }
        },
        errors: {
          generic: 'Une erreur est survenue',
          network: 'Erreur de connexion au serveur',
          unauthorized: 'Non autorisé',
          notFound: 'Non trouvé',
          validation: 'Erreur de validation'
        }
      },
      en: {
        common: {
          save: 'Save',
          and: 'and',
          cancel: 'Cancel',
          edit: 'Edit',
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
          actions: 'Actions'
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
            season: 'Season',
            progress2025: 'Progress 2025',
            currentRanking: 'Current ranking',
            noTeamsFound: 'No teams found',
            viewFullLeaderboard: 'View full leaderboard',
            lastUpdated: 'Last updated:'
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
          },
          charts: {
            regionTitle: 'Distribution by Region',
            top10Title: 'Top 10 Teams by Points',
            pointsAxis: 'Points'
          }
        },
        games: {
          myGames: 'My Games',
          createGame: 'Create Game',
          joinGame: 'Join Game',
          joinWithCode: 'Join with code',
          noGames: 'No games',
          players: 'players',
          status: {
            creating: 'Creating',
            drafting: 'Drafting',
            active: 'Active',
            completed: 'Completed'
          }
        },
        errors: {
          generic: 'An error occurred',
          network: 'Server connection error',
          unauthorized: 'Unauthorized',
          notFound: 'Not found',
          validation: 'Validation error'
        }
      }
    };
  }
}
