import { TestBed } from '@angular/core/testing';
import { TranslationService } from './translation.service';

describe('TranslationService (es)', () => {
  let service: TranslationService;

  beforeEach(() => {
    localStorage.removeItem('app_language');
    TestBed.configureTestingModule({
      providers: [TranslationService]
    });
    service = TestBed.inject(TranslationService);
  });

  it('returns Spanish errors when available', () => {
    service.setLanguage('es');

    expect(service.translate('errors.generic')).toBe('Error');
    expect(service.translate('errors.network')).toBe('Error de red');
    expect(service.translate('errors.validation')).toBe('Error de validación');
  });

  it('returns Spanish games translations when available', () => {
    service.setLanguage('es');

    expect(service.translate('games.myGames')).toBe('Mis partidas');
    expect(service.translate('games.participantsTooltip')).toBe('Participantes / Máximo');
    expect(service.translate('games.joinGame')).toBe('Unirse a una partida');
    expect(service.translate('games.joinWithCode')).toBe('Unirse con un código');
    expect(service.translate('games.status.completed')).toBe('Completada');
  });

  it('returns Spanish legal translations when available', () => {
    service.setLanguage('es');

    expect(service.translate('legal.lastUpdate')).toBe('Última actualización');
    expect(service.translate('legal.subtitle')).toBe('Fortnite Fantasy League');
    expect(service.translate('legal.contact.title')).toBe('Contacto');
    expect(service.translate('legal.privacy.title')).toBe('Política de privacidad');
  });

  it('returns Spanish profile translations when available', () => {
    service.setLanguage('es');

    expect(service.translate('profile.title')).toBe('Perfil');
    expect(service.translate('profile.accountActions')).toBe('Acciones de la cuenta');
    expect(service.translate('profile.noEmailProvided')).toBe('No se proporcionó correo electrónico');
    expect(service.translate('profile.defaultRole')).toBe('Usuario');
    expect(service.translate('profile.changePasswordDialog.strength.weak')).toBe('Débil');
  });

  it('returns Spanish settings translations when available', () => {
    service.setLanguage('es');

    expect(service.translate('settings.emailNotifications')).toBe('Notificaciones por correo electrónico');
    expect(service.translate('settings.gamePreferences')).toBe('Preferencias de juego');
    expect(service.translate('settings.autoJoinDraftDesc')).toBe('Unirse automáticamente al draft cuando comience');
    expect(service.translate('settings.lightModeComingSoon')).toBe('(próximamente)');
    expect(service.translate('settings.lightModeHint')).toBe('El modo claro estará disponible en una próxima versión');
  });

  it('returns Spanish notifications translations when available', () => {
    service.setLanguage('es');

    expect(service.translate('notifications.close')).toBe('Cerrar');
    expect(service.translate('notifications.game.created')).toBe('Partida "{name}" creada con éxito');
    expect(service.translate('notifications.auth.sessionExpired')).toBe('Sesión expirada, vuelve a iniciar sesión');
  });

  it('returns Spanish leaderboard translations when available', () => {
    service.setLanguage('es');

    expect(service.translate('leaderboard.title')).toBe('Clasificación de jugadores - Temporada 2025');
    expect(service.translate('leaderboard.noPlayersFound')).toBe('No se encontraron jugadores');
    expect(service.translate('leaderboard.pageOf')).toBe('Página {current} de {total}');
    expect(service.translate('leaderboard.aria.playerRow')).toBe('Jugador en el puesto {rank}: {nickname} de {region} con {points} puntos');
    expect(service.translate('leaderboard.aria.filterResultsSingle')).toBe('1 jugador encontrado');
    expect(service.translate('leaderboard.errors.dataUnavailable')).toBe('Datos no disponibles (CSV no cargado)');
  });

  it('returns Spanish trades translations when available', () => {
    service.setLanguage('es');

    expect(service.translate('trades.title')).toBe('Centro de Intercambios');
    expect(service.translate('trades.filters.all')).toBe('Todos los intercambios');
    expect(service.translate('trades.tabs.pending')).toBe('Pendientes');
  });

  it('returns Spanish trades messages when available', () => {
    service.setLanguage('es');

    expect(service.translate('trades.errors.loadTrades')).toBe('Error al cargar los intercambios');
    expect(service.translate('trades.messages.tradeAccepted')).toBe('¡Intercambio aceptado con éxito!');
    expect(service.translate('trades.proposal.title')).toBe('Crear una propuesta de intercambio');
  });

  it('returns Spanish trade detail/history/form translations when available', () => {
    service.setLanguage('es');

    expect(service.translate('common.all')).toBe('Todos');
    expect(service.translate('trades.details.timelineProposedAction')).toBe('Intercambio propuesto');
    expect(service.translate('trades.messages.counterOfferSent')).toBe('¡Contraoferta enviada con éxito!');
    expect(service.translate('trades.history.title')).toBe('Historial de intercambios');
    expect(service.translate('trades.detail.titlePrefix')).toBe('Detalle del intercambio #');
    expect(service.translate('trades.form.titleNew')).toBe('Nuevo intercambio');
    expect(service.translate('trades.status.completed')).toBe('Completado');
  });

  it('returns Spanish dashboard translations when available', () => {
    service.setLanguage('es');

    expect(service.translate('dashboard.loading')).toBe('Cargando el panel...');
    expect(service.translate('dashboard.labels.currentRanking')).toBe('Clasificación actual');
    expect(service.translate('dashboard.aria.seasonProgressBarSuffix')).toBe('% completada');
    expect(service.translate('dashboard.messages.demoMode')).toBe('Modo demostración (backend sin conexión)');
    expect(service.translate('dashboard.messages.gameSelected')).toBe('Partida "{name}" seleccionada!');
  });

  it('returns Spanish draft translations when available', () => {
    service.setLanguage('es');

    expect(service.translate('draft.filters.allRegions')).toBe('Todas las regiones');
    expect(service.translate('draft.sort.pointsDesc')).toBe('Puntos (descendente)');
    expect(service.translate('draft.status.completed')).toBe('Completado');
    expect(service.translate('draft.ui.searchResultsTitle')).toBe('Resultados de búsqueda ({count})');
    expect(service.translate('draft.selection.clearFilters')).toBe('Borrar filtros');
  });

  it('returns Spanish teams translations when available', () => {
    service.setLanguage('es');

    expect(service.translate('teams.list.titleMine')).toBe('Mis Equipos');
    expect(service.translate('teams.edit.saveChanges')).toBe('Guardar cambios');
    expect(service.translate('teams.detail.backToTeams')).toBe('Volver a equipos');
  });
});
