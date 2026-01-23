/**
 * Polyfills for Node.js globals used by some libraries in the browser
 * This fixes "global is not defined" errors from libraries that expect Node.js environment
 */
(window as any).global = window;
(window as any).process = (window as any).process || { env: {} };

import 'zone.js';  // Included with Angular CLI. 