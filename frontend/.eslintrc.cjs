module.exports = {
  root: true,
  env: { browser: true, es2022: true },
  extends: [
    'eslint:recommended',
    'plugin:react/recommended',
    'plugin:react/jsx-runtime',
    'plugin:react-hooks/recommended',
  ],
  parserOptions: { ecmaVersion: 'latest', sourceType: 'module' },
  settings: { react: { version: '18.3' } },
  plugins: ['react-refresh'],
  ignorePatterns: ['dist', 'node_modules'],
  rules: {
    // The app deliberately passes loose props around; documenting every one of
    // them with PropTypes would add noise without catching anything the API
    // contract in api/endpoints.js does not already pin down.
    'react/prop-types': 'off',
    'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
    'no-unused-vars': ['error', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],
    'no-console': ['warn', { allow: ['warn', 'error'] }],
  },
  overrides: [
    {
      // A context module pairs a provider with the hook that reads it. Splitting
      // them across two files to satisfy fast refresh would make both harder to
      // follow for no runtime benefit.
      files: ['src/context/*.jsx', 'src/components/Field.jsx', 'src/components/Loader.jsx',
        'src/components/Rating.jsx', 'src/components/SearchFilters.jsx'],
      rules: { 'react-refresh/only-export-components': 'off' },
    },
  ],
};
