import { defineConfig } from 'vitest/config';

const isEmulatorRunning = Boolean(process.env.FIRESTORE_EMULATOR_HOST);

export default defineConfig({
  test: {
    include: isEmulatorRunning
      ? ['src/**/*.test.{ts,tsx}', 'firestore.rules.test.ts']
      : ['src/**/*.test.{ts,tsx}'],
    exclude: ['**/node_modules/**', '**/dist/**', '**/android-native/**'],
  },
});
