import firebaseRulesPlugin from '@firebase/eslint-plugin-security-rules';

export default [
  {
    ignores: ['dist/**/*', 'node_modules/**/*', 'android-native/**/*', 'media-signer-worker/**/*'],
  },
  firebaseRulesPlugin.configs['flat/recommended'],
];
