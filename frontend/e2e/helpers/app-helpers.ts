import { APIRequestContext, expect, Locator, Page } from '@playwright/test';

import { softDeleteLocalGameById } from './local-db-helpers';

const PROFILE_BUTTON_SELECTOR =
  'fieldset.user-selection-section button, button.user-profile-btn';
const DEFAULT_E2E_GAME_PREFIX = 'E2E-';
const DEFAULT_BACKEND_URL = process.env['BACKEND_URL'] ?? 'http://localhost:8080';
const DEV_PROFILE_MAP = {
  admin: {
    id: '1',
    username: 'admin',
    email: 'admin@fortnite-pronos.com',
    role: 'Administrateur',
  },
  thibaut: {
    id: '2',
    username: 'thibaut',
    email: 'thibaut@fortnite-pronos.com',
    role: 'Joueur',
  },
  marcel: {
    id: '3',
    username: 'marcel',
    email: 'marcel@fortnite-pronos.com',
    role: 'Joueur',
  },
  teddy: {
    id: '4',
    username: 'teddy',
    email: 'teddy@fortnite-pronos.com',
    role: 'Joueur',
  },
} as const;

interface GameApiDto {
  id: string;
  name?: string;
  invitationCode?: string | null;
}

async function getPersistedInvitationCode(
  request: APIRequestContext,
  username: string,
  gameId: string
): Promise<string> {
  const headers = { 'X-Test-User': username };
  const encodedUsername = encodeURIComponent(username);
  const response = await request.get(
    `${DEFAULT_BACKEND_URL}/api/games/${gameId}?user=${encodedUsername}`,
    { headers }
  );

  if (!response.ok()) {
    return '';
  }

  const game = (await response.json()) as GameApiDto;
  return game.invitationCode?.trim() ?? '';
}

export async function waitForLoginPage(page: Page): Promise<void> {
  await page.goto('/login');
  await page
    .locator('.user-selection-login, .user-controlled-login')
    .first()
    .waitFor({ state: 'visible', timeout: 10_000 });
}

export async function loginWithProfile(
  page: Page,
  profile: number | string = 0
): Promise<void> {
  await waitForLoginPage(page);

  await clickProfileButton(page, profile);
  await page.waitForURL(/\/games/, { timeout: 15_000 });
}

export async function forceLoginWithProfile(
  page: Page,
  username: keyof typeof DEV_PROFILE_MAP
): Promise<void> {
  const profile = DEV_PROFILE_MAP[username];
  await performRealProfileLogin(page, username, profile);
  return;
  const storedProfile = {
    ...profile,
    lastLoginDate: new Date().toISOString(),
  };
  // Attempt real JWT login via backend API
  let jwtToken: string | null = null;
  await page.evaluate(() => {
    sessionStorage.clear();
    localStorage.removeItem('lastUser');
    localStorage.removeItem('autoLogin');
  });
  let jwtUser: object | null = null;
  try {
    const loginResp = await page.request.post(
      `${DEFAULT_BACKEND_URL}/api/auth/login`,
      { data: { username, password: 'Admin1234' } }
    );
    if (loginResp.ok()) {
      const body = await loginResp.json() as { token: string; user: object };
      jwtToken = body.token ?? null;
      jwtUser = body.user ?? null;
    }
  } catch {
    // Docker not running — inject synthetic token so isLoggedIn() returns true.
    // Subsequent API calls will fail → guard URL check skips the test gracefully.
  }

  const syntheticToken = 'e2e-synthetic-token-unused';

  await page.goto('/login');
  await page.evaluate(
    ({ user, token, jwtUserData, synthetic }) => {
      sessionStorage.clear();
      localStorage.removeItem('lastUser');
      localStorage.removeItem('autoLogin');
      sessionStorage.setItem('currentUser', JSON.stringify(user));
      sessionStorage.setItem('jwt_token', token ?? synthetic);
      if (jwtUserData) {
        sessionStorage.setItem('jwt_user', JSON.stringify(jwtUserData));
      }
      localStorage.setItem('lastUser', JSON.stringify(user));
      localStorage.setItem('autoLogin', 'true');
    },
    { user: storedProfile, token: jwtToken, jwtUserData: jwtUser, synthetic: syntheticToken }
  );
  await page.goto('/games');
  await page.waitForURL(/\/games/, { timeout: 15_000 });
}

async function clickProfileButton(page: Page, profile: number | string): Promise<void> {
  const maxAttempts = 3;

  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    const profileBtn =
      typeof profile === 'string'
        ? page
            .locator(
              `fieldset.user-selection-section button:has-text("${profile}"), button.user-profile-btn:has-text("${profile}")`
            )
            .first()
        : page.locator(PROFILE_BUTTON_SELECTOR).nth(profile);

    await profileBtn.waitFor({ state: 'visible', timeout: 10_000 });

    try {
      await profileBtn.click();
      return;
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      const canRetry =
        message.includes('detached from the DOM') ||
        message.includes('element is not attached');

      if (!canRetry || attempt === maxAttempts) {
        throw error;
      }
    }
  }
}

async function performRealProfileLogin(
  page: Page,
  username: keyof typeof DEV_PROFILE_MAP,
  profile: (typeof DEV_PROFILE_MAP)[keyof typeof DEV_PROFILE_MAP]
): Promise<void> {
  await page.goto('/login');
  await page.evaluate(() => {
    sessionStorage.clear();
    localStorage.removeItem('lastUser');
    localStorage.removeItem('autoLogin');
  });

  const profileButton = page
    .locator(
      `fieldset.user-selection-section button:has-text("${username}"), button.user-profile-btn:has-text("${username}")`
    )
    .first();

  if (await profileButton.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await clickProfileButton(page, username);
  } else {
    const loginResp = await page.request.post(
      `${DEFAULT_BACKEND_URL}/api/auth/login`,
      { data: { username, password: 'Admin1234' } }
    );

    if (!loginResp.ok()) {
      throw new Error(
        `forceLoginWithProfile failed for ${username}: ${loginResp.status()} ${await loginResp.text()}`
      );
    }

    const body = await loginResp.json() as {
      token?: string;
      user?: { id?: string; email?: string; role?: string };
    };
    const jwtToken = body.token ?? null;
    if (!hasJwtFormat(jwtToken)) {
      throw new Error(`forceLoginWithProfile received an invalid JWT token for ${username}`);
    }

    const storedProfile = {
      ...profile,
      id: body.user?.id ?? profile.id,
      email: body.user?.email ?? profile.email,
      role: body.user?.role ?? profile.role,
      lastLoginDate: new Date().toISOString(),
    };

    await page.evaluate(
      ({ user, token, jwtUserData }) => {
        sessionStorage.setItem('currentUser', JSON.stringify(user));
        sessionStorage.setItem('jwt_token', token);
        if (jwtUserData) {
          sessionStorage.setItem('jwt_user', JSON.stringify(jwtUserData));
        }
        localStorage.setItem('lastUser', JSON.stringify(user));
        localStorage.setItem('autoLogin', 'true');
      },
      { user: storedProfile, token: jwtToken, jwtUserData: body.user ?? null }
    );
    await page.goto('/games');
  }

  await page.waitForURL(/\/games/, { timeout: 15_000 });

  const persistedToken = await page.evaluate(() => sessionStorage.getItem('jwt_token'));
  if (!hasJwtFormat(persistedToken)) {
    throw new Error(`forceLoginWithProfile persisted a non-JWT token for ${username}`);
  }
}

export async function cleanupE2eGames(
  request: APIRequestContext,
  username: string,
  prefix = DEFAULT_E2E_GAME_PREFIX
): Promise<void> {
  const headers = { 'X-Test-User': username };
  const encodedUsername = encodeURIComponent(username);
  const response = await request.get(
    `${DEFAULT_BACKEND_URL}/api/games?user=${encodedUsername}`,
    { headers }
  );

  if (!response.ok()) {
    throw new Error(
      `Unable to list games for ${username}: ${response.status()} ${response.statusText()}`
    );
  }

  const games = (await response.json()) as GameApiDto[];
  const e2eGames = games.filter(game => game.name?.startsWith(prefix));

  for (const game of e2eGames) {
    const deleteResponse = await request.delete(
      `${DEFAULT_BACKEND_URL}/api/games/${game.id}?user=${encodedUsername}`,
      { headers }
    );

    if (deleteResponse.ok() || deleteResponse.status() === 404) {
      continue;
    }

    const leaveResponse = await request.post(
      `${DEFAULT_BACKEND_URL}/api/games/${game.id}/leave?user=${encodedUsername}`,
      { headers }
    );

    if (leaveResponse.ok() || leaveResponse.status() === 404) {
      continue;
    }

    try {
      softDeleteLocalGameById(game.id);
      continue;
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      console.warn(
        `E2E cleanup skipped for ${game.id} (${username}): delete=${deleteResponse.status()} leave=${leaveResponse.status()} softDelete=${errorMessage}`
      );
    }
  }
}

export async function waitForInvitationCodePersistence(
  request: APIRequestContext,
  username: string,
  gameId: string,
  invitationCode: string
): Promise<void> {
  const headers = { 'X-Test-User': username };
  const encodedUsername = encodeURIComponent(username);

  await expect
    .poll(
      async () => {
        const response = await request.get(
          `${DEFAULT_BACKEND_URL}/api/games/${gameId}?user=${encodedUsername}`,
          { headers }
        );
        if (!response.ok()) {
          return '';
        }

        const game = (await response.json()) as GameApiDto;
        return game.invitationCode ?? '';
      },
      { timeout: 10_000 }
    )
    .toBe(invitationCode);
}

export async function loginAsAdmin(page: Page): Promise<boolean> {
  await waitForLoginPage(page);

  const adminBtn = page
    .locator(
      'fieldset.user-selection-section button:has-text("admin"), button.user-profile-btn:has-text("admin")'
    )
    .first();
  const adminVisible = await adminBtn
    .isVisible({ timeout: 3_000 })
    .catch(() => false);

  if (adminVisible) {
    await adminBtn.click();
    await page.waitForURL(/\/games/, { timeout: 15_000 });
    return true;
  }

  const adminIdentifier = process.env['E2E_ADMIN_USER'];
  if (!adminIdentifier) {
    return false;
  }

  const showAltBtn = page.locator('button.show-alternative').first();
  if (await showAltBtn.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await showAltBtn.click();
  }

  const identifierInput = page
    .locator('input[formcontrolname="identifier"], input[formControlName="identifier"]')
    .first();
  await identifierInput.waitFor({ state: 'visible', timeout: 5_000 });
  await identifierInput.fill(adminIdentifier);

  const submitBtn = page.locator('button[type="submit"]').first();
  await submitBtn.click();
  await page.waitForURL(/\/games/, { timeout: 15_000 });
  return true;
}

export async function openCreateGamePage(page: Page): Promise<void> {
  await page.goto('/games/create');
  await expect(page.locator('.ultra-fast-create')).toBeVisible({
    timeout: 10_000,
  });
}

export async function createQuickGame(
  page: Page,
  gameName: string
): Promise<string> {
  await openCreateGamePage(page);

  const nameInput = page
    .locator(
      'input[formcontrolname="name"], input[formControlName="name"], .game-name-mega input'
    )
    .first();
  await nameInput.waitFor({ state: 'visible', timeout: 8_000 });
  await nameInput.fill(gameName);

  const submitBtn = page.locator('button.mega-create-btn').first();
  await expect(submitBtn).toBeEnabled({ timeout: 5_000 });
  await submitBtn.click();

  try {
    await page.waitForURL(/\/games\/[0-9a-f-]{36}/, { timeout: 20_000 });
  } catch (error) {
    const bodyText = (await page.locator('body').textContent()) ?? '';
    const limitMessage = bodyText.match(/User cannot have more than \d+ active games\. Current: \d+/);
    if (limitMessage) {
      throw new Error(`Game creation failed: ${limitMessage[0]}`);
    }
    throw error;
  }

  const match = page.url().match(/\/games\/([0-9a-f-]{36})/);
  if (!match) {
    throw new Error(`Could not extract game ID from URL: ${page.url()}`);
  }

  return match[1];
}

export async function generateInvitationCode(
  page: Page,
  request?: APIRequestContext,
  username?: string,
  gameId?: string
): Promise<string> {
  await page
    .locator('.invitation-code-item, .code-value, button.generate-code-btn')
    .first()
    .waitFor({ state: 'visible', timeout: 10_000 });

  const codeEl = page.locator('span.code-value').first();
  const previousCode = ((await codeEl.textContent().catch(() => '')) ?? '').trim();
  if (previousCode && previousCode !== '-') {
    return previousCode;
  }

  if (request && username && gameId) {
    try {
      await expect
        .poll(
          async () => getPersistedInvitationCode(request, username, gameId),
          { timeout: 5_000 }
        )
        .not.toBe('');
    } catch {
      // Fall back to the UI action when the code is not yet persisted.
    }
    const codeFromBackend = await getPersistedInvitationCode(request, username, gameId);
    if (codeFromBackend) {
      await syncInvitationCodeUi(page, codeEl, codeFromBackend);
      return codeFromBackend;
    }
  }

  const generateBtn = page.locator('button.generate-code-btn').first();
  const regenerateBtn = page.locator('button.regenerate-btn').first();

  if (await generateBtn.isVisible({ timeout: 10_000 }).catch(() => false)) {
    await generateBtn.click();
  } else if (
    await regenerateBtn.isVisible({ timeout: 10_000 }).catch(() => false)
  ) {
    await regenerateBtn.click();
  }

  const confirmBtn = page
    .locator('mat-dialog-actions button.confirm-btn, mat-dialog-actions button[color="primary"]')
    .first();
  if (await confirmBtn.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await confirmBtn.click();
  }

  if (request && username && gameId) {
    await expect
      .poll(
        async () => getPersistedInvitationCode(request, username, gameId),
        { timeout: 15_000 }
      )
      .not.toBe('');
    const codeFromBackend = await getPersistedInvitationCode(request, username, gameId);
    if (codeFromBackend) {
      await syncInvitationCodeUi(page, codeEl, codeFromBackend);
      return codeFromBackend;
    }
  }

  await expect
    .poll(
      async () => {
        const currentCode = ((await codeEl.textContent()) ?? '').trim();
        if (!currentCode || currentCode === '-') {
          return '';
        }
        return currentCode;
      },
      { timeout: 15_000 }
    )
    .not.toBe('');

  const resolvedCode = ((await codeEl.textContent()) ?? '').trim();
  const finalCode = resolvedCode || previousCode;
  if (!finalCode || finalCode === '-') {
    throw new Error('Invitation code not found after generation');
  }

  return finalCode;
}

async function syncInvitationCodeUi(
  page: Page,
  codeLocator: Locator,
  invitationCode: string
): Promise<void> {
  if (await codeLocator.isVisible({ timeout: 1_000 }).catch(() => false)) {
    try {
      await expect(codeLocator).toContainText(invitationCode, { timeout: 5_000 });
      return;
    } catch {
      await page.reload();
      await page
        .locator('.invitation-code-item, .code-value, button.generate-code-btn')
        .first()
        .waitFor({ state: 'visible', timeout: 10_000 });
      await expect(codeLocator).toContainText(invitationCode, { timeout: 5_000 });
    }
  }
}

function hasJwtFormat(token: string | null): token is string {
  return Boolean(token && token.split('.').length === 3);
}

export async function joinWithInvitationCode(
  page: Page,
  invitationCode: string
): Promise<void> {
  await page.goto('/games/join');
  await page.locator('.join-game-card').first().waitFor({
    state: 'visible',
    timeout: 10_000,
  });

  const codeInput = page.locator('input[name="invitationCode"]').first();
  await codeInput.fill(invitationCode);
  await expect(codeInput).toHaveValue(invitationCode);

  const joinButton = page.locator('button.join-button[type="submit"]').first();
  await expect(joinButton).toBeEnabled({ timeout: 5_000 });
  await codeInput.press('Enter');

  try {
    await page.waitForURL(/\/games\/[0-9a-f-]{36}(\/dashboard)?/, { timeout: 20_000 });
    return;
  } catch {
    const errorFeedback = page.locator('.join-feedback--error, [role="alert"]').first();
    if (await errorFeedback.isVisible({ timeout: 1_000 }).catch(() => false)) {
      const errorText = (await errorFeedback.textContent())?.trim() ?? 'unknown join error';
      throw new Error(`Join with invitation code failed: ${errorText}`);
    }

    await expect(joinButton).toBeEnabled({ timeout: 10_000 });
    throw new Error(
      `Join with invitation code did not navigate and rendered no explicit error (url=${page.url()})`
    );
  }
}
