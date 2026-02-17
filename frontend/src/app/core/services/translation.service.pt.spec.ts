import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TranslationService, Translations } from './translation.service';

describe('TranslationService (pt)', () => {
  let service: TranslationService;
  let httpMock: HttpTestingController;

  const mockTranslationsPt: Translations = {
    common: {
      all: 'Todos'
    },
    errors: {
      generic: 'Erro',
      network: 'Erro de conexão',
      validation: 'Erro de validação'
    },
    games: {
      myGames: 'Minhas partidas',
      participantsTooltip: 'Participantes / Máximo',
      joinGame: 'Entrar em uma partida',
      joinWithCode: 'Entrar com um código',
      status: {
        completed: 'Concluída'
      }
    },
    legal: {
      lastUpdate: 'Última atualização',
      subtitle: 'Fortnite Fantasy League',
      contact: {
        title: 'Contato'
      },
      privacy: {
        title: 'Política de privacidade'
      }
    },
    profile: {
      title: 'Perfil',
      accountActions: 'Ações da conta',
      noEmailProvided: 'Nenhum e-mail informado',
      defaultRole: 'Usuário',
      statsDialog: {
        loading: 'Carregando estatísticas...'
      }
    },
    settings: {
      emailNotifications: 'Notificações por e-mail',
      showOnlineStatus: 'Mostrar status online',
      resetDefaults: 'Restaurar padrões',
      lightModeComingSoon: '(em breve)',
      lightModeHint: 'O modo claro estará disponível em uma próxima versão'
    },
    notifications: {
      close: 'Fechar',
      game: {
        joined: 'Você entrou em "{name}"'
      },
      auth: {
        sessionExpired: 'Sessão expirada, faça login novamente'
      }
    },
    leaderboard: {
      title: 'Classificação de jogadores - Temporada 2025',
      noPlayersFound: 'Nenhum jogador encontrado',
      pageOf: 'Página {current} de {total}',
      aria: {
        playerRow: 'Jogador na posição {rank}: {nickname} da região {region} com {points} pontos',
        filterResultsMultiple: '{count} jogadores encontrados'
      },
      errors: {
        dataUnavailable: 'Dados indisponíveis (CSV não carregado)'
      }
    },
    trades: {
      title: 'Centro de Trocas',
      filters: {
        all: 'Todas as trocas'
      },
      tabs: {
        pending: 'Pendentes'
      },
      errors: {
        loadTrades: 'Erro ao carregar as trocas',
        actionFailed: 'A ação falhou. Tente novamente.'
      },
      messages: {
        tradeAccepted: 'Troca aceita com sucesso!'
      },
      proposal: {
        title: 'Criar uma proposta de troca'
      },
      details: {
        timelineProposedAction: 'Troca proposta'
      },
      history: {
        title: 'Histórico de trocas'
      },
      detail: {
        titlePrefix: 'Detalhe da troca #'
      },
      form: {
        titleNew: 'Nova troca'
      },
      status: {
        cancelled: 'Cancelada'
      }
    },
    dashboard: {
      loading: 'Carregando o painel...',
      labels: {
        currentRanking: 'Classificação atual'
      },
      aria: {
        seasonProgressBarSuffix: '% concluída'
      },
      messages: {
        demoMode: 'Modo demonstração (backend offline)',
        gameSelected: 'Partida "{name}" selecionada!'
      }
    },
    draft: {
      filters: {
        allRegions: 'Todas as regiões'
      },
      sort: {
        pointsDesc: 'Pontos (descendente)'
      },
      status: {
        completed: 'Concluído'
      },
      ui: {
        searchResultsTitle: 'Resultados da busca ({count})'
      },
      selection: {
        clearFilters: 'Limpar filtros'
      }
    },
    teams: {
      list: {
        titleMine: 'Meus Times'
      },
      edit: {
        saveChanges: 'Salvar alterações'
      },
      detail: {
        backToTeams: 'Voltar para times'
      }
    }
  };

  const mockTranslationsOther: Translations = {};

  beforeEach(() => {
    localStorage.removeItem('app_language');
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TranslationService]
    });
    service = TestBed.inject(TranslationService);
    httpMock = TestBed.inject(HttpTestingController);

    // Mock HTTP requests for all languages
    const requests = httpMock.match(req => req.url.includes('assets/i18n/'));
    requests.forEach(req => {
      const lang = req.request.url.split('/').pop()?.replace('.json', '') || 'en';
      req.flush(lang === 'pt' ? mockTranslationsPt : mockTranslationsOther);
    });
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('returns Portuguese errors when available', () => {
    service.setLanguage('pt');

    expect(service.translate('errors.generic')).toBe('Erro');
    expect(service.translate('errors.network')).toBe('Erro de conexão');
    expect(service.translate('errors.validation')).toBe('Erro de validação');
  });

  it('returns Portuguese games translations when available', () => {
    service.setLanguage('pt');

    expect(service.translate('games.myGames')).toBe('Minhas partidas');
    expect(service.translate('games.participantsTooltip')).toBe('Participantes / Máximo');
    expect(service.translate('games.joinGame')).toBe('Entrar em uma partida');
    expect(service.translate('games.joinWithCode')).toBe('Entrar com um código');
    expect(service.translate('games.status.completed')).toBe('Concluída');
  });

  it('returns Portuguese legal translations when available', () => {
    service.setLanguage('pt');

    expect(service.translate('legal.lastUpdate')).toBe('Última atualização');
    expect(service.translate('legal.subtitle')).toBe('Fortnite Fantasy League');
    expect(service.translate('legal.contact.title')).toBe('Contato');
    expect(service.translate('legal.privacy.title')).toBe('Política de privacidade');
  });

  it('returns Portuguese profile translations when available', () => {
    service.setLanguage('pt');

    expect(service.translate('profile.title')).toBe('Perfil');
    expect(service.translate('profile.accountActions')).toBe('Ações da conta');
    expect(service.translate('profile.noEmailProvided')).toBe('Nenhum e-mail informado');
    expect(service.translate('profile.defaultRole')).toBe('Usuário');
    expect(service.translate('profile.statsDialog.loading')).toBe('Carregando estatísticas...');
  });

  it('returns Portuguese settings translations when available', () => {
    service.setLanguage('pt');

    expect(service.translate('settings.emailNotifications')).toBe('Notificações por e-mail');
    expect(service.translate('settings.showOnlineStatus')).toBe('Mostrar status online');
    expect(service.translate('settings.resetDefaults')).toBe('Restaurar padrões');
    expect(service.translate('settings.lightModeComingSoon')).toBe('(em breve)');
    expect(service.translate('settings.lightModeHint')).toBe('O modo claro estará disponível em uma próxima versão');
  });

  it('returns Portuguese notifications translations when available', () => {
    service.setLanguage('pt');

    expect(service.translate('notifications.close')).toBe('Fechar');
    expect(service.translate('notifications.game.joined')).toBe('Você entrou em "{name}"');
    expect(service.translate('notifications.auth.sessionExpired')).toBe('Sessão expirada, faça login novamente');
  });

  it('returns Portuguese leaderboard translations when available', () => {
    service.setLanguage('pt');

    expect(service.translate('leaderboard.title')).toBe('Classificação de jogadores - Temporada 2025');
    expect(service.translate('leaderboard.noPlayersFound')).toBe('Nenhum jogador encontrado');
    expect(service.translate('leaderboard.pageOf')).toBe('Página {current} de {total}');
    expect(service.translate('leaderboard.aria.playerRow')).toBe('Jogador na posição {rank}: {nickname} da região {region} com {points} pontos');
    expect(service.translate('leaderboard.aria.filterResultsMultiple')).toBe('{count} jogadores encontrados');
    expect(service.translate('leaderboard.errors.dataUnavailable')).toBe('Dados indisponíveis (CSV não carregado)');
  });

  it('returns Portuguese trades translations when available', () => {
    service.setLanguage('pt');

    expect(service.translate('trades.title')).toBe('Centro de Trocas');
    expect(service.translate('trades.filters.all')).toBe('Todas as trocas');
    expect(service.translate('trades.tabs.pending')).toBe('Pendentes');
  });

  it('returns Portuguese trades messages when available', () => {
    service.setLanguage('pt');

    expect(service.translate('trades.errors.loadTrades')).toBe('Erro ao carregar as trocas');
    expect(service.translate('trades.messages.tradeAccepted')).toBe('Troca aceita com sucesso!');
    expect(service.translate('trades.proposal.title')).toBe('Criar uma proposta de troca');
  });

  it('returns Portuguese trade detail/history/form translations when available', () => {
    service.setLanguage('pt');

    expect(service.translate('common.all')).toBe('Todos');
    expect(service.translate('trades.details.timelineProposedAction')).toBe('Troca proposta');
    expect(service.translate('trades.errors.actionFailed')).toBe('A ação falhou. Tente novamente.');
    expect(service.translate('trades.history.title')).toBe('Histórico de trocas');
    expect(service.translate('trades.detail.titlePrefix')).toBe('Detalhe da troca #');
    expect(service.translate('trades.form.titleNew')).toBe('Nova troca');
    expect(service.translate('trades.status.cancelled')).toBe('Cancelada');
  });

  it('returns Portuguese dashboard translations when available', () => {
    service.setLanguage('pt');

    expect(service.translate('dashboard.loading')).toBe('Carregando o painel...');
    expect(service.translate('dashboard.labels.currentRanking')).toBe('Classificação atual');
    expect(service.translate('dashboard.aria.seasonProgressBarSuffix')).toBe('% concluída');
    expect(service.translate('dashboard.messages.demoMode')).toBe('Modo demonstração (backend offline)');
    expect(service.translate('dashboard.messages.gameSelected')).toBe('Partida "{name}" selecionada!');
  });

  it('returns Portuguese draft translations when available', () => {
    service.setLanguage('pt');

    expect(service.translate('draft.filters.allRegions')).toBe('Todas as regiões');
    expect(service.translate('draft.sort.pointsDesc')).toBe('Pontos (descendente)');
    expect(service.translate('draft.status.completed')).toBe('Concluído');
    expect(service.translate('draft.ui.searchResultsTitle')).toBe('Resultados da busca ({count})');
    expect(service.translate('draft.selection.clearFilters')).toBe('Limpar filtros');
  });

  it('returns Portuguese teams translations when available', () => {
    service.setLanguage('pt');

    expect(service.translate('teams.list.titleMine')).toBe('Meus Times');
    expect(service.translate('teams.edit.saveChanges')).toBe('Salvar alterações');
    expect(service.translate('teams.detail.backToTeams')).toBe('Voltar para times');
  });
});
