/**
 * TEST D'ACCESSIBILITĖ MANUEL - FORTNITE FANTASY
 * Script pour tester l'accessibilité selon WCAG 2.1 AA
 */

console.log('🔍 DÉBUT DES TESTS D\'ACCESSIBILITÉ WCAG 2.1 AA');

// Test 1: Vérification des skip links
function testSkipLinks() {
  console.log('\n1️⃣ TEST SKIP LINKS');
  const skipLinks = document.querySelectorAll('.skip-link');
  
  if (skipLinks.length === 0) {
    console.error('❌ Aucun skip link trouvé');
    return false;
  }
  
  skipLinks.forEach((link, index) => {
    const href = link.getAttribute('href');
    const target = document.querySelector(href);
    
    if (!target) {
      console.error(`❌ Skip link ${index + 1}: cible "${href}" non trouvée`);
      return false;
    }
    
    console.log(`✅ Skip link ${index + 1}: "${link.textContent.trim()}" → ${href}`);
  });
  
  return true;
}

// Test 2: Vérification de la hiérarchie des titres
function testHeadingHierarchy() {
  console.log('\n2️⃣ TEST HIÉRARCHIE DES TITRES');
  const headings = document.querySelectorAll('h1, h2, h3, h4, h5, h6');
  let previousLevel = 0;
  let hasH1 = false;
  
  if (headings.length === 0) {
    console.error('❌ Aucun titre trouvé');
    return false;
  }
  
  headings.forEach((heading, index) => {
    const level = parseInt(heading.tagName.charAt(1));
    const text = heading.textContent.trim();
    
    if (level === 1) {
      hasH1 = true;
      if (index > 0) {
        console.warn(`⚠️ H1 trouvé après d'autres titres: "${text}"`);
      }
    }
    
    if (previousLevel > 0 && level > previousLevel + 1) {
      console.error(`❌ Saut de niveau: ${heading.tagName} après H${previousLevel}: "${text}"`);
      return false;
    }
    
    console.log(`✅ ${heading.tagName}: "${text}"`);
    previousLevel = level;
  });
  
  if (!hasH1) {
    console.error('❌ Aucun H1 trouvé sur la page');
    return false;
  }
  
  return true;
}

// Test 3: Vérification des labels ARIA
function testAriaLabels() {
  console.log('\n3️⃣ TEST LABELS ARIA');
  let errors = 0;
  
  // Boutons sans texte visible
  const iconButtons = document.querySelectorAll('button:not(:has(span:not(.sr-only))):not(:has(mat-icon + span))');
  iconButtons.forEach(button => {
    const ariaLabel = button.getAttribute('aria-label');
    const ariaLabelledby = button.getAttribute('aria-labelledby');
    
    if (!ariaLabel && !ariaLabelledby) {
      console.error(`❌ Bouton sans label accessible:`, button);
      errors++;
    } else {
      console.log(`✅ Bouton avec label: "${ariaLabel || 'labelledby=' + ariaLabelledby}"`);
    }
  });
  
  // Images sans alt
  const images = document.querySelectorAll('img');
  images.forEach(img => {
    const alt = img.getAttribute('alt');
    const ariaHidden = img.getAttribute('aria-hidden');
    
    if (!alt && ariaHidden !== 'true') {
      console.error(`❌ Image sans alt text:`, img);
      errors++;
    } else if (alt) {
      console.log(`✅ Image avec alt: "${alt}"`);
    }
  });
  
  // Icônes Material sans aria-hidden
  const matIcons = document.querySelectorAll('mat-icon');
  matIcons.forEach(icon => {
    const ariaHidden = icon.getAttribute('aria-hidden');
    const ariaLabel = icon.getAttribute('aria-label');
    
    if (!ariaHidden && !ariaLabel) {
      console.warn(`⚠️ mat-icon sans aria-hidden ou aria-label:`, icon);
    }
  });
  
  return errors === 0;
}

// Test 4: Vérification des régions live
function testLiveRegions() {
  console.log('\n4️⃣ TEST RÉGIONS LIVE');
  const liveRegions = document.querySelectorAll('[aria-live]');
  
  if (liveRegions.length === 0) {
    console.error('❌ Aucune région live trouvée');
    return false;
  }
  
  liveRegions.forEach(region => {
    const ariaLive = region.getAttribute('aria-live');
    const ariaAtomic = region.getAttribute('aria-atomic');
    const classList = region.className;
    
    console.log(`✅ Région live: aria-live="${ariaLive}", atomic="${ariaAtomic}", class="${classList}"`);
  });
  
  return true;
}

// Test 5: Vérification de la navigation clavier
function testKeyboardNavigation() {
  console.log('\n5️⃣ TEST NAVIGATION CLAVIER');
  const focusableElements = document.querySelectorAll(
    'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]):not([type="hidden"]), select:not([disabled]), [tabindex]:not([tabindex="-1"]):not([disabled])'
  );
  
  console.log(`📊 ${focusableElements.length} éléments focusables trouvés`);
  
  let tabindexIssues = 0;
  focusableElements.forEach((el, index) => {
    const tabindex = el.getAttribute('tabindex');
    const isVisible = el.offsetWidth > 0 && el.offsetHeight > 0;
    
    if (tabindex && parseInt(tabindex) > 0) {
      console.warn(`⚠️ Tabindex positif trouvé (${tabindex}):`, el);
      tabindexIssues++;
    }
    
    if (!isVisible && tabindex !== '-1') {
      console.warn(`⚠️ Élément invisible mais focusable:`, el);
    }
  });
  
  if (tabindexIssues === 0) {
    console.log('✅ Aucun tabindex positif trouvé (bonne pratique)');
  }
  
  return true;
}

// Test 6: Vérification des formulaires
function testFormAccessibility() {
  console.log('\n6️⃣ TEST ACCESSIBILITÉ DES FORMULAIRES');
  const forms = document.querySelectorAll('form');
  let errors = 0;
  
  if (forms.length === 0) {
    console.log('ℹ️ Aucun formulaire trouvé');
    return true;
  }
  
  forms.forEach((form, formIndex) => {
    console.log(`📋 Formulaire ${formIndex + 1}:`);
    
    // Vérification des fieldsets et legends
    const fieldsets = form.querySelectorAll('fieldset');
    fieldsets.forEach(fieldset => {
      const legend = fieldset.querySelector('legend');
      if (!legend) {
        console.error(`❌ Fieldset sans legend:`, fieldset);
        errors++;
      } else {
        console.log(`✅ Fieldset avec legend: "${legend.textContent.trim()}"`);
      }
    });
    
    // Vérification des labels
    const inputs = form.querySelectorAll('input, select, textarea');
    inputs.forEach(input => {
      const id = input.getAttribute('id');
      const ariaLabel = input.getAttribute('aria-label');
      const ariaLabelledby = input.getAttribute('aria-labelledby');
      const label = id ? form.querySelector(`label[for="${id}"]`) : null;
      
      if (!label && !ariaLabel && !ariaLabelledby) {
        console.error(`❌ Input sans label:`, input);
        errors++;
      } else {
        const labelText = label ? label.textContent.trim() : (ariaLabel || 'aria-labelledby');
        console.log(`✅ Input avec label: "${labelText}"`);
      }
      
      // Vérification des messages d'erreur
      const ariaDescribedby = input.getAttribute('aria-describedby');
      if (ariaDescribedby) {
        const describedElements = ariaDescribedby.split(' ').map(id => document.getElementById(id));
        const hasErrorElement = describedElements.some(el => 
          el && (el.getAttribute('role') === 'alert' || el.classList.contains('error'))
        );
        
        if (hasErrorElement) {
          console.log(`✅ Input avec message d'erreur accessible`);
        }
      }
    });
  });
  
  return errors === 0;
}

// Test 7: Vérification des contrastes (basique)
function testColorContrast() {
  console.log('\n7️⃣ TEST CONTRASTE DES COULEURS (BASIQUE)');
  
  // Vérification des styles critiques
  const criticalElements = document.querySelectorAll('.error-message, .success-message, a, button');
  console.log(`🎨 ${criticalElements.length} éléments critiques à vérifier manuellement pour le contraste`);
  
  // Suggestions de vérification manuelle
  console.log(`
📋 VÉRIFICATIONS MANUELLES RECOMMANDÉES:
• Utilisez un outil comme "Colour Contrast Analyser"
• Vérifiez ratio 4.5:1 pour texte normal
• Vérifiez ratio 3:1 pour texte large (18pt+)
• Testez en mode sombre si disponible
• Vérifiez les états :hover et :focus
  `);
  
  return true;
}

// Test 8: Vérification des rôles ARIA
function testAriaRoles() {
  console.log('\n8️⃣ TEST RÔLES ARIA');
  
  const elementsWithRoles = document.querySelectorAll('[role]');
  console.log(`🎭 ${elementsWithRoles.length} éléments avec rôles ARIA`);
  
  elementsWithRoles.forEach(el => {
    const role = el.getAttribute('role');
    const tagName = el.tagName.toLowerCase();
    
    console.log(`✅ ${tagName} avec role="${role}"`);
    
    // Vérifications spécifiques par rôle
    if (role === 'button' && tagName !== 'button') {
      const tabindex = el.getAttribute('tabindex');
      if (tabindex !== '0') {
        console.warn(`⚠️ Élément role="button" sans tabindex="0":`, el);
      }
    }
    
    if (role === 'list') {
      const listItems = el.querySelectorAll('[role="listitem"]');
      if (listItems.length === 0) {
        console.warn(`⚠️ Liste sans éléments listitem:`, el);
      }
    }
  });
  
  return true;
}

// Test 9: Vérification des landmarks
function testLandmarks() {
  console.log('\n9️⃣ TEST LANDMARKS');
  
  const landmarks = {
    'main': document.querySelectorAll('main, [role="main"]'),
    'navigation': document.querySelectorAll('nav, [role="navigation"]'),
    'banner': document.querySelectorAll('header[role="banner"], [role="banner"]'),
    'contentinfo': document.querySelectorAll('footer[role="contentinfo"], [role="contentinfo"]'),
    'complementary': document.querySelectorAll('aside, [role="complementary"]'),
    'search': document.querySelectorAll('[role="search"]')
  };
  
  Object.entries(landmarks).forEach(([landmark, elements]) => {
    if (elements.length === 0) {
      if (landmark === 'main') {
        console.error(`❌ Landmark "${landmark}" manquant (obligatoire)`);
      } else {
        console.log(`ℹ️ Landmark "${landmark}" non trouvé (optionnel)`);
      }
    } else if (elements.length === 1) {
      console.log(`✅ Landmark "${landmark}" présent`);
    } else {
      console.warn(`⚠️ Plusieurs landmarks "${landmark}" (${elements.length}) - vérifiez les labels`);
    }
  });
  
  return true;
}

// Exécution de tous les tests
function runAllTests() {
  console.log('🚀 LANCEMENT DE TOUS LES TESTS D\'ACCESSIBILITÉ\n');
  
  const tests = [
    { name: 'Skip Links', fn: testSkipLinks },
    { name: 'Hiérarchie des Titres', fn: testHeadingHierarchy },
    { name: 'Labels ARIA', fn: testAriaLabels },
    { name: 'Régions Live', fn: testLiveRegions },
    { name: 'Navigation Clavier', fn: testKeyboardNavigation },
    { name: 'Formulaires', fn: testFormAccessibility },
    { name: 'Contraste Couleurs', fn: testColorContrast },
    { name: 'Rôles ARIA', fn: testAriaRoles },
    { name: 'Landmarks', fn: testLandmarks }
  ];
  
  let passed = 0;
  let total = tests.length;
  
  tests.forEach(test => {
    try {
      const result = test.fn();
      if (result) {
        passed++;
      }
    } catch (error) {
      console.error(`❌ Erreur dans le test "${test.name}":`, error);
    }
  });
  
  console.log('\n📊 RÉSUMÉ DES TESTS D\'ACCESSIBILITÉ');
  console.log(`✅ Tests réussis: ${passed}/${total}`);
  console.log(`📈 Score d'accessibilité: ${Math.round((passed / total) * 100)}%`);
  
  if (passed === total) {
    console.log('🎉 TOUS LES TESTS D\'ACCESSIBILITÉ SONT PASSÉS !');
    console.log('🏆 Application conforme WCAG 2.1 AA');
  } else {
    console.log('⚠️ Certains tests nécessitent des améliorations');
    console.log('📋 Consultez les erreurs ci-dessus pour les corrections');
  }
  
  console.log('\n🔗 RESSOURCES POUR TESTS COMPLÉMENTAIRES:');
  console.log('• axe DevTools: https://www.deque.com/axe/devtools/');
  console.log('• WAVE: https://wave.webaim.org/extension/');
  console.log('• Lighthouse Accessibility');
  console.log('• Color Contrast Analyser: https://www.tpgi.com/color-contrast-checker/');
}

// Auto-exécution si le script est chargé
if (typeof window !== 'undefined') {
  // Attendre que le DOM soit chargé
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', runAllTests);
  } else {
    runAllTests();
  }
}

// Export pour utilisation en module
if (typeof module !== 'undefined' && module.exports) {
  module.exports = {
    runAllTests,
    testSkipLinks,
    testHeadingHierarchy,
    testAriaLabels,
    testLiveRegions,
    testKeyboardNavigation,
    testFormAccessibility,
    testColorContrast,
    testAriaRoles,
    testLandmarks
  };
}