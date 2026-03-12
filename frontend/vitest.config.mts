import { defineConfig } from 'vitest/config';
import angular from '@analogjs/vite-plugin-angular';

export default defineConfig({
  plugins: [angular({ tsconfig: './tsconfig.spec.json' })],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: [
      '@analogjs/vite-plugin-angular/setup-vitest',
      'src/vitest-setup.ts',
    ],
    include: ['src/**/*.spec.ts'],
    reporters: ['verbose'],
    pool: 'forks',
    coverage: {
      provider: 'v8',
      reporter: ['text-summary', 'html', 'lcov'],
      include: ['src/app/**/*.ts'],
      exclude: ['src/app/**/*.spec.ts', 'src/app/**/*.module.ts'],
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        includePaths: ['src/app/shared/styles'],
        silenceDeprecations: ['import', 'global-builtin', 'mixed-decls'],
      },
    },
  },
});
