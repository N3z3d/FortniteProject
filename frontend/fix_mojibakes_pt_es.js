const fs = require('fs');
const path = require('path');

const filePath = path.join(__dirname, 'src', 'app', 'core', 'services', 'translation.service.ts');

console.log('Reading file:', filePath);
let content = fs.readFileSync(filePath, 'utf8');

// Count mojibakes before
const beforeCount = (content.match(/Í[£µšŠ‚¢]/g) || []).length;
console.log(`Mojibakes found: ${beforeCount}`);

// Portuguese specific mojibakes
const replacements = [
  // Portuguese ã (most common)
  [/Í£/g, 'ã'],  // não, classificação, informações, etc.

  // Portuguese õ  [/Íµ/g, 'õ'],  // ações, opções, botões, etc.

  // Spanish/Portuguese ú
  [/Íš/g, 'ú'],  // último, menú, etc.

  // Portuguese â
  [/Í¢/g, 'â'],  // âmbito, etc.

  // Portuguese ê
  [/ÍŠ/g, 'ê'],  // você, português, etc.

  // Other patterns
  [/Í‚/g, 'â'],  // alternative encoding
  [/Í(?![£µšŠ‚¢])/g, 'í'],  // standalone Í should be í
];

let totalReplacements = 0;

replacements.forEach(([pattern, replacement]) => {
  const matches = content.match(pattern);
  if (matches) {
    console.log(`Replacing ${matches.length} occurrences of ${pattern} with "${replacement}"`);
    totalReplacements += matches.length;
    content = content.replace(pattern, replacement);
  }
});

// Write back
fs.writeFileSync(filePath, content, 'utf8');

// Count mojibakes after
const afterCount = (content.match(/Í[£µšŠ‚¢]/g) || []).length;

console.log('\n=== Results ===');
console.log(`Total replacements: ${totalReplacements}`);
console.log(`Mojibakes before: ${beforeCount}`);
console.log(`Mojibakes after: ${afterCount}`);
console.log(`Fixed: ${beforeCount - afterCount} (${Math.round((beforeCount - afterCount) / beforeCount * 100)}%)`);
console.log('\nDone! File updated.');
