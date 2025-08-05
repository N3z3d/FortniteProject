/**
 * Test d'accessibilité avec Lighthouse
 * Ce script lance des audits d'accessibilité sur les pages principales de l'application
 */

const lighthouse = require('lighthouse');
const chromeLauncher = require('chrome-launcher');
const fs = require('fs');
const path = require('path');

// Configuration des pages à tester
const pagesToTest = [
  {
    url: 'http://localhost:4200',
    name: 'home'
  },
  {
    url: 'http://localhost:4200/dashboard',
    name: 'dashboard'
  },
  {
    url: 'http://localhost:4200/leaderboard',
    name: 'leaderboard'
  },
  {
    url: 'http://localhost:4200/games/create',
    name: 'create-game'
  },
  {
    url: 'http://localhost:4200/draft',
    name: 'draft'
  }
];

// Configuration Lighthouse pour l'accessibilité
const lighthouseConfig = {
  extends: 'lighthouse:default',
  categories: {
    accessibility: {
      title: 'Accessibility',
      description: 'These checks highlight opportunities to improve the accessibility of your web app.',
      auditRefs: [
        {id: 'accesskeys', weight: 3},
        {id: 'aria-allowed-attr', weight: 10},
        {id: 'aria-hidden-body', weight: 10},
        {id: 'aria-hidden-focus', weight: 3},
        {id: 'aria-input-field-name', weight: 3},
        {id: 'aria-required-attr', weight: 10},
        {id: 'aria-required-children', weight: 10},
        {id: 'aria-required-parent', weight: 10},
        {id: 'aria-roles', weight: 10},
        {id: 'aria-toggle-field-name', weight: 3},
        {id: 'aria-valid-attr-value', weight: 10},
        {id: 'aria-valid-attr', weight: 10},
        {id: 'button-name', weight: 10},
        {id: 'bypass', weight: 3},
        {id: 'color-contrast', weight: 3},
        {id: 'definition-list', weight: 3},
        {id: 'dlitem', weight: 3},
        {id: 'document-title', weight: 3},
        {id: 'duplicate-id-aria', weight: 10},
        {id: 'duplicate-id-active', weight: 10},
        {id: 'form-field-multiple-labels', weight: 2},
        {id: 'frame-title', weight: 3},
        {id: 'heading-order', weight: 2},
        {id: 'html-has-lang', weight: 3},
        {id: 'html-lang-valid', weight: 3},
        {id: 'image-alt', weight: 10},
        {id: 'input-image-alt', weight: 10},
        {id: 'label', weight: 10},
        {id: 'landmark-one-main', weight: 3},
        {id: 'link-name', weight: 3},
        {id: 'list', weight: 3},
        {id: 'listitem', weight: 3},
        {id: 'meta-refresh', weight: 10},
        {id: 'meta-viewport', weight: 10},
        {id: 'object-alt', weight: 3},
        {id: 'tabindex', weight: 3},
        {id: 'table-fake-caption', weight: 3},
        {id: 'td-headers-attr', weight: 3},
        {id: 'th-has-data-cells', weight: 3},
        {id: 'valid-lang', weight: 3},
        {id: 'video-caption', weight: 10}
      ]
    }
  },
  settings: {
    onlyCategories: ['accessibility'],
    throttlingMethod: 'simulate',
    throttling: {
      rttMs: 150,
      throughputKbps: 1638.4,
      cpuSlowdownMultiplier: 4
    }
  }
};

async function runAccessibilityAudit() {
  console.log('🚀 Démarrage des audits d\'accessibilité Lighthouse...\n');
  
  // Lancement de Chrome
  const chrome = await chromeLauncher.launch({chromeFlags: ['--headless']});
  
  const results = [];
  
  try {
    for (const page of pagesToTest) {
      console.log(`📊 Test de la page: ${page.name} (${page.url})`);
      
      try {
        const runnerResult = await lighthouse(page.url, {
          port: chrome.port,
          onlyCategories: ['accessibility']
        }, lighthouseConfig);
        
        const accessibility = runnerResult.lhr.categories.accessibility;
        const score = Math.round(accessibility.score * 100);
        
        results.push({
          page: page.name,
          url: page.url,
          score: score,
          audits: runnerResult.lhr.audits
        });
        
        console.log(`   ✅ Score d'accessibilité: ${score}/100\n`);
        
      } catch (error) {
        console.log(`   ❌ Erreur lors du test: ${error.message}\n`);
        results.push({
          page: page.name,
          url: page.url,
          score: 0,
          error: error.message
        });
      }
    }
  } finally {
    await chrome.kill();
  }
  
  // Génération du rapport
  generateReport(results);
  
  return results;
}

function generateReport(results) {
  console.log('📋 RAPPORT D\'ACCESSIBILITÉ WCAG 2.1 AA\n');
  console.log('=======================================\n');
  
  let totalScore = 0;
  let validResults = 0;
  
  results.forEach(result => {
    if (result.error) {
      console.log(`❌ ${result.page.toUpperCase()}: ERREUR`);
      console.log(`   URL: ${result.url}`);
      console.log(`   Erreur: ${result.error}\n`);
    } else {
      console.log(`${result.score >= 90 ? '✅' : result.score >= 70 ? '⚠️' : '❌'} ${result.page.toUpperCase()}: ${result.score}/100`);
      console.log(`   URL: ${result.url}`);
      
      // Affichage des problèmes critiques
      const failedAudits = Object.entries(result.audits)
        .filter(([key, audit]) => audit.score !== null && audit.score < 1)
        .sort((a, b) => (a[1].score || 0) - (b[1].score || 0));
      
      if (failedAudits.length > 0) {
        console.log('   🔍 Problèmes détectés:');
        failedAudits.slice(0, 5).forEach(([key, audit]) => {
          console.log(`      - ${audit.title}: ${audit.description || 'Échec'}`);
        });
        if (failedAudits.length > 5) {
          console.log(`      ... et ${failedAudits.length - 5} autres problèmes`);
        }
      }
      console.log('');
      
      totalScore += result.score;
      validResults++;
    }
  });
  
  if (validResults > 0) {
    const averageScore = Math.round(totalScore / validResults);
    console.log(`🎯 SCORE MOYEN: ${averageScore}/100`);
    
    if (averageScore >= 90) {
      console.log('🏆 EXCELLENT! Votre application respecte les standards WCAG 2.1 AA');
    } else if (averageScore >= 70) {
      console.log('👍 BON! Quelques améliorations mineures nécessaires');
    } else {
      console.log('⚠️  ATTENTION! Des améliorations importantes sont nécessaires');
    }
  }
  
  console.log('\n=======================================');
  
  // Sauvegarde du rapport détaillé
  const reportPath = path.join(__dirname, 'accessibility-report.json');
  fs.writeFileSync(reportPath, JSON.stringify(results, null, 2));
  console.log(`📄 Rapport détaillé sauvegardé: ${reportPath}`);
  
  // Recommandations générales
  console.log('\n🔧 RECOMMANDATIONS GÉNÉRALES:');
  console.log('- Vérifiez que tous les boutons ont des labels descriptifs');
  console.log('- Assurez-vous que la hiérarchie des titres est logique (h1 > h2 > h3)');
  console.log('- Testez la navigation au clavier sur tous les éléments interactifs');
  console.log('- Vérifiez les contrastes de couleur (minimum 4.5:1 pour le texte normal)');
  console.log('- Utilisez des régions ARIA appropriées (main, navigation, complementary)');
  console.log('- Testez avec des lecteurs d\'écran (NVDA, JAWS, VoiceOver)');
}

// Fonction pour les recommandations spécifiques
function getSpecificRecommendations(results) {
  const recommendations = [];
  
  results.forEach(result => {
    if (!result.error && result.audits) {
      // Analyse des audits échoués pour des recommandations spécifiques
      Object.entries(result.audits).forEach(([key, audit]) => {
        if (audit.score !== null && audit.score < 1) {
          switch (key) {
            case 'button-name':
              recommendations.push(`${result.page}: Ajouter des labels descriptifs aux boutons`);
              break;
            case 'color-contrast':
              recommendations.push(`${result.page}: Améliorer le contraste des couleurs`);
              break;
            case 'heading-order':
              recommendations.push(`${result.page}: Corriger la hiérarchie des titres`);
              break;
            case 'bypass':
              recommendations.push(`${result.page}: Ajouter des liens de navigation rapide (skip links)`);
              break;
            case 'aria-valid-attr':
              recommendations.push(`${result.page}: Corriger les attributs ARIA invalides`);
              break;
          }
        }
      });
    }
  });
  
  return [...new Set(recommendations)]; // Supprime les doublons
}

// Exécution si le script est appelé directement
if (require.main === module) {
  runAccessibilityAudit()
    .then(results => {
      const recommendations = getSpecificRecommendations(results);
      
      if (recommendations.length > 0) {
        console.log('\n🎯 RECOMMANDATIONS SPÉCIFIQUES:');
        recommendations.forEach(rec => console.log(`- ${rec}`));
      }
      
      console.log('\n✨ Audit terminé!');
      process.exit(0);
    })
    .catch(error => {
      console.error('❌ Erreur lors de l\'audit:', error);
      process.exit(1);
    });
}

module.exports = { runAccessibilityAudit, generateReport };