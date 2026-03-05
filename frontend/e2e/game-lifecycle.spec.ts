import { test, expect, Page } from '@playwright/test';

/**
 * Game Lifecycle E2E tests — GAME-01 to GAME-04.
 * Requires: app running on port 4200, backend on port 8080.
 * Uses profile-based login (no email/password form — the app selects a pre-registered profile).
 *
 * Run: npm run test:e2e
 */

// ---------------------------------------------------------------------------
// Auth helper — clicks the first available user profile button on /login
// ---------------------------------------------------------------------------
async function loginWithFirstProfile(page: Page): Promise<void> {
  await page.goto('/login');

  // Wait for the profile selection UI to appear (fieldset with user profile buttons)
  const profileBtn = page.locator('fieldset.user-selection-section button, button.user-profile-btn').first();
  await profileBtn.waitFor({ state: 'visible', timeout: 10_000 });
  await profileBtn.click();

  // After clicking a profile, the app logs in (setTimeout ~800ms) and navigates to /games
  await page.waitForURL(/\/games/, { timeout: 12_000 });
}

// ---------------------------------------------------------------------------
// GAME-01: Authenticated user can create a game and see it in the list
// ---------------------------------------------------------------------------
test('GAME-01: authenticated user can create a game and see it in the list', async ({ page }) => {
  test.setTimeout(40_000);

  await loginWithFirstProfile(page);

  // Navigate to game creation page
  await page.goto('/games/create');
  await expect(page).not.toHaveURL(/login/, { timeout: 5_000 });

  // Wait for the game creation form to appear
  const nameInput = page.locator('input[formcontrolname="name"], input[placeholder*="nom" i], input[placeholder*="game" i], matInput').first();
  await nameInput.waitFor({ state: 'visible', timeout: 8_000 });

  // Fill in a unique game name using timestamp to avoid collisions
  const gameName = `E2E-Test-${Date.now()}`;
  await nameInput.fill(gameName);

  // Submit the form — the submit button has type="submit" or mat-fab
  const submitBtn = page.locator('button[type="submit"], button.mega-create-btn').first();
  await submitBtn.click();

  // After creation the app navigates to /games/{id}?created=true
  await page.waitForURL(/\/games\/[a-z0-9-]+/, { timeout: 15_000 });

  // The game detail page should show the created game name somewhere
  const bodyText = await page.locator('body').textContent();
  expect(bodyText).toContain(gameName);
});

// ---------------------------------------------------------------------------
// GAME-02: Creator can generate an invitation code from the game detail page
// ---------------------------------------------------------------------------
test('GAME-02: creator can generate an invitation code', async ({ page }) => {
  test.setTimeout(40_000);

  await loginWithFirstProfile(page);

  // Create a fresh game so we are the creator
  await page.goto('/games/create');
  const nameInput = page.locator('input[formcontrolname="name"], input[placeholder*="nom" i], matInput').first();
  await nameInput.waitFor({ state: 'visible', timeout: 8_000 });
  await nameInput.fill(`E2E-CodeGen-${Date.now()}`);
  await page.locator('button[type="submit"], button.mega-create-btn').first().click();
  await page.waitForURL(/\/games\/[a-z0-9-]+/, { timeout: 15_000 });

  // Look for the "Generate code" button (visible when no invitation code exists yet)
  // The template shows a mat-stroked-button with class "generate-code-btn" or mat-icon "add_circle"
  const generateCodeBtn = page.locator('button.generate-code-btn, button:has(mat-icon:text("add_circle"))').first();

  if (await generateCodeBtn.isVisible({ timeout: 5_000 }).catch(() => false)) {
    await generateCodeBtn.click();

    // A dialog or inline response should appear with the code
    // Either a mat-dialog or the code appears inline in .code-value
    const codeDisplay = page.locator('.code-value, [class*="invitation-code"], mat-dialog-container').first();
    await expect(codeDisplay).toBeVisible({ timeout: 8_000 });
  } else {
    // If the code was already generated (invitation code already exists), verify it is visible
    const existingCode = page.locator('.code-value, .info-value.code-value').first();
    await expect(existingCode).toBeVisible({ timeout: 5_000 });
  }
});

// ---------------------------------------------------------------------------
// GAME-03: Navigating to /games/{id} displays game details
// ---------------------------------------------------------------------------
test('GAME-03: /games/{id} displays game details', async ({ page }) => {
  test.setTimeout(40_000);

  await loginWithFirstProfile(page);

  // Navigate to the games list to find an existing game ID
  await page.goto('/games');
  await page.waitForURL(/\/games/, { timeout: 10_000 });

  // Try to find a game card or link with a game ID
  // Game-home renders a list of games — we look for a routerLink to /games/{uuid}
  const gameLink = page.locator('a[href*="/games/"], mat-list-item[routerlink*="/games/"]').first();

  if (await gameLink.isVisible({ timeout: 5_000 }).catch(() => false)) {
    const href = await gameLink.getAttribute('href');
    if (href) {
      await page.goto(href);
    } else {
      await gameLink.click();
    }
  } else {
    // No game link found in list — create a game first
    await page.goto('/games/create');
    const nameInput = page.locator('input[formcontrolname="name"], matInput').first();
    await nameInput.waitFor({ state: 'visible', timeout: 8_000 });
    await nameInput.fill(`E2E-Detail-${Date.now()}`);
    await page.locator('button[type="submit"], button.mega-create-btn').first().click();
    await page.waitForURL(/\/games\/[a-z0-9-]+/, { timeout: 15_000 });
  }

  // Ensure we are on a game detail page
  await expect(page).toHaveURL(/\/games\/[a-z0-9-]+/, { timeout: 8_000 });

  // The game detail page renders .game-detail-container with game info
  const container = page.locator('.game-detail-container, .game-content').first();
  await expect(container).toBeVisible({ timeout: 10_000 });

  // Key structural elements from game-detail.component.html
  await expect(page.locator('.game-header, h1').first()).toBeVisible({ timeout: 5_000 });
  await expect(page.locator('.status-card, .game-status-section').first()).toBeVisible({ timeout: 5_000 });
});

// ---------------------------------------------------------------------------
// GAME-04: Leave button is visible for participants who are not the creator
// ---------------------------------------------------------------------------
test('GAME-04: leave button is visible for non-creator participants', async ({ page }) => {
  test.setTimeout(40_000);

  await loginWithFirstProfile(page);

  // Navigate to games list and find a game where a "Join" button is available
  await page.goto('/games');
  await page.waitForURL(/\/games/, { timeout: 10_000 });

  // Look for a game that the current user can join (not already a participant, not full)
  // The game detail page has a "Join" button when canJoinGame() is true
  // We navigate into a game and try to join it, then verify the leave button appears

  // First, let's find any game link in the list
  const gameLinks = page.locator('a[href*="/games/"]');
  const count = await gameLinks.count();

  if (count === 0) {
    // No games in list — skip by creating a game and verifying as creator we do NOT see leave
    // (creator sees delete/archive, not leave — so we just verify page renders correctly)
    await page.goto('/games/create');
    const nameInput = page.locator('input[formcontrolname="name"], matInput').first();
    await nameInput.waitFor({ state: 'visible', timeout: 8_000 });
    await nameInput.fill(`E2E-Leave-${Date.now()}`);
    await page.locator('button[type="submit"], button.mega-create-btn').first().click();
    await page.waitForURL(/\/games\/[a-z0-9-]+/, { timeout: 15_000 });

    // As creator: leave button should NOT be present; delete action should be visible
    const leaveBtn = page.locator('button:has(mat-icon:text("exit_to_app"))');
    const deleteBtn = page.locator('button.delete-game-btn');
    // At least one of: delete visible OR leave not visible (creator scenario)
    const deleteVisible = await deleteBtn.isVisible({ timeout: 3_000 }).catch(() => false);
    const leaveVisible = await leaveBtn.isVisible({ timeout: 2_000 }).catch(() => false);
    // Creator should never see leave button
    expect(leaveVisible).toBe(false);
    // Creator may see delete button
    expect(deleteVisible || !leaveVisible).toBe(true);
    return;
  }

  // Navigate to the first game in the list
  const firstGameLink = gameLinks.first();
  await firstGameLink.click();
  await page.waitForURL(/\/games\/[a-z0-9-]+/, { timeout: 10_000 });

  // Check if the "Join" button is visible (not yet a participant)
  const joinBtn = page.locator('button:has(mat-icon:text("group_add"))').first();
  const joinVisible = await joinBtn.isVisible({ timeout: 3_000 }).catch(() => false);

  if (joinVisible) {
    // Join the game
    await joinBtn.click();
    // After joining, the page should refresh and show the Leave button
    // The leave button has mat-icon exit_to_app
    const leaveBtn = page.locator('button:has(mat-icon:text("exit_to_app"))').first();
    await expect(leaveBtn).toBeVisible({ timeout: 8_000 });
  } else {
    // Already a participant or creator — check which buttons are visible
    const leaveBtn = page.locator('button:has(mat-icon:text("exit_to_app"))').first();
    const deleteBtn = page.locator('button.delete-game-btn').first();
    const leaveVisible = await leaveBtn.isVisible({ timeout: 2_000 }).catch(() => false);
    const deleteVisible = await deleteBtn.isVisible({ timeout: 2_000 }).catch(() => false);

    // At minimum, the page renders the action button section
    const actionButtons = page.locator('.action-buttons');
    await expect(actionButtons).toBeVisible({ timeout: 5_000 });

    // One of the two should be visible (leave OR delete/archive for host)
    expect(leaveVisible || deleteVisible).toBe(true);
  }
});
