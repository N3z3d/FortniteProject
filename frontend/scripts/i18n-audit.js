#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

const PROJECT_ROOT = path.resolve(__dirname, '..');
const I18N_DIR = path.join(PROJECT_ROOT, 'src', 'assets', 'i18n');
const SOURCE_DIR = path.join(PROJECT_ROOT, 'src', 'app');
const LANGUAGES = ['en', 'fr', 'es', 'pt'];

const suspiciousMarkers = [
  '\u00C3\u00A9',
  '\u00C3\u00A8',
  '\u00C3\u00AA',
  '\u00C3\u00AB',
  '\u00C3\u00A0',
  '\u00C3\u00A2',
  '\u00C3\u00A7',
  '\u00C3\u00B9',
  '\u00C3\u00BB',
  '\u00C3\u00B4',
  '\u00C3\u00A1',
  '\u00C3\u00AD',
  '\u00C3\u00B3',
  '\u00C3\u00BA',
  '\u00C3\u00B1',
  '\u00C3\u00BC',
  '\u00C2\u00A9',
  '\uFFFD'
];

const keyRegexes = [
  /\bt\.t\(\s*(['"`])([^'"`]+)\1/g,
  /\bt\.translate\(\s*(['"`])([^'"`]+)\1/g,
  /\btranslate\(\s*(['"`])([^'"`]+)\1/g
];

const flattenTranslations = (node, prefix = '', out = {}) => {
  for (const [key, value] of Object.entries(node)) {
    const fullKey = prefix ? `${prefix}.${key}` : key;
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      flattenTranslations(value, fullKey, out);
      continue;
    }
    out[fullKey] = String(value);
  }
  return out;
};

const collectFiles = (dir, extensions, out = []) => {
  for (const fileName of fs.readdirSync(dir)) {
    const filePath = path.join(dir, fileName);
    const stats = fs.statSync(filePath);
    if (stats.isDirectory()) {
      collectFiles(filePath, extensions, out);
      continue;
    }

    if (!extensions.some(extension => filePath.endsWith(extension))) {
      continue;
    }

    if (filePath.endsWith('.spec.ts')) {
      continue;
    }

    out.push(filePath);
  }

  return out;
};

const loadTranslations = () => {
  const translationsByLang = {};
  for (const lang of LANGUAGES) {
    const filePath = path.join(I18N_DIR, `${lang}.json`);
    const raw = fs.readFileSync(filePath, 'utf8');
    translationsByLang[lang] = JSON.parse(raw);
  }
  return translationsByLang;
};

const compareLanguageKeys = flattenedByLang => {
  const baseLanguage = 'en';
  const baseKeys = Object.keys(flattenedByLang[baseLanguage]).sort();
  const errors = [];

  for (const lang of LANGUAGES) {
    if (lang === baseLanguage) {
      continue;
    }

    const langKeys = new Set(Object.keys(flattenedByLang[lang]));
    const missing = baseKeys.filter(key => !langKeys.has(key));
    const extra = [...langKeys].filter(key => !baseKeys.includes(key));

    if (missing.length > 0) {
      errors.push(`${lang} missing ${missing.length} keys (sample: ${missing.slice(0, 10).join(', ')})`);
    }

    if (extra.length > 0) {
      errors.push(`${lang} has ${extra.length} extra keys (sample: ${extra.slice(0, 10).join(', ')})`);
    }
  }

  return errors;
};

const findSuspiciousValues = flattenedByLang => {
  const issues = [];
  for (const lang of LANGUAGES) {
    for (const [key, value] of Object.entries(flattenedByLang[lang])) {
      for (const marker of suspiciousMarkers) {
        if (value.includes(marker)) {
          issues.push(`${lang}.${key} -> ${value}`);
          break;
        }
      }
    }
  }
  return issues;
};

const findUsedTranslationKeys = () => {
  const sourceFiles = collectFiles(SOURCE_DIR, ['.ts', '.html']);
  const usedKeys = new Set();

  for (const filePath of sourceFiles) {
    const content = fs.readFileSync(filePath, 'utf8');
    for (const regex of keyRegexes) {
      let match;
      while ((match = regex.exec(content)) !== null) {
        const key = match[2];
        if (!key.includes('.') || key.endsWith('.') || key.includes('${')) {
          continue;
        }
        usedKeys.add(key);
      }
    }
  }

  return usedKeys;
};

const findMissingUsedKeys = (usedKeys, flattenedByLang) => {
  const issues = [];
  for (const lang of LANGUAGES) {
    const keys = new Set(Object.keys(flattenedByLang[lang]));
    const missing = [...usedKeys].filter(key => !keys.has(key));
    if (missing.length > 0) {
      issues.push(`${lang} missing ${missing.length} used keys (sample: ${missing.slice(0, 10).join(', ')})`);
    }
  }
  return issues;
};

const main = () => {
  const translations = loadTranslations();
  const flattenedByLang = {};
  for (const lang of LANGUAGES) {
    flattenedByLang[lang] = flattenTranslations(translations[lang]);
  }

  const keySetIssues = compareLanguageKeys(flattenedByLang);
  const suspiciousValueIssues = findSuspiciousValues(flattenedByLang);
  const usedKeys = findUsedTranslationKeys();
  const missingUsedKeyIssues = findMissingUsedKeys(usedKeys, flattenedByLang);

  const allIssues = [
    ...keySetIssues,
    ...missingUsedKeyIssues,
    ...suspiciousValueIssues.map(issue => `suspicious value: ${issue}`)
  ];

  if (allIssues.length > 0) {
    console.error('i18n audit failed:');
    for (const issue of allIssues) {
      console.error(`- ${issue}`);
    }
    process.exit(1);
  }

  console.log(`i18n audit passed (${usedKeys.size} keys checked).`);
};

main();
