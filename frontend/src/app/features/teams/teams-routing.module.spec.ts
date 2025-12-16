import { TEAMS_ROUTES } from './teams-routing.module';

describe('TeamsRoutingModule', () => {
  it('déclare les routes (dont alias legacy) attendues', () => {
    const paths = TEAMS_ROUTES.map(route => route.path);

    expect(paths).toContain('');
    expect(paths).toContain('create');
    expect(paths).toContain('detail/:id');
    expect(paths).toContain('edit/:id');
    expect(paths).toContain(':id');
    expect(paths).toContain(':id/edit');
  });
});

